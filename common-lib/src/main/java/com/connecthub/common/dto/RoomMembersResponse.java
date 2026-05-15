package com.connecthub.common.dto;

import java.util.List;

public record RoomMembersResponse(
        List<String> members
) {
}

