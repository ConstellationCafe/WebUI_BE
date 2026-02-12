package com.help.backend.domain.repository.entity;

public interface ColumnMetaView {
    String getColName();
    Integer getIsPrimary();
    Integer getIsNullable();
}