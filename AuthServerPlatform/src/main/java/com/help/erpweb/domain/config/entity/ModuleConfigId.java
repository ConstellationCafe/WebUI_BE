package com.help.erpweb.domain.config.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ModuleConfigId implements Serializable {

    @Column(name = "bot_id", length = 30, nullable = false)
    private String botId;

    @Column(name = "module_id", length = 30, nullable = false)
    private String moduleId;
}