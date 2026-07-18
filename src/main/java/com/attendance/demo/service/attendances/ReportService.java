package com.attendance.demo.service.attendances;

import com.attendance.demo.dto.filter.AttendanceFilter;
import com.attendance.demo.entity.AttendanceRecord;
import com.attendance.demo.entity.User;
import com.attendance.demo.repository.AttendanceRepository;
import com.attendance.demo.specification.AttendanceSpecification;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private static final String[] HEADERS = {
            "Docente", "Rol", "Fecha", "Entrada", "Salida", "Estado", "Notas", "Foto", "Firma", "Huella"
    };

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Transactional(readOnly = true)
    public byte[] generateExcel(AttendanceFilter filter) throws IOException {
        List<AttendanceRecord> records = attendanceRepository.findAll(AttendanceSpecification.filter(filter));
        Map<String, byte[]> imageCache = new HashMap<>();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Asistencias");
            Drawing<?> drawing = sheet.createDrawingPatriarch();

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (AttendanceRecord r : records) {
                Row row = sheet.createRow(rowNum);
                row.setHeightInPoints(60);
                User user = r.getUser();

                row.createCell(0).setCellValue(user.getFullName());
                row.createCell(1).setCellValue(roleLabel(user.getRole()));
                row.createCell(2).setCellValue(r.getDate().toString());
                row.createCell(3).setCellValue(r.getTimeIn() != null ? r.getTimeIn().toString() : "");
                row.createCell(4).setCellValue(r.getTimeOut() != null ? r.getTimeOut().toString() : "No registrada");
                row.createCell(5).setCellValue(r.getStatus().name());
                row.createCell(6).setCellValue(r.getNotes() != null ? r.getNotes() : "");

                embedExcelImage(workbook, drawing, sheet, imageCache, user.getPhotoUrl(), 7, rowNum);
                embedExcelImage(workbook, drawing, sheet, imageCache, user.getSignatureUrl(), 8, rowNum);
                embedExcelImage(workbook, drawing, sheet, imageCache, user.getFingerprintUrl(), 9, rowNum);

                rowNum++;
            }

            for (int i = 0; i < 7; i++) {
                sheet.autoSizeColumn(i);
            }
            for (int i = 7; i < HEADERS.length; i++) {
                sheet.setColumnWidth(i, 12 * 256);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void embedExcelImage(Workbook workbook, Drawing<?> drawing, Sheet sheet,
                                  Map<String, byte[]> imageCache, String url, int col, int rowNum) {
        byte[] bytes = fetchImageBytes(url, imageCache);
        if (bytes == null) return;
        try {
            int pictureIdx = workbook.addPicture(bytes, detectPoiPictureType(bytes));
            ClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, col, rowNum, col + 1, rowNum + 1);
            drawing.createPicture(anchor, pictureIdx);
        } catch (Exception ignored) {
            // Corrupt/unsupported image bytes — leave the cell blank rather than failing the report.
        }
    }

    @Transactional(readOnly = true)
    public byte[] generatePdf(AttendanceFilter filter) throws DocumentException {
        List<AttendanceRecord> records = attendanceRepository.findAll(AttendanceSpecification.filter(filter));
        Map<String, byte[]> imageCache = new HashMap<>();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 40, 30);
        PdfWriter.getInstance(doc, out);
        doc.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(220, 53, 69));
        Paragraph title = new Paragraph("Reporte de Asistencias", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(6);
        doc.add(title);

        Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(100, 100, 100));
        Paragraph sub = new Paragraph("Generado el: " + LocalDate.now(), subFont);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(18);
        doc.add(sub);

        PdfPTable table = new PdfPTable(HEADERS.length);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.3f, 1f, 1.2f, 1f, 1f, 1f, 1.8f, 1f, 1f, 1f});

        Font hFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        for (String h : HEADERS) {
            PdfPCell cell = new PdfPCell(new Phrase(h, hFont));
            cell.setBackgroundColor(new Color(220, 53, 69));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(7);
            table.addCell(cell);
        }

        Font rFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
        boolean alt = false;
        for (AttendanceRecord r : records) {
            Color bg = alt ? new Color(248, 249, 250) : Color.WHITE;
            alt = !alt;
            User user = r.getUser();

            addCell(table, user.getFullName(), rFont, bg);
            addCell(table, roleLabel(user.getRole()), rFont, bg);
            addCell(table, r.getDate().toString(), rFont, bg);
            addCell(table, r.getTimeIn() != null ? r.getTimeIn().toString() : "-", rFont, bg);
            addCell(table, r.getTimeOut() != null ? r.getTimeOut().toString() : "No registrada", rFont, bg);
            addCell(table, r.getStatus().name(), rFont, bg);
            addCell(table, r.getNotes() != null ? r.getNotes() : "-", rFont, bg);

            addImageCell(table, fetchImageBytes(user.getPhotoUrl(), imageCache), bg);
            addImageCell(table, fetchImageBytes(user.getSignatureUrl(), imageCache), bg);
            addImageCell(table, fetchImageBytes(user.getFingerprintUrl(), imageCache), bg);
        }

        doc.add(table);

        Font footFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, new Color(150, 150, 150));
        Paragraph foot = new Paragraph("Total registros: " + records.size(), footFont);
        foot.setSpacingBefore(10);
        doc.add(foot);

        doc.close();
        return out.toByteArray();
    }

    private void addCell(PdfPTable table, String text, com.lowagie.text.Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addImageCell(PdfPTable table, byte[] bytes, Color bg) {
        PdfPCell cell;
        if (bytes != null) {
            try {
                Image img = Image.getInstance(bytes);
                img.scaleToFit(35, 35);
                cell = new PdfPCell(img, false);
            } catch (Exception e) {
                cell = new PdfPCell();
            }
        } else {
            cell = new PdfPCell();
        }
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private String roleLabel(User.Role role) {
        return role == User.Role.DIRECTOR ? "Director" : "Docente";
    }

    /** Downloads an image once per unique URL per report, caching bytes for reuse across rows. */
    private byte[] fetchImageBytes(String url, Map<String, byte[]> cache) {
        if (url == null || url.isBlank()) return null;
        if (cache.containsKey(url)) return cache.get(url);

        byte[] bytes = null;
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("GET");
            try (InputStream in = conn.getInputStream()) {
                bytes = in.readAllBytes();
            }
        } catch (Exception ignored) {
            bytes = null;
        }
        cache.put(url, bytes);
        return bytes;
    }

    private int detectPoiPictureType(byte[] bytes) {
        if (bytes.length >= 4
                && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return Workbook.PICTURE_TYPE_PNG;
        }
        return Workbook.PICTURE_TYPE_JPEG;
    }
}
