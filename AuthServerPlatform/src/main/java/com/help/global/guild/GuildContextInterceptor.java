package com.help.global.guild;

import com.help.authserver.domain.user.entity.config.ErpSubscriber;
import com.help.authserver.domain.user.repository.config.ERPSubscriberRepository;
import com.help.global.common.exception.CustomException;
import com.help.global.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * {@code /api/**} 요청(단 {@code /auth/**} 제외)마다 {@code X-Guild-Id} 헤더를 읽어
 * {@link ERPSubscriberRepository}로 botId를 조회하고 {@link GuildContext}에 저장한다.
 * <p>
 * HandlerInterceptor로 구현한 이유: Servlet Filter에서 예외를 던지면
 * {@code @RestControllerAdvice}(GlobalExceptionHandler)가 잡지 못하고 컨테이너 기본
 * 오류 페이지로 새 나간다. HandlerInterceptor#preHandle은 DispatcherServlet의
 * try-catch 안에서 실행되므로, 여기서 던진 {@link CustomException}은
 * GlobalExceptionHandler가 정상적으로 JSON 응답(403)으로 변환해준다.
 */
@Slf4j
@RequiredArgsConstructor
public class GuildContextInterceptor implements HandlerInterceptor {
    public static final String GUILD_ID_HEADER = "X-Guild-Id";

    private final ERPSubscriberRepository erpSubscriberRepository;

    @Override
    public boolean preHandle(
        @NonNull final HttpServletRequest request,
        @NonNull final HttpServletResponse response,
        @NonNull final Object handler
    ) {
        final String guildId = request.getHeader(GUILD_ID_HEADER);
        if (guildId == null || guildId.isBlank()) {
            log.warn("{} 헤더가 없는 요청: {}", GUILD_ID_HEADER, request.getRequestURI());
            throw new CustomException(ErrorCode.GUILD_HEADER_MISSING);
        }

        final String botId = erpSubscriberRepository.findByGuildId(guildId)
            .map(ErpSubscriber::getBotId)
            .orElseThrow(() -> {
                log.warn("등록되지 않은 guildId로 요청: {}", guildId);
                return new CustomException(ErrorCode.GUILD_NOT_REGISTERED);
            });

        GuildContext.setBotId(botId);
        return true;
    }

    @Override
    public void afterCompletion(
        @NonNull final HttpServletRequest request,
        @NonNull final HttpServletResponse response,
        @NonNull final Object handler,
        final Exception ex
    ) {
        GuildContext.clear();
    }
}
