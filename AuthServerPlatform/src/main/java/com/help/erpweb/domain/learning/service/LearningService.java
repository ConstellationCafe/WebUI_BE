package com.help.erpweb.domain.learning.service;

import com.help.erpweb.domain.learning.dto.request.repository.LearningDto;
import com.help.erpweb.domain.learning.projection.LearningProjection;
import com.help.erpweb.domain.learning.repository.LearningRepository;
import com.help.erpweb.domain.metadata.response.ColumnMetaDto;
import com.help.global.authorization.Authorization;
import com.help.global.common.response.ApiResponse;
import com.help.global.data.MembershipID;
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
            teacher = findSkByDiscordId(
                    user.getUsername()
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
                    findSkByDiscordId(
                            dto.getTeacher()
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
     * Discord ID â Users.sk
     *
     * ê¸°ì¡´ DB í¨ì ì¬ì¬ì©
     */
    private String findSkByDiscordId(
            String discordId
    ) {
        Object result =
                entityManager
                        .createNativeQuery("""
                            SELECT Constellation_Network.search_sk(
                                :cardType,
                                :membershipId
                            )
                            """)
                        .setParameter(
                                "cardType",
                                MembershipID.discord.name()
                        )
                        .setParameter(
                                "membershipId",
                                discordId
                        )
                        .getSingleResult();

        if (result == null) {
            throw new IllegalArgumentException(
                    "ì¡´ì¬íì§ ìë Discord IDìëë¤: "
                            + discordId
            );
        }

        return result.toString();
    }
}
