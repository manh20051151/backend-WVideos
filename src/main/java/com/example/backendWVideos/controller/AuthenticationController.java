package com.example.backendWVideos.controller;

import com.nimbusds.jose.JOSEException;
import com.example.backendWVideos.dto.request.*;
import com.example.backendWVideos.dto.response.AuthenticationResponse;
import com.example.backendWVideos.dto.response.IntrospectResponse;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.service.AuthenticationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;

import java.text.ParseException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    AuthenticationService authenticationService;

    @PostMapping("/token")
    @CircuitBreaker(name = "authentication", fallbackMethod = "authenticationFallback")
    @RateLimiter(name = "authentication")
    @Retry(name = "authentication")
    ApiResponse<AuthenticationResponse> authenticationResponseApiResponse(@RequestBody AuthenticationRequest request) {
        var result = authenticationService.authentication(request);
        return ApiResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }

    @PostMapping("/introspect")
    @CircuitBreaker(name = "introspect", fallbackMethod = "introspectFallback")
    @RateLimiter(name = "introspect")
    @Retry(name = "introspect")
    ApiResponse<IntrospectResponse> authenticationResponseApiResponse(@RequestBody IntrospectRequest request) throws ParseException, JOSEException {
        var result = authenticationService.introspect(request);
        return ApiResponse.<IntrospectResponse>builder()
                .result(result)
                .build();
    }

    @PostMapping("/refresh")
    @CircuitBreaker(name = "refresh", fallbackMethod = "refreshFallback")
    @RateLimiter(name = "refresh")
    @Retry(name = "refresh")
    ApiResponse<AuthenticationResponse> authenticate(@RequestBody RefreshRequest request)
            throws ParseException, JOSEException {
        var result = authenticationService.refreshToken(request);
        return ApiResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout(@RequestBody LogoutRequest request)
            throws ParseException, JOSEException {
        authenticationService.logout(request);
        return ApiResponse.<Void>builder()
                .build();
    }

    @PostMapping("/infinite-token")
    @PreAuthorize("hasRole('ADMIN')")  // Chỉ ADMIN mới có quyền tạo infinite token
    @CircuitBreaker(name = "authentication", fallbackMethod = "authenticationFallback")
    @RateLimiter(name = "authentication")
    @Retry(name = "authentication")
    ApiResponse<AuthenticationResponse> generateInfiniteToken(@RequestBody InfiniteTokenRequest request) {
        var result = authenticationService.generateInfiniteToken(request);
        return ApiResponse.<AuthenticationResponse>builder()
                .code(1000)
                .message("Tạo token vô hạn thành công")
                .result(result)
                .build();
    }

    // Fallback methods
    private ApiResponse<AuthenticationResponse> authenticationFallback(AuthenticationRequest request, Exception e) {
        if (e instanceof AppException) {
            throw (AppException) e;
        }
        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    private ApiResponse<IntrospectResponse> introspectFallback(IntrospectRequest request, Exception e) {
        if (e instanceof AppException) {
            throw (AppException) e;
        }
        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    private ApiResponse<AuthenticationResponse> refreshFallback(RefreshRequest request, Exception e) {
        if (e instanceof AppException) {
            throw (AppException) e;
        }
        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
}
