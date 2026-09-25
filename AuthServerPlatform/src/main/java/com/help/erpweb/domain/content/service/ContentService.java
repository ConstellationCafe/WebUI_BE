package com.help.erpweb.domain.content.service;

import com.help.erpweb.domain.content.dto.request.repository.ContentDto;
import com.help.erpweb.domain.content.projection.ContentProjection;
import com.help.erpweb.domain.content.repository.ContentRepository;
import com.help.erpweb.domain.metadata.response.ColumnMetaDto;
import com.help.global.authorization.Authorization;
import com.help.global.common.response.ApiResponse;
import com.help.global.data.MembershipID;
import com.help.global.guild.GuildContext;
import com.help.global.jwt.CustomUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentService {

    private final ContentRepository contentRepository;
    private final Authorization authorization;

    @PersistenceContext
    private final EntityManager entityManager;

    public ApiResponse<?> getContentList(
            CustomUser user,
            int page,
            int size,
            String searchColumn,
            String searchValue,
            String sortColumn,
            String sortDirection
    ) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.max(size, 1);

        String recommender = null;

        if (!authorization.isAdmin(user)) {
            recommender =
                    findSkByDiscordId(
                            user.getUsername()
                    );
        }

        List<ColumnMetaDto> metadata =
                contentRepository.findColumnMetas(
                                ContentRepository.schemaName,
                                ContentRepository.tableName
                        )
                        .stream()
                        .map(v ->
                                ColumnMetaDto.builder()
                                        .colName(v.getColName())
                                        .isPrimary(v.getIsPrimary())
                                        .isNullable(v.getIsNullable())
                                        .build()
                        )
                        .toList();

        Set<String> allowedColumns =
                metadata.stream()
                        .map(ColumnMetaDto::getColName)
                        .collect(Collectors.toSet());

        Page<ContentProjection> contentPage =
                contentRepository.findPage(
                        recommender,
                        normalizedPage - 1,
                        normalizedSize,
                        searchColumn,
                        searchValue,
                        sortColumn,
                        sortDirection,
                        allowedColumns
                );

        List<ContentDto> contentList =
                contentPage
                        .getContent()
                        .stream()
                        .map(entity ->
                                ContentDto.builder()
                                        .cnValue(
                                                entity.getCnValue()
                                        )
                                        // APIì recommenderë Discord ID
                                        .recommender(
                                                entity.getRecommender()
                                        )
                                        .build()
                        )
                        .toList();

        Map<String, Object> body =
                new HashMap<>();

        body.put("metadata", metadata);
        body.put("entities", contentList);
        body.put("page", normalizedPage);
        body.put("size", normalizedSize);
        body.put(
                "totalElements",
                contentPage.getTotalElements()
        );
        body.put(
                "totalPages",
                contentPage.getTotalPages()
        );
        body.put(
                "hasNext",
                contentPage.hasNext()
        );

        return ApiResponse.success(body);
    }

    @Transactional
    public ApiResponse<?> saveAll(
            CustomUser user,
            List<ContentDto> contentList
    ) {
        if (contentList == null
                || contentList.isEmpty()) {
            return ApiResponse.success(
                    "No data to save"
            );
        }

        List<String> results =
                new ArrayList<>();

        for (ContentDto dto : contentList) {
            String result =
                    contentRepository.callContentProcedure(
                            MembershipID.discord.name(),

                            // DTO recommender = Discord ID
                            dto.getRecommender(),

                            dto.getCnValue()
                    );

            results.add(
                    dto.getCnValue()
                            + " ì¶ì² ê²°ê³¼ : "
                            + result
            );
        }

        return ApiResponse.success(results);
    }

    @Transactional
    public ApiResponse<?> deleteAll(
            CustomUser user,
            List<ContentDto> contentList
    ) {
        if (contentList == null
                || contentList.isEmpty()) {
            return ApiResponse.success(
                    "No data to delete"
            );
        }

        int delCount = 0;

        Map<String, List<String>> valuesByRecommender =
                contentList.stream()
                        .collect(
                                Collectors.groupingBy(
                                        ContentDto::getRecommender,
                                        Collectors.mapping(
                                                ContentDto::getCnValue,
                                                Collectors.toList()
                                        )
                                )
                        );

        for (Map.Entry<String, List<String>> entry
                : valuesByRecommender.entrySet()) {

            String recommenderSk =
                    findSkByDiscordId(
                            entry.getKey()
                    );

            List<String> values =
                    entry.getValue()
                            .stream()
                            .distinct()
                            .toList();

            delCount +=
                    contentRepository.deleteByMnValue(
                            recommenderSk,
                            values
                    );
        }

        int totalCount =
                contentList.size();

        String result =
                "ì´ "
                        + totalCount
                        + "í ì¤ "
                        + delCount
                        + "í ì­ì ë¨";

        return ApiResponse.success(result);
    }

    /**
     * ADR-0001: search_sk는 이제 botId까지 포함해 sk를 조회한다.
     * botId는 현재 요청의 GuildContext에서 가져온다.
     */
    private String findSkByDiscordId(
            String discordId
    ) {
        return (String) entityManager
                .createNativeQuery("""
                    SELECT Constellation_Network.search_sk(
                        :botId,
                        :cardType,
                        :membershipId
                    )
                """)
                .setParameter(
                        "botId",
                        GuildContext.requireBotId()
                )
                .setParameter(
                        "cardType",
                        MembershipID.discord.name()
                )
                .setParameter(
                        "membershipId",
                        discordId
                )
                .getSingleResult();
    }
}
