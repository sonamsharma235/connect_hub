package com.connecthub.auth.security;

import com.connecthub.auth.domain.AppUser;
import com.connecthub.auth.dto.UserProfileResponse;
import com.connecthub.auth.service.AuthService;
import com.connecthub.common.dto.AuthUser;
import com.connecthub.common.security.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final JwtService jwtService;
    private final String redirectBaseUrl;

    public OAuth2LoginSuccessHandler(AuthService authService,
                                     JwtService jwtService,
                                     @Value("${app.oauth2.redirect-url:http://localhost:3000/oauth-success}") String redirectBaseUrl) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.redirectBaseUrl = redirectBaseUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauthUser = oauthToken.getPrincipal();
        AppUser user = authService.upsertOAuthUser(oauthToken.getAuthorizedClientRegistrationId(), oauthUser.getAttributes());

        String token = jwtService.generateToken(
                new AuthUser(user.getEmail(), user.getName()),
                Map.of("role", user.getRole(), "provider", user.getProvider().name())
        );

        String targetUrl = UriComponentsBuilder
                .fromUriString(redirectBaseUrl)
                .queryParam("token", token)
                .queryParam("email", user.getEmail())
                .build()
                .toUriString();

        response.sendRedirect(targetUrl);
    }
}
