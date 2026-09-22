package com.example.backendWVideos.util;

import java.util.Map;

/**
 * Template HTML email (đăng ký, quên mật khẩu) - thiết kế table-based, inline CSS
 * để hiển thị nhất quán trên Gmail/Outlook/Apple Mail.
 *
 * Template mặc định chứa placeholder dạng {{ten}} - admin có thể chỉnh sửa nội dung
 * lưu trong DB (bảng email_templates), hệ thống render placeholder khi gửi:
 *   {{email}}    - email người nhận
 *   {{name}}     - tên hiển thị người nhận
 *   {{url}}      - link hành động (bắt buộc phải có trong body)
 *   {{minutes}}  - thời hạn token (phút)
 */
public final class EmailTemplates {

    private EmailTemplates() {
    }

    public static final String KEY_CONFIRMATION = "CONFIRMATION";
    public static final String KEY_RESET_PASSWORD = "RESET_PASSWORD";

    public static final String DEFAULT_SUBJECT_CONFIRMATION = "Xác nhận đăng ký tài khoản WVideos";
    public static final String DEFAULT_SUBJECT_RESET_PASSWORD = "Khôi phục mật khẩu WVideos";

    private static final String BRAND = "#009688";
    private static final String BRAND_DARK = "#00796b";
    private static final String BRAND_LIGHT = "#e0f2f1";
    private static final String TEXT_PRIMARY = "#1f2937";
    private static final String TEXT_SECONDARY = "#6b7280";
    private static final String PAGE_BG = "#f4f6f8";

    /**
     * Khung chung: nền xám nhạt, card trắng bo góc, header teal với logo, footer mờ.
     */
    private static String shell(String title, String bodyHtml) {
        return """
            <!DOCTYPE html>
            <html lang="vi">
            <body style="margin:0;padding:0;background-color:%s;">
            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:%s;padding:32px 16px;">
            <tr><td align="center">
            <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%%;background-color:#ffffff;border-radius:12px;overflow:hidden;font-family:Arial,Helvetica,sans-serif;box-shadow:0 1px 3px rgba(0,0,0,0.08);">

              <!-- Header -->
              <tr><td style="background-color:%s;padding:28px 40px;">
                <span style="display:inline-block;background-color:#ffffff;color:%s;font-size:22px;font-weight:bold;padding:4px 10px;border-radius:8px;">wd</span><span style="color:#ffffff;font-size:22px;font-weight:bold;">video</span>
              </td></tr>

              <!-- Tiêu đề -->
              <tr><td style="padding:32px 40px 0 40px;">
                <h2 style="margin:0 0 16px 0;font-size:22px;color:%s;">%s</h2>
              </td></tr>

              <!-- Nội dung -->
              <tr><td style="padding:0 40px 32px 40px;font-size:15px;line-height:1.7;color:%s;">
                %s
              </td></tr>

              <!-- Footer -->
              <tr><td style="padding:20px 40px;background-color:#f9fafb;border-top:1px solid #eceff1;text-align:center;">
                <p style="margin:0 0 6px 0;font-size:12px;color:#9ca3af;">Email này được gửi tự động từ hệ thống WVideos. Vui lòng không trả lời email này.</p>
                <p style="margin:0;font-size:12px;color:#9ca3af;">© 2026 WVideos. All rights reserved.</p>
              </td></tr>

            </table>
            </td></tr>
            </table>
            </body>
            </html>
            """.formatted(PAGE_BG, PAGE_BG, BRAND, BRAND, TEXT_PRIMARY, title, TEXT_PRIMARY, bodyHtml);
    }

    /**
     * Khối nút CTA + link dự phòng (dùng được cả khi client chặn nút).
     */
    private static String ctaButton(String url, String label) {
        return """
            <table role="presentation" cellpadding="0" cellspacing="0" style="margin:8px 0 24px 0;">
            <tr><td style="border-radius:8px;background-color:%s;">
              <a href="%s" style="display:inline-block;padding:14px 36px;font-size:15px;font-weight:bold;color:#ffffff;text-decoration:none;border-radius:8px;background-color:%s;">%s</a>
            </td></tr>
            </table>
            <p style="margin:0 0 24px 0;font-size:13px;color:%s;">Hoặc copy link sau vào trình duyệt:<br>
            <a href="%s" style="color:%s;word-break:break-all;">%s</a></p>
            """.formatted(BRAND, url, BRAND, label, TEXT_SECONDARY, url, BRAND_DARK, url);
    }

