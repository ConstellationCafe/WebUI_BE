package com.help.erpweb.domain.modules.erp.point.service;

import com.help.erpweb.domain.modules.erp.point.dto.request.AdminPointTransactionRequest;
import com.help.erpweb.domain.modules.erp.point.dto.request.TransactionType;
import com.help.erpweb.domain.modules.erp.point.dto.request.repository.PointLogDto;
import com.help.erpweb.domain.modules.erp.point.dto.response.AdminPointDetailResponse;
import com.help.erpweb.domain.modules.erp.point.dto.response.AdminPointLogResponse;
import com.help.erpweb.domain.modules.erp.point.dto.response.AdminPointMemberPageResponse;
import com.help.erpweb.domain.modules.erp.point.dto.response.AdminPointMemberResponse;
import com.help.erpweb.domain.modules.erp.point.entity.PointLogEntity;
import com.help.erpweb.domain.modules.erp.point.exception.InsufficientCoinException;
import com.help.erpweb.domain.modules.erp.point.exception.PointBalanceLimitException;
import com.help.erpweb.domain.modules.erp.point.exception.PointLogConflictException;
import com.help.erpweb.domain.modules.erp.point.repository.AdminPointRepository;
import com.help.erpweb.domain.modules.erp.point.repository.MembershipRepository;
import com.help.erpweb.domain.modules.erp.point.repository.PointRepository;
import com.help.erpweb.domain.metadata.response.ColumnMetaDto;
import com.help.erpweb.domain.notification.entity.NotificationCategory;
import com.help.erpweb.domain.notification.entity.NotificationTargetType;
import com.help.erpweb.domain.notification.exception.NotificationTargetNotFoundException;
import com.help.erpweb.domain.notification.service.NotificationCommand;
import com.help.erpweb.domain.notification.service.NotificationPublisher;
import com.help.global.chat.ChatIdentities;
import com.help.global.chat.ChatUser;
import com.help.global.common.response.ApiResponse;
import com.help.global.guild.GuildContext;
import com.help.global.jwt.CustomUser;
import java.time.Clock;
import java.time.LocalDateTime;
import java.text.NumberFormat;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 2026-09-26: 기존 AdminPointService(관리자용 포인트 CRUD)와
 * MembershipService(본인 포인트 로그 조회)를 하나로 합쳤다 — 둘 다 "포인트"라는
 * 같은 리소스를 다루는 코드일 뿐, 로직을 분리해 둘 이유가 없었다. 인가 수준이
 * 다른 건 이제 컨트롤러 쪽에서만 구분한다: {@code PointController}(본인 조회,
 * 일반 인증만 필요)와 {@code AdminPointController}(관리자 전용, 클래스 레벨
 * {@code @PreAuthorize})가 이 서비스를 공유한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {
    /** 회원이 알림에서 바로 자기 포인트 내역으로 이동하는 FE 경로. */
    static final String POINT_LOG_LINK = "/point_log";

    private final AdminPointRepository adminPointRepository;
    private final PointRepository pointRepository;
    private final MembershipRepository membershipRepository;
    private final NotificationPublisher notificationPublisher;
    private final Clock clock = Clock.systemUTC();

    /**
     * 본인 포인트 로그 조회. sk(=(botId, discordId)로 결정되는 방 스코프
     * 신원)로 조회하므로 관리자 권한이 필요 없다(MembershipService.getPointLog
     * 였던 메서드).
     */
    public ApiResponse<?> getPointLog(
            CustomUser user,
            int page,
            int size
    ) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.max(size, 1);

        ChatUser chatUser = ChatIdentities.fromPrincipal(user);

        String sk =
                membershipRepository.findSkByChatUser(
                        chatUser
                );

        List<ColumnMetaDto> metadata =
                pointRepository.findColumnMetas(
                                PointRepository.schemaName,
                                PointRepository.tableName
                        )
                        .stream()
                        .filter(v -> !"sk".equals(v.getColName()))
                        .map(v ->
                                ColumnMetaDto.builder()
                                        .colName(v.getColName())
                                        .isPrimary(v.getIsPrimary())
                                        .isNullable(v.getIsNullable())
                                        .build()
                        )
                        .toList();

        Pageable pageable =
                PageRequest.of(
                        normalizedPage - 1,
                        normalizedSize
                );

        Page<PointLogEntity> pointPage =
                pointRepository.findBySk(
                        sk,
                        pageable
                );

        List<PointLogDto> pointList =
                pointPage
                        .getContent()
                        .stream()
                        .map(entity ->
                                PointLogDto.builder()
                                        .amount(
                                                entity
                                                        .getAmount()
                                                        .toString()
                                        )
                                        .at(
                                                entity
                                                        .getAt()
                                                        .toString()
                                        )
                                        .description(
                                                entity.getDescription()
                                        )
                                        .build()
                        )
                        .toList();

        Map<String, Object> body = new HashMap<>();
        body.put("metadata", metadata);
        body.put("entities", pointList);
        body.put("page", normalizedPage);
        body.put("size", normalizedSize);
        body.put("totalElements", pointPage.getTotalElements());
        body.put("totalPages", pointPage.getTotalPages());
        body.put("hasNext", pointPage.hasNext());
        return ApiResponse.success(body);
    }

    @Transactional(transactionManager = "constellationTransactionManager", readOnly = true)
    public AdminPointMemberPageResponse getMembers(String discordId, int page, int size) {
        int offset = (page - 1) * size;
        long totalElements = adminPointRepository.countActiveMembers(discordId);
        List<AdminPointMemberResponse> items =
                adminPointRepository.findActiveMembers(discordId, offset, size);
        int totalPages = calculateTotalPages(totalElements, size);
        return new AdminPointMemberPageResponse(
                items,
                page,
                size,
                totalElements,
                totalPages,
                page < totalPages
        );
    }

    @Transactional(transactionManager = "constellationTransactionManager", readOnly = true)
    public AdminPointDetailResponse getMember(String discordId, int page, int size) {
        AdminPointMemberResponse member = adminPointRepository.findActiveMember(discordId);
        long totalElements = adminPointRepository.countLogs(discordId);
        int totalPages = calculateTotalPages(totalElements, size);
        List<AdminPointLogResponse> logs =
                adminPointRepository.findLogs(discordId, (page - 1) * size, size);
        return new AdminPointDetailResponse(
                member.discordId(),
                member.username(),
                member.state(),
                member.coin(),
                logs,
                page,
                size,
                totalElements,
                totalPages,
                page < totalPages
        );
    }

    @Transactional(transactionManager = "constellationTransactionManager")
    public AdminPointDetailResponse transact(
            String discordId,
            AdminPointTransactionRequest request
    ) {
        String sk = adminPointRepository.lockActiveMemberAndGetSk(discordId);
        int signedAmount = request.type() == TransactionType.DEPOSIT
                ? request.amount()
                : -request.amount();
        adjustBalance(sk, signedAmount);
        adminPointRepository.insertLog(
                sk,
                signedAmount,
                LocalDateTime.now(clock.withZone(ZoneOffset.UTC)),
                request.description().trim()
        );
        notifyTransaction(discordId, request.type(), request.amount(), request.description().trim());
        return getMember(discordId, 1, 20);
    }

    /**
     * ADR-0004 내부 발행 예시: 관리자가 입금·출금하면 대상 회원에게 개인 알림을 보낸다.
     * 같은 트랜잭션에서 저장되므로 포인트 처리가 롤백되면 알림도 남지 않는다.
     * 대상이 현재 채팅방의 재적 회원이 아니면 알림만 건너뛰고 포인트 처리는 계속한다.
     */
    private void notifyTransaction(String discordId, TransactionType type, int amount, String description) {
        boolean deposit = type == TransactionType.DEPOSIT;
        String formattedAmount = NumberFormat.getIntegerInstance(Locale.KOREA).format(amount);
        try {
            notificationPublisher.publish(NotificationCommand.internal(
                    GuildContext.requireBotId(),
                    NotificationTargetType.USER,
                    discordId,
                    NotificationCategory.POINT,
                    deposit ? "포인트가 입금되었습니다" : "포인트가 출금되었습니다",
                    formattedAmount + "포인트 " + (deposit ? "입금" : "출금") + " · " + description,
                    POINT_LOG_LINK,
                    "point"
            ));
        } catch (NotificationTargetNotFoundException ex) {
            log.info("포인트 알림 대상이 현재 채팅방의 재적 회원이 아니어서 알림을 건너뜁니다");
        }
    }

    @Transactional(transactionManager = "constellationTransactionManager")
    public AdminPointDetailResponse updateLog(
            String discordId,
            int originalAmount,
            LocalDateTime at,
            Integer amount,
            String description
    ) {
        String sk = adminPointRepository.lockActiveMemberAndGetSk(discordId);
        AdminPointLogResponse current = adminPointRepository.lockLog(sk, originalAmount, at);
        int nextAmount = amount == null ? current.amount() : amount;
        String nextDescription = description == null
                ? current.description()
                : description.trim();
        long difference = (long) nextAmount - current.amount();
        if (difference != 0) {
            adjustBalance(sk, difference);
        }
        if (difference != 0 || !Objects.equals(nextDescription, current.description())) {
            try {
                adminPointRepository.updateLog(
                        sk,
                        originalAmount,
                        at,
                        nextAmount,
                        nextDescription
                );
            } catch (DataIntegrityViolationException ex) {
                throw new PointLogConflictException();
            }
        }
        return getMember(discordId, 1, 20);
    }

    @Transactional(transactionManager = "constellationTransactionManager")
    public AdminPointDetailResponse deleteLog(
            String discordId,
            int originalAmount,
            LocalDateTime at
    ) {
        String sk = adminPointRepository.lockActiveMemberAndGetSk(discordId);
        AdminPointLogResponse current = adminPointRepository.lockLog(sk, originalAmount, at);
        adjustBalance(sk, -(long) current.amount());
        adminPointRepository.deleteLog(sk, originalAmount, at);
        return getMember(discordId, 1, 20);
    }

    private void adjustBalance(String sk, long difference) {
        long nextCoin = (long) adminPointRepository.findCoin(sk) + difference;
        if (nextCoin < 0) {
            throw new InsufficientCoinException();
        }
        if (nextCoin > Integer.MAX_VALUE) {
            throw new PointBalanceLimitException();
        }
        adminPointRepository.updateCoin(sk, (int) nextCoin);
    }

    private int calculateTotalPages(long totalElements, int size) {
        return (int) Math.ceil((double) totalElements / size);
    }
}
