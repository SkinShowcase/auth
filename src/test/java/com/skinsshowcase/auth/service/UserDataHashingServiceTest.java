package com.skinsshowcase.auth.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserDataHashingServiceTest {

    private final UserDataHashingService hashing = new UserDataHashingService();

    @Test
    void sha256_nullOrBlank_returnsNull() {
        assertThat(hashing.sha256(null)).isNull();
        assertThat(hashing.sha256("")).isNull();
        assertThat(hashing.sha256("   ")).isNull();
    }

    @Test
    void sha256_trimsInput() {
        var a = hashing.sha256("hello");
        var b = hashing.sha256("  hello  ");
        assertThat(a).isNotNull().isEqualTo(b);
    }

    @Test
    void sha256_knownVector() {
        // echo -n "test" | sha256sum
        assertThat(hashing.sha256("test"))
                .isEqualTo("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08");
    }
}
