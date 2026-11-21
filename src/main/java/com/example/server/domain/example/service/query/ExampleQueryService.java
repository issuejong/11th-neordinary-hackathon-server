package com.example.server.domain.example.service.query;

import com.example.server.domain.example.dto.ExampleResponseDTO;

public interface ExampleQueryService {

  ExampleResponseDTO find(Long id);
}
