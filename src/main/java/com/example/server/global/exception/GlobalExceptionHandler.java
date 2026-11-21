package com.example.server.global.exception;

import com.example.server.global.response.ApiResponse;
import com.example.server.global.response.code.BaseCode;
import com.example.server.global.response.code.status.ErrorStatus;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 모든 예외를 ApiResponse 실패 형식으로 변환한다.
 * 400: 요청 값 오류 / 404: 없는 리소스 / 500: 그 외 예상하지 못한 예외(내부 메시지는 노출하지 않음)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
    return failure(exception.getBaseCode());
  }

  /** @Valid @RequestBody 검증 실패: 어떤 필드가 왜 틀렸는지 result에 담는다. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Map<String, String>>> handleInvalidBody(
      MethodArgumentNotValidException exception
  ) {
    Map<String, String> errors = new LinkedHashMap<>();
    for (FieldError error : exception.getBindingResult().getFieldErrors()) {
      errors.putIfAbsent(error.getField(), error.getDefaultMessage());
    }
    return failure(ErrorStatus._BAD_REQUEST, errors);
  }

  /** @RequestParam, @PathVariable 등에 붙인 제약(@Min, @Size ...) 위반 */
  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<ApiResponse<Map<String, String>>> handleInvalidParameter(
      HandlerMethodValidationException exception
  ) {
    Map<String, String> errors = new LinkedHashMap<>();
    exception.getParameterValidationResults().forEach(result -> {
      String name = result.getMethodParameter().getParameterName();
      result.getResolvableErrors()
          .forEach(error -> errors.putIfAbsent(name, error.getDefaultMessage()));
    });
    return failure(ErrorStatus._BAD_REQUEST, errors);
  }

  @ExceptionHandler({
      ConstraintViolationException.class,
      MethodArgumentTypeMismatchException.class,
      HttpMessageNotReadableException.class,
      MissingServletRequestParameterException.class,
      MissingServletRequestPartException.class,
      MissingRequestHeaderException.class,
      MultipartException.class
  })
  public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(Exception exception) {
    if (exception instanceof MaxUploadSizeExceededException) {
      return failure(ErrorStatus._FILE_TOO_LARGE);
    }
    return failure(ErrorStatus._BAD_REQUEST);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleMissingResource() {
    return failure(ErrorStatus._NOT_FOUND);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed() {
    return failure(ErrorStatus._METHOD_NOT_ALLOWED);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ApiResponse<Void>> handleUnsupportedMediaType() {
    return failure(ErrorStatus._UNSUPPORTED_MEDIA_TYPE);
  }

  /** 유니크 키 충돌 등 (동시 요청으로 같은 기기 토큰이 두 번 가입되는 경우 등) */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleConflict(DataIntegrityViolationException exception) {
    log.warn("Data integrity violation: {}", exception.getMostSpecificCause().getMessage());
    return failure(ErrorStatus._CONFLICT);
  }

  /** 위에서 처리하지 못한 모든 예외. 원인은 로그로만 남긴다. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
    log.error("Unhandled exception", exception);
    return failure(ErrorStatus._INTERNAL_SERVER_ERROR);
  }

  private ResponseEntity<ApiResponse<Void>> failure(BaseCode code) {
    return ResponseEntity.status(code.getHttpStatus()).body(ApiResponse.onFailure(code));
  }

  private <T> ResponseEntity<ApiResponse<T>> failure(BaseCode code, T data) {
    return ResponseEntity.status(code.getHttpStatus()).body(ApiResponse.onFailure(code, data));
  }
}
