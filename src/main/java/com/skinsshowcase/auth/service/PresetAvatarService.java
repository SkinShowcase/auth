package com.skinsshowcase.auth.service;

import com.skinsshowcase.auth.config.AuthPublicApiProperties;
import com.skinsshowcase.auth.dto.PresetAvatarOptionDto;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PresetAvatarService {

    public static final int PRESET_AVATAR_COUNT = 8;

    private static final String PRESET_AVATAR_FILE_EXTENSION = "jpg";

    private final AuthPublicApiProperties publicApiProperties;
    private final ResourceLoader resourceLoader;

    public PresetAvatarService(AuthPublicApiProperties publicApiProperties, ResourceLoader resourceLoader) {
        this.publicApiProperties = publicApiProperties;
        this.resourceLoader = resourceLoader;
    }

    public List<PresetAvatarOptionDto> listOptions() {
        var out = new ArrayList<PresetAvatarOptionDto>();
        for (var id = 1; id <= PRESET_AVATAR_COUNT; id++) {
            out.add(new PresetAvatarOptionDto(id, publicUrlFor(id)));
        }
        return out;
    }

    public String publicUrlFor(int presetId) {
        requireValidPresetId(presetId);
        var base = publicApiProperties.getBaseUrl().trim();
        if (base.endsWith("/")) {
            return base + "auth/avatars/" + presetId;
        }
        return base + "/auth/avatars/" + presetId;
    }

    public int requireValidPresetId(Integer presetId) {
        if (presetId == null) {
            throw new IllegalArgumentException("presetAvatarId required");
        }
        if (presetId < 1 || presetId > PRESET_AVATAR_COUNT) {
            throw new IllegalArgumentException("presetAvatarId must be between 1 and " + PRESET_AVATAR_COUNT);
        }
        return presetId;
    }

    public byte[] loadImageBytes(int presetId) {
        requireValidPresetId(presetId);
        var resource = resourceLoader.getResource(
                "classpath:preset-avatars/" + presetId + "." + PRESET_AVATAR_FILE_EXTENSION);
        if (!resource.exists()) {
            throw new IllegalStateException("Preset avatar asset missing: " + presetId);
        }
        return readAllBytes(resource);
    }

    private static byte[] readAllBytes(Resource resource) {
        try (var in = resource.getInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
