package com.example.server.domain.example.converter;

import com.example.server.domain.example.dto.ExampleResponseDTO;
import com.example.server.domain.example.entity.ExampleItem;

public final class ExampleConverter {

  private ExampleConverter() {
  }

  public static ExampleResponseDTO toResponse(ExampleItem item) {
    return new ExampleResponseDTO(item.getId(), item.getName());
  }
}
