package com.help.erpweb.domain.content.repository;

import com.help.erpweb.domain.content.projection.ContentProjection;
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
public class ContentRepositoryImpl implements ContentRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public String callContentProcedure(
            String cardType,
            String membershipID,
            String cnValue
    ) {
        StoredProcedureQuery sp =
                em.createStoredProcedureQuery(
                        "ChatBot.recommend_content"
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
                "cn_value",
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
        sp.setParameter("cn_value", cnValue);

        sp.execute();

        return (String) sp.getOutputParameterValue(
                "recommend_result"
        );
    }

    @Override
    public Page<ContentProjection> findPage(
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
                    "ê²ìí  ì ìë ì»¬ë¼ìëë¤: "
                            + searchColumn
            );
        }

        if (hasSort
                && !allowedColumns.contains(sortColumn)) {
            throw new IllegalArgumentException(
                    "ì ë ¬í  ì ìë ì»¬ë¼ìëë¤: "
                            + sortColumn
            );
        }

        boolean filterByRecommender =
                recommender != null
                        && !recommender.isBlank();

        StringBuilder sql = new StringBuilder("""
            SELECT
                c.cn_value,
                u.discordID
            FROM ChatBot.RecommendContent c
            LEFT JOIN Constellation_Network.Users u
                ON c.recommender = u.sk
            WHERE 1 = 1
        """);

        StringBuilder countSql = new StringBuilder("""
            SELECT COUNT(*)
            FROM ChatBot.RecommendContent c
            LEFT JOIN Constellation_Network.Users u
                ON c.recommender = u.sk
            WHERE 1 = 1
        """);

        if (filterByRecommender) {
            sql.append(
                    " AND c.recommender = :recommender"
            );

            countSql.append(
                    " AND c.recommender = :recommender"
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
                        " AND c.`"
                                + searchColumn
                                + "` = :searchValue"
                );

                countSql.append(
                        " AND c.`"
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
                        " ORDER BY c.`"
                                + sortColumn
                                + "` "
                                + direction
                );
            }
        } else {
            sql.append(
                    " ORDER BY c.cn_value ASC"
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

        List<ContentProjection> content =
                rows.stream()
                        .map(row ->
                                new ContentProjection(
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
