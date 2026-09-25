package com.help.erpweb.domain.membership.service;

import com.help.erpweb.domain.membership.dto.request.AdminPointTransactionRequest;
import com.help.erpweb.domain.membership.dto.request.TransactionType;
import com.help.erpweb.domain.membership.dto.response.AdminPointDetailResponse;
import com.help.erpweb.domain.membership.dto.response.AdminPointLogResponse;
import com.help.erpweb.domain.membership.dto.response.AdminPointMemberPageResponse;
import com.help.erpweb.domain.membership.dto.response.AdminPointMemberResponse;
import com.help.erpweb.domain.membership.exception.InsufficientCoinException;
import com.help.erpweb.domain.membership.exception.PointBalanceLimitException;
import com.help.erpweb.domain.membership.exception.PointLogConflictException;
import com.help.erpweb.domain.membership.repository.AdminPointRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminPointService {
    private final AdminPointRepository adminPointRepository;
    private final Clock clock = Clock.systemUTC();

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
        return getMember(discordId, 1, 20);
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
