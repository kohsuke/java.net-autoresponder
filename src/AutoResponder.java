
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class reads a single message from the command line (piped in from
 * procmail), unbundles the attachment, and replies to it with a canned
 * response.
 * <p/>
 * http://java.sun.com/j2ee/1.4/docs/api/javax/mail/internet/MimeMessage.html#MimeMessage(javax.mail.Session,%20java.io.InputStream)
 * http://cvsync.red.iplanet.com/cvsweb/jaxb-ri-wrapper/build-poster/src/com/sun/jaxb/ac/Main.java?rev=1.4&content-type=text/x-cvsweb-markup&cvsroot=WPTS
 *
 * @author Ryan.Shoemaker@Sun.COM
 */
public class AutoResponder {

    private static final Session session = Session.getInstance(System.getProperties());

    // This is the complete message as received from java.net ezmlm.
    // It should have an attachment containing the user's original e-mail
    // that is being moderated.
    private final ModerateMessage moderateMessage;

    // text file containing the contents of the auto response
    private final File autoResponseText;

    // send a adminAddress to this address for tracking purposes
    private final Address adminAddress;

    private AutoResponder(Address bcc, File message) throws MessagingException, IOException, NotModerateMessageException {
        this.adminAddress = bcc;
        moderateMessage = new ModerateMessage(session, System.in);
        autoResponseText = message;
    }

    /**
     * return the contents of the response text file as a String
     *
     * @return contents of the file as a String
     */
    private String getAutoresponseText() throws IOException {
        // build macro substitution list
        Map m = new HashMap();
        m.put("${projectName}",moderateMessage.getProjectName());
        m.put("${listName}",moderateMessage.getListName());

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(autoResponseText),"UTF-8"));

        String line = br.readLine();
        while (line != null) {
            pw.println(substitute(line,m));
            line = br.readLine();
        }
        pw.flush();
        return sw.toString();
    }

    /**
     * Performs keyword substitution of the line.
     */
    private String substitute(String line, Map m) {
        OUTER:
        while(true) {
            for (Iterator itr = m.entrySet().iterator(); itr.hasNext();) {
                Map.Entry e = (Map.Entry) itr.next();
                String key = (String) e.getKey();
                int idx = line.indexOf(key);
                if(idx>=0) {
                    line = line.substring(0,idx)+e.getValue()+line.substring(idx+key.length());
                    continue OUTER;
                }
            }
            return line;
        }
    }

    private int run() throws MessagingException, IOException, NotModerateMessageException {

        MimeMessage userMessage = moderateMessage.getUserMessage();

        // check if the list is indeed in the TO or CC address.
        // some spams don't even bother to have the TO field.
        Address[] recipients = userMessage.getRecipients(Message.RecipientType.TO);
        Address[] cc = userMessage.getRecipients(Message.RecipientType.CC);
        if(!hasListAddress(recipients) && !hasListAddress(cc))
            throw new NotModerateMessageException("TO nor CC header doesn't have the list name. Probably a spam",moderateMessage);


        // start building the auto-reply
        MimeMessage reply = (MimeMessage) userMessage.reply(false);
        // pretend as if this message is coming from the list owner.
        // in that way the reply to the autoresponder will go to the owner
        reply.setFrom(moderateMessage.getListOwnerAddress());

        // send a copy to me and set a nice header for filtering purposes
        reply.setRecipient(Message.RecipientType.BCC, adminAddress);
        reply.setHeader("X-Javanet-Autoresponder", getRecipients(recipients));
        reply.setHeader("X-Javanet-Autoresponder-Project", moderateMessage.getProjectName());
        reply.setHeader("X-Javanet-Autoresponder-List", moderateMessage.getListName());

        Multipart multipart = new MimeMultipart();
        reply.setContent(multipart);

        // Fill the message with the text from the auto reply file
        multipart.addBodyPart(MimeBodyParts.createTextPart(getAutoresponseText()));
        // attach the original user message
        multipart.addBodyPart(MimeBodyParts.createMailPart(userMessage));

        // Send the message
        Transport.send(reply);

        System.out.println("Sent a response to "+userMessage.getFrom()[0]+" about '"+userMessage.getSubject()+"'");

        return 0;
    }

    /**
     * Returns true if the recipients list includes the list address.
     */
    private boolean hasListAddress(Address[] recipients) {
        if(recipients==null)    return false;
        InternetAddress listAddress = moderateMessage.getListAddress();
        for( int i=0; i<recipients.length; i++ ) {
            if(recipients[i].toString().indexOf(listAddress.getAddress())!=-1)
                return true;
        }
        return false;
    }

    // forward the message along unchanged because it doesn't appear to
    // be a MODERATE message.
    //
    // the message will be sent "To" the "daemonAddress" address specified
    // on the command line.
    private static void processError(NotModerateMessageException x, Address recipient) throws MessagingException {
        // Create the message to forward
        Message forward = new MimeMessage(session);

        // Fill in header
        MimeMessage srcMsg = x.getMimeMessage();
        forward.setSubject("Fwd: " + srcMsg.getSubject());
        forward.setFrom(srcMsg.getFrom()[0]);
        forward.addRecipient(Message.RecipientType.TO,recipient);

        // Create a multi-part to combine the parts
        Multipart multipart = new MimeMultipart();
        forward.setContent(multipart);

        multipart.addBodyPart(MimeBodyParts.createTextPart("JavaNet Autoresponder says: " + x.getMessage()));
        multipart.addBodyPart(MimeBodyParts.createMailPart(srcMsg));

        // Send message
        Transport.send(forward);

        System.out.println("Sent an error "+x.getMessage());
    }

    // return a comma separated list of recipients
    private String getRecipients(Address[] recipientAddresses) {
        StringBuffer recipients = new StringBuffer();
        for (int i = 0; i < recipientAddresses.length; i++) {
            Address recipientAddress = recipientAddresses[i];
            recipients.append(recipientAddress.toString());
            if (i != recipientAddresses.length - 1)
                recipients.append(',');
        }
        return recipients.toString();
    }


    public static void process(Address admin, File message) throws MessagingException, IOException {
        try {
            new AutoResponder(admin,message).run();
        } catch (NotModerateMessageException e) {
            processError(e,admin);
        }
    }

    public static void main(String[] args) throws MessagingException, IOException {
        System.exit(main0(args));
    }
    public static int main0(String[] args) throws MessagingException, IOException {
        if (args.length != 2) {
            System.err.println("usage: java AutoResponder <human admin address> <pathToReplyTextFile>");
            System.err.println("Read the MODERATE e-mail from stdin, and reply a reminder");
            return -1;
        }

        process(
            new InternetAddress(args[0]),
            new File(args[1]));
        return 0;
    }
}
