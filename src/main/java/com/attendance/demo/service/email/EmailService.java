package com.attendance.demo.service.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Service
public class EmailService {

    private static final String EMAILJS_API_URL = "https://api.emailjs.com/api/v1.0/email/send";

    private final RestClient restClient = RestClient.create();

    @Value("${app.emailjs.service-id:}")
    private String serviceId;

    @Value("${app.emailjs.template-id:}")
    private String templateId;

    @Value("${app.emailjs.public-key:}")
    private String publicKey;

    @Value("${app.emailjs.private-key:}")
    private String privateKey;

    @Value("${app.frontend-url:}")
    private String frontendUrl;

    public void sendVerificationCode(String to, String code) {
        String subject = "Verifica tu correo - Attendance App";
        String body = """
                <p>Gracias por registrarte en Attendance App.</p>
                <p>Tu código de verificación es:</p>
                <p style="text-align:center;font-size:28px;font-weight:bold;letter-spacing:4px;color:#dc2626;">%s</p>
                <p>Este código vence en 15 minutos. Si no creaste una cuenta, ignora este mensaje.</p>
                """.formatted(code);
        send(to, subject, body);
    }

    public void sendPasswordResetCode(String to, String code) {
        String subject = "Código para restablecer tu contraseña";
        String body = """
                <p>Recibimos una solicitud para restablecer tu contraseña.</p>
                <p>Tu código de verificación es:</p>
                <p style="text-align:center;font-size:28px;font-weight:bold;letter-spacing:4px;color:#dc2626;">%s</p>
                <p>Este código vence en 15 minutos. Si no solicitaste esto, ignora este mensaje.</p>
                """.formatted(code);
        send(to, subject, body);
    }

    public void sendMonthlyReportReady(String to, String toName, String monthLabel, String excelUrl, String pdfUrl) {
        String subject = "Reporte mensual de asistencias (" + monthLabel + ") - Attendance App";
        String body = """
                <p>Hola %s,</p>
                <p>El reporte de asistencias de <strong>%s</strong> ya está disponible.</p>
                <p style="text-align:center;margin-top:20px;">
                  <a href="%s" style="display:inline-block;background:#dc2626;color:#fff;padding:10px 20px;border-radius:6px;text-decoration:none;margin-right:10px;">
                    Descargar Excel
                  </a>
                  <a href="%s" style="display:inline-block;background:#374151;color:#fff;padding:10px 20px;border-radius:6px;text-decoration:none;">
                    Descargar PDF
                  </a>
                </p>
                <p style="margin-top:16px;color:#6b7280;font-size:13px;">
                  Nota: los registros de asistencia de %s serán eliminados de la base de datos tras el envío de este correo, para mantener el uso de almacenamiento bajo control.
                </p>
                """.formatted(toName, monthLabel, excelUrl, pdfUrl, monthLabel);
        send(to, subject, body);
    }

    public void sendDailyDigest(String to, String toName, String dateLabel,
                                 long totalToday,
                                 long presentTeacher, long lateTeacher, long absentTeacher,
                                 long presentDirector, long lateDirector, long absentDirector) {
        String subject = "Resumen de asistencias del " + dateLabel + " - Attendance App";
        String body = """
                <p>Hola %s,</p>
                <p>Resumen de asistencias de hoy, %s (total registros: %d):</p>
                <table style="border-collapse:collapse;width:100%%;margin-top:10px;">
                  <tr style="background:#374151;color:#fff;">
                    <th style="padding:8px;text-align:left;">Rol</th>
                    <th style="padding:8px;">Presente</th>
                    <th style="padding:8px;">Tarde</th>
                    <th style="padding:8px;">Ausente</th>
                  </tr>
                  <tr>
                    <td style="padding:8px;border-bottom:1px solid #e5e7eb;">Docentes</td>
                    <td style="padding:8px;text-align:center;border-bottom:1px solid #e5e7eb;">%d</td>
                    <td style="padding:8px;text-align:center;border-bottom:1px solid #e5e7eb;">%d</td>
                    <td style="padding:8px;text-align:center;border-bottom:1px solid #e5e7eb;">%d</td>
                  </tr>
                  <tr>
                    <td style="padding:8px;">Director</td>
                    <td style="padding:8px;text-align:center;">%d</td>
                    <td style="padding:8px;text-align:center;">%d</td>
                    <td style="padding:8px;text-align:center;">%d</td>
                  </tr>
                </table>
                """.formatted(toName, dateLabel, totalToday,
                presentTeacher, lateTeacher, absentTeacher,
                presentDirector, lateDirector, absentDirector);
        send(to, subject, body);
    }

    private void send(String to, String subject, String message) {
        send(templateId, to, Map.of("to_email", to, "subject", subject, "message", message));
    }

    private void send(String templateId, String to, Map<String, Object> templateParams) {
        if (to == null || to.isBlank()) {
            throw new EmailSendException("No hay una dirección de correo destino", null);
        }
        Map<String, Object> payload = Map.of(
                "service_id", serviceId,
                "template_id", templateId,
                "user_id", publicKey,
                "accessToken", privateKey,
                "template_params", templateParams
        );
        try {
            restClient.post()
                    .uri(EMAILJS_API_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new EmailSendException("No se pudo enviar el correo a " + to, e);
        }
    }
}
