---
name: Antigravity - WVideos Backend
description: Skill tối ưu cho Antigravity IDE khi làm việc với WVideos Backend Spring Boot project
---

# Antigravity IDE - WVideos Backend Skill

## Mục Đích
Skill này tối ưu hóa cách Antigravity AI làm việc với WVideos Backend, bao gồm code generation, debugging, refactoring, và best practices cho Spring Boot development.

## Project Context

**Type**: Spring Boot 3.2.2 REST API
**Language**: Java 17
**Build Tool**: Maven
**Database**: MySQL 8 + Redis
**Architecture**: 3-layer (Controller → Service → Repository)

## Antigravity Workflow

### 1. Code Generation

#### Tạo Service Mới
```java
// Prompt: "Tạo CommentService với CRUD operations"
// Antigravity sẽ generate:

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {
    private final CommentRepository commentRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    
    @Transactional
    public CommentResponse createComment(String userEmail, String videoId, CommentRequest request) {
        log.info("🚀 User {} đang tạo comment cho video {}", userEmail, videoId);
        
        // Validate user
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        // Validate video
        Video video = videoRepository.findById(videoId)
            .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));
        
        // Create comment
        Comment comment = Comment.builder()
            .content(request.getContent())
            .user(user)
            .video(video)
            .build();
        
        comment = commentRepository.save(comment);
        log.info("✅ Comment created successfully: {}", comment.getId());
        
        return commentMapper.toResponse(comment);
    }
}
```

#### Tạo Controller Endpoint
```java
// Prompt: "Thêm endpoint GET /videos/{id}/comments"
// Antigravity sẽ generate:

@Operation(summary = "Get video comments", description = "Lấy danh sách comment của video")
@GetMapping("/{videoId}/comments")
public ApiResponse<Page<CommentResponse>> getVideoComments(
        @PathVariable String videoId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    Page<CommentResponse> comments = commentService.getVideoComments(videoId, pageable);
    
    return ApiResponse.<Page<CommentResponse>>builder()
            .result(comments)
            .build();
}
```

### 2. Debugging với Antigravity

#### Analyze Error Logs
```
Prompt: "Phân tích lỗi này: NullPointerException at VideoService.uploadVideo line 145"

Antigravity sẽ:
1. Đọc VideoService.java line 145
2. Trace dependencies (DoodStreamService, VideoRepository)
3. Check null safety
4. Suggest fix với null checks hoặc Optional
```

#### Fix Common Issues
```java
// Issue: Token refresh không hoạt động
// Prompt: "Sửa lỗi token refresh trong CustomJwtDecoder"

// Antigravity sẽ check:
// 1. verifyToken() method logic
// 2. Redis connection
// 3. JWT expiry time configuration
// 4. Refresh endpoint implementation
```

### 3. Refactoring

#### Extract Method
```java
// Prompt: "Extract validation logic thành method riêng"

// Before
public VideoResponse uploadVideo(String userEmail, MultipartFile file, VideoUploadRequest request) {
    User user = userRepository.findByEmail(userEmail)
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    
    if (file.isEmpty()) {
        throw new AppException(ErrorCode.FILE_EMPTY);
    }
    
    if (file.getSize() > MAX_FILE_SIZE) {
        throw new AppException(ErrorCode.FILE_TOO_LARGE);
    }
    
    // ... upload logic
}

// After (Antigravity refactor)
public VideoResponse uploadVideo(String userEmail, MultipartFile file, VideoUploadRequest request) {
    User user = validateUser(userEmail);
    validateFile(file);
    
    // ... upload logic
}

private User validateUser(String email) {
    return userRepository.findByEmail(email)
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
}

private void validateFile(MultipartFile file) {
    if (file.isEmpty()) {
        throw new AppException(ErrorCode.FILE_EMPTY);
    }
    
    if (file.getSize() > MAX_FILE_SIZE) {
        throw new AppException(ErrorCode.FILE_TOO_LARGE);
    }
}
```

### 4. Code Review với Antigravity

#### Security Review
```
Prompt: "Review security issues trong AuthenticationController"

Antigravity sẽ check:
- SQL Injection risks
- XSS vulnerabilities
- CSRF protection
- Rate limiting
- Input validation
- Password handling
- Token security
```

#### Performance Review
```
Prompt: "Tối ưu performance cho VideoService.getPublicVideos()"

Antigravity suggestions:
- Add @Cacheable annotation
- Optimize N+1 queries với @EntityGraph
- Add database indexes
- Implement pagination
- Use DTO projection thay vì full entity
```

## Antigravity Commands

### Quick Actions

```bash
# Generate boilerplate
/generate service CommentService
/generate controller CommentController
/generate entity Comment
/generate dto CommentRequest CommentResponse

# Refactoring
/refactor extract-method VideoService.uploadVideo
/refactor rename oldName newName
/refactor move-class VideoService com.example.service.video

# Testing
/test generate VideoServiceTest
/test run VideoServiceTest
/test coverage VideoService

# Documentation
/doc generate VideoController
/doc swagger VideoController
```

