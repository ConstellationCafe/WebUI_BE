package com.help.erpweb.domain.config.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "module_config")
public class ModuleConfig {

    @EmbeddedId
    private ModuleConfigId id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "json")
    private JsonNode config;

    @Column(name = "last_update", nullable = false)
    private LocalDateTime lastUpdate;

    public String getBotId() {
        return id.getBotId();
    }

    public String getModuleId() {
        return id.getModuleId();
    }
}