package com.help.backend.domain.metadata.repository;

import com.help.backend.domain.metadata.entity.ColumnMetaView;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class GlobalRepositoryImpl implements GlobalRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<ColumnMetaView> findColumnMetas(String schemaName, String tableName) {
        return em.createNativeQuery("""
            SELECT
                c.COLUMN_NAME AS colName,
                CASE WHEN k.COLUMN_NAME IS NOT NULL THEN 1 ELSE 0 END AS isPrimary,
                CASE WHEN c.IS_NULLABLE = 'YES' THEN 1 ELSE 0 END AS isNullable
            FROM INFORMATION_SCHEMA.COLUMNS c
            LEFT JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE k
                   ON k.TABLE_SCHEMA = c.TABLE_SCHEMA
                  AND k.TABLE_NAME = c.TABLE_NAME
                  AND k.COLUMN_NAME = c.COLUMN_NAME
                  AND k.CONSTRAINT_NAME = 'PRIMARY'
            WHERE c.TABLE_SCHEMA = :schemaName
              AND c.TABLE_NAME = :tableName
            ORDER BY c.ORDINAL_POSITION
        """, "ColumnMetaMapping") // 필요하면 SqlResultSetMapping
                .setParameter("schemaName", schemaName)
                .setParameter("tableName", tableName)
                .getResultList();
    }
}
