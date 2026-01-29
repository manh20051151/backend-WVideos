package com.example.backendWVideos.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.example.backendWVideos.dto.request.*;
import com.example.backendWVideos.dto.response.AuthenticationResponse;
import com.example.backendWVideos.entity.InvalidatedToken;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.dto.response.IntrospectResponse;
import com.example.backendWVideos.repository.InvalidatedTokenRepository;
import com.example.backendWVideos.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.time.temporal.ChronoUnit;
import java.util.StringJoiner;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    InvalidatedTokenRepository invalidatedTokenRepository;
    UserRepository userRepository;
    
    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;
    
    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected long REFRESHABLE_DURATION;

    @Transactional
    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.getToken();
        boolean isValid = true;
        try {
            verifyToken(token, false);
        } catch (AppException e) {
            isValid = false;
        }
        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }

    @Transactional
    public AuthenticationResponse authentication(AuthenticationRequest request) {
        // Dùng query bao gồm cả user bị khóa để phân biệt rõ hai trường hợp:
        // - Email không tồn tại -> USER_NOT_EXISTED
        // - Email tồn tại nhưng locked = true -> USER_LOCKED
        User user = userRepository.findByEmailIncludingLocked(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.isLocked()) {
            throw new AppException(ErrorCode.USER_LOCKED);
        }

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String token = generateToken(user);
        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }

    @Transactional
    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        try {
            var signToken = verifyToken(request.getToken(), true);

            String jit = signToken.getJWTClaimsSet().getJWTID();
            Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime();

            InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                    .id(jit)
                    .expiryTime(expiryTime)
                    .build();

            invalidatedTokenRepository.save(invalidatedToken);
        } catch (AppException exception) {
            log.info("Token already expired");
        }
    }

    @Transactional
    public AuthenticationResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException {
        var signedJWT = verifyToken(request.getToken(), true);

        var jit = signedJWT.getJWTClaimsSet().getJWTID();
        var expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .id(jit)
                .expiryTime(expiryTime)
                .build();

        invalidatedTokenRepository.save(invalidatedToken);

        // Subject trong token là email, không phải username
        var email = signedJWT.getJWTClaimsSet().getSubject();
        log.info("[refreshToken] Tìm user với email: {}", email);

        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[refreshToken] Không tìm thấy user với email: {}", email);
                    return new AppException(ErrorCode.UNAUTHENTICATED);
                });

        var token = generateToken(user);
        log.info("[refreshToken] Tạo token mới thành công cho user: {}", email);

        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }

    public String generateToken(OAuth2User oauth2User) {
        // Dùng query bao gồm cả user bị khóa để báo lỗi đúng khi login Google
        User user = userRepository.findByEmailIncludingLocked(oauth2User.getAttribute("email"))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Nếu tài khoản đã bị khóa thì không cho đăng nhập, trả về lỗi rõ ràng để FE hiển thị thông báo
        if (user.isLocked()) {
            throw new AppException(ErrorCode.USER_LOCKED);
        }

        return generateToken(user);
    }

    private String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getEmail())
                .issuer("manh")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()
                ))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", buildScope(user))
                .build();
        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);
        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Không thể tạo token");
            throw new RuntimeException(e);
        }
    }

    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");

        if (!CollectionUtils.isEmpty(user.getRoles())) {
            user.getRoles().forEach(role -> {
                stringJoiner.add("ROLE_" + role.getName());
                if (!CollectionUtils.isEmpty(role.getPermissions()))
                    role.getPermissions().forEach(permission -> stringJoiner.add(permission.getName()));
            });
        }
        return stringJoiner.toString();
    }

//    private SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
//        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());
//
//        SignedJWT signedJWT = SignedJWT.parse(token);
//
//        Date expiryTime = (isRefresh)
//                ? new Date(signedJWT.getJWTClaimsSet().getIssueTime()
//                .toInstant().plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS).toEpochMilli())
//                : signedJWT.getJWTClaimsSet().getExpirationTime();
//
//        var verified = signedJWT.verify(verifier);
//
//        if (!(verified && expiryTime.after(new Date()))) throw new AppException(ErrorCode.UNAUTHENTICATED);
//
//        if (invalidatedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID()))
//            throw new AppException(ErrorCode.UNAUTHENTICATED);
//
//        return signedJWT;
//    }

    @Transactional
    public AuthenticationResponse generateInfiniteToken(InfiniteTokenRequest request) {
        // Dùng query bao gồm cả user bị khóa để phân biệt rõ khi tạo infinite token
        User user = userRepository.findByEmailIncludingLocked(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.isLocked()) {
            throw new AppException(ErrorCode.USER_LOCKED);
        }

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String token = generateInfiniteToken(user);
        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }

    private String generateInfiniteToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getEmail())
                .issuer("manh")
                .issueTime(new Date())
                // Không set expirationTime để token không bao giờ hết hạn
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", buildScope(user))
                .claim("infinite", true)  // Thêm flag để đánh dấu đây là infinite token
                .build();
        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);
        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Không thể tạo infinite token");
            throw new RuntimeException(e);
        }
    }

    // Sửa lại phương thức verifyToken để hỗ trợ infinite token
    private SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);

        String jti = signedJWT.getJWTClaimsSet().getJWTID();
        log.info("[verifyToken] isRefresh={}, jti={}", isRefresh, jti);

        // Kiểm tra xem có phải infinite token không
        boolean isInfiniteToken = signedJWT.getJWTClaimsSet().getClaim("infinite") != null &&
                (boolean) signedJWT.getJWTClaimsSet().getClaim("infinite");

        Date expiryTime;
        if (isInfiniteToken) {
            // Nếu là infinite token, set thời gian hết hạn là 100 năm từ thời điểm hiện tại
            expiryTime = new Date(Instant.now().plus(36500, ChronoUnit.DAYS).toEpochMilli());
        } else {
            expiryTime = (isRefresh)
                    ? new Date(signedJWT.getJWTClaimsSet().getIssueTime()
                    .toInstant().plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS).toEpochMilli())
                    : signedJWT.getJWTClaimsSet().getExpirationTime();
        }

        var verified = signedJWT.verify(verifier);
        log.info("[verifyToken] verified={}, expiryTime={}, now={}", verified, expiryTime, new Date());

        if (!(verified && expiryTime.after(new Date()))) {
            log.warn("[verifyToken] Token không hợp lệ: verified={}, expired={}", verified, !expiryTime.after(new Date()));
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if (invalidatedTokenRepository.existsById(jti)) {
            log.warn("[verifyToken] Token đã bị invalidate: jti={}", jti);
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return signedJWT;
    }
}
