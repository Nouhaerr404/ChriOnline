package ma.ensate.util;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class EmailService {

    private static final String SENDER_EMAIL = ConfigLoader.get("MAIL_ADDRESS", "");
    private static final String APP_PASSWORD  = ConfigLoader.get("MAIL_PASSWORD", "");

    private final Session session;

    public EmailService() {
        this.session = createSession();
    }

    private Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, APP_PASSWORD);
            }
        });
    }

    public void sendHtml(String toEmail, String subject, String htmlBody) throws MessagingException {
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(SENDER_EMAIL));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject);
        message.setContent(htmlBody, "text/html; charset=utf-8");

        Transport.send(message);
    }
}