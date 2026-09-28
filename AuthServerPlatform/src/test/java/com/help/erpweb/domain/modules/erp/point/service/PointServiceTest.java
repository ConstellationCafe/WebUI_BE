package com.help.erpweb.domain.modules.erp.point.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.help.erpweb.domain.modules.erp.point.dto.request.AdminPointTransactionRequest;
import com.help.erpweb.domain.modules.erp.point.dto.request.TransactionType;
import com.help.erpweb.domain.modules.erp.point.dto.response.AdminPointMemberResponse;
import com.help.erpweb.domain.modules.erp.point.dto.response.AdminPointLogResponse;
import com.help.erpweb.domain.metadata.entity.ColumnMetaView;
import com.help.erpweb.domain.metadata.response.ColumnMetaDto;
import com.help.erpweb.domain.modules.erp.point.dto.request.repository.PointLogDto;
import com.help.erpweb.domain.modules.erp.point.entity.PointLogEntity;
import com.help.erpweb.domain.modules.erp.point.exception.InsufficientCoinException;
import com.help.erpweb.domain.modules.erp.point.repository.AdminPointRepository;
import com.help.erpweb.domain.modules.erp.point.repository.MembershipRepository;
import com.help.erpweb.domain.modules.erp.point.repository.PointRepository;
import com.help.global.chat.ChatUser;
import com.help.global.common.response.ApiResponse;
import com.help.global.jwt.CustomUser;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * 2026-09-26: AdminPointServiceTest에서 이름을 바꿈(AdminPointService가
 * PointService로 합쳐짐에 따라). 관리자용 포인트 CRUD 동작에 더해, 본인 포인트
 * 로그 조회(getPointLog, 구 MembershipService.getPointLog)에 대한 테스트를
 * 추가했다 — 서비스 통합 시점까지 이 메서드는 테스트가 없었다.
 */
@ExtendWith(MockitoExtension.class)
class PointServiceTest {
    @Mock
    private AdminPointRepository repository;

    @Mock
    private PointRepository pointRepository;

    @Mock
    private MembershipRepository membershipRepository;

    private PointService service;

