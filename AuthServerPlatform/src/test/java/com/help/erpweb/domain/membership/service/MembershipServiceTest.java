package com.help.erpweb.domain.membership.service;

import com.help.erpweb.domain.membership.dto.request.repository.PointAdjustmentRequest;
import com.help.erpweb.domain.membership.dto.request.repository.PointAdjustmentType;
import com.help.erpweb.domain.membership.repository.MembershipRepository;
import com.help.erpweb.domain.membership.repository.PointRepository;
import com.help.global.common.exception.CustomException;
import com.help.global.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {
    @Mock
    private PointRepository pointRepository;

    @Mock
    private MembershipRepository membershipRepository;

    private MembershipService service;

    @BeforeEach
    void setUp() {
        service = new MembershipService(pointRepository, membershipRepository);
    }

    @Test
    void depositIncreasesBalanceAndWritesPositiveLogInSameFlow() {
        when(membershipRepository.findActivePointMemberByDiscordId("123"))
                .thenReturn(new Object[]{"123", "member", "sk-123", 100});
        when(membershipRepository.findCoinBySk("sk-123")).thenReturn(150L);
        PointAdjustmentRequest request = PointAdjustmentRequest.builder()
                .type(PointAdjustmentType.DEPOSIT)
                .amount(50)
                .description("monthly reward")
                .build();

        service.adjustPoint("123", request);

        verify(membershipRepository).depositCoin("sk-123", 50);
        verify(membershipRepository).insertPointLog(
                eq("sk-123"),
                eq(50),
                any(LocalDateTime.class),
                eq("monthly reward")
        );
        verify(membershipRepository, never()).withdrawCoin(anyString(), anyInt());
    }

    @Test
    void withdrawalWritesNegativeLogWhenBalanceUpdateSucceeds() {
        when(membershipRepository.findActivePointMemberByDiscordId("123"))
                .thenReturn(new Object[]{"123", "member", "sk-123", 100});
        when(membershipRepository.withdrawCoin("sk-123", 25)).thenReturn(1);
        when(membershipRepository.findCoinBySk("sk-123")).thenReturn(75L);
        PointAdjustmentRequest request = PointAdjustmentRequest.builder()
                .type(PointAdjustmentType.WITHDRAW)
                .amount(25)
                .description("shop purchase")
                .build();

        service.adjustPoint("123", request);

        ArgumentCaptor<Integer> amountCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(membershipRepository).insertPointLog(
                eq("sk-123"),
                amountCaptor.capture(),
                any(LocalDateTime.class),
                eq("shop purchase")
        );
        assertEquals(-25, amountCaptor.getValue());
    }

    @Test
    void withdrawalWithInsufficientBalanceDoesNotWriteLog() {
        when(membershipRepository.findActivePointMemberByDiscordId("123"))
                .thenReturn(new Object[]{"123", "member", "sk-123", 100});
        when(membershipRepository.withdrawCoin("sk-123", 101)).thenReturn(0);
        PointAdjustmentRequest request = PointAdjustmentRequest.builder()
                .type(PointAdjustmentType.WITHDRAW)
                .amount(101)
                .description("overdraw attempt")
                .build();

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.adjustPoint("123", request)
        );

        assertEquals(
                ErrorCode.INSUFFICIENT_POINT_BALANCE.getMessage(),
                exception.getMessage()
        );
        verify(membershipRepository, never()).insertPointLog(
                anyString(),
                anyInt(),
                any(LocalDateTime.class),
                anyString()
        );
    }

    @Test
    void inactiveMemberCannotBeAdjusted() {
        PointAdjustmentRequest request = PointAdjustmentRequest.builder()
                .type(PointAdjustmentType.DEPOSIT)
                .amount(1)
                .description("test")
                .build();

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.adjustPoint("123", request)
        );

        assertEquals(ErrorCode.POINT_MEMBER_NOT_FOUND.getMessage(), exception.getMessage());
        verify(membershipRepository, never()).depositCoin(anyString(), anyInt());
    }
}
