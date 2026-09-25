package com.help.erpweb.domain.menu.service;

import com.help.erpweb.domain.menu.dto.request.repository.MenuDto;
import com.help.erpweb.domain.menu.projection.MenuProjection;
import com.help.erpweb.domain.menu.repository.MenuRepository;
import com.help.erpweb.domain.metadata.response.ColumnMetaDto;
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
public class MenuService {

    private final MenuRepository menuRepository;
    private final Authorization authorization;

    @PersistenceContext
    private final EntityManager entityManager;

    public ApiResponse<?> getMenuList(
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
                menuRepository.findColumnMetas(
                                MenuRepository.schemaName,
                                MenuRepository.tableName
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

        Page<MenuProjection> menuPage =
                menuRepository.findPage(
                        recommender,
                        normalizedPage - 1,
                        normalizedSize,
                        searchColumn,
                        searchValue,
                        sortColumn,
                        sortDirection,
                        allowedColumns
                );

        List<MenuDto> menuList =
                menuPage.getContent()
                        .stream()
                        .map(entity ->
                                MenuDto.builder()
                                        .mnValue(
                                                entity.getMnValue()
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
        body.put("entities", menuList);
        body.put("page", normalizedPage);
        body.put("size", normalizedSize);
        body.put(
                "totalElements",
                menuPage.getTotalElements()
        );
        body.put(
                "totalPages",
                menuPage.getTotalPages()
        );
        body.put(
                "hasNext",
                menuPage.hasNext()
        );

        return ApiResponse.success(body);
    }

    @Transactional
    public ApiResponse<?> saveAll(
            CustomUser user,
            List<MenuDto> menuList
    ) {
        if (menuList == null
                || menuList.isEmpty()) {
            return ApiResponse.success(
                    "No data to save"
            );
        }

        List<String> results =
                new ArrayList<>();

        for (MenuDto dto : menuList) {
            String result =
                    menuRepository.callMenuProcedure(
                            MembershipID.discord.name(),
                            dto.getRecommender(),
                            dto.getMnValue()
                    );

            results.add(
                    dto.getMnValue()
                            + " ì¶ì² ê²°ê³¼ : "
                            + result
            );
        }

        return ApiResponse.success(results);
    }

    @Transactional
    public ApiResponse<?> deleteAll(
            CustomUser user,
            List<MenuDto> menuList
    ) {
        if (menuList == null
                || menuList.isEmpty()) {
            return ApiResponse.success(
                    "No data to delete"
            );
        }

        int delCount = 0;

        Map<String, List<String>> valuesByRecommender =
                menuList.stream()
                        .collect(
                                Collectors.groupingBy(
                                        MenuDto::getRecommender,
                                        Collectors.mapping(
                                                MenuDto::getMnValue,
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
                    menuRepository.deleteByMnValue(
                            recommenderSk,
                            values
                    );
        }

        int totalCount = menuList.size();

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
