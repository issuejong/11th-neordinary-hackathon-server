package com.example.server.global.config;

import com.example.server.global.auth.LoginUser;
import com.example.server.global.init.DemoData;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import java.util.Arrays;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

  static {
    // @LoginUser Long 파라미터는 쿼리 파라미터로 문서화하지 않는다 (헤더로 대체)
    SpringDocUtils.getConfig().addAnnotationsToIgnore(LoginUser.class);
  }

  @Bean
  public OpenAPI serverOpenApi() {
    return new OpenAPI()
        .info(new Info()
            .title("11th-neordinary-hackathon-server API")
            .description("""
                NEORDINARY HACKATHON 8조 루틴 공유 서비스 API

                - 모든 응답은 `{ isSuccess, code, message, result }` 형식입니다.
                - 작성자가 필요한 API는 `X-User-Id` 헤더(사용자 publicId)가 필요합니다. \
                예시 값(데모 사용자)이 미리 채워져 있어 바로 실행할 수 있습니다.
                """)
            .version("v1"));
  }

  /** @LoginUser 파라미터는 숨기고, 대신 X-User-Id 헤더 입력칸(예시값 포함)을 문서에 추가한다. */
  @Bean
  public OperationCustomizer loginUserHeaderCustomizer() {
    return (operation, handlerMethod) -> {
      boolean needsLogin = Arrays.stream(handlerMethod.getMethodParameters())
          .anyMatch(p -> p.hasParameterAnnotation(LoginUser.class));
      if (needsLogin) {
        operation.addParametersItem(new HeaderParameter()
            .name(LoginUser.HEADER)
            .required(true)
            .description("요청한 사용자의 publicId (임시 인증). 데모 사용자: " + DemoData.USER_ID)
            .example(DemoData.USER_ID)
            .schema(new StringSchema()));
      }
      return operation;
    };
  }
}
