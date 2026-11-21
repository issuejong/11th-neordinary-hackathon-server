package com.example.server.global.exception;

import com.example.server.global.response.ApiResponse;
import com.example.server.global.response.code.BaseCode;
import com.example.server.global.response.code.status.ErrorStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
    return failure(exception.getBaseCode());
  }

  @ExceptionHandler({
      MethodArgumentNotValidException.class,
      MethodArgumentTypeMismatchException.class,
      HttpMessageNotReadableException.class
  })
  public ResponseEntity<ApiResponse<Void>> handleInvalidRequest() {
    return failure(ErrorStatus._BAD_REQUEST);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleMissingResource() {
    return failure(ErrorStatus._NOT_FOUND);
  }

  private ResponseEntity<ApiResponse<Void>> failure(BaseCode code) {
    return ResponseEntity.status(code.getHttpStatus()).body(ApiResponse.onFailure(code));
  }
}
