# Script để copy lại các file bị BOM

$sourceBase = 'e:\projectDoc\source\backend\src\main\java\iuh\fit\backend'
$targetBase = 'e:\project\WVideos\backendWVideos\src\main\java\com\example\backendWVideos'

$oldPackage = 'iuh.fit.backend'
$newPackage = 'com.example.backendWVideos'

Write-Host "Re-copying files with correct encoding..." -ForegroundColor Cyan

# Danh sách các file bị lỗi BOM
$errorFiles = @(
    'config\JwtAuthenticationEntryPoint.java',
    'config\SecurityConfig.java',
    'security\CurrentUser.java',
    'mapper\UserMapper.java',
    'dto\request\ChangePasswordRequest.java',
    'dto\request\LogoutRequest.java',
    'dto\request\UserUpdateByUserRequest.java',
    'dto\request\AuthenticationRequest.java',
    'dto\response\IntrospectResponse.java',
    'entity\Permission.java',
    'entity\InvalidatedToken.java',
    'controller\AuthenticationController.java'
)

$fixed = 0

foreach ($file in $errorFiles) {
    $sourcePath = Join-Path $sourceBase $file
    $targetPath = Join-Path $targetBase $file
    
    if (Test-Path $sourcePath) {
        try {
            # Đọc file từ source
            $content = [System.IO.File]::ReadAllText($sourcePath, [System.Text.Encoding]::UTF8)
            
            # Loại bỏ BOM nếu có
            if ($content[0] -eq [char]0xFEFF) {
                $content = $content.Substring(1)
            }
            
            # Thay đổi package
            $content = $content -replace "package $oldPackage", "package $newPackage"
            $content = $content -replace "import $oldPackage", "import $newPackage"
            
            # Ghi file không có BOM
            $utf8NoBom = New-Object System.Text.UTF8Encoding $false
            [System.IO.File]::WriteAllText($targetPath, $content, $utf8NoBom)
            
            Write-Host "Fixed: $file" -ForegroundColor Green
            $fixed++
        }
        catch {
            Write-Host "Error: $file - $_" -ForegroundColor Red
        }
    }
    else {
        Write-Host "Not found: $file" -ForegroundColor Yellow
    }
}

Write-Host "`nTotal files fixed: $fixed" -ForegroundColor Cyan
