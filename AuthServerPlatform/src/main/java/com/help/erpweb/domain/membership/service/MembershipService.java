package com.help.erpweb.domain.membership.service;

import com.help.erpweb.domain.membership.dto.request.repository.PointLogDto;
import com.help.erpweb.domain.membership.entity.PointLogEntity;
import com.help.erpweb.domain.membership.repository.MembershipRepository;
import com.help.erpweb.domain.membership.repository.PointRepository;
import com.help.erpweb.domain.metadata.response.ColumnMetaDto;
import com.help.global.common.response.ApiResponse;
import com.help.global.jwt.CustomUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MembershipService {
    private final PointRepository pointRepository;
    private final MembershipRepository membershipRepository;

    public ApiResponse<?> getPointLog(
            CustomUser user,
            int page,
            int size
    ) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.max(size, 1);

        String discordId = user.getUsername();

        String sk =
                membershipRepository.findSkByDiscordId(
                        discordId
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
}
