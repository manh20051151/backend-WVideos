package com.example.backendWVideos.config;

import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.repository.UserRepository;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Date;

/**
 * Xác thực JWT từ header Authorization của frame STOMP CONNECT,
 * gán principal (user id) để Spring định tuyến thông báo tới đúng người dùng (/user/...).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtStompInterceptor implements ChannelInterceptor {

    private final UserRepository userRepository;

    @Value("${jwt.signerKey}")
    private String signerKey;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            if (token != null && !token.isBlank()) {
                try {
                    SignedJWT signedJWT = SignedJWT.parse(token);
                    JWSVerifier verifier = new MACVerifier(signerKey.getBytes());
                    if (signedJWT.verify(verifier)) {
                        Date expiry = signedJWT.getJWTClaimsSet().getExpirationTime();
                        if (expiry == null || expiry.after(new Date())) {
                            String email = signedJWT.getJWTClaimsSet().getSubject();
                            User user = userRepository.findByEmail(email).orElse(null);
                            if (user != null) {
                                Authentication auth = new UsernamePasswordAuthenticationToken(
                                        user.getId(), null, java.util.List.of());
                                accessor.setUser(auth);
                                log.info("🔐 WebSocket authenticated cho user {}", user.getId());
                            }
                        }
                    }
                } catch (ParseException e) {
                    log.warn("WebSocket JWT parse lỗi: {}", e.getMessage());
                } catch (Exception e) {
                    log.warn("WebSocket JWT verify lỗi: {}", e.getMessage());
                }
            }
        }
        return message;
    }
}
