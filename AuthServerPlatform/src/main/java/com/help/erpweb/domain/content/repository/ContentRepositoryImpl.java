package com.help.erpweb.domain.content.repository;

import com.help.erpweb.domain.content.entity.ContentEntity;
import jakarta.persistence.*;
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
    public String callContentProcedure(String cardType,
                                       String membershipID,
                                       String cnValue) {

        StoredProcedureQuery sp = em.createStoredProcedureQuery("ChatBot.recommend_content");

        sp.registerStoredProcedureParameter("card_type", String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("membershipID", String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("cn_value", String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("recommend_result", String.class, ParameterMode.OUT);

        sp.setParameter("card_type", cardType);
        sp.setParameter("membershipID", membershipID);
        sp.setParameter("cn_value", cnValue);

        sp.execute();

        return (String) sp.getOutputParameterValue("recommend_result");
    }

    @Override
    public Page<ContentEntity> findPage(
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
        /*
         * 컬럼 검증
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
        boolean filterByRecommender = recommender != null
                                      && !recommender.isBlank();
        StringBuilder sql = new StringBuilder("""
            SELECT *
            FROM ChatBot.Content
            WHERE 1 = 1
        """);
        StringBuilder countSql = new StringBuilder("""
            SELECT COUNT(*)
            FROM ChatBot.Content
            WHERE 1 = 1
        """);

        if (filterByRecommender) {
            sql.append(
                    " AND recommender = :recommender"
            );
            countSql.append(
                    " AND recommender = :recommender"
            );
        }
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
        }
        Query query = em.createNativeQuery(
                sql.toString(),
                ContentEntity.class
        );
        Query countQuery = em.createNativeQuery(
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
        // page는 0-based
        query.setFirstResult(page * size);
        query.setMaxResults(size);

        @SuppressWarnings("unchecked")
        List<ContentEntity> content = query.getResultList();
        Number total = (Number) countQuery.getSingleResult();

        Pageable pageable = PageRequest.of(page, size);
        return new PageImpl<>(
                content,
                pageable,
                total.longValue()
        );
    }
}
