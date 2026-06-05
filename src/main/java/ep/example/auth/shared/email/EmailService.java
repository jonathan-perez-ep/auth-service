package ep.example.auth.shared.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${MAIL_FROM:noreply@auth-service.com}")
    private String mailFrom;

    @Value("${ISSUER_URI:http://localhost:9000}")
    private String issuerUri;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendConfirmationEmail(String toEmail, String token) {
        String confirmationLink = issuerUri + "/auth/confirm?token=" + token;

        String body = "Haz clic en el siguiente enlace para confirmar tu cuenta:\n\n"
                + confirmationLink
                + "\n\nSi no solicitaste este registro, ignora este mensaje.";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(toEmail);
        message.setSubject("Confirma tu cuenta");
        message.setText(body);

        mailSender.send(message);
    }
}
