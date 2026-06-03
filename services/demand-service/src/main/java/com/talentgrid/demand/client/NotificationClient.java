package com.talentgrid.demand.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "notification-service", url = "${app.services.notification.url:http://localhost:8084}")
public interface NotificationClient {
    @GetMapping("/api/v1/notifications/health")
    String healthCheck();
}
