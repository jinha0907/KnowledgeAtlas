package com.projectkg.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI projectKgOpenApi() {
    return new OpenAPI()
        .info(new Info()
            .title("Project Knowledge Graph API")
            .description("MVP API for notion ingest, retrieval, and decision tracking")
            .version("v1")
            .contact(new Contact().name("KnowledgeAtlas Team"))
            .license(new License().name("MIT")));
  }
}
