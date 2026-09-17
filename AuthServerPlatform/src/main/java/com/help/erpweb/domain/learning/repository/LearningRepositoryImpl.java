package com.help.erpweb.domain.learning.repository;

import com.help.erpweb.domain.learning.projection.LearningProjection;
import jakarta.persistence.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public class LearningRepositoryImpl implements LearningRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public String callLearningProcedure(
            String cardType,
            String membershipID,
            String lnKey,
            String lnValue
    ) {
        StoredProcedureQuery sp =
                em.createStoredProcedureQuery("ChatBot.learning");

        sp.registerStoredProcedureParameter(
                "card_type",
                String.class,
                ParameterMode.IN
        );

        sp.registerStoredProcedureParameter(
                "membershipID",
                String.class,
                ParameterMode.IN
        );

        sp.registerStoredProcedureParameter(
                "ln_key",
                String.class,
                ParameterMode.IN
        );

        sp.registerStoredProcedureParameter(
                "ln_value",
                String.class,
                ParameterMode.IN
        );

        sp.registerStoredProcedureParameter(
                "recommend_result",
                String.class,
                ParameterMode.OUT
        );

        sp.setParameter(
                "card_type",
                cardType
        );

        sp.setParameter(
                "membershipID",
                membershipID
        );

        sp.setParameter(
                "ln_key",
                lnKey
        );

        sp.setParameter(
                "ln_value",
                lnValue
        );

        sp.execute();

        return (String) sp.getOutputParameterValue(
                "recommend_result"
        );
    }

    @Override
    public Page<LearningProjection> findPage(
            String teacher,
            int page,
            int size,
            String searchColumn,
            String searchValue,
            String sortColumn,
            String sortDirection,
            Set<String> allowedColumns
    ) {
        boolean hasSearch =
                searchColumn != null
                        && !searchColumn.isBlank()
                        && searchValue != null
                        && !searchValue.isBlank();

        boolean hasSort =
                sortColumn != null
                        && !sortColumn.isBlank();

        /*
         * 동적 컬럼명 검증
         */
        if (hasSearch && !allowedColumns.contains(searchColumn)) {
            throw new IllegalArgumentException(
                    "검색할 수 없는 컬럼입니다: " + searchColumn
            );
        }

        if (hasSort && !allowedColumns.contains(sortColumn)) {
            throw new IllegalArgumentException(
                    "정렬할 수 없는 컬럼입니다: " + sortColumn
            );
        }

        /*
         * 일반 사용자는 자신의 데이터만 조회하기 위해
         * 실제 DB teacher(SK)를 전달받는다.
         *
         * 관리자는 null이므로 전체 조회한다.
         */
        boolean filterByTeacher =
                teacher != null
                        && !teacher.isBlank();

        /*
         * 중요:
         *
         * l.teacher(SK)는 FE로 보내지 않는다.
         *
         * Users와 JOIN해서 얻은 discordID를
         * LearningProjection.teacher에 넣는다.
         */
        StringBuilder sql = new StringBuilder("""
            SELECT
                l.ln_key,
                l.ln_value,
                u.discordID
            FROM ChatBot.Learning l
            LEFT JOIN Constellation_Network.Users u
                ON l.teacher = u.sk
            WHERE 1 = 1
            """);

        StringBuilder countSql = new StringBuilder("""
            SELECT COUNT(*)
            FROM ChatBot.Learning l
            WHERE 1 = 1
            """);

        if (filterByTeacher) {
            sql.append(
                    " AND l.teacher = :teacher"
            );

            countSql.append(
                    " AND l.teacher = :teacher"
            );
        }

        if (hasSearch) {
            /*
             * teacher 검색은 주의해야 한다.
             *
             * FE에서 teacher는 Discord ID지만
             * DB metadata의 teacher는 SK 컬럼이다.
             *
             * 따라서 teacher 검색만 JOIN된 discordID를 사용한다.
             */
            if ("teacher".equals(searchColumn)) {
                sql.append(
                        " AND u.discordID = :searchValue"
                );

                /*
                 * count 쿼리에도 Users JOIN 필요
                 */
                countSql = new StringBuilder("""
                    SELECT COUNT(*)
                    FROM ChatBot.Learning l
                    LEFT JOIN Constellation_Network.Users u
                        ON l.teacher = u.sk
                    WHERE 1 = 1
                    """);

                if (filterByTeacher) {
                    countSql.append(
                            " AND l.teacher = :teacher"
                    );
                }

                countSql.append(
                        " AND u.discordID = :searchValue"
                );
            } else {
                sql.append(
                        " AND l.`"
                                + searchColumn
                                + "` = :searchValue"
                );

                countSql.append(
                        " AND l.`"
                                + searchColumn
                                + "` = :searchValue"
                );
            }
        }

        /*
         * 정렬
         */
        if (hasSort) {
            String direction =
                    "ASC".equalsIgnoreCase(sortDirection)
                            ? "ASC"
                            : "DESC";

            /*
             * teacher는 FE에서 Discord ID이므로
             * Discord ID 기준 정렬
             */
            if ("teacher".equals(sortColumn)) {
                sql.append(
                        " ORDER BY u.discordID "
                                + direction
                );
            } else {
                sql.append(
                        " ORDER BY l.`"
                                + sortColumn
                                + "` "
                                + direction
                );
            }
        } else {
            /*
             * Pagination 결과 순서 고정
             */
            sql.append(
                    " ORDER BY l.`ln_key` ASC"
            );
        }

        Query query =
                em.createNativeQuery(
                        sql.toString()
                );

        Query countQuery =
                em.createNativeQuery(
                        countSql.toString()
                );

        if (filterByTeacher) {
            query.setParameter(
                    "teacher",
                    teacher
            );

            countQuery.setParameter(
                    "teacher",
                    teacher
            );
        }

        if (hasSearch) {
            query.setParameter(
                    "searchValue",
                    searchValue
            );

            countQuery.setParameter(
                    "searchValue",
                    searchValue
            );
        }

        /*
         * Repository page는 0-based
         */
        query.setFirstResult(
                page * size
        );

        query.setMaxResults(
                size
        );

        @SuppressWarnings("unchecked")
        List<Object[]> rows =
                query.getResultList();

        List<LearningProjection> content =
                rows.stream()
                        .map(row ->
                                new LearningProjection(
                                        row[0] != null
                                                ? row[0].toString()
                                                : "",
                                        row[1] != null
                                                ? row[1].toString()
                                                : "",
                                        row[2] != null
                                                ? row[2].toString()
                                                : ""
                                )
                        )
                        .toList();

        Number total =
                (Number) countQuery.getSingleResult();

        Pageable pageable =
                PageRequest.of(
                        page,
                        size
                );

        return new PageImpl<>(
                content,
                pageable,
                total.longValue()
        );
    }
}