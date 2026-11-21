package com.example.server.domain.example.controller;

import com.example.server.domain.example.dto.ExampleRequestDTO;
import com.example.server.domain.example.dto.ExampleResponseDTO;
import com.example.server.domain.example.service.command.ExampleCommandService;
import com.example.server.domain.example.service.query.ExampleQueryService;
import com.example.server.global.response.ApiResponse;
import com.example.server.global.response.code.status.SuccessStatus;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/examples")
@RequiredArgsConstructor
public class ExampleController {

  private final ExampleCommandService commandService;
  private final ExampleQueryService queryService;

  @PostMapping
  public ResponseEntity<ApiResponse<ExampleResponseDTO>> create(
      @Valid @RequestBody ExampleRequestDTO.Create request
  ) {
    ExampleResponseDTO result = commandService.create(request);
    return ResponseEntity.created(URI.create("/api/v1/examples/" + result.id()))
        .body(ApiResponse.of(SuccessStatus._CREATED, result));
  }

  @GetMapping("/{id}")
  public ApiResponse<ExampleResponseDTO> find(@PathVariable Long id) {
    return ApiResponse.onSuccess(queryService.find(id));
  }
}
