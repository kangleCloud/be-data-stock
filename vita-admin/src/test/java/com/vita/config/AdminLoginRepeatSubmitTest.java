package com.vita.config;

import com.vita.auth.dto.AuthLoginDto;
import com.vita.controller.auth.AuthController;
import com.vita.repeat.annotation.RepeatSubmit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminLoginRepeatSubmitTest {

    @Test
    void loginShouldNotUseRepeatSubmitWindow() throws NoSuchMethodException {
        var login = AuthController.class.getMethod("login", AuthLoginDto.class);
        assertThat(login.getAnnotation(RepeatSubmit.class)).isNull();
    }
}
