package org.afterglow.eventinventoryservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.ThreadLocalRandom;

@Setter
@Getter
@Entity
@Table(name = "venues")
public class Venue {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private Integer capacity;

    @Column(length = 1000)
    private String facilities;

    @Column(nullable = false)
    private boolean active = true;

    @PrePersist
    public void generateId() {
        if (id == null || id.isBlank()) {
            id = String.format("VEN-%05d", ThreadLocalRandom.current().nextInt(100000));
        }
    }

}