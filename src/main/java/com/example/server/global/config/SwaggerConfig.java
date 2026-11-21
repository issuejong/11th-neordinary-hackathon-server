package com.example.server.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

  @Bean
  public OpenAPI serverOpenApi() {
    return new OpenAPI()
        .info(new Info()
            .title("11th-neordinary-hackathon-server API")
            .description("11th-neordinary-hackathon-server REST API")
            .version("v1"));
  }
}
