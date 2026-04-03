package ma.ensate.server.services;

import ma.ensate.util.ConfigLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

public class EmailService {

    private static final Logger logger = LogManager.getLogger(EmailService.class);

    private static final String SMTP_HOST = ConfigLoader.get("SMTP_HOST", "smtp.gmail.com");
    private static final int    SMTP_PORT = ConfigLoader.getInt("SMTP_PORT", 587);
    private static final String SMTP_USER = ConfigLoader.get("MAIL_ADDRESS", "");
    private static final String SMTP_PASS = ConfigLoader.get("MAIL_PASSWORD", "");
    private static final String FROM_NAME = "ChriOnline";

    private EmailService() {}

    public static void envoyerCodeOtp(String destinataire, String code) throws MessagingException, UnsupportedEncodingException {
        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", String.valueOf(SMTP_PORT));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USER, SMTP_PASS);
            }
        });

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(SMTP_USER, FROM_NAME));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
        msg.setSubject("ChriOnline — Code de vérification 2FA");
        msg.setText(
            "Bonjour,\n\n" +
            "Votre code de vérification à deux facteurs est :\n\n" +
            "    " + code + "\n\n" +
            "Ce code expire dans 5 minutes.\n\n" +
            "Si vous n'avez pas tenté de vous connecter, ignorez cet email.\n\n" +
            "— L'équipe ChriOnline"
        );

        Transport.send(msg);
        logger.info("Code OTP envoyé à : " + destinataire);
    }
}
