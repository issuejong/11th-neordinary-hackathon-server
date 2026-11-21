package com.example.server.global.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health")
@RestController
public class HealthController {

  @Operation(summary = "헬스 체크", description = "서버가 살아 있으면 `OK`(text/plain)를 반환합니다. 배포 스모크 테스트에서 사용합니다.")
  @GetMapping(value = "/health", produces = "text/plain")
  public String health() {
    return "OK";
  }
}
