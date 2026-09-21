package com.help.erpweb.domain.academy.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.help.erpweb.domain.academy.service.AcademyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AcademyControllerRequirementsTest {

    private AcademyService academyService;
    private AcademyController academyController;

    @BeforeEach
    void setUp() {
        academyService = mock(AcademyService.class);
        academyController = new AcademyController(academyService);
    }

    @Test
    void academyListEndpointDelegatesToService() {
        assertNotNull(academyController.getAcademies());

        verify(academyService).getAcademies();
    }

    @Test
    void subjectListEndpointDelegatesToService() {
        assertNotNull(academyController.getSubjects(1));

        verify(academyService).getSubjects(1);
    }
}
