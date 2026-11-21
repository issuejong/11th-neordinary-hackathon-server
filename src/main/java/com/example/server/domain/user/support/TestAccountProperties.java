package com.example.server.domain.user.support;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.test-accounts")
public record TestAccountProperties(@DefaultValue("2m") Duration leaseDuration) {
}
