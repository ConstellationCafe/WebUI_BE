package com.help.erpweb.domain.modules.chatbot.content.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.help.erpweb.domain.modules.chatbot.content.dto.request.repository.ContentDto;
import com.help.erpweb.domain.modules.chatbot.content.service.ContentService;
import com.help.global.jwt.CustomUser;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.executable.ExecutableValidator;
import java.lang.reflect.Method;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ContentRequirementsValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void acceptsMaximumPageSize() throws Exception {
        ContentController controller = new ContentController(
                mock(ContentService.class)
        );
        Method method = ContentController.class.getMethod(
                "getContentList",
                CustomUser.class,
                int.class,
                int.class,
                String.class,
                String.class,
                String.class,
                String.class
        );
        ExecutableValidator executableValidator =
                validator.forExecutables();

        Set<ConstraintViolation<ContentController>> violations =
                executableValidator.validateParameters(
                        controller,
                        method,
                        new Object[] {null, 1, 100, null, null, null, "DESC"}
                );

        assertTrue(violations.isEmpty());
    }

    @Test
    void rejectsPageBelowOneAndPageSizeAboveMaximum() throws Exception {
        ContentController controller = new ContentController(
                mock(ContentService.class)
        );
        Method method = ContentController.class.getMethod(
                "getContentList",
                CustomUser.class,
                int.class,
                int.class,
                String.class,
                String.class,
                String.class,
                String.class
        );
        ExecutableValidator executableValidator =
                validator.forExecutables();

        Set<ConstraintViolation<ContentController>> violations =
                executableValidator.validateParameters(
                        controller,
                        method,
                        new Object[] {null, 0, 101, null, null, null, "DESC"}
                );

        assertFalse(violations.isEmpty());
        assertTrue(
                violations.stream().anyMatch(
                        violation -> violation.getPropertyPath()
                                .toString()
                                .contains("getContentList")
                )
        );
    }

    @Test
    void rejectsContentWithBlankRequiredFields() {
        ContentDto content = new ContentDto("", " ");

        assertFalse(validator.validate(content).isEmpty());
    }
}
