package org.afterglow.paymentservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InboxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String message_id;
    private String message_type;
    private String processed_at;
}
