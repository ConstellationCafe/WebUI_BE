package com.help.global.config;

import com.help.global.config.properties.KisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({KisProperties.class})
public class AppConfig {
}
