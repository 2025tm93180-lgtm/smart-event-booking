
package org.afterglow.eventinventoryservice.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "service", "event-inventory-service",
                "status", "UP"
        );
    }
}
