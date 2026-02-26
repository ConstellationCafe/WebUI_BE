package com.help.backend.domain.metadata.entity;

public interface ColumnMetaView {
    String getColName();
    Integer getIsPrimary();
    Integer getIsNullable();
}