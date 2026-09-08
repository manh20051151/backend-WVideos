package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.response.StreamtapeUploadResult;
import com.example.backendWVideos.dto.response.StreamtapeUploadServerResponse;
import com.example.backendWVideos.entity.Video;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.repository.VideoRepository;
import com.example.backendWVideos.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreamtapeService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final VideoRepository videoRepository;

    @Value("${streamtape.api-login}")
    private String apiLogin;

    @Value("${streamtape.api-key}")
    private String apiKey;

    @Value("${streamtape.api-base-url}")
    private String apiBaseUrl;

    @Value("${streamtape.upload-endpoint}")
    private String uploadEndpoint;

    @Value("${streamtape.file-info-endpoint}")
    private String fileInfoEndpoint;

    @Value("${streamtape.getsplash-endpoint}")
    private String getsplashEndpoint;

    /**
     * Lấy upload URL từ Streamtape
     */
    public String getUploadUrl() {
        try {
            String url = apiBaseUrl + uploadEndpoint + "?login=" + apiLogin + "&key=" + apiKey;

            log.info("Đang lấy upload URL từ Streamtape...");

            ResponseEntity<StreamtapeUploadServerResponse> response = restTemplate.getForEntity(
                url,
                StreamtapeUploadServerResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                StreamtapeUploadServerResponse body = response.getBody();

                if (body.getStatus() != null && body.getStatus() == 200
                        && body.getResult() != null && body.getResult().getUrl() != null) {
                    log.info("✅ Lấy upload URL thành công: {}", body.getResult().getUrl());
                    return body.getResult().getUrl();
                }
            }

            throw new AppException(ErrorCode.STREAMTAPE_ERROR);

        } catch (Exception e) {
            log.error("❌ Lỗi khi lấy upload URL Streamtape: {}", e.getMessage());
            throw new AppException(ErrorCode.STREAMTAPE_ERROR);
        }
    }

    /**
     * Upload file Multipart lên Streamtape
     */
    public StreamtapeUploadResult uploadFile(MultipartFile file, String uploadUrl) {
        try {
            log.info("Đang upload file {} ({} bytes) lên Streamtape...",
                file.getOriginalFilename(), file.getSize());
            log.info("Upload URL: {}", uploadUrl);

            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            return doUpload(fileResource, file.getOriginalFilename(), uploadUrl);

        } catch (Exception e) {
            log.error("❌ Lỗi khi upload file lên Streamtape: {} - {}", e.getClass().getName(), e.getMessage());
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }

    /**
     * Upload byte[] lên Streamtape
     */
    public StreamtapeUploadResult uploadFileBytes(byte[] fileBytes, String fileName, String uploadUrl) {
        try {
            log.info("Đang upload file {} ({} bytes) lên Streamtape...", fileName, fileBytes.length);
            log.info("Upload URL: {}", uploadUrl);

            ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            };

            return doUpload(fileResource, fileName, uploadUrl);

        } catch (Exception e) {
            log.error("❌ Lỗi khi upload byte[] lên Streamtape: {} - {}", e.getClass().getName(), e.getMessage());
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }

    private StreamtapeUploadResult doUpload(ByteArrayResource fileResource, String fileName, String uploadUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file1", fileResource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        log.info("Gửi request upload đến: {}", uploadUrl);

        long startTime = System.currentTimeMillis();
        ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, requestEntity, String.class);
        long endTime = System.currentTimeMillis();

        log.info("Upload hoàn tất sau {} ms", (endTime - startTime));

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, Object> responseBody = parseJson(response.getBody());
            log.info("Response body: {}", responseBody);

            if (responseBody != null && responseBody.get("status") instanceof Number
                    && ((Number) responseBody.get("status")).intValue() == 200) {

                Object resultObj = responseBody.get("result");
                if (resultObj instanceof Map) {
                    Map<String, Object> result = (Map<String, Object>) resultObj;

                    String fileId = (String) result.get("id");
                    log.info("📊 Streamtape result: {}", result);

                    StreamtapeUploadResult uploadResult = StreamtapeUploadResult.builder()
                            .fileId(fileId)
                            .title((String) result.get("title"))
                            .size(result.get("size") != null ? String.valueOf(result.get("size")) : null)
                            .embedUrl(fileId != null ? "https://streamtape.com/e/" + fileId : null)
                            .build();

                    log.info("✅ Upload thành công! FileId: {}", uploadResult.getFileId());
                    return uploadResult;
                }
            }

            log.error("❌ Streamtape trả về status không hợp lệ: {}", responseBody != null ? responseBody.get("status") : null);
        }

        log.error("❌ Response không hợp lệ: status={}, body={}",
            response.getStatusCode(), response.getBody());
        throw new AppException(ErrorCode.UPLOAD_FAILED);
    }

    private Map<String, Object> parseJson(String body) {
        try {
            return objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("❌ Không parse được JSON response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Lấy thông tin file từ Streamtape
     */
    public Map<String, Object> getFileInfo(String fileId) {
        try {
            String url = apiBaseUrl + fileInfoEndpoint + "?file=" + fileId
                    + "&login=" + apiLogin + "&key=" + apiKey;

            log.info("🔍 Calling Streamtape file info API: {}", url.replace(apiKey, "***"));

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                log.info("📊 Streamtape file info response: {}", responseBody);
                return responseBody;
            }

            throw new AppException(ErrorCode.STREAMTAPE_ERROR);

        } catch (Exception e) {
            log.error("❌ Lỗi khi lấy thông tin file Streamtape: {}", e.getMessage());
            throw new AppException(ErrorCode.STREAMTAPE_ERROR);
        }
    }

    /**
     * Lấy danh sách file/folder từ Streamtape
     */
    public Map<String, Object> getFileList() {
        try {
            String url = apiBaseUrl + "/file/listfolder?login=" + apiLogin + "&key=" + apiKey;

            log.info("🔍 Calling Streamtape listfolder API: {}", url.replace(apiKey, "***"));

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }

            throw new AppException(ErrorCode.STREAMTAPE_ERROR);

        } catch (Exception e) {
            log.error("❌ Lỗi khi lấy danh sách file Streamtape: {}", e.getMessage());
            throw new AppException(ErrorCode.STREAMTAPE_ERROR);
        }
    }

    /**
     * Lấy thumbnail (splash image) của file từ Streamtape.
     * URL thật có dạng: https://thumb.tapecontent.net/thumb/{fileId}/{thumbId}.jpg
     * (thumbId là id riêng, không thể tự build). Ta lấy từ og:image của trang embed
     * (Streamtape trỏ og:image thẳng đến URL thumbnail thật).
     */
    public String getSplashImage(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return null;
        }
        log.info("🖼️ Lấy splash image Streamtape cho file: {}", fileId);

        String url = apiBaseUrl + getsplashEndpoint + "?file=" + fileId
                + "&login=" + apiLogin + "&key=" + apiKey;

        int[] retryDelaysMs = {0, 3000, 8000, 15000, 25000};
        for (int i = 0; i < retryDelaysMs.length; i++) {
            if (retryDelaysMs[i] > 0) {
                try {
                    log.info("⏳ Retry getsplash lần {} sau {}ms", i + 1, retryDelaysMs[i]);
                    Thread.sleep(retryDelaysMs[i]);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            try {
                String splash = fetchSplashDirect(url);
                if (splash != null && !splash.isEmpty()) {
                    log.info("✅ Streamtape splash URL (getsplash) lần thử {}: {}", i + 1, splash);
                    return splash;
                }
            } catch (Exception e) {
                log.warn("⚠️ Lỗi lấy splash qua getsplash lần {}: {}", i + 1, e.getMessage());
            }
        }
        log.warn("⚠️ Getsplash trả 404 sau {} lần thử, chuyển sang scraping embed page", retryDelaysMs.length);

        String ogImage = scrapeOgImage(fileId);
        if (ogImage != null && !ogImage.isEmpty()) {
            log.info("✅ Streamtape splash URL (og:image): {}", ogImage);
            return ogImage;
        }
        log.warn("⚠️ Không scrape được og:image từ trang embed cho file: {}", fileId);

        return null;
    }

    private String fetchSplashDirect(String url) {
        HttpURLConnection conn = null;
        try {
            java.net.URL splashUrl = new java.net.URL(url);
            conn = (HttpURLConnection) splashUrl.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", BROWSER_UA);
            conn.setRequestProperty("Accept", "application/json,text/html,*/*");

            int status = conn.getResponseCode();
            String contentType = conn.getContentType();

            java.io.InputStream is = (status >= 400)
                    ? conn.getErrorStream()
                    : conn.getInputStream();
            String body = is != null
                    ? new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                    : "";

            log.info("📊 Streamtape getsplash response: status={}, contentType={}, body={}", status, contentType, body);

            if ((status == java.net.HttpURLConnection.HTTP_OK || status == 302)
                    && contentType != null && contentType.startsWith("image/")) {
                String location = conn.getHeaderField("Location");
                if (location != null && !location.isEmpty()) {
                    return location;
                }
            }

            Map<String, Object> map = parseJson(body);
            if (map != null && map.get("result") != null) {
                return map.get("result").toString();
            }
        } catch (Exception e) {
            log.warn("⚠️ Lỗi fetch splash trực tiếp: {}", e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.disconnect(); } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private static final String BROWSER_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    /**
     * Lấy direct video URL (mp4) từ Streamtape để phát trực tiếp,
     * tương tự endpoint /stream-url của DoodStream.
     *
     * Cách 1 (ưu tiên): Account API (login/key) - file/dlticket + file/dl.
     *   Dùng được cho video thuộc tài khoản Streamtape của bạn (video upload qua WVideos),
     *   KHÔNG bị IP-bind như get_video của trang embed.
     * Cách 2 (fallback): scrape trang embed + gọi get_video (có thể bị Streamtape chặn theo IP -> 500).
     */
    @SuppressWarnings("unchecked")
    public String getDirectVideoUrl(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return null;
        }
        log.info("🎬 Đang lấy direct video URL Streamtape cho file: {}", fileId);

        // Cache hit: nếu đã resolve trước đó thì trả luôn (tránh gọi Streamtape mỗi lần load)
        try {
            Video cached = videoRepository.findByFileCode(fileId).orElse(null);
            if (cached != null && cached.getDownloadUrl() != null && !cached.getDownloadUrl().isEmpty()) {
                log.info("💾 Dùng cache direct URL cho file: {}", fileId);
                return cached.getDownloadUrl();
            }
        } catch (Exception ignored) {
            // fallback xuống luồng resolve bình thường
        }

        // ---- Cách 1: Account API ----
        try {
            String ticketUrl = apiBaseUrl + "/file/dlticket?file=" + fileId
                    + "&login=" + apiLogin + "&key=" + apiKey;
            Map<String, Object> ticketResp = restTemplate.getForObject(ticketUrl, Map.class);
            log.info("📋 Streamtape dlticket status: {}", ticketResp != null ? ticketResp.get("status") : "null");
            if (ticketResp != null && ticketResp.get("result") instanceof Map) {
                Map<String, Object> result = (Map<String, Object>) ticketResp.get("result");
                String ticket = result.get("ticket") != null ? result.get("ticket").toString() : null;
                if (ticket != null && !ticket.isEmpty()) {
                    String dlUrl = apiBaseUrl + "/file/dl?file=" + fileId + "&ticket=" + ticket;
                    String directUrl = null;
                    // Streamtape rate-limit: sau khi lấy ticket phải chờ vài giây mới gọi dl được
                    // (trả 403 "You need to wait X more seconds"). Thử lại với backoff.
                    for (int attempt = 0; attempt < 4 && directUrl == null; attempt++) {
                        if (attempt > 0) {
                            try {
                                Thread.sleep(4000L * attempt);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                        Map<String, Object> dlResp = restTemplate.getForObject(dlUrl, Map.class);
                        log.info("📋 Streamtape dl response (lần {}): {}", attempt + 1, dlResp);
                        if (dlResp != null && dlResp.get("result") instanceof Map) {
                            Map<String, Object> dlResult = (Map<String, Object>) dlResp.get("result");
                            Object url = dlResult.get("url");
                            if (url != null && !url.toString().isEmpty()) {
                                directUrl = url.toString();
                            }
                        } else if (dlResp != null
                                && dlResp.get("status") instanceof Number
                                && ((Number) dlResp.get("status")).intValue() == 403) {
                            log.warn("⚠️ Streamtape dl bị rate-limit, chờ rồi thử lại (lần {})...", attempt + 1);
                        } else {
                            break;
                        }
                    }
                    if (directUrl != null) {
                        log.info("✅ Streamtape direct URL (account API): {}", directUrl);
                        cacheDirectUrl(fileId, directUrl);
                        return directUrl;
                    }
                } else {
                    log.warn("⚠️ dlticket không có ticket (file chưa convert xong?): {}", result);
                }
            } else {
                log.warn("⚠️ dlticket result không đúng định dạng (file chưa convert/account chưa bật download?): {}", ticketResp);
            }
            log.warn("⚠️ Account API không trả về direct URL, thử scrape embed...");
        } catch (Exception e) {
            log.warn("⚠️ Lỗi lấy direct URL qua account API: {}", e.getMessage());
        }

        // ---- Cách 2: scrape embed + get_video (fallback) ----
        try {
            String embedHtml = fetchHtml("https://streamtape.com/e/" + fileId, BROWSER_UA, null);
            if (embedHtml != null) {
                String getVideoPath = extractGetVideoPath(embedHtml);
                if (getVideoPath != null) {
                    String getVideoUrl = "https://streamtape.com" + getVideoPath.replaceFirst("^/streamtape\\.com", "");
                    String json = fetchHtml(getVideoUrl, BROWSER_UA, "https://streamtape.com/e/" + fileId);
                    if (json != null) {
                        Map<String, Object> map = parseJson(json);
                        if (map != null && map.get("result") instanceof Map) {
                            Map<String, Object> r = (Map<String, Object>) map.get("result");
                            Object u = r.get("url");
                            if (u != null && !u.toString().isEmpty()) {
                                log.info("✅ Streamtape direct URL (scrape): {}", u);
                                cacheDirectUrl(fileId, u.toString());
                                return u.toString();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ Lỗi scrape embed Streamtape: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Lưu direct URL vào video.downloadUrl để các lần load sau không phải gọi Streamtape nữa.
     * Chỉ lưu khi chưa có (tránh ghi đè). URL có thể hết hạn theo thời gian; nếu playback lỗi,
     * frontend sẽ fallback về iframe và lần load sau sẽ resolve lại.
     */
    private void cacheDirectUrl(String fileId, String url) {
        if (fileId == null || url == null || url.isEmpty()) {
            return;
        }
        try {
            Video v = videoRepository.findByFileCode(fileId).orElse(null);
            if (v != null && (v.getDownloadUrl() == null || v.getDownloadUrl().isEmpty())) {
                v.setDownloadUrl(url);
                videoRepository.save(v);
                log.info("💾 Đã cache direct URL vào video.downloadUrl cho file: {}", fileId);
            }
        } catch (Exception e) {
            log.warn("⚠️ Không lưu được cache direct URL: {}", e.getMessage());
        }
    }

    private String extractGetVideoPath(String html) {
        for (String id : new String[]{"robotlink", "ideoolink", "botlink"}) {
            Pattern p = Pattern.compile(
                    "id=\"" + id + "\"[^>]*>([^<]+)",
                    Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(html);
            if (m.find()) {
                String path = m.group(1).trim();
                if (path.contains("get_video")) {
                    log.info("✅ Tìm thấy get_video path từ div {}: {}", id, path);
                    return path;
                }
            }
        }
        Pattern p = Pattern.compile("(/streamtape\\.com/get_video\\?[^\\s\"'<]+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(html);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private String fetchHtml(String urlStr, String ua, String referer) {
        HttpURLConnection conn = null;
        try {
            java.net.URL url = new java.net.URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", ua);
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            if (referer != null && !referer.isEmpty()) {
                conn.setRequestProperty("Referer", referer);
            }
            int status = conn.getResponseCode();
            java.io.InputStream is = (status >= 400) ? conn.getErrorStream() : conn.getInputStream();
            if (is == null) {
                return null;
            }
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            is.close();
            return baos.toString(java.nio.charset.StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            log.warn("⚠️ Lỗi fetch {}: {}", urlStr, e.getMessage());
            return null;
        } finally {
            if (conn != null) {
                try { conn.disconnect(); } catch (Exception ignored) {}
            }
        }
    }

    private String scrapeOgImage(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return null;
        }

        String[] urls = {
                "https://streamtape.com/v/" + fileId,
                "https://streamtape.com/e/" + fileId
        };

        for (String embedUrl : urls) {
            HttpURLConnection conn = null;
            try {
                java.net.URL url = new java.net.URL(embedUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("User-Agent", BROWSER_UA);
                conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
                conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");

                int status = conn.getResponseCode();
                String contentType = conn.getContentType();
                java.io.InputStream is = conn.getInputStream();
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int len;
                while ((len = is.read(buf)) != -1) {
                    baos.write(buf, 0, len);
                }
                is.close();
                String html = baos.toString(java.nio.charset.StandardCharsets.UTF_8.name());

                if (html != null && !html.isEmpty() && contentType != null && contentType.contains("text/html")) {
                    Pattern pattern = Pattern.compile(
                            "<meta[^>]+?(?:property|name)=[\"']og:image[\"'][^>]*?content=[\"']([^\"']+)[\"']",
                            Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(html);
                    if (!matcher.find()) {
                        pattern = Pattern.compile(
                                "<meta[^>]+?content=[\"']([^\"']+)[\"'][^>]*?(?:property|name)=[\"']og:image[\"']",
                                Pattern.CASE_INSENSITIVE);
                        matcher = pattern.matcher(html);
                    }
                    if (matcher.find()) {
                        String ogImage = matcher.group(1);
                        log.info("✅ Streamtape splash URL (og:image) from {}: {}", embedUrl, ogImage);
                        return ogImage;
                    }

                    Pattern thumbPattern = Pattern.compile(
                            "https://thumb\\.tapecontent\\.net/thumb/" + Pattern.quote(fileId) + "/[A-Za-z0-9]+\\.jpg",
                            Pattern.CASE_INSENSITIVE);
                    Matcher thumbMatcher = thumbPattern.matcher(html);
                    if (thumbMatcher.find()) {
                        String thumbUrl = thumbMatcher.group();
                        log.info("✅ Streamtape splash URL (thumb) from {}: {}", embedUrl, thumbUrl);
                        return thumbUrl;
                    }

                    Pattern thumbAnyPattern = Pattern.compile(
                            "https://thumb\\.tapecontent\\.net/thumb/" + Pattern.quote(fileId) + "/[^\"'<>\\s]+",
                            Pattern.CASE_INSENSITIVE);
                    Matcher thumbAnyMatcher = thumbAnyPattern.matcher(html);
                    if (thumbAnyMatcher.find()) {
                        String thumbUrl = thumbAnyMatcher.group();
                        log.info("✅ Streamtape splash URL (thumb-any) from {}: {}", embedUrl, thumbUrl);
                        return thumbUrl;
                    }

                    Pattern urlAnyPattern = Pattern.compile(
                            "https?://[^\"'<>\\s]*thumb[^\"'<>\\s]*" + Pattern.quote(fileId) + "[^\"'<>\\s]*",
                            Pattern.CASE_INSENSITIVE);
                    Matcher urlAnyMatcher = urlAnyPattern.matcher(html);
                    if (urlAnyMatcher.find()) {
                        String thumbUrl = urlAnyMatcher.group();
                        log.info("✅ Streamtape splash URL (url-any) from {}: {}", embedUrl, thumbUrl);
                        return thumbUrl;
                    }

                    log.warn("⚠️ Không tìm thấy thumbnail trong HTML từ {} cho file: {}, htmlLength={}", embedUrl, fileId, html.length());
                    if (html.length() < 5000) {
                        log.warn("⚠️ HTML từ {} (đầy đủ): {}", embedUrl, html);
                    } else {
                        log.warn("⚠️ HTML từ {} (1000 ký tự đầu): {}", embedUrl, html.substring(0, 1000));
                        log.warn("⚠️ HTML từ {} (1000 ký tự cuối): {}", embedUrl, html.substring(html.length() - 1000));
                    }
                } else {
                    log.warn("⚠️ Streamtape {} trả về non-HTML hoặc rỗng: status={}, contentType={}, length={}", embedUrl, status, contentType, html != null ? html.length() : 0);
                }
            } catch (Exception e) {
                log.warn("⚠️ Lỗi scrape từ {}: {} - {}", embedUrl, e.getClass().getName(), e.getMessage());
            } finally {
                if (conn != null) {
                    try { conn.disconnect(); } catch (Exception ignored) {}
                }
            }
        }

        return null;
    }
}