### Code Analysis

```bash
# Find issues
/analyze security VideoController
/analyze performance VideoService
/analyze complexity VideoService

# Find usages
/find usages VideoService
/find implementations VideoRepository
/find references Video entity

# Dependencies
/deps analyze
/deps update
/deps vulnerabilities
```

## Antigravity Best Practices

### 1. Context-Aware Generation

Khi generate code, Antigravity sẽ:
- ✅ Đọc existing patterns trong project
- ✅ Follow naming conventions
- ✅ Sử dụng đúng annotations (@Service, @Transactional, etc.)
- ✅ Add logging với emoji
- ✅ Handle exceptions với AppException
- ✅ Comment bằng Tiếng Việt

### 2. Smart Imports

```java
// Antigravity tự động add imports cần thiết
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
```

### 3. Type-Safe Code

```java
// Antigravity ensure type safety
public ApiResponse<VideoResponse> getVideo(String id) {
    // Return type matches ApiResponse<VideoResponse>
    VideoResponse video = videoService.getVideoById(id);
    return ApiResponse.<VideoResponse>builder()
            .result(video)
            .build();
}
```

### 4. Error Prevention

Antigravity sẽ cảnh báo:
- ⚠️ Null pointer risks
- ⚠️ Resource leaks
- ⚠️ Unclosed streams
- ⚠️ Missing @Transactional
- ⚠️ N+1 query problems
- ⚠️ Security vulnerabilities

## Antigravity Shortcuts

### Code Navigation
- `Ctrl+Click` - Go to definition
- `Alt+F7` - Find usages
- `Ctrl+B` - Go to implementation
- `Ctrl+H` - Type hierarchy

### Code Generation
- `Alt+Insert` - Generate code (getter, setter, constructor)
- `Ctrl+O` - Override methods
- `Ctrl+I` - Implement methods

### Refactoring
- `Shift+F6` - Rename
- `Ctrl+Alt+M` - Extract method
- `Ctrl+Alt+V` - Extract variable
- `Ctrl+Alt+C` - Extract constant

## Antigravity Integration

### Git Integration
```bash
# Commit với conventional commits
/git commit "feat: thêm comment service"
/git commit "fix: sửa lỗi token refresh"
/git commit "refactor: cải thiện video service"

# Branch management
/git branch feature/comment-system
/git checkout develop
/git merge feature/comment-system
```

### Database Tools
```bash
# Query database
/db query "SELECT * FROM videos WHERE status = 'ACTIVE'"
/db schema videos
/db indexes videos

# Migrations
/db migrate create add_comment_table
/db migrate up
/db migrate down
```

### Testing Tools
```bash
# Run tests
/test run all
/test run VideoServiceTest
/test coverage

# Generate test data
/test data generate users 10
/test data generate videos 50
```

## Antigravity AI Prompts

### Effective Prompts

✅ **Good Prompts**:
- "Tạo service để quản lý comments với CRUD operations"
- "Thêm endpoint GET /videos/{id}/comments với pagination"
- "Refactor VideoService.uploadVideo để tách validation logic"
- "Fix NullPointerException trong VideoService line 145"
- "Tối ưu performance cho getPublicVideos() method"

❌ **Bad Prompts**:
- "Fix bug" (quá chung chung)
- "Make it better" (không rõ ràng)
- "Add feature" (thiếu context)

### Context Prompts

```
# Provide context cho better results
"Trong VideoService, tạo method để sync video info từ DoodStream API.
Method này cần:
- Gọi DoodStreamService.getFileInfo()
- Update video entity với thông tin mới
- Handle errors nếu DoodStream API fails
- Log progress với emoji
- Return VideoResponse"
```

## Troubleshooting với Antigravity

### Common Issues

#### Issue: Build fails
```bash
/analyze build-error
# Antigravity sẽ check:
# - Maven dependencies
# - Java version compatibility
# - Compilation errors
# - Missing resources
```

#### Issue: Tests fail
```bash
/test analyze-failure VideoServiceTest
# Antigravity sẽ:
# - Read test logs
# - Identify failing assertions
# - Suggest fixes
# - Show related code
```

#### Issue: Runtime exception
```bash
/debug analyze-exception NullPointerException VideoService:145
# Antigravity sẽ:
# - Show code at line 145
# - Trace variable origins
# - Suggest null checks
# - Show similar fixed issues
```

## Performance Tips

### Antigravity Indexing
- ✅ Wait for indexing to complete
- ✅ Exclude node_modules, target folders
- ✅ Use .gitignore patterns

### Memory Settings
```bash
# Increase heap size for large projects
-Xmx4g -Xms2g
```

### Cache Management
```bash
/cache clear
/cache rebuild
```

## Resources

- [Antigravity Docs](https://antigravity.dev/docs)
- [Spring Boot Best Practices](https://spring.io/guides)
- [Project README](../README.md)
- [API Documentation](http://localhost:8080/swagger-ui.html)
