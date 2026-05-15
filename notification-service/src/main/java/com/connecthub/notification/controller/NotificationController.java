package com.connecthub.notification.controller;

import com.connecthub.notification.dto.MarkReadResponse;
import com.connecthub.notification.dto.NotificationResponse;
import com.connecthub.notification.dto.UnreadCountResponse;
import com.connecthub.notification.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/unread")
    public List<NotificationResponse> unread(@AuthenticationPrincipal User principal) {
        return notificationService.unreadForUser(principal.getUsername());
    }

    @GetMapping("/unread/count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal User principal) {
        return new UnreadCountResponse(notificationService.unreadCountForUser(principal.getUsername()));
    }

    @PostMapping("/rooms/{roomCode}/read")
    public MarkReadResponse markRoomRead(@PathVariable String roomCode,
                                         @AuthenticationPrincipal User principal) {
        return new MarkReadResponse(notificationService.markRoomRead(principal.getUsername(), roomCode));
    }
}

