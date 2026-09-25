package com.help.erpweb.domain.learning.service;

import com.help.erpweb.domain.learning.dto.request.repository.LearningDto;
import com.help.erpweb.domain.learning.projection.LearningProjection;
import com.help.erpweb.domain.learning.repository.LearningRepository;
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

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LearningService {

    private final LearningRepository learningRepository;

    private final Authorization authorization;

    @PersistenceContext
    private final EntityManager entityManager;

    public ApiResponse<?> getLearningList(
            CustomUser user,
            int page,
            int size,
            String searchColumn,
            String searchValue,
            String sortColumn,
            String sortDirection
    ) {
        int normalizedPage =
                Math.max(page, 1);

        int normalizedSize =
                Math.max(size, 1);

        String teacher = null;

        /*
         * ì¼ë° ì¬ì©ì:
         * ìì ì ë°ì´í°ë§ ì¡°í
         *
         * ê´ë¦¬ì:
         * teacher = null
         * ì ì²´ ì¡°í
         */
        if (!authorization.isAdmin(user)) {
            teacher = findSkByChatUser(
                    ChatIdentities.fromPrincipal(user)
            );
        }

        /*
         * DB metadata ì¡°í
         */
        List<ColumnMetaDto> metadata =
                learningRepository
                        .findColumnMetas(
                                LearningRepository.schemaName,
                                LearningRepository.tableName
                        )
                        .stream()
                        .map(v ->
                                ColumnMetaDto.builder()
                                        .colName(
                                                v.getColName()
                                        )
                                        .isPrimary(
                                                v.getIsPrimary()
                                        )
                                        .isNullable(
                                                v.getIsNullable()
                                        )
                                        .build()
                        )
                        .toList();

        /*
         * ê²ì/ì ë ¬ íì© ì»¬ë¼
         */
        Set<String> allowedColumns =
                metadata.stream()
                        .map(
                                ColumnMetaDto::getColName
                        )
                        .collect(
                                Collectors.toSet()
                        );

        /*
         * API pageë 1-based
         * Repository pageë 0-based
         */
        Page<LearningProjection> learningPage =
                learningRepository.findPage(
                        teacher,
                        normalizedPage - 1,
                        normalizedSize,
                        searchColumn,
                        searchValue,
                        sortColumn,
                        sortDirection,
                        allowedColumns
                );

        /*
         * Projection.teacherìë
         * SKê° ìëë¼ Discord IDê° ë¤ì´ ìë¤.
         */
        List<LearningDto> learningList =
                learningPage
                        .getContent()
                        .stream()
                        .map(entity ->
                                LearningDto.builder()
                                        .lnKey(
                                                entity.getLnKey()
                                        )
                                        .lnValue(
                                                entity.getLnValue()
                                        )
                                        .teacher(
                                                entity.getTeacher()
                                        )
                                        .build()
                        )
                        .toList();

        Map<String, Object> body =
                new HashMap<>();

        body.put(
                "metadata",
                metadata
        );

        body.put(
                "entities",
                learningList
        );

        body.put(
                "page",
                normalizedPage
        );

        body.put(
                "size",
                normalizedSize
        );

        body.put(
                "totalElements",
                learningPage.getTotalElements()
        );

        body.put(
                "totalPages",
                learningPage.getTotalPages()
        );

        body.put(
                "hasNext",
                learningPage.hasNext()
        );

        return ApiResponse.success(
                body
        );
    }

    @Transactional
    public ApiResponse<?> saveAll(
            CustomUser user,
            List<LearningDto> learningList
    ) {
        if (learningList == null
                || learningList.isEmpty()) {

            return ApiResponse.success(
                    "No data to save"
            );
        }

        List<String> results =
                new ArrayList<>();

        for (LearningDto dto : learningList) {

            /*
             * dto.teacherë Discord ID
             *
             * Stored Procedure ë´ë¶ìì:
             *
             * search_sk(
             *     "discord",
             *     dto.teacher
             * )
             *
             * ë¥¼ íµí´ SKë¡ ë³ííë¤.
             */
            String result =
                    learningRepository.callLearningProcedure(
                            MembershipID.discord.name(),
                            dto.getTeacher(),
                            dto.getLnKey(),
                            dto.getLnValue()
                    );

            results.add(
                    dto.getLnKey()
                            + " íìµ ê²°ê³¼ : "
                            + result
            );
        }

        return ApiResponse.success(
                results
        );
    }

    @Transactional
    public ApiResponse<?> deleteAll(
            CustomUser user,
            List<LearningDto> learningList
    ) {
        if (learningList == null
                || learningList.isEmpty()) {

            return ApiResponse.success(
                    "No data to delete"
            );
        }

        int deleteCount = 0;

        /*
         * teacherë¥¼ ì¬ì©ìê° ìì í  ì ìì¼ë¯ë¡
         * ëª¨ë  íì teacherê° ê°ë¤ê³  ê°ì íë©´ ì ëë¤.
         *
         * ê° ì­ì  ëìë§ë¤:
         *
         * Discord ID
         *      â
         * search_sk()
         *      â
         * SK
         *      â
         * deleteByLnKey()
         */
        for (LearningDto dto : learningList) {

            String teacherSk =
                    findSkByChatUser(
                            ChatIdentities.ofDiscord(dto.getTeacher())
                    );

            int deleted =
                    learningRepository.deleteByLnKey(
                            teacherSk,
                            List.of(
                                    dto.getLnKey()
                            )
                    );

            deleteCount += deleted;
        }

        String result =
                "ì´ "
                        + learningList.size()
                        + "í ì¤ "
                        + deleteCount
                        + "í ì­ì ë¨";

        return ApiResponse.success(
                result
        );
    }

    /*
     * ChatUser -> Users.sk
     *
     * 기존 DB 함수 재사용. ChatUser를 받아 cardType/membershipId를 platform에서
     * 그대로 뽑아 쓰므로 discord뿐 아니라 다른 ChatUser 구현체가 생겨도 바뀌지 않는다.
     */
    private String findSkByChatUser(
            ChatUser chatUser
    ) {
        // ADR-0001: 기존 search_sk(cardType, membershipId)는 봇 저장소가 그대로 쓰므로
        // 그대로 두고, botId까지 포함해 sk를 조회하는 search_sk_by_bot(botId, cardType,
        // membershipId)를 별도로 새로 만들어 쓴다(봇 코드와의 시그니처 충돌 회피).
        // botId는 현재 요청의 GuildContext에서 가져온다.
        Object result =
                entityManager
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

        if (result == null) {
            throw new IllegalArgumentException(
                    "존재하지 않는 ID입니다: "
                            + chatUser.getExternalId()
            );
        }

        return result.toString();
    }
}
