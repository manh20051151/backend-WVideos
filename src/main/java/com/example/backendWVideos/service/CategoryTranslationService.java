package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.request.CategoryTranslationUpsertRequest;
import com.example.backendWVideos.dto.response.CategoryTranslationResponse;
import com.example.backendWVideos.entity.Category;
import com.example.backendWVideos.entity.CategoryTranslation;
import com.example.backendWVideos.entity.NewsCategory;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.repository.CategoryRepository;
import com.example.backendWVideos.repository.CategoryTranslationRepository;
import com.example.backendWVideos.repository.NewsCategoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dịch tự động TÊN danh mục (thể loại video + danh mục tin tức) sang nhiều ngôn ngữ
 * bằng Gemini. Danh mục là tập nhỏ, thay đổi hiếm nên:
 *
 * - Admin tạo mới/đổi tên -> dịch lại ngay (async, sau commit).
 * - Scheduler chạy định kỳ quét danh mục còn thiếu bản dịch để backfill/tự chữa lỗi mạng.
 * - API public GET /categories, /news-categories nhận Accept-Language
 *   -> trả tên đã bản địa hóa, fallback tên tiếng Việt gốc.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryTranslationService {

    public static final String OWNER_VIDEO = "video";
    public static final String OWNER_NEWS = "news";

    private final CategoryRepository categoryRepository;
    private final NewsCategoryRepository newsCategoryRepository;
    private final CategoryTranslationRepository categoryTranslationRepository;
    private final GeminiApiClient geminiApiClient;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Dịch 1 tên danh mục sang mọi ngôn ngữ đích (async). An toàn gọi lại nhiều lần.
     */
    @Async("taskExecutor")
    public void translateCategoryAsync(String ownerType, String ownerId, String name) {
        try {
            if (!geminiApiClient.isEnabled() || name == null || name.isBlank()) {
                return;
            }
            List<String> targets = geminiApiClient.getTargetLocales();
            if (targets.isEmpty() || categoryTranslationRepository.countByOwnerTypeAndOwnerId(ownerType, ownerId) >= targets.size()) {
                return;
            }

            log.info("🌐 [CategoryTranslation] Dịch danh mục {} {} ({})", ownerType, ownerId, name);
            String json = geminiApiClient.generateJson(buildBatchPrompt(List.of(name), targets));
            if (json == null) {
                return;
            }

            JsonNode translations = MAPPER.readTree(json).get(name);
            upsertOne(ownerType, ownerId, translations, targets);
        } catch (Exception e) {
            log.error("🌐 [CategoryTranslation] Lỗi dịch danh mục {} {}: {}", ownerType, ownerId, e.getMessage());
        }
    }

    /**
     * Gọi trong transaction sau khi tạo danh mục - dịch sau khi commit.
     */
    public void scheduleTranslate(String ownerType, String ownerId, String name) {
        registerAfterCommit(() -> translateCategoryAsync(ownerType, ownerId, name));
    }

    /**
     * Đổi tên danh mục -> xóa bản dịch cũ, dịch lại sau commit.
     */
    public void scheduleRetranslate(String ownerType, String ownerId, String newName) {
        if (!geminiApiClient.isEnabled()) {
            return;
        }
        categoryTranslationRepository.deleteByOwnerTypeAndOwnerId(ownerType, ownerId);
        registerAfterCommit(() -> translateCategoryAsync(ownerType, ownerId, newName));
    }

    /**
     * Xóa danh mục -> dọn bản dịch tránh bản mồ côi.
     */
    public void deleteTranslations(String ownerType, String ownerId) {
        categoryTranslationRepository.deleteByOwnerTypeAndOwnerId(ownerType, ownerId);
    }

    private record Pending(String ownerType, String ownerId, String name) {}

    /**
     * Backfill + tự chữa lành: dịch mọi danh mục còn thiếu bản dịch (async, 1 lần gọi batch).
     */
    @Async("taskExecutor")
    public void translateMissingCategoriesAsync() {
        try {
            if (!geminiApiClient.isEnabled()) {
                return;
            }
            List<String> targets = geminiApiClient.getTargetLocales();
            if (targets.isEmpty()) {
                return;
            }

            List<Pending> missing = new ArrayList<>();
            missing.addAll(collectMissing(OWNER_VIDEO, targets.size(),
                    categoryRepository.findAllActiveOrderBySortOrder().stream()
                            .collect(Collectors.toMap(Category::getId, Category::getName))));
            missing.addAll(collectMissing(OWNER_NEWS, targets.size(),
                    newsCategoryRepository.findAllActiveOrderBySortOrder().stream()
                            .collect(Collectors.toMap(NewsCategory::getId, NewsCategory::getName))));

            if (missing.isEmpty()) {
                return;
            }

            log.info("🌐 [CategoryTranslation] Backfill {} danh mục còn thiếu bản dịch", missing.size());

            // 1 lần gọi Gemini dịch batch toàn bộ tên còn thiếu
            List<String> names = missing.stream().map(Pending::name).distinct().toList();
            String json = geminiApiClient.generateJson(buildBatchPrompt(names, targets));
            if (json == null) {
                return;
            }
            JsonNode root = MAPPER.readTree(json);

            int saved = 0;
            for (Pending p : missing) {
                JsonNode entry = root.get(p.name());
                if (entry == null) {
                    continue;
                }
                // Chỉ chèn ngôn ngữ còn thiếu, không ghi đè bản dịch đã có
                // (bảo vệ bản dịch admin đã sửa tay cho ngôn ngữ khác của cùng danh mục)
                saved += upsertOne(p.ownerType(), p.ownerId(), entry, targets, false);
            }
            log.info("🌐 [CategoryTranslation] ✅ Backfill xong: {} bản dịch", saved);
        } catch (Exception e) {
            log.error("🌐 [CategoryTranslation] Lỗi backfill danh mục: {}", e.getMessage());
        }
    }

    /**
     * Tên đã bản địa hóa cho danh sách danh mục theo locale của request.
     * Trả Map<ownerId, tên đã dịch>; danh mục không có bản dịch thì không có trong map.
     */
    public Map<String, String> getLocalizedNames(String ownerType, List<String> ownerIds, Locale locale) {
        if (ownerIds == null || ownerIds.isEmpty() || locale == null) {
            return Map.of();
        }
        String contentLocale = resolveContentLocale(locale);
        if (contentLocale == null) {
            return Map.of();
        }
        return categoryTranslationRepository
                .findByOwnerTypeAndOwnerIdInAndLocale(ownerType, ownerIds, contentLocale)
                .stream()
                .collect(Collectors.toMap(CategoryTranslation::getOwnerId, CategoryTranslation::getName));
    }

    /**
     * Danh sách bản dịch của 1 danh mục cho trang admin quản lý:
     * trả đủ mọi ngôn ngữ đích, ngôn ngữ chưa dịch thì name = null.
     */
    public List<CategoryTranslationResponse> getTranslationsForAdmin(String ownerType, String ownerId) {
        List<String> targets = geminiApiClient.getTargetLocales();
        if (targets.isEmpty()) {
            return List.of();
        }
        Map<String, CategoryTranslation> existing = categoryTranslationRepository
                .findByOwnerTypeAndOwnerId(ownerType, ownerId).stream()
                .collect(Collectors.toMap(CategoryTranslation::getLocale, t -> t));
        return targets.stream()
                .map(locale -> {
                    CategoryTranslation t = existing.get(locale);
                    return CategoryTranslationResponse.builder()
                            .locale(locale)
                            .name(t != null ? t.getName() : null)
                            .updatedAt(t != null ? t.getUpdatedAt() : null)
                            .build();
                })
                .toList();
    }

    /**
     * Admin cập nhật bản dịch tên danh mục sau khi Gemini dịch (sửa lại cho tự nhiên,
     * dịch tay ngôn ngữ còn thiếu, hoặc xóa bản dịch sai - name để trống).
     */
    @org.springframework.transaction.annotation.Transactional
    public List<CategoryTranslationResponse> updateTranslations(
            String ownerType, String ownerId, List<CategoryTranslationUpsertRequest.Item> items) {
        List<String> targets = geminiApiClient.getTargetLocales();
        for (CategoryTranslationUpsertRequest.Item item : items) {
            if (item.getLocale() == null || targets.stream().noneMatch(t -> t.equalsIgnoreCase(item.getLocale()))) {
                throw new AppException(ErrorCode.UNSUPPORTED_TRANSLATION_LOCALE);
            }
        }

        for (CategoryTranslationUpsertRequest.Item item : items) {
            String locale = targets.stream()
                    .filter(t -> t.equalsIgnoreCase(item.getLocale())).findFirst().orElseThrow();
            String name = item.getName() != null ? item.getName().trim() : "";

            if (name.isEmpty()) {
                // Name rỗng -> xóa bản dịch, frontend sẽ fallback tên tiếng Việt gốc
                categoryTranslationRepository
                        .findFirstByOwnerTypeAndOwnerIdAndLocale(ownerType, ownerId, locale)
                        .ifPresent(categoryTranslationRepository::delete);
                continue;
            }

            CategoryTranslation t = categoryTranslationRepository
                    .findFirstByOwnerTypeAndOwnerIdAndLocale(ownerType, ownerId, locale)
                    .orElseGet(() -> CategoryTranslation.builder()
                            .ownerType(ownerType).ownerId(ownerId).locale(locale).build());
            t.setName(name);
            categoryTranslationRepository.save(t);
        }
        log.info("🌐 [CategoryTranslation] Admin cập nhật {} bản dịch cho danh mục {} {}",
                items.size(), ownerType, ownerId);

        return getTranslationsForAdmin(ownerType, ownerId);
    }

    /**
     * Chuẩn hóa locale của request về mã ngôn ngữ bản dịch đang lưu.
     * VD: "zh" hoặc "zh-TW" -> "zh-CN", "en-US" -> "en".
     */
    private String resolveContentLocale(Locale locale) {
        List<String> targets = geminiApiClient.getTargetLocales();
        for (String t : targets) {
            if (t.equalsIgnoreCase(locale.toString())) {
                return t;
            }
        }
        String lang = locale.getLanguage();
        for (String t : targets) {
            if (t.equalsIgnoreCase(lang) || t.toLowerCase().startsWith(lang.toLowerCase() + "-")) {
                return t;
            }
        }
        return null;
    }

    private List<Pending> collectMissing(String ownerType, int targetCount, Map<String, String> all) {
        if (all.isEmpty()) {
            return List.of();
        }
        Map<String, Long> counts = categoryTranslationRepository.countByOwnerGrouped(ownerType).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]));
        return all.entrySet().stream()
                .filter(e -> counts.getOrDefault(e.getKey(), 0L) < targetCount)
                .map(e -> new Pending(ownerType, e.getKey(), e.getValue()))
                .toList();
    }

    private int upsertOne(String ownerType, String ownerId, JsonNode translations, List<String> targets) {
        return upsertOne(ownerType, ownerId, translations, targets, true);
    }

    /**
     * @param overwriteExisting false = bỏ qua ngôn ngữ đã có bản dịch (backfill),
     *                          true = ghi đè (tạo mới / dịch lại sau khi đã xóa sạch).
     */
    private int upsertOne(String ownerType, String ownerId, JsonNode translations, List<String> targets,
                          boolean overwriteExisting) {
        if (translations == null) {
            return 0;
        }
        int saved = 0;
        for (String locale : targets) {
            JsonNode value = translations.get(locale);
            if (value == null || value.asText().isBlank()) {
                continue;
            }
            CategoryTranslation existing = categoryTranslationRepository
                    .findFirstByOwnerTypeAndOwnerIdAndLocale(ownerType, ownerId, locale).orElse(null);
            if (existing != null && !overwriteExisting) {
                continue;
            }
            CategoryTranslation t = existing != null ? existing
                    : CategoryTranslation.builder()
                            .ownerType(ownerType).ownerId(ownerId).locale(locale).build();
            t.setName(value.asText());
            categoryTranslationRepository.save(t);
            saved++;
        }
        return saved;
    }

    private String buildBatchPrompt(List<String> names, List<String> targets) {
        String namesJson = MAPPER.valueToTree(names).toString();
        return """
                Translate these video platform category names from Vietnamese into these languages: %s

                Rules:
                - Category names are short labels like "Hành động" (Action), "Giải trí" (Entertainment).
                - Keep translations short and natural as category labels.
                - Keep proper nouns and brand names unchanged.

                Names: %s

                Respond with ONLY a JSON object (no markdown, no extra text) keyed by the ORIGINAL name,
                with one entry per language code:
                {"Tên gốc": {"en": "...", "zh-CN": "..."}}
                """.formatted(String.join(", ", targets), namesJson);
    }

    private void registerAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }
}
