package com.induztek.equipos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {

    @Bean
    public Clock reloj() {
        return Clock.systemDefaultZone();
    }
}
