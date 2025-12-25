package com.help.authserver.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.help.authserver.global.config.properties.KisProperties;

@Configuration
@EnableConfigurationProperties({KisProperties.class})
public class AppConfig {
}
