package org.afterglow.paymentservice.repository;

import org.afterglow.paymentservice.model.InboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InboxMessageRepository extends JpaRepository<InboxMessage, String> {
}
