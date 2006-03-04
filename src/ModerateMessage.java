import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.AddressException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a MODERATE message.
 *
 * @author Kohsuke Kawaguchi
 */
public class ModerateMessage extends MimeMessage {

    private final InternetAddress listAddress;
    private final String projectName;
    private final String listName;

    public ModerateMessage(Session session, InputStream inputStream) throws MessagingException, IOException, NotModerateMessageException {
        super(session, inputStream);

        // perform some simply sanity checks
        if (!(getSubject().startsWith("MODERATE")))
            throw new NotModerateMessageException("Subject line does not contain\"MODERATE\"",this);

        if (!(getContent() instanceof Multipart))
            throw new NotModerateMessageException("Message does not contain multi-part Mime attachment",this);

        // get the content of the message
        Multipart content = (Multipart) getContent();
        if (content.getCount() != 2)
            throw new NotModerateMessageException("This apprears to be a \"MODERATE\" message, but it has the wrong number of attachments",this);


        // determine the list to which this message is sent.
        // the subject is of the form "MODERATE for users@jaxb.dev.java.net"
        String subject = getSubject();
        subject = subject.substring(subject.lastIndexOf(' ')+1);
        this.listAddress = new InternetAddress(subject);
        this.projectName = subject.substring(subject.indexOf('@')+1,subject.length()-13);  // gets 'jaxb'
        this.listName = subject.substring(0,subject.indexOf('@')); // gets 'users'
    }

    /**
     * Gets the list e-mail address like "users@jaxb.dev.java.net"
     */
    public InternetAddress getListAddress() {
        return listAddress;
    }

    /**
     * Gets the java.net project name like "jaxb".
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * Gets the list name portion like "users".
     */
    public String getListName() {
        return listName;
    }

    /**
     * Gets the user message enclosed in this moderation message.
     */
    public MimeMessage getUserMessage() throws MessagingException, IOException {
        Multipart content = (Multipart) getContent();
        MimeBodyPart body = (MimeBodyPart) content.getBodyPart(1);
        return new MimeMessage(session, body.getInputStream());
    }

    /**
     * Gets the e-mail address of the list owner.
     */
    public Address getListOwnerAddress() throws AddressException {
        return new InternetAddress(getListName()+"-owner@"+getProjectName()+".dev.java.net");
    }
}
