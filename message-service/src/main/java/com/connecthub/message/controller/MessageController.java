package com.connecthub.message.controller;

import com.connecthub.message.dto.ChatMessageRequest;
import com.connecthub.message.dto.ChatMessageResponse;
import com.connecthub.message.dto.LatestMessageResponse;
import com.connecthub.message.dto.LatestMessagesRequest;
import com.connecthub.message.service.ChatMessageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final ChatMessageService chatMessageService;

    public MessageController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    @GetMapping("/rooms/{roomCode}")
    public List<ChatMessageResponse> roomMessages(@PathVariable String roomCode,
                                                  @AuthenticationPrincipal User principal) {
        return chatMessageService.getMessages(roomCode, principal.getUsername());
    }

    @PostMapping("/rooms/{roomCode}")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse sendMessage(@PathVariable String roomCode,
                                           @Valid @RequestBody ChatMessageRequest request,
                                           @AuthenticationPrincipal User principal) {
        return chatMessageService.saveMessage(roomCode, principal.getUsername(), request.content());
    }

    @PostMapping("/latest")
    public List<LatestMessageResponse> latestMessages(@RequestBody(required = false) LatestMessagesRequest request,
                                                      @AuthenticationPrincipal User principal) {
        List<String> roomCodes = request == null || request.roomCodes() == null ? List.of() : request.roomCodes();
        List<String> normalized = roomCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(code -> !code.isBlank())
                .distinct()
                .limit(60)
                .toList();

        String userEmail = principal.getUsername();
        return normalized.stream()
                .map(code -> new LatestMessageResponse(code, chatMessageService.getLatestMessage(code, userEmail)))
                .toList();
    }
}
