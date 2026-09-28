package com.help.erpweb.domain.modules.erp.point.repository;

import com.help.erpweb.domain.modules.erp.point.entity.PointLogEntity;
import com.help.erpweb.domain.metadata.repository.GlobalRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PointRepository
        extends JpaRepository<PointLogEntity, Long>, GlobalRepository {

    String schemaName = "Constellation_Network";
    String tableName = "PayLog";

    @Query(
            value = """
                    SELECT *
                    FROM Constellation_Network.PayLog
                    WHERE sk = :sk
                    ORDER BY at DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM Constellation_Network.PayLog
                    WHERE sk = :sk
                    """,
            nativeQuery = true
    )
    Page<PointLogEntity> findBySk(
            @Param("sk") String sk,
            Pageable pageable
    );
}
