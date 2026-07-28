package com.attendance.demo.service.cloud;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key",    apiKey,
                "api_secret", apiSecret,
                "secure",     true
        ));
    }

    /**
     * Uploads an image and returns its public URL.
     *
     * @param file      the image file to upload
     * @param folder    Cloudinary folder (e.g. "signatures")
     * @param publicId  stable public ID (e.g. "signature_42")
     */
    @SuppressWarnings("unchecked")
    public String uploadImage(MultipartFile file, String folder, String publicId) throws IOException {
        Map<String, Object> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder",    folder,
                        "public_id", publicId,
                        "overwrite", true
                )
        );
        return (String) result.get("secure_url");
    }

    /**
     * Uploads an arbitrary file (e.g. xlsx/pdf) as a raw resource and returns its public URL.
     *
     * @param bytes     the file content
     * @param folder    Cloudinary folder (e.g. "monthly-reports")
     * @param publicId  stable public ID (e.g. "asistencias_2026-07")
     */
    @SuppressWarnings("unchecked")
    public String uploadRaw(byte[] bytes, String folder, String publicId) throws IOException {
        Map<String, Object> result = cloudinary.uploader().upload(
                bytes,
                ObjectUtils.asMap(
                        "folder",       folder,
                        "public_id",    publicId,
                        "resource_type", "raw",
                        "overwrite",    true
                )
        );
        return (String) result.get("secure_url");
    }

    /** Deletes an image by its full public ID (folder/publicId). */
    public void deleteImage(String fullPublicId) throws Exception {
        cloudinary.uploader().destroy(fullPublicId, ObjectUtils.emptyMap());
    }
}
