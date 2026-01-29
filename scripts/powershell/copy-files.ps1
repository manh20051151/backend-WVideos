$sourceBase = 'e:\projectDoc\source\backend\src\main\java\iuh\fit\backend'
$targetBase = 'e:\project\WVideos\backendWVideos\src\main\java\com\example\backendWVideos'

$oldPackage = 'iuh.fit.backend'
$newPackage = 'com.example.backendWVideos'

Write-Host 'Starting copy process...' -ForegroundColor Green

# Create folders
$folders = @('config', 'controller', 'dto\request', 'dto\response', 'entity', 'enums', 'exception', 'mapper', 'repository', 'security', 'service', 'validator')

foreach ($folder in $folders) {
    $targetFolder = Join-Path $targetBase $folder
    if (!(Test-Path $targetFolder)) {
        New-Item -ItemType Directory -Path $targetFolder -Force | Out-Null
        Write-Host "Created: $folder"
    }
}

# Files to copy
$files = @(
    'entity\User.java',
    'entity\Role.java',
    'entity\Permission.java',
    'entity\InvalidatedToken.java',
    'repository\UserRepository.java',
    'repository\RoleRepository.java',
    'repository\PermissionRepository.java',
    'repository\InvalidatedTokenRepository.java',
    'dto\request\AuthenticationRequest.java',
    'dto\request\IntrospectRequest.java',
    'dto\request\RefreshRequest.java',
    'dto\request\LogoutRequest.java',
    'dto\request\InfiniteTokenRequest.java',
    'dto\request\UserCreateRequest.java',
    'dto\request\UserUpdateRequest.java',
    'dto\request\UserUpdateByUserRequest.java',
    'dto\request\ChangePasswordRequest.java',
    'dto\response\AuthenticationResponse.java',
    'dto\response\IntrospectResponse.java',
    'dto\response\UserResponse.java',
    'service\AuthenticationService.java',
    'service\UserService.java',
    'controller\AuthenticationController.java',
    'controller\UserController.java',
    'controller\ApiResponse.java',
    'config\SecurityConfig.java',
    'config\CustomJwtDecoder.java',
    'config\JwtAuthenticationEntryPoint.java',
    'config\ApplicationInitConfig.java',
    'config\OAuth2LoginSuccessHandler.java',
    'config\CustomOAuth2AuthorizationRequestResolver.java',
    'security\CurrentUser.java',
    'exception\AppException.java',
    'exception\ErrorCode.java',
    'exception\GlobalExceptionHandler.java',
    'enums\AuthProvider.java',
    'mapper\UserMapper.java'
)

$copied = 0
$skipped = 0

foreach ($file in $files) {
    $sourcePath = Join-Path $sourceBase $file
    $targetPath = Join-Path $targetBase $file
    
    if (Test-Path $sourcePath) {
        Copy-Item -Path $sourcePath -Destination $targetPath -Force
        
        # Update package names
        $content = Get-Content $targetPath -Raw -Encoding UTF8
        $content = $content -replace "package $oldPackage", "package $newPackage"
        $content = $content -replace "import $oldPackage", "import $newPackage"
        $content | Set-Content $targetPath -Encoding UTF8 -NoNewline
        
        Write-Host "Copied: $file" -ForegroundColor Green
        $copied++
    }
    else {
        Write-Host "Skipped: $file (not found)" -ForegroundColor Yellow
        $skipped++
    }
}

Write-Host "`nSummary:" -ForegroundColor Cyan
Write-Host "Copied: $copied files" -ForegroundColor Green
Write-Host "Skipped: $skipped files" -ForegroundColor Yellow
Write-Host "`nNext steps:" -ForegroundColor Cyan
Write-Host "1. Update application.yaml"
Write-Host "2. Create database: CREATE DATABASE db_wvideos;"
Write-Host "3. Run: mvn clean install"
Write-Host "4. Run: mvn spring-boot:run"
