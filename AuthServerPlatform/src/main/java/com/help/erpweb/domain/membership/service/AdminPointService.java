package com.help.erpweb.domain.membership.service;

import com.help.erpweb.domain.membership.dto.request.AdminPointTransactionRequest;
import com.help.erpweb.domain.membership.dto.request.TransactionType;
import com.help.erpweb.domain.membership.dto.response.AdminPointDetailResponse;
import com.help.erpweb.domain.membership.dto.response.AdminPointLogResponse;
import com.help.erpweb.domain.membership.dto.response.AdminPointMemberPageResponse;
import com.help.erpweb.domain.membership.dto.response.AdminPointMemberResponse;
import com.help.erpweb.domain.membership.exception.InsufficientCoinException;
import com.help.erpweb.domain.membership.repository.AdminPointRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminPointService {
    private final AdminPointRepository adminPointRepository;
    private final Clock clock = Clock.systemUTC();

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
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

    @Transactional
    public AdminPointDetailResponse transact(
            String discordId,
            AdminPointTransactionRequest request
    ) {
        String sk = adminPointRepository.lockActiveMemberAndGetSk(discordId);
        int signedAmount = request.type() == TransactionType.DEPOSIT
                ? request.amount()
                : -request.amount();
        int currentCoin = adminPointRepository.findCoin(sk);
        int nextCoin = Math.addExact(currentCoin, signedAmount);
        if (nextCoin < 0) {
            throw new InsufficientCoinException();
        }

        adminPointRepository.updateCoin(sk, nextCoin);
        adminPointRepository.insertLog(
                sk,
                signedAmount,
                LocalDateTime.now(clock.withZone(ZoneOffset.UTC)),
                request.description().trim()
        );
        return getMember(discordId, 1, 20);
    }

    private int calculateTotalPages(long totalElements, int size) {
        return (int) Math.ceil((double) totalElements / size);
    }
}
