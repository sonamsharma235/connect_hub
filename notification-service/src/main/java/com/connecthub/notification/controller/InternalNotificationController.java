package com.connecthub.notification.controller;

import com.connecthub.common.dto.MessageNotificationCreateRequest;
import com.connecthub.common.dto.MessageNotificationCreateResponse;
import com.connecthub.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/notifications")
public class InternalNotificationController {

    private final NotificationService notificationService;

    public InternalNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageNotificationCreateResponse createMessageNotifications(@RequestBody MessageNotificationCreateRequest request) {
        return new MessageNotificationCreateResponse(notificationService.createMessageNotifications(request));
    }
}

