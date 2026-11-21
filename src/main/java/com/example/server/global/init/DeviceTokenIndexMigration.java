package com.example.server.global.init;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 기존 MySQL DB의 토큰 고유 인덱스를 제거한다. 일반 인덱스는 JPA 스키마 갱신이 생성한다. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "spring.datasource.driver-class-name", havingValue = "com.mysql.cj.jdbc.Driver")
@RequiredArgsConstructor
public class DeviceTokenIndexMigration implements ApplicationRunner {

  private final JdbcTemplate jdbcTemplate;

  @Override
  public void run(ApplicationArguments args) {
    if (!hasLegacyUniqueIndex()) {
      return;
    }
    try {
      jdbcTemplate.execute("ALTER TABLE users DROP INDEX uk_users_device_token");
    } catch (DataAccessException exception) {
      // 다른 인스턴스가 먼저 제거한 경우에는 마이그레이션이 끝난 상태다.
      if (hasLegacyUniqueIndex()) {
        throw exception;
      }
    }
  }

  private boolean hasLegacyUniqueIndex() {
    return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
        SELECT EXISTS (
          SELECT 1 FROM information_schema.statistics
          WHERE table_schema = DATABASE() AND table_name = 'users'
            AND index_name = 'uk_users_device_token' AND non_unique = 0
        )
        """, Boolean.class));
  }
}
