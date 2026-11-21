package com.example.server.domain.example.service.command;

import com.example.server.domain.example.dto.ExampleRequestDTO;
import com.example.server.domain.example.dto.ExampleResponseDTO;

public interface ExampleCommandService {

  ExampleResponseDTO create(ExampleRequestDTO.Create request);
}
