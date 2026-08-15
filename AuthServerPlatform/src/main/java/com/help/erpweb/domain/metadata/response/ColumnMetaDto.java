package com.help.erpweb.domain.metadata.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColumnMetaDto {
    public String colName;
    public Integer isPrimary;
    public Integer isNullable;
}
