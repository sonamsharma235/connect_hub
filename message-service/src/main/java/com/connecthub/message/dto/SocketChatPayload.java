package com.connecthub.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SocketChatPayload(
        @NotBlank @Size(max = 2000) String content
) {
}
