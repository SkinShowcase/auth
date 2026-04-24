package com.skinsshowcase.auth.controller;

import com.skinsshowcase.auth.dto.PresetAvatarOptionDto;
import com.skinsshowcase.auth.service.PresetAvatarService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Пресетные аватарки (файлы на сервере). Список и отдача изображений публичны (без JWT).
 */
@RestController
@RequestMapping("/auth/avatars")
public class PresetAvatarController {

    private final PresetAvatarService presetAvatarService;

    public PresetAvatarController(PresetAvatarService presetAvatarService) {
        this.presetAvatarService = presetAvatarService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PresetAvatarOptionDto>> listPresets() {
        var body = presetAvatarService.listOptions();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(body);
    }

    @GetMapping(value = "/{presetId}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getPresetImage(@PathVariable int presetId) {
        var bytes = presetAvatarService.loadImageBytes(presetId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(24, TimeUnit.HOURS).cachePublic())
                .contentType(MediaType.IMAGE_JPEG)
                .body(bytes);
    }
}