    @BeforeEach
    void setUp() {
        service = new PointService(repository, pointRepository, membershipRepository);
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

    @Test
    void descriptionEditOnlyUpdatesPayLog() {
        LocalDateTime at = LocalDateTime.of(2026, 9, 24, 3, 4, 5);
        prepareLogMutation(1200, 500, at);

        service.updateLog("123", 500, at, null, " 수정된 내역 ");

        verify(repository, never()).updateCoin(any(), anyInt());
        verify(repository).updateLog("member-sk", 500, at, 500, "수정된 내역");
    }

    @Test
    void amountEditAppliesOnlyDifferenceToCoinTable() {
        LocalDateTime at = LocalDateTime.of(2026, 9, 24, 3, 4, 5);
        prepareLogMutation(4200, -5800, at);
        when(repository.findCoin("member-sk")).thenReturn(4200);

        service.updateLog("123", -5800, at, -3000, null);

        verify(repository).updateCoin("member-sk", 7000);
        verify(repository).updateLog("member-sk", -5800, at, -3000, "기존 내역");
    }

    @Test
    void deletingWithdrawalRestoresItsAbsoluteValue() {
        LocalDateTime at = LocalDateTime.of(2026, 9, 24, 3, 4, 5);
        prepareLogMutation(4200, -5800, at);
        when(repository.findCoin("member-sk")).thenReturn(4200);

        service.deleteLog("123", -5800, at);

        verify(repository).updateCoin("member-sk", 10000);
        verify(repository).deleteLog("member-sk", -5800, at);
    }

    @Test
    void deletingDepositCannotMakeBalanceNegative() {
        LocalDateTime at = LocalDateTime.of(2026, 9, 24, 3, 4, 5);
        when(repository.lockActiveMemberAndGetSk("123")).thenReturn("member-sk");
        when(repository.lockLog("member-sk", 500, at))
                .thenReturn(new AdminPointLogResponse(500, at, "기존 내역"));
        when(repository.findCoin("member-sk")).thenReturn(100);

        assertThatThrownBy(() -> service.deleteLog("123", 500, at))
                .isInstanceOf(InsufficientCoinException.class);
        verify(repository, never()).updateCoin(any(), anyInt());
        verify(repository, never()).deleteLog(any(), anyInt(), any());
    }

    private void prepareLogMutation(int coin, int amount, LocalDateTime at) {
        when(repository.lockActiveMemberAndGetSk("123")).thenReturn("member-sk");
        when(repository.lockLog("member-sk", amount, at))
                .thenReturn(new AdminPointLogResponse(amount, at, "기존 내역"));
        when(repository.findActiveMember("123"))
                .thenReturn(new AdminPointMemberResponse("123", "별자리", "재적", coin));
        when(repository.countLogs("123")).thenReturn(1L);
        when(repository.findLogs("123", 0, 20)).thenReturn(List.of());
    }

    @Test
    void getPointLogReturnsOwnLogsScopedBySkAndExcludesSkColumnFromMetadata() {
        CustomUser user = CustomUser.of("123", "OAUTH_USER", "ROLE_USER", null);
        when(membershipRepository.findSkByChatUser(any(ChatUser.class)))
                .thenReturn("member-sk");
        when(pointRepository.findColumnMetas(
                eq(PointRepository.schemaName),
                eq(PointRepository.tableName)
        )).thenReturn(List.of(
                columnMeta("sk", 1, 0),
                columnMeta("amount", 0, 0),
                columnMeta("at", 0, 0)
        ));

        LocalDateTime at = LocalDateTime.of(2026, 9, 26, 10, 0);
        PointLogEntity entity = PointLogEntity.of("member-sk", 500, at, "충전");
        Page<PointLogEntity> page =
                new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1);
        when(pointRepository.findBySk(eq("member-sk"), any(Pageable.class)))
                .thenReturn(page);

        ApiResponse<?> response = service.getPointLog(user, 1, 20);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getResponse();

        @SuppressWarnings("unchecked")
        List<ColumnMetaDto> metadata = (List<ColumnMetaDto>) body.get("metadata");
        assertThat(metadata)
                .extracting(ColumnMetaDto::getColName)
                .containsExactly("amount", "at");

        @SuppressWarnings("unchecked")
        List<PointLogDto> entities = (List<PointLogDto>) body.get("entities");
        assertThat(entities).hasSize(1);
        assertThat(entities.get(0).getAmount()).isEqualTo("500");
        assertThat(entities.get(0).getDescription()).isEqualTo("충전");

        assertThat(body.get("page")).isEqualTo(1);
        assertThat(body.get("size")).isEqualTo(20);
        assertThat(body.get("totalElements")).isEqualTo(1L);
        assertThat(body.get("hasNext")).isEqualTo(false);
    }

    @Test
    void getPointLogNormalizesNonPositivePageAndSize() {
        CustomUser user = CustomUser.of("123", "OAUTH_USER", "ROLE_USER", null);
        when(membershipRepository.findSkByChatUser(any(ChatUser.class)))
                .thenReturn("member-sk");
        when(pointRepository.findColumnMetas(
                eq(PointRepository.schemaName),
                eq(PointRepository.tableName)
        )).thenReturn(List.of());
        when(pointRepository.findBySk(eq("member-sk"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        ApiResponse<?> response = service.getPointLog(user, 0, -5);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getResponse();
        assertThat(body.get("page")).isEqualTo(1);
        assertThat(body.get("size")).isEqualTo(1);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(pointRepository).findBySk(eq("member-sk"), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1);
    }

    private ColumnMetaView columnMeta(String colName, Integer isPrimary, Integer isNullable) {
        return new ColumnMetaView() {
            @Override
            public String getColName() {
                return colName;
            }

            @Override
            public Integer getIsPrimary() {
                return isPrimary;
            }

            @Override
            public Integer getIsNullable() {
                return isNullable;
            }
        };
    }
}
