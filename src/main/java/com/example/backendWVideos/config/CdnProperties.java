package com.example.backendWVideos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "cdn")
@Getter
@Setter
public class CdnProperties {
    private String thumbnailDomain;
    private String publicDomain;

    public String convertThumbnailUrl(String url) {
        if (url == null || url.isEmpty()) return url;
        if (publicDomain == null) return url;
        
        // Replace các domain cũ về publicDomain
        if (url.contains("thumbcdn.com")) {
            return url.replace("thumbcdn.com", publicDomain);
        }
        if (url.contains("img.doodcdn.io")) {
            return url.replace("img.doodcdn.io", publicDomain);
        }
        if (url.contains("thumb.doodcdn.co")) {
            return url.replace("thumb.doodcdn.co", publicDomain);
        }
        return url;
    }
}