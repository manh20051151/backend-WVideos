package com.example.backendWVideos.util;

/**
 * Tiện ích sinh slug (URL-friendly) từ chuỗi tiếng Việt có dấu
 */
public final class SlugUtils {

    private SlugUtils() {
    }

    /**
     * Chuyển chuỗi thành slug: bỏ dấu tiếng Việt, viết thường, khoảng trắng thành dấu gạch ngang
     * Trả về fallback nếu kết quả rỗng
     */
    public static String slugify(String input, String fallback) {
        String result = slugify(input);
        return result.isEmpty() ? fallback : result;
    }

    /**
     * Chuyển chuỗi thành slug: bỏ dấu tiếng Việt, viết thường, khoảng trắng thành dấu gạch ngang
     * Trả về chuỗi rỗng nếu không sinh được slug hợp lệ
     */
    public static String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        return java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "") // Bỏ dấu tiếng Việt
                .replaceAll("[đĐ]", "d")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }
}