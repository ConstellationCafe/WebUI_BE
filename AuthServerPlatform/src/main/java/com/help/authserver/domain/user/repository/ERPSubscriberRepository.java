package com.help.authserver.domain.user.repository;

import com.help.authserver.domain.user.entity.ErpSubscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ERPSubscriberRepository extends JpaRepository<ErpSubscriber, String> {
    List<ErpSubscriber> findByGuildIdIn(List<String> guildIds);
}