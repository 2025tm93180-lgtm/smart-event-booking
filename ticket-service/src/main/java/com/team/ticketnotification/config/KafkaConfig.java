package com.team.ticketnotification.config;
import com.team.ticketnotification.messaging.InvalidBookingEventException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
@Configuration
public class KafkaConfig {
    @Bean NewTopic confirmations(@Value("${app.kafka.topic}") String topic,
        @Value("${app.kafka.partitions}") int partitions,@Value("${app.kafka.replicas}") int replicas) {
        return TopicBuilder.name(topic).partitions(partitions).replicas(replicas).build();
    }
    @Bean NewTopic deadLetters(@Value("${app.kafka.topic}") String topic,
        @Value("${app.kafka.partitions}") int partitions,@Value("${app.kafka.replicas}") int replicas) {
        return TopicBuilder.name(topic+".DLT").partitions(partitions).replicas(replicas).build();
    }
    @Bean CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String,String> template) {
        var recoverer=new DeadLetterPublishingRecoverer(template,
            (record,ex)->new TopicPartition(record.topic()+".DLT",record.partition()));
        recoverer.setFailIfSendResultIsError(true); // Never skip a message if DLT publication failed.
        var handler=new DefaultErrorHandler(recoverer,new FixedBackOff(1000L,3L));
        handler.addNotRetryableExceptions(InvalidBookingEventException.class);
        // With RECORD ack mode, ackAfterHandle commits successfully recovered records.
        return handler;
    }
}
