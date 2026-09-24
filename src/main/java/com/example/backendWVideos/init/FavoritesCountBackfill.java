package com.example.backendWVideos.init;

import com.example.backendWVideos.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đồng bộ cột favorites_count với số LIKE thật trong bảng video_reactions lúc khởi động.
 * Trước đây favorites_count không được ghi gì (luôn = 0) nên sort "Yêu thích"
 * trên trang chủ lẫn trang category cho thứ tự sai. Backfill này idempotent,
 * chạy mỗi lần boot với chi phí 1 câu UPDATE.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FavoritesCountBackfill implements ApplicationRunner {

    final VideoRepository videoRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int updated = videoRepository.backfillFavoritesCount();
        log.info("✅ Đồng bộ favorites_count theo số LIKE thật: {} video được cập nhật", updated);
    }
}
