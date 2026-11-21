package com.example.server.domain.example.service.command;

import com.example.server.domain.example.converter.ExampleConverter;
import com.example.server.domain.example.dto.ExampleRequestDTO;
import com.example.server.domain.example.dto.ExampleResponseDTO;
import com.example.server.domain.example.entity.ExampleItem;
import com.example.server.domain.example.repository.ExampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExampleCommandServiceImpl implements ExampleCommandService {

  private final ExampleRepository repository;

  @Override
  @Transactional
  public ExampleResponseDTO create(ExampleRequestDTO.Create request) {
    ExampleItem item = repository.save(new ExampleItem(request.name()));
    return ExampleConverter.toResponse(item);
  }
}
