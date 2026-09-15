package com.help.erpweb.domain.learning.repository;

import com.help.erpweb.domain.learning.entity.LearningEntity;
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
    public Page<LearningEntity> findPage(
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

        // 동적 컬럼명 whitelist 검증
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

        StringBuilder sql = new StringBuilder("""
            SELECT *
            FROM ChatBot.Learning
            WHERE teacher = :teacher
            """);

        StringBuilder countSql = new StringBuilder("""
            SELECT COUNT(*)
            FROM ChatBot.Learning
            WHERE teacher = :teacher
            """);

        // 검색
        if (hasSearch) {
            sql.append(
                    " AND `"
                            + searchColumn
                            + "` = :searchValue"
            );

            countSql.append(
                    " AND `"
                            + searchColumn
                            + "` = :searchValue"
            );
        }

        // 정렬
        if (hasSort) {
            String direction =
                    "ASC".equalsIgnoreCase(sortDirection)
                            ? "ASC"
                            : "DESC";

            sql.append(
                    " ORDER BY `"
                            + sortColumn
                            + "` "
                            + direction
            );
        } else {
            /*
             * Pagination 결과가 매 요청마다 흔들리지 않도록
             * PK 기준 기본 정렬
             */
            sql.append(" ORDER BY `ln_key` ASC");
        }

        Query query =
                em.createNativeQuery(
                        sql.toString(),
                        LearningEntity.class
                );

        Query countQuery =
                em.createNativeQuery(
                        countSql.toString()
                );

        query.setParameter(
                "teacher",
                teacher
        );

        countQuery.setParameter(
                "teacher",
                teacher
        );

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

        // page는 0-based
        query.setFirstResult(
                page * size
        );

        query.setMaxResults(
                size
        );

        @SuppressWarnings("unchecked")
        List<LearningEntity> content =
                query.getResultList();

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