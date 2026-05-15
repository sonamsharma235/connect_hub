package com.connecthub.auth.config;

import com.connecthub.common.security.JwtService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    JwtService jwtService(JwtProperties properties) {
        return new JwtService(properties.secret(), properties.expirationMillis());
    }
}
