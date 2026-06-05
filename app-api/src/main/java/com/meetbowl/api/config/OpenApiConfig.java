package com.meetbowl.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI와 /v3/api-docs에 노출되는 API 문서의 기본 메타데이터를 설정한다.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI meetbowlOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Meetbowl API")
                        .description("Meetbowl api-server REST API documentation")
                        .version("v1")
                        .license(new License().name("Private")))
                .servers(List.of(new Server()
                        .url("/")
                        .description("Current server")));
    }
}
