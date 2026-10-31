package com.team.ticketnotification.notification;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
@Service
@ConditionalOnProperty(name="app.notifications.mode",havingValue="smtp")
public class SmtpEmailSender {
    private final JavaMailSender sender;
    private final String from;
    public SmtpEmailSender(JavaMailSender sender,@Value("${app.notifications.from:}") String from) {
        this.sender=sender;this.from=from;
        if(!validAddress(from)) throw new IllegalArgumentException("SMTP mode requires a valid MAIL_FROM email address.");
    }
    private static boolean validAddress(String value) {
        if(value==null || value.isBlank() || value.contains("\r") || value.contains("\n"))return false;
        try {
            var address=new InternetAddress(value,true);address.validate();
            return value.equals(address.getAddress()); // Exactly one address, no display-name syntax.
        } catch(MessagingException ex) {return false;}
    }
    public void send(NotificationDeliveryStore.Delivery delivery) {
        if(!validAddress(delivery.recipient())) throw new EmailDeliveryException("Recipient email is missing or invalid; provide customerEmail on a new confirmed booking.",false);
        MimeMessage message;
        try {
            message=sender.createMimeMessage();
            var helper=new MimeMessageHelper(message,true,StandardCharsets.UTF_8.name());
            helper.setFrom(from);helper.setTo(delivery.recipient());helper.setSubject("Your event ticket is ready");
            String text="Your event ticket is ready.\n\nBooking: "+delivery.bookingId()+"\nEvent: "+delivery.eventId()+
                "\nSeat: "+delivery.seatId()+"\nTicket code: "+delivery.ticketCode()+"\nIssued at: "+delivery.issuedAt()+"\n\nKeep this email for your records.";
            String html="<html><body><h2>Your event ticket is ready</h2><p>Booking: "+escape(delivery.bookingId())+
                "</p><p>Event: "+delivery.eventId()+"</p><p>Seat: "+delivery.seatId()+"</p><p>Ticket code: <strong>"+
                escape(delivery.ticketCode())+"</strong></p><p>Issued at: "+delivery.issuedAt()+"</p><p>Keep this email for your records.</p></body></html>";
            helper.setText(text,html);message.setHeader("X-Notification-ID",delivery.id());
            String domain=from.substring(from.lastIndexOf('@')+1);
            // Copy only after content exists. Preserve the logical message ID on retries.
            message=new MimeMessage(message) {
                @Override protected void updateMessageID() throws MessagingException {
                    setHeader("Message-ID","<ticket-notification-"+delivery.id()+"@"+domain+">");
                }
            };
            message.setHeader("Message-ID","<ticket-notification-"+delivery.id()+"@"+domain+">");
        } catch(MessagingException | MailException ex) {
            throw new EmailDeliveryException("Email preparation failed.",false);
        }
        try {sender.send(message);}
        catch(MailAuthenticationException ex) {throw new EmailDeliveryException("SMTP authentication failed; verify server credentials.",false);}
        catch(MailException ex) {throw new EmailDeliveryException("SMTP delivery outcome is uncertain; check provider logs before retrying.",true);}
    }
    private static String escape(String value) {return HtmlUtils.htmlEscape(value);}
}
