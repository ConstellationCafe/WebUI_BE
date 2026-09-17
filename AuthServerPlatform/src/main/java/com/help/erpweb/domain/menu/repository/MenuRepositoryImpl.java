package com.help.erpweb.domain.menu.repository;

import com.help.erpweb.domain.menu.projection.MenuProjection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public class MenuRepositoryImpl
        implements MenuRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public String callMenuProcedure(
            String cardType,
            String membershipID,
            String mnValue
    ) {
        StoredProcedureQuery sp =
                em.createStoredProcedureQuery(
                        "ChatBot.recommend_menu"
                );

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
                "mn_value",
                String.class,
                ParameterMode.IN
        );

        sp.registerStoredProcedureParameter(
                "recommend_result",
                String.class,
                ParameterMode.OUT
        );

        sp.setParameter("card_type", cardType);
        sp.setParameter("membershipID", membershipID);
        sp.setParameter("mn_value", mnValue);

        sp.execute();

        return (String) sp.getOutputParameterValue(
                "recommend_result"
        );
    }

    @Override
    public Page<MenuProjection> findPage(
            String recommender,
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

        if (hasSearch
                && !allowedColumns.contains(searchColumn)) {
            throw new IllegalArgumentException(
                    "검색할 수 없는 컬럼입니다: "
                            + searchColumn
            );
        }

        if (hasSort
                && !allowedColumns.contains(sortColumn)) {
            throw new IllegalArgumentException(
                    "정렬할 수 없는 컬럼입니다: "
                            + sortColumn
            );
        }

        boolean filterByRecommender =
                recommender != null
                        && !recommender.isBlank();

        StringBuilder sql = new StringBuilder("""
            SELECT
                m.mn_value,
                u.discordID
            FROM ChatBot.RecommendMenu m
            LEFT JOIN Constellation_Network.Users u
                ON m.recommender = u.sk
            WHERE 1 = 1
        """);

        StringBuilder countSql = new StringBuilder("""
            SELECT COUNT(*)
            FROM ChatBot.RecommendMenu m
            LEFT JOIN Constellation_Network.Users u
                ON m.recommender = u.sk
            WHERE 1 = 1
        """);

        if (filterByRecommender) {
            sql.append(
                    " AND m.recommender = :recommender"
            );

            countSql.append(
                    " AND m.recommender = :recommender"
            );
        }

        if (hasSearch) {
            if ("recommender".equals(searchColumn)) {
                sql.append(
                        " AND u.discordID = :searchValue"
                );

                countSql.append(
                        " AND u.discordID = :searchValue"
                );
            } else {
                sql.append(
                        " AND m.`"
                                + searchColumn
                                + "` = :searchValue"
                );

                countSql.append(
                        " AND m.`"
                                + searchColumn
                                + "` = :searchValue"
                );
            }
        }

        if (hasSort) {
            String direction =
                    "ASC".equalsIgnoreCase(sortDirection)
                            ? "ASC"
                            : "DESC";

            if ("recommender".equals(sortColumn)) {
                sql.append(
                        " ORDER BY u.discordID "
                                + direction
                );
            } else {
                sql.append(
                        " ORDER BY m.`"
                                + sortColumn
                                + "` "
                                + direction
                );
            }
        } else {
            sql.append(
                    " ORDER BY m.mn_value ASC"
            );
        }

        Query query =
                em.createNativeQuery(sql.toString());

        Query countQuery =
                em.createNativeQuery(
                        countSql.toString()
                );

        if (filterByRecommender) {
            query.setParameter(
                    "recommender",
                    recommender
            );

            countQuery.setParameter(
                    "recommender",
                    recommender
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

        query.setFirstResult(page * size);
        query.setMaxResults(size);

        @SuppressWarnings("unchecked")
        List<Object[]> rows =
                query.getResultList();

        List<MenuProjection> content =
                rows.stream()
                        .map(row ->
                                new MenuProjection(
                                        (String) row[0],
                                        (String) row[1]
                                )
                        )
                        .toList();

        Number total =
                (Number) countQuery.getSingleResult();

        Pageable pageable =
                PageRequest.of(page, size);

        return new PageImpl<>(
                content,
                pageable,
                total.longValue()
        );
    }
}