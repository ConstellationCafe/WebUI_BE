package com.help.erpweb.domain.modules.chatbot.content.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.util.Set;

import com.help.erpweb.domain.modules.chatbot.content.service.ContentService;
import com.help.global.jwt.CustomUser;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.executable.ExecutableValidator;
import org.junit.jupiter.api.Test;

class ContentControllerValidationTest {

    @Test
    void rejectsPageSizeAboveMaximum() throws Exception {
        ContentController controller = new ContentController(mock(ContentService.class));
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
        ExecutableValidator validator = Validation.buildDefaultValidatorFactory()
                .getValidator()
                .forExecutables();

        Set<ConstraintViolation<ContentController>> violations =
                validator.validateParameters(
                        controller,
                        method,
                        new Object[] {null, 1, 101, null, null, null, "DESC"}
                );

        assertFalse(violations.isEmpty());
    }
}
