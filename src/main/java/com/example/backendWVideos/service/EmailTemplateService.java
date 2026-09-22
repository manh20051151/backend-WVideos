package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.request.EmailTemplateUpdateRequest;
import com.example.backendWVideos.dto.response.EmailTemplateResponse;
import com.example.backendWVideos.entity.EmailTemplate;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.repository.EmailTemplateRepository;
import com.example.backendWVideos.util.EmailTemplates;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailTemplateService {

    final EmailTemplateRepository emailTemplateRepository;
    final SiteSettingService siteSettingService;

    /**
     * Kết quả sau khi render placeholder.
     */
    public record RenderedEmail(String subject, String body) {
    }

    /**
     * Lấy template (từ DB nếu admin đã chỉnh, ngược lại dùng mặc định) và render placeholder.
     * Tự động tiêm khối logo theo cấu hình của admin (placeholder {{logo_block}}).
     */
    public RenderedEmail getRendered(String key, Map<String, String> values) {
        EmailTemplate template = emailTemplateRepository.findByTemplateKey(key).orElse(null);

        String subject = template != null ? template.getSubject() : EmailTemplates.defaultSubject(key);
        String body = template != null ? template.getBody() : EmailTemplates.defaultBody(key);

        Map<String, String> allValues = new java.util.HashMap<>(values);
        String logoUrl = siteSettingService.getLogoUrl();
        allValues.put("logo_block", logoUrl != null
                ? EmailTemplates.logoImgHtml(logoUrl)
                : EmailTemplates.textLogoHtml());

        return new RenderedEmail(
                EmailTemplates.render(subject, allValues),
                EmailTemplates.render(body, allValues));
    }

    /**
     * Danh sách template cho admin: nội dung hiện tại + nội dung mặc định để đối chiếu.
     */
    public List<EmailTemplateResponse> getAllForAdmin() {
        List<EmailTemplateResponse> result = new ArrayList<>();
        for (String key : List.of(EmailTemplates.KEY_CONFIRMATION, EmailTemplates.KEY_RESET_PASSWORD)) {
            EmailTemplate template = emailTemplateRepository.findByTemplateKey(key).orElse(null);
            result.add(EmailTemplateResponse.builder()
                    .templateKey(key)
                    .subject(template != null ? template.getSubject() : EmailTemplates.defaultSubject(key))
                    .body(template != null ? template.getBody() : EmailTemplates.defaultBody(key))
                    .updatedAt(template != null ? template.getUpdatedAt() : null)
                    .defaultSubject(EmailTemplates.defaultSubject(key))
                    .defaultBody(EmailTemplates.defaultBody(key))
                    .customized(template != null)
                    .build());
        }
        return result;
    }

    /**
     * Admin cập nhật nội dung template (upsert).
     */
    @Transactional
    public EmailTemplateResponse update(String key, EmailTemplateUpdateRequest request) {
        if (!EmailTemplates.isValidKey(key)) {
            throw new AppException(ErrorCode.EMAIL_TEMPLATE_NOT_FOUND);
        }

        // Body phải chứa {{url}} để đảm bảo email luôn có link hành động
        if (!request.getBody().contains("{{url}}")) {
            throw new AppException(ErrorCode.EMAIL_TEMPLATE_MISSING_URL);
        }

        EmailTemplate template = emailTemplateRepository.findByTemplateKey(key).orElseGet(() ->
                EmailTemplate.builder().templateKey(key).build());

        template.setSubject(request.getSubject());
        template.setBody(request.getBody());
        template.setUpdatedAt(LocalDateTime.now());
        emailTemplateRepository.save(template);

        log.info("Admin cập nhật email template {}: subject='{}'", key, request.getSubject());
        return toResponse(template);
    }

    /**
     * Xóa bản chỉnh sửa, quay về template mặc định trong code.
     */
    @Transactional
    public EmailTemplateResponse reset(String key) {
        if (!EmailTemplates.isValidKey(key)) {
            throw new AppException(ErrorCode.EMAIL_TEMPLATE_NOT_FOUND);
        }
        emailTemplateRepository.deleteByTemplateKey(key);
        log.info("Admin reset email template {} về mặc định", key);
        return EmailTemplateResponse.builder()
                .templateKey(key)
                .subject(EmailTemplates.defaultSubject(key))
                .body(EmailTemplates.defaultBody(key))
                .updatedAt(null)
                .defaultSubject(EmailTemplates.defaultSubject(key))
                .defaultBody(EmailTemplates.defaultBody(key))
                .customized(false)
                .build();
    }

    private EmailTemplateResponse toResponse(EmailTemplate template) {
        return EmailTemplateResponse.builder()
                .templateKey(template.getTemplateKey())
                .subject(template.getSubject())
                .body(template.getBody())
                .updatedAt(template.getUpdatedAt())
                .defaultSubject(EmailTemplates.defaultSubject(template.getTemplateKey()))
                .defaultBody(EmailTemplates.defaultBody(template.getTemplateKey()))
                .customized(true)
                .build();
    }
}
