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

        boolean filterByTeacher =
                teacher != null
                        && !teacher.isBlank();

        StringBuilder sql = new StringBuilder("""
        SELECT
            l.ln_key,
            l.ln_value,
            l.teacher,
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

        if (hasSort) {
            String direction =
                    "ASC".equalsIgnoreCase(sortDirection)
                            ? "ASC"
                            : "DESC";

            sql.append(
                    " ORDER BY l.`"
                            + sortColumn
                            + "` "
                            + direction
            );
        } else {
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
                                        (String) row[0],
                                        (String) row[1],
                                        (String) row[2],
                                        (String) row[3]
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