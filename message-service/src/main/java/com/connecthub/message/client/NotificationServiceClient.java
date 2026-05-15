package com.connecthub.message.client;

import com.connecthub.common.dto.MessageNotificationCreateRequest;
import com.connecthub.common.dto.MessageNotificationCreateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service")
public interface NotificationServiceClient {

    @PostMapping("/api/internal/notifications/messages")
    MessageNotificationCreateResponse createMessageNotifications(@RequestBody MessageNotificationCreateRequest request);
}

