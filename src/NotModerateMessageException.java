import javax.mail.internet.MimeMessage;

/**
 * Thrown when a message isn't a java.net moderation message.
 *
 * This problem is recovered by sending out the error to the owner.
 *
 * @author Kohsuke Kawaguchi
 */
class NotModerateMessageException extends Exception {

    private final MimeMessage msg;

    public NotModerateMessageException(String message, MimeMessage msg) {
        super(message);
        this.msg = msg;
    }

    public NotModerateMessageException(Throwable cause, MimeMessage msg) {
        super(cause);
        this.msg = msg;
    }

    public NotModerateMessageException(String message, Throwable cause, MimeMessage msg) {
        super(message, cause);
        this.msg = msg;
    }

    /**
     * Gets the message parsed as a {@link MimeMessage}.
     */
    public MimeMessage getMimeMessage() {
        return msg;
    }
}
