package com.example.backendWVideos.enums;

import lombok.Getter;

@Getter
public enum VideoProvider {
    DOODSTREAM("doodstream"),
    STREAMTAPE("streamtape");

    private final String value;

    VideoProvider(String value) {
        this.value = value;
    }

    public static VideoProvider fromValue(String value) {
        if (value == null) {
            return STREAMTAPE;
        }
        for (VideoProvider provider : values()) {
            if (provider.value.equalsIgnoreCase(value)) {
                return provider;
            }
        }
        return STREAMTAPE;
    }
}
