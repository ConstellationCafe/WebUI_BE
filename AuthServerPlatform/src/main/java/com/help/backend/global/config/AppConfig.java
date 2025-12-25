package com.help.backend.global.config;

import com.help.backend.global.config.properties.KisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({KisProperties.class})
public class AppConfig {
}
