package com.example.server.domain.example.repository;

import com.example.server.domain.example.entity.ExampleItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExampleRepository extends JpaRepository<ExampleItem, Long> {
}
