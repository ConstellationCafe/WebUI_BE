package com.help.erpweb.domain.modules.chatbot.learning.repository;

import com.help.erpweb.domain.modules.chatbot.learning.projection.LearningProjection;
import com.help.global.guild.GuildContext;
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
         * ëì  ì»¬ë¼ëª ê²ì¦
         */
        if (hasSearch && !allowedColumns.contains(searchColumn)) {
            throw new IllegalArgumentException(
                    "ê²ìí  ì ìë ì»¬ë¼ìëë¤: " + searchColumn
            );
        }

        if (hasSort && !allowedColumns.contains(sortColumn)) {
            throw new IllegalArgumentException(
                    "ì ë ¬í  ì ìë ì»¬ë¼ìëë¤: " + sortColumn
            );
        }

        /*
         * ì¼ë° ì¬ì©ìë ìì ì ë°ì´í°ë§ ì¡°ííê¸° ìí´
         * ì¤ì  DB teacher(SK)ë¥¼ ì ë¬ë°ëë¤.
         *
         * ê´ë¦¬ìë nullì´ë¯ë¡ ì ì²´ ì¡°ííë¤.
         */
        boolean filterByTeacher =
                teacher != null
                        && !teacher.isBlank();

        /*
         * ì¤ì:
         *
         * l.teacher(SK)ë FEë¡ ë³´ë´ì§ ìëë¤.
         *
         * Usersì JOINí´ì ì»ì discordIDë¥¼
         * LearningProjection.teacherì ë£ëë¤.
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
             * teacher ê²ìì ì£¼ìí´ì¼ íë¤.
             *
             * FEìì teacherë Discord IDì§ë§
             * DB metadataì teacherë SK ì»¬ë¼ì´ë¤.
             *
             * ë°ë¼ì teacher ê²ìë§ JOINë discordIDë¥¼ ì¬ì©íë¤.
             */
            if ("teacher".equals(searchColumn)) {
                sql.append(
                        " AND u.discordID = :searchValue AND u.bot_id = :botId"
                );

                /*
                 * count ì¿¼ë¦¬ìë Users JOIN íì
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
                        " AND u.discordID = :searchValue AND u.bot_id = :botId"
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
         * ì ë ¬
         */
        if (hasSort) {
            String direction =
                    "ASC".equalsIgnoreCase(sortDirection)
                            ? "ASC"
                            : "DESC";

            /*
             * teacherë FEìì Discord IDì´ë¯ë¡
             * Discord ID ê¸°ì¤ ì ë ¬
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
             * Pagination ê²°ê³¼ ìì ê³ ì 
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

            if ("teacher".equals(searchColumn)) {
                final String botId = GuildContext.requireBotId();
                query.setParameter("botId", botId);
                countQuery.setParameter("botId", botId);
            }
        }

        /*
         * Repository pageë 0-based
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
