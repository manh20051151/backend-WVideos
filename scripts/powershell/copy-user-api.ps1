# Script PowerShell để copy User API từ DocPro Backend sang snha Backend
# Tự động thay đổi package name và import statements

$sourceBase = 'e:\projectDoc\source\backend\src\main\java\iuh\fit\backend'
$targetBase = 'e:\project\WVideos\backendWVideos\src\main\java\com\example\backendWVideos'

$oldPackage = 'iuh.fit.backend'
$newPackage = 'com.example.backendWVideos'

Write-Host '🚀 Bắt đầu copy User API từ DocPro sang snha...' -ForegroundColor Green
Write-Host ''

# Tạo cấu trúc thư mục
$folders = @(
    'config',
    'controller',
    'dto\request',
    'dto\response',
    'entity',
    'enums',
    'exception',
    'mapper',
    'repository',
    'security',
    'service',
    'validator'
)

Write-Host '📁 Tạo cấu trúc thư mục...' -ForegroundColor Cyan
foreach ($folder in $folders) {
    $targetFolder = Join-Path $targetBase $folder
    if (!(Test-Path $targetFolder)) {
        New-Item -ItemType Directory -Path $targetFolder -Force | Out-Null
        Write-Host "  ✓ Tạo: $folder" -ForegroundColor Gray
    }
}
Write-Host ''

# Danh sách file cần copy
$filesToCopy = @{
    'entity' = @(
        'User.java',
        'Role.java',
        'Permission.java',
        'InvalidatedToken.java'
    )
    
    'repository' = @(
        'UserRepository.java',
        'RoleRepository.java',
        'PermissionRepository.java',
        'InvalidatedTokenRepository.java'
    )
    
    'dto\request' = @(
        'AuthenticationRequest.java',
        'IntrospectRequest.java',
        'RefreshRequest.java',
        'LogoutRequest.java',
        'InfiniteTokenRequest.java',
        'UserCreateRequest.java',
        'UserUpdateRequest.java',
        'UserUpdateByUserRequest.java',
        'ChangePasswordRequest.java',
        'ForgotPasswordRequest.java'
    )
    
    'dto\response' = @(
        'AuthenticationResponse.java',
        'IntrospectResponse.java',
        'UserResponse.java',
        'RoleResponse.java',
        'PermissionResponse.java'
    )
    
    'service' = @(
        'AuthenticationService.java',
        'UserService.java',
        'RoleService.java',
        'PermissionService.java'
    )
    
    'controller' = @(
        'AuthenticationController.java',
        'UserController.java',
        'ApiResponse.java'
    )
    
    'config' = @(
        'SecurityConfig.java',
        'CustomJwtDecoder.java',
        'JwtAuthenticationEntryPoint.java',
        'ApplicationInitConfig.java',
        'CustomOAuth2AuthorizationRequestResolver.java',
        'OAuth2LoginSuccessHandler.java',
        'UserArgumentResolver.java',
        'WebMvcConfig.java'
    )
    
    'security' = @(
        'CurrentUser.java'
    )
    
    'exception' = @(
        'AppException.java',
        'ErrorCode.java',
        'GlobalExceptionHandler.java'
    )
    
    'enums' = @(
        'AuthProvider.java'
    )
    
    'mapper' = @(
        'UserMapper.java',
        'RoleMapper.java',
        'PermissionMapper.java'
    )
    
    'validator' = @(
        'DobValidator.java',
        'DobConstraint.java'
    )
}

# Function để thay đổi package và import
function Update-PackageAndImports {
    param (
        [string]$filePath,
        [string]$oldPkg,
        [string]$newPkg
    )
    
    if (Test-Path $filePath) {
        $content = Get-Content $filePath -Raw -Encoding UTF8
        
        # Thay đổi package declaration
        $content = $content -replace "package $oldPkg", "package $newPkg"
        
        # Thay đổi import statements
        $content = $content -replace "import $oldPkg", "import $newPkg"
        
        # Lưu file với UTF-8 encoding
        $content | Set-Content $filePath -Encoding UTF8 -NoNewline
    }
}

# Copy files
$totalFiles = 0
$copiedFiles = 0
$skippedFiles = 0

foreach ($folder in $filesToCopy.Keys) {
    Write-Host "📦 Copy files từ: $folder" -ForegroundColor Yellow
    
    foreach ($file in $filesToCopy[$folder]) {
        $totalFiles++
        $sourcePath = Join-Path $sourceBase "$folder\$file"
        $targetPath = Join-Path $targetBase "$folder\$file"
        
        if (Test-Path $sourcePath) {
            try {
                Copy-Item -Path $sourcePath -Destination $targetPath -Force
                Update-PackageAndImports -filePath $targetPath -oldPkg $oldPackage -newPkg $newPackage
                Write-Host "  ✓ $file" -ForegroundColor Green
                $copiedFiles++
            }
            catch {
                Write-Host "  ✗ $file - Lỗi: $_" -ForegroundColor Red
                $skippedFiles++
            }
        }
        else {
            Write-Host "  ⊘ $file - Không tìm thấy" -ForegroundColor DarkGray
            $skippedFiles++
        }
    }
    Write-Host ''
}

# Summary
Write-Host '═══════════════════════════════════════════════════════' -ForegroundColor Cyan
Write-Host '✨ HOÀN THÀNH!' -ForegroundColor Green
Write-Host '═══════════════════════════════════════════════════════' -ForegroundColor Cyan
Write-Host ''
Write-Host '📊 Thống kê:' -ForegroundColor Yellow
Write-Host "  • Tổng số file: $totalFiles" -ForegroundColor White
Write-Host "  • Đã copy: $copiedFiles" -ForegroundColor Green
Write-Host "  • Bỏ qua: $skippedFiles" -ForegroundColor DarkGray
Write-Host ''
Write-Host '📋 Các bước tiếp theo:' -ForegroundColor Yellow
Write-Host '  1. Cập nhật application.yaml với cấu hình database và JWT' -ForegroundColor White
Write-Host '  2. Tạo database: CREATE DATABASE db_wvideos;' -ForegroundColor White
Write-Host '  3. Chạy: mvn clean install' -ForegroundColor White
Write-Host '  4. Chạy: mvn spring-boot:run' -ForegroundColor White
Write-Host '  5. Truy cập Swagger UI: http://localhost:8080/swagger-ui.html' -ForegroundColor White
Write-Host ''
Write-Host '📖 Xem chi tiết trong file: COPY_USER_API_GUIDE.md' -ForegroundColor Cyan
Write-Host ''
