package com.example.server.global.config;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

/**
 * Swagger UI index.html에 커스텀 테마(swagger/custom.css)를 넣는다.
 * springdoc 기본 변환(swagger-initializer.js 설정 주입)은 그대로 유지한다.
 */
@Component
public class SwaggerThemeTransformer extends SwaggerIndexPageTransformer {

  private final String css;

  public SwaggerThemeTransformer(SwaggerUiConfigProperties swaggerUiConfig, SwaggerUiOAuthProperties swaggerUiOAuthProperties,
      SwaggerWelcomeCommon swaggerWelcomeCommon, ObjectMapperProvider objectMapperProvider) throws IOException {
    super(swaggerUiConfig, swaggerUiOAuthProperties, swaggerWelcomeCommon, objectMapperProvider);
    this.css = new ClassPathResource("swagger/custom.css").getContentAsString(StandardCharsets.UTF_8);
  }

  @Override
  public Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain chain)
      throws IOException {
    Resource transformed = super.transform(request, resource, chain);
    if (!"index.html".equals(resource.getFilename())) {
      return transformed;
    }
    String html = transformed.getContentAsString(StandardCharsets.UTF_8);
    String themed = html.replace("</head>", "<style>\n" + css + "\n</style>\n</head>");
    return new TransformedResource(resource, themed.getBytes(StandardCharsets.UTF_8));
  }
}
