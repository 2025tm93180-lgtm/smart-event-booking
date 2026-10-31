package com.smartbooking.booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class InventoryClientConfig {

    @Bean
    public RestClient inventoryRestClient() {
        return RestClient.builder()
                .baseUrl("http://localhost:9091")
                .build();
    }
}
