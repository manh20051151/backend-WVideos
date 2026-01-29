package com.example.backendWVideos.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * Custom OAuth2AuthorizationRequestResolver để force HTTPS redirect URI
 * Giải quyết vấn đề redirect_uri_mismatch khi backend nhận HTTP request từ reverse proxy
 * Chỉ force HTTPS khi có X-Forwarded-Proto: https để tránh vòng lặp redirect
 */
public class CustomOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2AuthorizationRequestResolver.class);
    
    private final OAuth2AuthorizationRequestResolver defaultResolver;

    public CustomOAuth2AuthorizationRequestResolver(OAuth2AuthorizationRequestResolver defaultResolver) {
        this.defaultResolver = defaultResolver;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return resolve(request, null);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        String requestURI = request.getRequestURI();
        
        // Chỉ xử lý OAuth2 authorization requests
        if (!requestURI.contains("/oauth2/authorization/")) {
            // Không phải OAuth2 request, để default resolver xử lý
            return defaultResolver.resolve(request, clientRegistrationId);
        }
        
        // Extract clientRegistrationId từ URI nếu null
        if (clientRegistrationId == null || clientRegistrationId.isEmpty()) {
            String[] parts = requestURI.split("/oauth2/authorization/");
            if (parts.length > 1) {
                String extractedId = parts[1].split("/")[0].split("\\?")[0]; // Remove query params
                if (!extractedId.isEmpty()) {
                    clientRegistrationId = extractedId;
                }
            }
        }
        
        // Nếu vẫn không có clientRegistrationId, không thể resolve
        if (clientRegistrationId == null || clientRegistrationId.isEmpty()) {
            log.warn("Cannot extract clientRegistrationId from URI: {}", requestURI);
            return defaultResolver.resolve(request, null);
        }
        
        log.info("CustomOAuth2AuthorizationRequestResolver.resolve() called - clientRegistrationId: {}, URI: {}", 
                clientRegistrationId, requestURI);
        
        OAuth2AuthorizationRequest originalRequest = defaultResolver.resolve(request, clientRegistrationId);
        
        if (originalRequest == null) {
            log.error("Original OAuth2AuthorizationRequest is null - clientRegistrationId: {}, URI: {}. " +
                    "This may cause redirect loop!", clientRegistrationId, requestURI);
            // Không return null để tránh vòng lặp, để default resolver xử lý
            return defaultResolver.resolve(request, clientRegistrationId);
        }

        // Log headers để debug
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        String scheme = request.getScheme();
        String host = request.getServerName();
        String redirectUri = originalRequest.getRedirectUri();
        
        log.info("OAuth2 Authorization Request - Scheme: {}, Host: {}, X-Forwarded-Proto: {}, X-Forwarded-Host: {}, RedirectURI: {}", 
                scheme, host, forwardedProto, forwardedHost, redirectUri);

        // Luôn force HTTPS cho production domain để tránh vòng lặp redirect
        // Vì đã hardcode HTTPS trong application.yaml, nhưng vẫn check để đảm bảo
        if (redirectUri != null && redirectUri.startsWith("http://") && 
            (host != null && (host.contains("tailieudoc.me") || host.contains("api.tailieudoc.me")))) {
            String httpsRedirectUri = redirectUri.replace("http://", "https://");
            log.info("Converting redirect URI from HTTP to HTTPS: {} -> {}", redirectUri, httpsRedirectUri);
            
            return OAuth2AuthorizationRequest.from(originalRequest)
                    .redirectUri(httpsRedirectUri)
                    .build();
        }

        log.info("Keeping original redirect URI: {}", redirectUri);
        return originalRequest;
    }
}
