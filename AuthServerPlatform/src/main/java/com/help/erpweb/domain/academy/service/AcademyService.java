package com.help.erpweb.domain.academy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.help.authserver.domain.user.entity.config.ErpSubscriber;
import com.help.erpweb.domain.config.entity.ModuleConfig;
import com.help.authserver.domain.user.repository.config.ERPSubscriberRepository;
import com.help.erpweb.domain.config.repository.ModuleConfigRepository;
import com.help.erpweb.domain.academy.dto.AcademyResponse;
import com.help.erpweb.domain.academy.dto.ClassResponse;
import com.help.erpweb.domain.academy.dto.TeacherResponse;
import com.help.erpweb.domain.academy.entity.AcademyClass;
import com.help.erpweb.domain.academy.entity.Teacher;
import com.help.erpweb.domain.academy.repository.AcademyClassRepository;
import com.help.erpweb.domain.academy.repository.AcademyRepository;
import com.help.erpweb.domain.academy.repository.TeacherRepository;
import com.help.erpweb.domain.membership.repository.MembershipRepository;
import com.help.global.jwt.CustomUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademyService {
    private static final String MODULE_ID = "network_operations";

    private final AcademyRepository academyRepository;
    private final AcademyClassRepository academyClassRepository;
    private final TeacherRepository teacherRepository;
    private final ModuleConfigRepository moduleConfigRepository;
    private final ERPSubscriberRepository erpSubscriberRepository;
    private final MembershipRepository membershipRepository;

    public List<AcademyResponse> getAcademies() {
        return academyRepository.findAll()
                .stream()
                .map(academy ->
                        new AcademyResponse(
                                academy.getId(),
                                academy.getName()
                        )
                )
                .toList();
    }

    public List<ClassResponse> getClasses(
            Integer academyId
    ) {
        return academyClassRepository
                .findByAcademy_Id(academyId)
                .stream()
                .map(academyClass ->
                        new ClassResponse(
                                academyClass.getId(),
                                academyClass.getClassNumber()
                        )
                )
                .toList();
    }

    public List<TeacherResponse> getTeachers(
            Integer academyId
    ) {
        return teacherRepository
                .findByAcademyClass_Academy_Id(academyId)
                .stream()
                .map(this::toTeacherResponse)
                .toList();
    }

    /**
     * 로그인 사용자의 Discord ID
     * → Constellation_Network.search_sk()
     * → SK
     * → Teachers / Students
     *
     * 현재는 SK 조회까지만 이 서비스에서 제공.
     */
    public String findUserSk(
            CustomUser user
    ) {
        return membershipRepository.findSkByDiscordId(user.getUsername());
    }

    /**
     * ConfigDB에서 network_operations의
     * academy 설정을 가져온다.
     */
    public JsonNode getAcademyConfig(
            String guildId
    ) {
        ErpSubscriber subscriber = erpSubscriberRepository
                .findByGuildId(guildId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "등록된 guild가 아닙니다. guildId=" + guildId
                        )
                );

        ModuleConfig moduleConfig =
                moduleConfigRepository
                        .findByIdBotIdAndIdModuleId(
                                subscriber.getBotId(),
                                MODULE_ID
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "network_operations 설정이 없습니다."
                                )
                        );

        JsonNode config = moduleConfig.getConfig();

        return config
                .path("config")
                .path("add_on")
                .path("academy");
    }

    private TeacherResponse toTeacherResponse(
            Teacher teacher
    ) {
        AcademyClass academyClass =
                teacher.getAcademyClass();

        /*
         * Teacher 테이블에는 이름이 없기 때문에
         * 여기서는 일단 SK를 반환하지 않고 id만 사용.
         *
         * 실제 이름은 Constellation_Network.Users를
         * SK 기준으로 조회해서 넣어야 한다.
         */
        return new TeacherResponse(
                teacher.getId(),
                teacher.getSk(),
                academyClass != null
                        ? academyClass.getId()
                        : null,
                academyClass != null
                        ? academyClass.getClassNumber()
                        : null
        );
    }
}