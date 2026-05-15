package com.connecthub.message.dto;

import java.util.List;

public record LatestMessagesRequest(
        List<String> roomCodes
) {
}

