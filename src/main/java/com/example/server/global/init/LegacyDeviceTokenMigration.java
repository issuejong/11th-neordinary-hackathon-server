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

/** 기존 MySQL에서 새 사용자를 만들 때 과거 기기 토큰 컬럼을 생략할 수 있게 한다. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "spring.datasource.driver-class-name", havingValue = "com.mysql.cj.jdbc.Driver")
@RequiredArgsConstructor
public class LegacyDeviceTokenMigration implements ApplicationRunner {

  private final JdbcTemplate jdbcTemplate;

  @Override
  public void run(ApplicationArguments args) {
    if (!requiresLegacyToken()) {
      return;
    }
    try {
      jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN device_token VARCHAR(255) NULL");
    } catch (DataAccessException exception) {
      if (requiresLegacyToken()) {
        throw exception;
      }
    }
  }

  private boolean requiresLegacyToken() {
    return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
        SELECT EXISTS (
          SELECT 1 FROM information_schema.columns
          WHERE table_schema = DATABASE() AND table_name = 'users'
            AND column_name = 'device_token' AND is_nullable = 'NO'
        )
        """, Boolean.class));
  }
}
