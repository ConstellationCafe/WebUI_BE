package com.help.erpweb.domain.membership.service;

import com.help.erpweb.domain.membership.dto.request.repository.PointAdjustmentRequest;
import com.help.erpweb.domain.membership.dto.request.repository.PointAdjustmentType;
import com.help.erpweb.domain.membership.dto.request.repository.PointLogDto;
import com.help.erpweb.domain.membership.entity.PointLogEntity;
import com.help.erpweb.domain.membership.repository.MembershipRepository;
import com.help.erpweb.domain.membership.repository.PointRepository;
import com.help.erpweb.domain.metadata.response.ColumnMetaDto;
import com.help.global.common.exception.CustomException;
import com.help.global.common.exception.ErrorCode;
import com.help.global.common.response.ApiResponse;
import com.help.global.jwt.CustomUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MembershipService {
    private final PointRepository pointRepository;
    private final MembershipRepository membershipRepository;

    public ApiResponse<?> getPointLog(CustomUser user, int page, int size) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.max(size, 1);
        String discordId = user.getUsername();
        String sk = membershipRepository.findSkByDiscordId(discordId);

        List<ColumnMetaDto> metadata =
                pointRepository.findColumnMetas(
                                PointRepository.schemaName,
                                PointRepository.tableName
                        )
                        .stream()
                        .filter(v -> !"sk".equals(v.getColName()))
                        .map(v -> ColumnMetaDto.builder()
                                .colName(v.getColName())
                                .isPrimary(v.getIsPrimary())
                                .isNullable(v.getIsNullable())
                                .build())
                        .toList();

        Pageable pageable = PageRequest.of(normalizedPage - 1, normalizedSize);
        Page<PointLogEntity> pointPage = pointRepository.findBySk(sk, pageable);
        List<PointLogDto> pointList = pointPage.getContent().stream()
                .map(entity -> PointLogDto.builder()
                        .amount(entity.getAmount().toString())
                        .at(entity.getAt().toString())
                        .description(entity.getDescription())
                        .build())
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

    public ApiResponse<?> getActivePointMembers(String discordId, int page, int size) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.max(size, 1);
        long offset = (long) (normalizedPage - 1) * normalizedSize;
        String normalizedDiscordId = normalizeDiscordId(discordId);

        List<Map<String, Object>> members = membershipRepository
                .findActivePointMembers(normalizedDiscordId, offset, normalizedSize)
                .stream()
                .map(this::toMemberResponse)
                .toList();
        long totalElements = membershipRepository.countActivePointMembers(normalizedDiscordId);
        int totalPages = (int) ((totalElements + normalizedSize - 1) / normalizedSize);

        Map<String, Object> body = new HashMap<>();
        body.put("members", members);
        body.put("page", normalizedPage);
        body.put("size", normalizedSize);
        body.put("totalElements", totalElements);
        body.put("totalPages", totalPages);
        body.put("hasNext", normalizedPage < totalPages);
        return ApiResponse.success(body);
    }

    public ApiResponse<?> getPointMemberDetail(String discordId, int page, int size) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.max(size, 1);
        Object[] member = requireActivePointMember(discordId);
        String sk = member[2].toString();

        Page<PointLogEntity> pointPage = pointRepository.findBySk(
                sk,
                PageRequest.of(normalizedPage - 1, normalizedSize)
        );

        List<Map<String, Object>> logs = pointPage.getContent().stream()
                .map(this::toLogResponse)
                .toList();
        Map<String, Object> body = new HashMap<>();
        body.put("member", toMemberResponse(member));
        body.put("logs", logs);
        body.put("page", normalizedPage);
        body.put("size", normalizedSize);
        body.put("totalElements", pointPage.getTotalElements());
        body.put("totalPages", pointPage.getTotalPages());
        body.put("hasNext", pointPage.hasNext());
        return ApiResponse.success(body);
    }

    @Transactional(transactionManager = "constellationTransactionManager")
    public ApiResponse<?> adjustPoint(String discordId, PointAdjustmentRequest request) {
        Object[] member = requireActivePointMember(discordId);
        String sk = member[2].toString();
        int amount = request.getAmount();

        if (request.getType() == PointAdjustmentType.DEPOSIT) {
            membershipRepository.depositCoin(sk, amount);
        } else {
            int updatedRows = membershipRepository.withdrawCoin(sk, amount);
            if (updatedRows == 0) {
                throw new CustomException(ErrorCode.INSUFFICIENT_POINT_BALANCE);
            }
        }

        LocalDateTime at = LocalDateTime.now(Clock.systemUTC());
        int signedAmount = request.getType() == PointAdjustmentType.DEPOSIT
                ? amount
                : -amount;
        membershipRepository.insertPointLog(
                sk,
                signedAmount,
                at,
                request.getDescription().trim()
        );

        long balance = membershipRepository.findCoinBySk(sk);
        Map<String, Object> updatedMember = toMemberResponse(member);
        updatedMember.put("coin", balance);

        Map<String, Object> transaction = new HashMap<>();
        transaction.put("amount", signedAmount);
        transaction.put("at", at.toString());
        transaction.put("description", request.getDescription().trim());

        Map<String, Object> body = new HashMap<>();
        body.put("member", updatedMember);
        body.put("transaction", transaction);
        return ApiResponse.success(body);
    }

    private Object[] requireActivePointMember(String discordId) {
        Object[] member = membershipRepository.findActivePointMemberByDiscordId(
                normalizeDiscordId(discordId)
        );
        if (member == null) {
            throw new CustomException(ErrorCode.POINT_MEMBER_NOT_FOUND);
        }
        return member;
    }

    private String normalizeDiscordId(String discordId) {
        if (discordId == null || discordId.isBlank()) {
            return null;
        }
        return discordId.trim();
    }

    private Map<String, Object> toMemberResponse(Object[] row) {
        Map<String, Object> member = new HashMap<>();
        member.put("discordId", row[0] == null ? null : row[0].toString());
        member.put("username", row[1] == null ? null : row[1].toString());
        member.put("coin", row[3] == null ? 0L : ((Number) row[3]).longValue());
        return member;
    }

    private Map<String, Object> toLogResponse(PointLogEntity entity) {
        Map<String, Object> logResponse = new HashMap<>();
        logResponse.put("amount", entity.getAmount().toString());
        logResponse.put("at", entity.getAt().toString());
        logResponse.put("description", entity.getDescription());
        return logResponse;
    }
}
