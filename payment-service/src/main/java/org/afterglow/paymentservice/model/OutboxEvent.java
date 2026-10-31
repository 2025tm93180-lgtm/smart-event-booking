package org.afterglow.paymentservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String event_id;
    @Column(name = "aggregate_id")
    private String uuId;
    private String event_type;
    private String payload;
    @Column(name = "correlation_id")
    private String idempotencyKey;
    private String created_at;
    private String published_at;
    private int attempts;
}