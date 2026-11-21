package com.example.server.domain.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ExampleRequestDTO {

  public record Create(@NotBlank @Size(max = 100) String name) {
  }
}
