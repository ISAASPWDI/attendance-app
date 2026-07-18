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

    private void send(String to, String subject, String htmlBody) {
        if (to == null || to.isBlank()) {
            throw new EmailSendException("No hay una dirección de correo destino", null);
        }
        Map<String, Object> payload = Map.of(
                "service_id", serviceId,
                "template_id", templateId,
                "user_id", publicKey,
                "accessToken", privateKey,
                "template_params", Map.of(
                        "to_email", to,
                        "subject", subject,
                        "message", htmlBody
                )
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
