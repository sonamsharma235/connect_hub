package com.connecthub.message.config;

import com.connecthub.common.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Bean
    JwtService jwtService(@Value("${app.jwt.secret}") String secret,
                          @Value("${app.jwt.expiration-millis}") long expirationMillis) {
        return new JwtService(secret, expirationMillis);
    }
}
