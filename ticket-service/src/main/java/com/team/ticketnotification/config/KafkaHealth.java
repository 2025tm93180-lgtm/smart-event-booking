package com.team.ticketnotification.config;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;
@Component("kafka")
@ConditionalOnProperty(name="app.kafka.health-enabled",havingValue="true",matchIfMissing=true)
public class KafkaHealth extends AbstractHealthIndicator implements AutoCloseable {
    private final AdminClient admin;
    public KafkaHealth(KafkaAdmin kafkaAdmin) { this.admin=AdminClient.create(kafkaAdmin.getConfigurationProperties()); }
    @Override protected void doHealthCheck(Health.Builder builder) throws Exception {
        var nodes=admin.describeCluster().nodes().get(2,TimeUnit.SECONDS);
        if(nodes.isEmpty()) builder.down(); else builder.up();
    }
    @Override public void close() { admin.close(Duration.ofSeconds(2)); }
}
