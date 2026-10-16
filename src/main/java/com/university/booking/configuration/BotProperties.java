package com.university.booking.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "bot")
public class BotProperties {
    private String token;
}
