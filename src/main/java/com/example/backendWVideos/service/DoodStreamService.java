package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.response.DoodStreamUploadServerResponse;
import com.example.backendWVideos.dto.response.DoodStreamUploadResult;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoodStreamService {

    private final RestTemplate restTemplate;

    @Value("${doodstream.api-key}")
    private String apiKey;

    @Value("${doodstream.api-base-url}")
    private String apiBaseUrl;

    @Value("${doodstream.upload-server-endpoint}")
    private String uploadServerEndpoint;

    /**
     * Lấy upload server URL từ DoodStream
     */
    public String getUploadServer() {
        try {
            String url = apiBaseUrl + uploadServerEndpoint + "?key=" + apiKey;
            
            log.info("Đang lấy upload server từ DoodStream...");
            
            ResponseEntity<DoodStreamUploadServerResponse> response = restTemplate.getForEntity(
                url, 
                DoodStreamUploadServerResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                DoodStreamUploadServerResponse body = response.getBody();
                
                if (body.getStatus() == 200) {
                    log.info("✅ Lấy upload server thành công: {}", body.getResult());
                    return body.getResult();
                }
            }

            throw new AppException(ErrorCode.DOODSTREAM_ERROR);
            
        } catch (Exception e) {
            log.error("❌ Lỗi khi lấy upload server: {}", e.getMessage());
            throw new AppException(ErrorCode.DOODSTREAM_ERROR);
        }
    }

    /**
     * Upload file lên DoodStream
     */
    public DoodStreamUploadResult uploadFile(MultipartFile file, String uploadServerUrl) {
        try {
            log.info("Đang upload file {} ({} bytes) lên DoodStream...", 
                file.getOriginalFilename(), file.getSize());
            log.info("Upload server URL: {}", uploadServerUrl);

            // Tạo request body
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("api_key", apiKey);
            
            // Convert MultipartFile to ByteArrayResource
            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            body.add("file", fileResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Upload file
            String uploadUrl = uploadServerUrl + "?" + apiKey;
            log.info("Gửi request upload đến: {}", uploadUrl);
            
            long startTime = System.currentTimeMillis();
            ResponseEntity<Map> response = restTemplate.postForEntity(uploadUrl, requestEntity, Map.class);
            long endTime = System.currentTimeMillis();
            
            log.info("Upload hoàn tất sau {} ms", (endTime - startTime));

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                log.info("Response body: {}", responseBody);
                
                if ((Integer) responseBody.get("status") == 200) {
                    List<Map<String, Object>> results = (List<Map<String, Object>>) responseBody.get("result");
                    
                    if (results != null && !results.isEmpty()) {
                        Map<String, Object> result = results.get(0);
                        
                        log.info("📊 DoodStream result: {}", result);
                        log.info("🖼️ single_img: {}, splash_img: {}", 
                            result.get("single_img"), result.get("splash_img"));
                        
                        DoodStreamUploadResult uploadResult = DoodStreamUploadResult.builder()
                            .fileCode((String) result.get("filecode"))
                            .downloadUrl((String) result.get("download_url"))
                            .singleImg((String) result.get("single_img"))
                            .splashImg((String) result.get("splash_img"))
                            .protectedEmbed((String) result.get("protected_embed"))
                            .protectedDl((String) result.get("protected_dl"))
                            .size((String) result.get("size"))
                            .length((String) result.get("length"))
                            .uploaded((String) result.get("uploaded"))
                            .title((String) result.get("title"))
                            .canPlay((Integer) result.get("canplay"))
                            .status((Integer) result.get("status"))
                            .build();

                        log.info("✅ Upload thành công! FileCode: {}", uploadResult.getFileCode());
                        return uploadResult;
                    }
                }
                
                log.error("❌ DoodStream trả về status không hợp lệ: {}", responseBody.get("status"));
            }

            log.error("❌ Response không hợp lệ: status={}, body={}", 
                response.getStatusCode(), response.getBody());
            throw new AppException(ErrorCode.UPLOAD_FAILED);
            
        } catch (Exception e) {
            log.error("❌ Lỗi khi upload file: {} - {}", e.getClass().getName(), e.getMessage());
            e.printStackTrace();
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }

    /**
     * Upload file bytes lên DoodStream
     */
    public DoodStreamUploadResult uploadFileBytes(byte[] fileBytes, String fileName, String uploadServerUrl) {
        try {
            log.info("Đang upload file {} ({} bytes) lên DoodStream...", 
                fileName, fileBytes.length);
            log.info("Upload server URL: {}", uploadServerUrl);

            // Tạo request body
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("api_key", apiKey);
            
            // Convert byte[] to ByteArrayResource
            ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            };
            body.add("file", fileResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Upload file
            String uploadUrl = uploadServerUrl + "?" + apiKey;
            log.info("Gửi request upload đến: {}", uploadUrl);
            
            long startTime = System.currentTimeMillis();
            ResponseEntity<Map> response = restTemplate.postForEntity(uploadUrl, requestEntity, Map.class);
            long endTime = System.currentTimeMillis();
            
            log.info("Upload hoàn tất sau {} ms", (endTime - startTime));

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                log.info("Response body: {}", responseBody);
                
                if ((Integer) responseBody.get("status") == 200) {
                    List<Map<String, Object>> results = (List<Map<String, Object>>) responseBody.get("result");
                    
                    if (results != null && !results.isEmpty()) {
                        Map<String, Object> result = results.get(0);
                        
                        log.info("📊 DoodStream result: {}", result);
                        log.info("🖼️ single_img: {}, splash_img: {}", 
                            result.get("single_img"), result.get("splash_img"));
                        
                        DoodStreamUploadResult uploadResult = DoodStreamUploadResult.builder()
                            .fileCode((String) result.get("filecode"))
                            .downloadUrl((String) result.get("download_url"))
                            .singleImg((String) result.get("single_img"))
                            .splashImg((String) result.get("splash_img"))
                            .protectedEmbed((String) result.get("protected_embed"))
                            .protectedDl((String) result.get("protected_dl"))
                            .size((String) result.get("size"))
                            .length((String) result.get("length"))
                            .uploaded((String) result.get("uploaded"))
                            .title((String) result.get("title"))
                            .canPlay((Integer) result.get("canplay"))
                            .status((Integer) result.get("status"))
                            .build();

                        log.info("✅ Upload thành công! FileCode: {}", uploadResult.getFileCode());
                        return uploadResult;
                    }
                }
                
                log.error("❌ DoodStream trả về status không hợp lệ: {}", responseBody.get("status"));
            }

            log.error("❌ Response không hợp lệ: status={}, body={}", 
                response.getStatusCode(), response.getBody());
            throw new AppException(ErrorCode.UPLOAD_FAILED);
            
        } catch (Exception e) {
            log.error("❌ Lỗi khi upload file: {} - {}", e.getClass().getName(), e.getMessage());
            e.printStackTrace();
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }

    /**
     * Lấy thông tin file từ DoodStream
     */
    public Map<String, Object> getFileInfo(String fileCode) {
        try {
            String url = apiBaseUrl + "/file/info?key=" + apiKey + "&file_code=" + fileCode;
            
            log.info("🔍 Calling DoodStream file info API: {}", url.replace(apiKey, "***"));
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                log.info("📊 DoodStream file info response: {}", responseBody);
                return responseBody;
            }
            
            throw new AppException(ErrorCode.DOODSTREAM_ERROR);
            
        } catch (Exception e) {
            log.error("❌ Lỗi khi lấy thông tin file: {}", e.getMessage());
            throw new AppException(ErrorCode.DOODSTREAM_ERROR);
        }
    }

    /**
     * Lấy direct video URL (mp4) từ DoodStream để phát trực tiếp, tương tự
     * endpoint /videos/{fileCode}/stream-url nhưng gọi trực tiếp từ service.
     * Trả về null nếu không lấy được.
     */
    public String getDirectVideoUrl(String fileCode) {
        if (fileCode == null || fileCode.isBlank()) {
            return null;
        }
        try {
            String embedUrl = "https://dood.to/e/" + fileCode;
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> embedResponse = restTemplate.exchange(
                    embedUrl, HttpMethod.GET, entity, String.class);
            String html = embedResponse.getBody();
            if (html == null) {
                return null;
            }

            Pattern passMd5Pattern = Pattern.compile("/pass_md5/([^'\"]+)");
            Matcher matcher = passMd5Pattern.matcher(html);
            if (!matcher.find()) {
                return null;
            }

            String passMd5Url = "https://dood.to/pass_md5/" + matcher.group(1);
            ResponseEntity<String> passMd5Response = restTemplate.exchange(
                    passMd5Url, HttpMethod.GET, entity, String.class);
            String baseVideoUrl = passMd5Response.getBody();
            if (baseVideoUrl == null || baseVideoUrl.isEmpty() || baseVideoUrl.equals("RELOAD")) {
                return null;
            }

            String token = generateRandomToken(10);
            String expiry = String.valueOf(System.currentTimeMillis());
            return baseVideoUrl + token + "?token=" + token + "&expiry=" + expiry;
        } catch (Exception e) {
            log.error("❌ Lỗi lấy direct URL DoodStream: {}", e.getMessage());
            return null;
        }
    }

    private String generateRandomToken(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Lấy danh sách file từ DoodStream
     */
    public Map<String, Object> getFileList() {        try {
            String url = apiBaseUrl + "/file/list?key=" + apiKey;
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }
            
            throw new AppException(ErrorCode.DOODSTREAM_ERROR);
            
        } catch (Exception e) {
            log.error("❌ Lỗi khi lấy danh sách file: {}", e.getMessage());
            throw new AppException(ErrorCode.DOODSTREAM_ERROR);
        }
    }
}
