package com.help.backend.domain.membership.service;

import com.help.backend.domain.membership.dto.request.repository.PointLogDto;
import com.help.backend.domain.membership.repository.MembershipRepository;
import com.help.backend.domain.metadata.response.ColumnMetaDto;
import com.help.global.common.response.ApiResponse;
import com.help.global.data.MembershipID;
import com.help.global.jwt.CustomUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MembershipService {
    private final MembershipRepository membershipRepository;

    @PersistenceContext
    private final EntityManager entityManager;

    public ApiResponse<?> getPointLog(CustomUser user) {
        String discordId = user.getUsername();

        String sk = (String) entityManager
                .createNativeQuery("SELECT Constellation_Network.search_sk(:cardType, :membershipId)")
                .setParameter("cardType", MembershipID.discord.name())
                .setParameter("membershipId", discordId)
                .getSingleResult();

        List<ColumnMetaDto> metadata = membershipRepository.findColumnMetas(
                membershipRepository.schemaName,  membershipRepository.tableName)
                .stream()
                .map(v -> ColumnMetaDto.builder()
                        .colName(v.getColName())
                        .isPrimary(v.getIsPrimary())
                        .isNullable(v.getIsNullable())
                        .build())
                .toList();

        List<PointLogDto> membershipList = membershipRepository.findBySk(sk)
                .stream()
                .map(entity -> PointLogDto.builder()
                        .amount(entity.getAmount().toString())
                        .at(entity.getAt().toString())
                        .description(entity.getDescription())
                        .build())
                .toList();

        Map<String, Object> body = new HashMap<>();
        body.put("metadata", metadata);
        body.put("entities", membershipList);
        return ApiResponse.success(body);
    }
}
