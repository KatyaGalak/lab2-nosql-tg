package com.university.booking.configuration;

import com.pengrad.telegrambot.TelegramBot;
import com.university.booking.client.PersonIdContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(BotProperties.class)
public class TelegramConfiguration {

    @Bean
    public TelegramBot telegramBot(BotProperties properties) {
        return new TelegramBot(properties.getToken());
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.additionalInterceptors((request, body, execution) -> {
            String personId = PersonIdContext.get();
            if (personId != null) {
                request.getHeaders().set("Person-Id", personId);
            }
            return execution.execute(request, body);
        }).build();
    }
}
