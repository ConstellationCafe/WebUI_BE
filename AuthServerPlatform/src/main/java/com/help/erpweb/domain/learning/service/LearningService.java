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
         * 일반 사용자:
         * 자신의 데이터만 조회
         *
         * 관리자:
         * teacher = null
         * 전체 조회
         */
        if (!authorization.isAdmin(user)) {
            teacher = findSkByDiscordId(
                    user.getUsername()
            );
        }

        /*
         * DB metadata 조회
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
         * 검색/정렬 허용 컬럼
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
         * API page는 1-based
         * Repository page는 0-based
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
         * Projection.teacher에는
         * SK가 아니라 Discord ID가 들어 있다.
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
             * dto.teacher는 Discord ID
             *
             * Stored Procedure 내부에서:
             *
             * search_sk(
             *     "discord",
             *     dto.teacher
             * )
             *
             * 를 통해 SK로 변환한다.
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
                            + " 학습 결과 : "
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
         * teacher를 사용자가 수정할 수 있으므로
         * 모든 행의 teacher가 같다고 가정하면 안 된다.
         *
         * 각 삭제 대상마다:
         *
         * Discord ID
         *      ↓
         * search_sk()
         *      ↓
         * SK
         *      ↓
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
                "총 "
                        + learningList.size()
                        + "행 중 "
                        + deleteCount
                        + "행 삭제됨";

        return ApiResponse.success(
                result
        );
    }

    /*
     * Discord ID → Users.sk
     *
     * 기존 DB 함수 재사용
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
                    "존재하지 않는 Discord ID입니다: "
                            + discordId
            );
        }

        return result.toString();
    }
}