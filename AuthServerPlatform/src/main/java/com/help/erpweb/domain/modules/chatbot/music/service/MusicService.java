package com.help.erpweb.domain.modules.chatbot.music.service;

import com.help.erpweb.domain.metadata.response.ColumnMetaDto;
import com.help.erpweb.domain.modules.chatbot.music.dto.request.repository.MusicDto;
import com.help.erpweb.domain.modules.chatbot.music.projection.MusicProjection;
import com.help.erpweb.domain.modules.chatbot.music.repository.MusicRepository;
import com.help.global.authorization.Authorization;
import com.help.global.chat.ChatIdentities;
import com.help.global.chat.ChatUser;
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
public class MusicService {

    private final MusicRepository musicRepository;
    private final Authorization authorization;

    @PersistenceContext
    private final EntityManager entityManager;

    public ApiResponse<?> getMusicList(
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
                    findSkByChatUser(
                            ChatIdentities.fromPrincipal(user)
                    );
        }

        List<ColumnMetaDto> metadata =
                musicRepository.findColumnMetas(
                                MusicRepository.schemaName,
                                MusicRepository.tableName
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

        Page<MusicProjection> musicPage =
                musicRepository.findPage(
                        recommender,
                        normalizedPage - 1,
                        normalizedSize,
                        searchColumn,
                        searchValue,
                        sortColumn,
                        sortDirection,
                        allowedColumns
                );

        List<MusicDto> musicList =
                musicPage.getContent()
                        .stream()
                        .map(entity ->
                                MusicDto.builder()
                                        .videoId(
                                                entity.getVideoId()
                                        )
                                        .recommender(
                                                entity.getRecommender()
                                        )
                                        .build()
                        )
                        .toList();

        Map<String, Object> body =
                new HashMap<>();

        body.put("metadata", metadata);
        body.put("entities", musicList);
        body.put("page", normalizedPage);
        body.put("size", normalizedSize);
        body.put(
                "totalElements",
                musicPage.getTotalElements()
        );
        body.put(
                "totalPages",
                musicPage.getTotalPages()
        );
        body.put(
                "hasNext",
                musicPage.hasNext()
        );

        return ApiResponse.success(body);
    }

    @Transactional
    public ApiResponse<?> saveAll(
            CustomUser user,
            List<MusicDto> musicList
    ) {
        if (musicList == null
                || musicList.isEmpty()) {
            return ApiResponse.success(
                    "No data to save"
            );
        }

        List<String> results =
                new ArrayList<>();

        for (MusicDto dto : musicList) {
            String result =
                    musicRepository.callMusicProcedure(
                            MembershipID.discord.name(),
                            dto.getRecommender(),
                            dto.getVideoId()
                    );

            results.add(
                    dto.getVideoId()
                            + " ì¶ì² ê²°ê³¼ : "
                            + result
            );
        }

        return ApiResponse.success(results);
    }

    @Transactional
    public ApiResponse<?> deleteAll(
            CustomUser user,
            List<MusicDto> musicList
    ) {
        if (musicList == null
                || musicList.isEmpty()) {
            return ApiResponse.success(
                    "No data to delete"
            );
        }

        int delCount = 0;

        Map<String, List<String>> valuesByRecommender =
                musicList.stream()
                        .collect(
                                Collectors.groupingBy(
                                        MusicDto::getRecommender,
                                        Collectors.mapping(
                                                MusicDto::getVideoId,
                                                Collectors.toList()
                                        )
                                )
                        );

        for (Map.Entry<String, List<String>> entry
                : valuesByRecommender.entrySet()) {

            String recommenderSk =
                    findSkByChatUser(
                            ChatIdentities.ofDiscord(entry.getKey())
                    );

            List<String> values =
                    entry.getValue()
                            .stream()
                            .distinct()
                            .toList();

            delCount +=
                    musicRepository.deleteByMnValue(
                            recommenderSk,
                            values
                    );
        }

        int totalCount = musicList.size();

        String result =
                "ì´ "
                        + totalCount
                        + "í ì¤ "
                        + delCount
                        + "í ì­ì ë¨";

        return ApiResponse.success(result);
    }

    /**
     * ADR-0001: 기존 search_sk(cardType, membershipId)는 봇 저장소가 그대로 쓰므로
     * 그대로 두고, botId까지 포함해 sk를 조회하는 search_sk_by_bot(botId, cardType,
     * membershipId)를 별도로 새로 만들어 쓴다(봇 코드와의 시그니처 충돌 회피).
     * botId는 현재 요청의 GuildContext에서 가져온다.
     * <p>
     * ChatUser를 받아 cardType/membershipId를 platform에서 그대로 뽑아
     * 쓰므로 discord뿐 아니라 다른 ChatUser 구현체가 생겨도 바뀌지 않는다.
     */
    private String findSkByChatUser(
            ChatUser chatUser
    ) {
        return (String) entityManager
                .createNativeQuery("""
                    SELECT Constellation_Network.search_sk_by_bot(
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
                        chatUser.getPlatform().name()
                )
                .setParameter(
                        "membershipId",
                        chatUser.getExternalId()
                )
                .getSingleResult();
    }
}
