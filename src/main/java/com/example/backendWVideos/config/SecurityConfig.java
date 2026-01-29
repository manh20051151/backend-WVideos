package com.example.backendWVideos.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.http.SessionCreationPolicy;
import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final String[] PUBLIC_ENDPOINTS = {
        "/users/register", 
        "/users/confirm",
            "/auth/token",
            "/auth/infinite-token",
            "/auth/introspect",
        "/auth/logout", 
        "/auth/refresh",
        "/oauth2/**",
        "/login/oauth2/**"
    };
    
    private final String[] PUBLIC_ENDPOINTS_GET = {
        "/users/confirm",
        "/users/confirm/**",
        "/oauth2/**",
        "/login/oauth2/**"
    };

    private final String[] PUBLIC_ENDPOINTS_ALL = {
            "/api/documents/all/**",
            "/api/documents/slug/**",  // Public endpoint để xem tài liệu theo slug
            "/api/documents/smart-search/**",
            "/api/documents/telegram-images/**",
            "/api/documents/*/view",  // Public endpoint để track view
            "/api/activities/**",  // Public endpoint cho hoạt động gần đây
            "/api/backup/**",
            "/api/sepay/all/**",
            "/api/favorites/count/document/**",  // Public endpoint để đếm favorite
            "/api/chat",  // Public endpoint cho chat với Groq
            "/api/vector-indexing/**",  // Public endpoint cho vector indexing (có thể cần auth sau)
            "/ws/**",  // WebSocket endpoint
            "/webhook/**"  // Webhook endpoint cho CI/CD deploy
    };

    private final CustomJwtDecoder customJwtDecoder;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    public SecurityConfig(CustomJwtDecoder customJwtDecoder, 
                         OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
                         ClientRegistrationRepository clientRegistrationRepository) {
        this.customJwtDecoder = customJwtDecoder;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(request ->
                request.requestMatchers(HttpMethod.POST, PUBLIC_ENDPOINTS).permitAll()
                    .requestMatchers(HttpMethod.GET, PUBLIC_ENDPOINTS_GET).permitAll()
                    .requestMatchers("/auth/**", "/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/socket.io/**").permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS_ALL).permitAll()
                        .requestMatchers("/api/documents/status/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/events/status/notoken").permitAll()
                    .requestMatchers(HttpMethod.GET, "/users/notoken/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/news/status/notoken").permitAll()
                    .requestMatchers(HttpMethod.POST, "/users/forgot-password/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/categories/**").permitAll()  // Public GET requests cho categories
                    .requestMatchers(HttpMethod.GET, "/api/news-categories/**").permitAll()  // Public GET requests cho news categories
                    .requestMatchers(HttpMethod.GET, "/api/news/**").permitAll()  // Public GET requests cho news
                    .requestMatchers(HttpMethod.POST, "/api/news/*/view").permitAll()  // Public POST để track view
                    .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwtConfigurer ->
                    jwtConfigurer.decoder(customJwtDecoder)
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
                .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
            )
            .oauth2Login(oauth2 -> oauth2
                .successHandler(oAuth2LoginSuccessHandler)
                .authorizationEndpoint(authorization -> authorization
                    .baseUri("/oauth2/authorization")
                    .authorizationRequestResolver(authorizationRequestResolver()))
                .redirectionEndpoint(redirection -> redirection
                    .baseUri("/oauth2/callback/*"))
            )
            // Disable default form login redirect cho API requests
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
            );

        return httpSecurity.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Collections.singletonList(frontendUrl));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        configuration.setExposedHeaders(Arrays.asList(
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials",
            "Authorization",
            "Content-Disposition",
            "Content-Type"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public OAuth2AuthorizationRequestResolver authorizationRequestResolver() {
        DefaultOAuth2AuthorizationRequestResolver defaultResolver = 
            new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository,
                "/oauth2/authorization");
        return new CustomOAuth2AuthorizationRequestResolver(defaultResolver);
    }
}