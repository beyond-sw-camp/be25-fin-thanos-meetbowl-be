package com.meetbowl.api.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.meetbowl.api.common.ApiHeaders;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/** 외부 JWT와 서버 간 토큰을 같은 인증 방식으로 오해하지 않도록 OpenAPI 보안 계약을 한곳에서 정의한다. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI meetbowlOpenAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Meetbowl API")
                                .description("Meetbowl api-server REST API documentation")
                                .version("v1")
                                .license(new License().name("Private")))
                .servers(List.of(new Server().url("/").description("Current server")))
                .components(
                        new Components()
                                .addSecuritySchemes(ApiHeaders.AUTHORIZATION, bearerAuth())
                                .addSecuritySchemes(ApiHeaders.INTERNAL_TOKEN, internalToken()));
    }

    private SecurityScheme bearerAuth() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT access token");
    }

    private SecurityScheme internalToken() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name(ApiHeaders.INTERNAL_TOKEN)
                .description("Internal server-to-server token");
    }
}
