package com.example.server.global.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** app.* 설정 바인딩(@ConfigurationProperties)과 스케줄러(데모 데이터 복구) 활성화 */
@Configuration
@ConfigurationPropertiesScan("com.example.server")
@EnableScheduling
public class AppConfig {
}
