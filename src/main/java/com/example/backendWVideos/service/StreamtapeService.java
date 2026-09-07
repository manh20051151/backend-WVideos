package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.response.StreamtapeUploadResult;
import com.example.backendWVideos.dto.response.StreamtapeUploadServerResponse;
import com.example.backendWVideos.exception.AppException;
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
