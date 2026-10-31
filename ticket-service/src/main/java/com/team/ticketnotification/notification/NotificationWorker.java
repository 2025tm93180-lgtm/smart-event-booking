package com.team.ticketnotification.notification;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
@Service
@ConditionalOnProperty(name="app.notifications.enabled",havingValue="true",matchIfMissing=true)
public class NotificationWorker {
    private static final Logger log=LoggerFactory.getLogger(NotificationWorker.class);
    private final NotificationDeliveryStore store;
    private final Optional<SmtpEmailSender> smtp;
    private final boolean mockFail;
    private final int batch;
    public NotificationWorker(NotificationDeliveryStore store,Optional<SmtpEmailSender> smtp,
        @Value("${app.notifications.mock-fail:false}") boolean mockFail,@Value("${app.notifications.batch-size:20}") int batch) {
        this.store=store;this.smtp=smtp;this.mockFail=mockFail;this.batch=batch;
    }
    @Scheduled(fixedDelayString="${app.notifications.delay-ms:3000}")
    public void deliverPending() {
        store.recoverExpiredClaims();
        for(int i=0;i<batch;i++) {
            var delivery=store.claim(); // A short, committed DB transaction. No SMTP while holding row locks.
            if(delivery==null)return;
            try(var ignored=MDC.putCloseable("correlationId",delivery.correlationId())) {
                boolean success=false,uncertain=false;
                String error=null;
                try {
                    if("MOCK".equals(delivery.mode())) {
                        if(mockFail)throw new EmailDeliveryException("Simulated delivery failure",false);
                    } else if("SMTP".equals(delivery.mode()) && smtp.isPresent()) {
                        smtp.get().send(delivery);
                    } else throw new EmailDeliveryException("SMTP delivery is disabled or the delivery mode is invalid.",false);
                    success=true;
                } catch(EmailDeliveryException ex) {error=ex.getMessage();uncertain=ex.uncertain();}
                  catch(RuntimeException ex) {error="Unexpected delivery error; verify delivery before retrying.";uncertain=true;}
                // A failure to persist completion leaves the claim for expiry/reconciliation; never resend in this call.
                boolean recorded=store.finish(delivery,success,uncertain,error);
                log.info("Notification processed notificationId={} mode={} status={} deliveryUncertain={} completionRecorded={}",
                    delivery.id(),delivery.mode(),success?"SENT":"FAILED",uncertain,recorded);
            }
        }
    }
}
