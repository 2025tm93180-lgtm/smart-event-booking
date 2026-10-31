package com.team.ticketnotification.ticket;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1")
public class TicketController {
    private final TicketQueries queries;
    public TicketController(TicketQueries queries) { this.queries=queries; }
    @GetMapping("/tickets/{ticketId}")
    public TicketQueries.Ticket ticket(@PathVariable String ticketId,@AuthenticationPrincipal Jwt jwt) { return queries.ticket(ticketId,jwt); }
    @GetMapping("/tickets/by-booking/{bookingId}")
    public TicketQueries.Page<TicketQueries.Ticket> tickets(@PathVariable String bookingId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@AuthenticationPrincipal Jwt jwt) { return queries.tickets(bookingId,page,size,jwt); }
    @GetMapping("/notifications/by-booking/{bookingId}")
    public TicketQueries.Page<TicketQueries.Notification> notifications(@PathVariable String bookingId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@AuthenticationPrincipal Jwt jwt) { return queries.notifications(bookingId,page,size,jwt); }
    @PostMapping("/notifications/retry/{attemptId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String,String>> retry(@PathVariable String attemptId,@RequestParam(defaultValue="false") boolean allowDuplicate) {
        queries.retry(attemptId,allowDuplicate); return ResponseEntity.accepted().body(Map.of("status","PENDING","notificationId",attemptId));
    }
}
