package com.skinsshowcase.auth.service;

import com.skinsshowcase.auth.config.AuthPublicApiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PresetAvatarServiceTest {

    private PresetAvatarService service() {
        var props = new AuthPublicApiProperties("http://localhost:8080");
        return new PresetAvatarService(props, new DefaultResourceLoader());
    }

    @Test
    void listOptions_hasEightEntriesWithSequentialIds() {
        var list = service().listOptions();

        assertThat(list).hasSize(PresetAvatarService.PRESET_AVATAR_COUNT);
        for (var i = 0; i < list.size(); i++) {
            assertThat(list.get(i).id()).isEqualTo(i + 1);
            assertThat(list.get(i).url()).contains("/auth/avatars/" + (i + 1));
        }
    }

    @Test
    void publicUrlFor_withAndWithoutTrailingSlash() {
        var withSlash = new PresetAvatarService(
                new AuthPublicApiProperties("http://api/"), new DefaultResourceLoader());
        assertThat(withSlash.publicUrlFor(1)).isEqualTo("http://api/auth/avatars/1");

        var noSlash = new PresetAvatarService(
                new AuthPublicApiProperties("http://api"), new DefaultResourceLoader());
        assertThat(noSlash.publicUrlFor(8)).isEqualTo("http://api/auth/avatars/8");
    }

    @Test
    void requireValidPresetId_rejectsNullAndOutOfRange() {
        var svc = service();

        assertThatThrownBy(() -> svc.requireValidPresetId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
        assertThatThrownBy(() -> svc.requireValidPresetId(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> svc.requireValidPresetId(9))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(svc.requireValidPresetId(1)).isEqualTo(1);
    }

    @Test
    void loadImageBytes_readsClasspathAsset() {
        var bytes = service().loadImageBytes(1);

        assertThat(bytes).isNotEmpty();
    }
}
