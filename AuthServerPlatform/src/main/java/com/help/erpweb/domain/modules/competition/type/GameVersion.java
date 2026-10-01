package com.help.erpweb.domain.modules.competition.type;

import java.util.Arrays;

/**
 * 섀도우버스 게임 버전. DB({@code Competition.Winners.version})에는 {@link #value()}로 저장한다.
 * WebUI_FE {@code GameVersionType}과 같은 값을 쓴다.
 */
public enum GameVersion {
	S1("s1"),
	S2("s2");

	private final String value;

	GameVersion(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

	/** API·DB 값으로 찾는다. 모르는 값이면 null */
	public static GameVersion fromValue(String value) {
		return Arrays.stream(values())
			.filter(version -> version.value.equals(value))
			.findFirst()
			.orElse(null);
	}
}
