package com.example.server.domain.user.controller;

import com.example.server.domain.user.dto.TestAccountRequestDTO.Register;
import com.example.server.domain.user.dto.TestAccountResponseDTO;
import com.example.server.domain.user.service.TestAccountService;
import com.example.server.global.init.DemoData;
import com.example.server.global.response.ApiResponse;
import com.example.server.global.response.code.status.SuccessStatus;
import com.example.server.global.util.Ulid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Test Account")
@RestController
@RequestMapping("/api/test-accounts")
@RequiredArgsConstructor
public class TestAccountController {

  private final TestAccountService accounts;

  @Operation(summary = "테스트 계정 목록", description = "등록한 테스트 계정을 슬롯 순서대로 반환합니다.")
  @GetMapping
  public ApiResponse<List<TestAccountResponseDTO.Account>> list() {
    return ApiResponse.onSuccess(accounts.list());
  }

  @Operation(summary = "테스트 계정 등록", description = "빈 슬롯에 사용자와 테스트 계정을 함께 등록합니다.")
  @PostMapping
  public ResponseEntity<ApiResponse<TestAccountResponseDTO.Account>> register(@Valid @RequestBody Register request) {
    TestAccountResponseDTO.Account result = accounts.register(request);
    return ResponseEntity.created(URI.create("/api/test-accounts"))
        .body(ApiResponse.of(SuccessStatus._CREATED, result));
  }

  @Operation(summary = "테스트 계정 선택", description = "목록 응답의 publicId를 전달합니다. 사용 중인 계정은 409를 반환합니다.")
  @PostMapping("/{publicId}/claim")
  public ApiResponse<TestAccountResponseDTO.Claim> claim(
      @Parameter(description = "목록에서 가져온 테스트 계정 publicId (예시: 8번 심사용 데모 유저)", example = DemoData.USER_ID)
      @PathVariable @Pattern(regexp = Ulid.REGEX, message = Ulid.MESSAGE) String publicId
  ) {
    return ApiResponse.onSuccess(accounts.claim(publicId));
  }

  @Operation(summary = "테스트 계정 사용 시간 연장", security = @SecurityRequirement(name = "TestAccountSession"))
  @PostMapping("/{publicId}/heartbeat")
  public ApiResponse<TestAccountResponseDTO.Heartbeat> heartbeat(
      @Parameter(description = "사용 중인 테스트 계정 publicId", example = DemoData.USER_ID)
      @PathVariable @Pattern(regexp = Ulid.REGEX, message = Ulid.MESSAGE) String publicId,
      @Parameter(hidden = true) @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization
  ) {
    return ApiResponse.onSuccess(accounts.heartbeat(publicId, authorization));
  }

  @Operation(summary = "테스트 계정 사용 종료", security = @SecurityRequirement(name = "TestAccountSession"))
  @DeleteMapping("/{publicId}/claim")
  public ApiResponse<Void> release(
      @Parameter(description = "사용 중인 테스트 계정 publicId", example = DemoData.USER_ID)
      @PathVariable @Pattern(regexp = Ulid.REGEX, message = Ulid.MESSAGE) String publicId,
      @Parameter(hidden = true) @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization
  ) {
    accounts.release(publicId, authorization);
    return ApiResponse.onSuccess(null);
  }
}
