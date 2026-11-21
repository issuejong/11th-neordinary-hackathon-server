package com.example.server.domain.image.support;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/**
 * 이미지 정책 설정 (application.yml의 app.image.*). 용량 한도는 spring.servlet.multipart에서 관리한다.
 *
 * @param maxDimension 가로 · 세로 최대 픽셀
 * @param cacheMaxAge  이미지 조회 응답의 Cache-Control max-age
 */
@ConfigurationProperties("app.image")
public record ImageProperties(
    @DefaultValue("8192") int maxDimension,
    @DefaultValue("30d") Duration cacheMaxAge,
    @DefaultValue("5MB") DataSize profileMaxSize,
    @DefaultValue("5MB") DataSize stepMaxSize
) {
}
