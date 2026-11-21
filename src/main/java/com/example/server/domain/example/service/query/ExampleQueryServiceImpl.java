package com.example.server.domain.example.service.query;

import com.example.server.domain.example.converter.ExampleConverter;
import com.example.server.domain.example.dto.ExampleResponseDTO;
import com.example.server.domain.example.entity.ExampleItem;
import com.example.server.domain.example.repository.ExampleRepository;
import com.example.server.global.exception.BusinessException;
import com.example.server.global.response.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExampleQueryServiceImpl implements ExampleQueryService {

  private final ExampleRepository repository;

  @Override
  @Transactional(readOnly = true)
  public ExampleResponseDTO find(Long id) {
    ExampleItem item = repository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorStatus._NOT_FOUND));
    return ExampleConverter.toResponse(item);
  }
}
