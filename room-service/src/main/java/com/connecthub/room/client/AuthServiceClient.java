package com.connecthub.room.client;

import com.connecthub.common.dto.PublicUserProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @GetMapping("/api/internal/users/by-email/{email}")
    PublicUserProfileResponse byEmail(@PathVariable("email") String email);

    @GetMapping("/api/internal/users/by-username/{username}")
    PublicUserProfileResponse byUsername(@PathVariable("username") String username);
}