    /**
     * Icon đồng hồ (feather-style, stroke SVG) - inline SVG nhỏ, client không hỗ trợ
     * (Outlook desktop) sẽ bỏ qua nhưng câu chữ vẫn đọc được bình thường.
     */
    private static String iconClock(String color) {
        return """
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="%s" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:-2px;display:inline-block;"><circle cx="12" cy="12" r="10"></circle><polyline points="12 7 12 12 15 14"></polyline></svg>
            """.formatted(color);
    }

    /**
     * Khối chú ý (thời hạn token, lưu ý bảo mật).
     */
    private static String infoBox(String iconSvg, String html) {
        return """
            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin:0 0 20px 0;">
            <tr><td style="background-color:%s;border-left:4px solid %s;border-radius:0 8px 8px 0;padding:14px 18px;font-size:13px;line-height:1.6;color:%s;">
            %s %s
            </td></tr>
            </table>
            """.formatted(BRAND_LIGHT, BRAND, TEXT_PRIMARY, iconSvg, html);
    }

    private static String footnote(String html) {
        return """
            <p style="margin:0;padding-top:16px;border-top:1px solid #eceff1;font-size:13px;line-height:1.6;color:%s;">%s</p>
            """.formatted(TEXT_SECONDARY, html);
    }

    /**
     * Nội dung mặc định của email xác nhận đăng ký (chưa render placeholder).
     */
    public static String defaultConfirmationBody() {
        String body = """
            <p style="margin:0 0 16px 0;">Xin chào <strong>{{email}}</strong>,</p>
            <p style="margin:0 0 8px 0;">Cảm ơn bạn đã đăng ký tài khoản <strong>WVideos</strong>.<br>
            Nhấn nút bên dưới để kích hoạt tài khoản của bạn:</p>
            %s
            %s
            %s
            """.formatted(ctaButton("{{url}}", "Xác nhận đăng ký"),
                infoBox(iconClock(BRAND_DARK), "Link có hiệu lực trong <strong>{{minutes}} phút</strong> và chỉ sử dụng được <strong>một lần</strong>."),
                footnote("Nếu bạn không thực hiện việc đăng ký này, vui lòng bỏ qua email này."));
        return shell("Xác nhận đăng ký tài khoản", body);
    }

    /**
     * Nội dung mặc định của email đặt lại mật khẩu (chưa render placeholder).
     */
    public static String defaultResetPasswordBody() {
        String body = """
            <p style="margin:0 0 16px 0;">Xin chào <strong>{{name}}</strong>,</p>
            <p style="margin:0 0 8px 0;">Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản <strong>WVideos</strong> của bạn.<br>
            Nhấn nút bên dưới để đặt mật khẩu mới:</p>
            %s
            %s
            %s
            """.formatted(ctaButton("{{url}}", "Đặt mật khẩu mới"),
                infoBox(iconClock(BRAND_DARK), "Link có hiệu lực trong <strong>{{minutes}} phút</strong> và chỉ sử dụng được <strong>một lần</strong>."),
                footnote("Nếu bạn thường đăng nhập bằng Google, sau khi đặt mật khẩu bạn có thể dùng thêm email + mật khẩu để đăng nhập.<br>Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này — mật khẩu hiện tại của bạn không thay đổi."));
        return shell("Đặt lại mật khẩu", body);
    }

    /**
     * Tiêu đề mặc định theo key template.
     */
    public static String defaultSubject(String key) {
        return switch (key) {
            case KEY_CONFIRMATION -> DEFAULT_SUBJECT_CONFIRMATION;
            case KEY_RESET_PASSWORD -> DEFAULT_SUBJECT_RESET_PASSWORD;
            default -> throw new IllegalArgumentException("Template không tồn tại: " + key);
        };
    }

    /**
     * Nội dung mặc định theo key template.
     */
    public static String defaultBody(String key) {
        return switch (key) {
            case KEY_CONFIRMATION -> defaultConfirmationBody();
            case KEY_RESET_PASSWORD -> defaultResetPasswordBody();
            default -> throw new IllegalArgumentException("Template không tồn tại: " + key);
        };
    }

    /**
     * Thay các placeholder {{ten}} bằng giá trị thực. Placeholder không có giá trị
     * sẽ bị bỏ trống để tránh gửi email chứa cú pháp thô.
     */
    public static String render(String template, Map<String, String> values) {
        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}",
                    entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }

    /**
     * Kiểm tra key template hợp lệ (chỉ cho admin sửa các template đã biết).
     */
    public static boolean isValidKey(String key) {
        return KEY_CONFIRMATION.equals(key) || KEY_RESET_PASSWORD.equals(key);
    }
}
