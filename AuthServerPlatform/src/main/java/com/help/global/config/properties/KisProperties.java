package com.help.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kis")
public record KisProperties(
	String cano,
	String acntPrdtCd,
	String appkey,
	String appsecret
) {
}
