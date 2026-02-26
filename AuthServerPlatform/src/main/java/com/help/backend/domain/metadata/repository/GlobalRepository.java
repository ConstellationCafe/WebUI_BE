package com.help.backend.domain.metadata.repository;

import com.help.backend.domain.metadata.entity.ColumnMetaView;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GlobalRepository {
    @Query(value = """
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
        """, nativeQuery = true)
    List<ColumnMetaView> findColumnMetas(
            @Param("schemaName") String schemaName,
            @Param("tableName") String tableName
    );
}
