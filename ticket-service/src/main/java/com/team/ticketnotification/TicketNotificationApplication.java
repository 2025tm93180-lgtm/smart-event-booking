package com.team.ticketnotification;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication
@EnableScheduling
public class TicketNotificationApplication {
    public static void main(String[] args) { SpringApplication.run(TicketNotificationApplication.class, args); }
}
