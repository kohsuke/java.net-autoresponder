import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

/**
 * Factory methods for {@link MimeBodyPart}.
 *
 * @author Kohsuke Kawaguchi
 */
public class MimeBodyParts {
    public static MimeBodyPart createTextPart(String text) throws MessagingException {
        MimeBodyPart part = new MimeBodyPart();
        part.setText(text,"UTF-8");
        return part;
    }

    public static MimeBodyPart createMailPart(MimeMessage msg) throws MessagingException {
        MimeBodyPart part = new MimeBodyPart();
        part.setDataHandler(new DataHandler(msg,"message/rfc822"));
        return part;
    }
}
