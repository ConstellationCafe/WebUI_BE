package com.help.erpweb.domain.membership.repository;

import com.help.erpweb.domain.membership.entity.PointLogEntity;
import com.help.erpweb.domain.metadata.repository.GlobalRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointRepository extends JpaRepository<PointLogEntity, Long>, GlobalRepository {
    String schemaName = "Constellation_Network";
    String tableName = "PayLog";

    @Query(value = "SELECT * FROM Constellation_Network.PayLog WHERE sk = :sk",
            nativeQuery = true)
    List<PointLogEntity> findBySk(@Param("sk") String sk);
}