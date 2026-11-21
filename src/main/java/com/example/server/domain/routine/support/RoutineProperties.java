package com.example.server.domain.routine.support;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 루틴 정책 설정 (application.yml의 app.routine.*)
 *
 * @param rankingCursorTtl 좋아요순 · 참여순 목록 커서(순서 스냅샷)의 유효 시간
 */
@ConfigurationProperties("app.routine")
public record RoutineProperties(@DefaultValue("15m") Duration rankingCursorTtl) {
}
