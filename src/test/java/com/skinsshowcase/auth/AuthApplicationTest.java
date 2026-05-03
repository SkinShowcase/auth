package com.skinsshowcase.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

class AuthApplicationTest {

    @Test
    void main_startsSpringApplication() {
        try (var mocked = mockStatic(SpringApplication.class)) {
            ConfigurableApplicationContext ctx = mock(ConfigurableApplicationContext.class);
            mocked.when(() -> SpringApplication.run(eq(AuthApplication.class), any(String[].class)))
                    .thenReturn(ctx);

            AuthApplication.main(new String[] {});

            mocked.verify(() -> SpringApplication.run(eq(AuthApplication.class), any(String[].class)), times(1));
        }
    }
}
