package com.ninjaassemble.time;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GameClockConfig {
    @Bean
    Clock gameClock() { return Clock.systemUTC(); }
}
