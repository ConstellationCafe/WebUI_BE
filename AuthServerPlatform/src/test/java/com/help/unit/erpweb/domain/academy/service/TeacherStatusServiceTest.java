package com.help.erpweb.domain.academy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.help.erpweb.domain.academy.repository.AcademyClassRepository;
import com.help.erpweb.domain.academy.repository.AcademyRepository;
import com.help.erpweb.domain.academy.repository.TeacherRepository;
import com.help.erpweb.domain.modules.erp.point.repository.MembershipRepository;
import com.help.global.discord.identity.DiscordUserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TeacherStatusServiceTest {
	private AcademyRepository academyRepository;
	private AcademyClassRepository classRepository;
	private TeacherRepository teacherRepository;
	private TeacherStatusService service;

	@BeforeEach
	void setUp() {
		academyRepository = mock(AcademyRepository.class);
		classRepository = mock(AcademyClassRepository.class);
		teacherRepository = mock(TeacherRepository.class);
		service = new TeacherStatusService(
				academyRepository,
				classRepository,
				teacherRepository,
				mock(MembershipRepository.class),
				mock(DiscordUserRepository.class)
		);
	}

	@Test
	void statusOptionsIncludeSupportedSubjectsAndSkipUnscopedTeacherQueries() {
		when(academyRepository.findAll()).thenReturn(List.of());

		var response = service.getStatusOptions(null, null);

		assertThat(response).isNotNull();
		verify(academyRepository).findAll();
		verifyNoInteractions(classRepository, teacherRepository);
	}

	@Test
	void rejectsUnknownStatusBeforeQueryingTeachers() {
		assertThatThrownBy(() -> service.getTeacherStatuses(1, 2, null, "UNKNOWN", 1, 20))
				.isInstanceOf(IllegalArgumentException.class);
		verifyNoInteractions(teacherRepository);
	}
}
