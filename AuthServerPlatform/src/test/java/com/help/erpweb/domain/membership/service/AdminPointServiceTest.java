package com.help.erpweb.domain.membership.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.help.erpweb.domain.membership.dto.request.AdminPointTransactionRequest;
import com.help.erpweb.domain.membership.dto.request.TransactionType;
import com.help.erpweb.domain.membership.dto.response.AdminPointMemberResponse;
import com.help.erpweb.domain.membership.exception.InsufficientCoinException;
import com.help.erpweb.domain.membership.repository.AdminPointRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminPointServiceTest {
    @Mock
    private AdminPointRepository repository;

    private AdminPointService service;

    @BeforeEach
    void setUp() {
        service = new AdminPointService(repository);
    }

    @Test
    void activeMembersAreReturnedWithPagination() {
        AdminPointMemberResponse member =
                new AdminPointMemberResponse("123", "별자리", "재적", 1000);
        when(repository.countActiveMembers("123")).thenReturn(21L);
        when(repository.findActiveMembers("123", 20, 20)).thenReturn(List.of(member));

        var response = service.getMembers("123", 2, 20);

        assertThat(response.items()).containsExactly(member);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    void depositUpdatesBalanceAndWritesPositiveLog() {
        when(repository.lockActiveMemberAndGetSk("123")).thenReturn("member-sk");
        when(repository.findCoin("member-sk")).thenReturn(1000);
        when(repository.findActiveMember("123"))
                .thenReturn(new AdminPointMemberResponse("123", "별자리", "재적", 1500));
        when(repository.countLogs("123")).thenReturn(1L);
        when(repository.findLogs("123", 0, 20)).thenReturn(List.of());

        var response = service.transact(
                "123",
                new AdminPointTransactionRequest(TransactionType.DEPOSIT, 500, "이벤트 지급")
        );

        assertThat(response.coin()).isEqualTo(1500);
        verify(repository).updateCoin("member-sk", 1500);
        verify(repository).insertLog(eq("member-sk"), eq(500), any(), eq("이벤트 지급"));
    }

    @Test
    void withdrawalCannotMakeBalanceNegative() {
        when(repository.lockActiveMemberAndGetSk("123")).thenReturn("member-sk");
        when(repository.findCoin("member-sk")).thenReturn(100);

        var request = new AdminPointTransactionRequest(
                TransactionType.WITHDRAW,
                200,
                "상품 구매"
        );

        assertThatThrownBy(() -> service.transact("123", request))
                .isInstanceOf(InsufficientCoinException.class);
        verify(repository, never()).updateCoin(any(), eq(-100));
        verify(repository, never()).insertLog(any(), eq(-200), any(), any());
    }
}
