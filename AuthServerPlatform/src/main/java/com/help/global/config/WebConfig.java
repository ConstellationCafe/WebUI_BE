package com.help.global.config;

import com.help.authserver.domain.user.repository.config.ERPSubscriberRepository;
import com.help.global.guild.GuildContextInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final ERPSubscriberRepository erpSubscriberRepository;

    @Override
    public void addInterceptors(@NonNull final InterceptorRegistry registry) {
        registry.addInterceptor(new GuildContextInterceptor(erpSubscriberRepository))
            .addPathPatterns("/api/**")
            .excludePathPatterns("/auth/**");
    }
}
