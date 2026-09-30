package com.help.erpweb.domain.config.dto.response;

import java.util.List;

/** 메뉴에 필요한 모듈과 부가 기능 이름만 공개하고 원본 설정은 서버에 남긴다. */
public record ModuleConfigResponse(String moduleId, List<String> addOns) {
}
