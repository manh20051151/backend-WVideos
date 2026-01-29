# Script để fix tất cả BOM trong các file Java

$sourceBase = 'e:\projectDoc\source\backend\src\main\java\iuh\fit\backend'
$targetBase = 'e:\project\WVideos\backendWVideos\src\main\java\com\example\backendWVideos'

$oldPackage = 'iuh.fit.backend'
$newPackage = 'com.example.backendWVideos'

Write-Host "Fixing ALL Java files..." -ForegroundColor Cyan

# Lấy tất cả file Java trong target
$allFiles = Get-ChildItem -Path $targetBase -Recurse -Filter "*.java"
$fixed = 0
$skipped = 0

foreach ($targetFile in $allFiles) {
    # Tính relative path
    $relativePath = $targetFile.FullName.Substring($targetBase.Length + 1)
    
    # Tìm file source tương ứng
    $sourcePath = Join-Path $sourceBase $relativePath
    
    if (Test-Path $sourcePath) {
        try {
            # Đọc từ source
            $content = [System.IO.File]::ReadAllText($sourcePath, [System.Text.Encoding]::UTF8)
            
            # Loại bỏ BOM
            if ($content[0] -eq [char]0xFEFF) {
                $content = $content.Substring(1)
            }
            
            # Thay package
            $content = $content -replace "package $oldPackage", "package $newPackage"
            $content = $content -replace "import $oldPackage", "import $newPackage"
            
            # Ghi không có BOM
            $utf8NoBom = New-Object System.Text.UTF8Encoding $false
            [System.IO.File]::WriteAllText($targetFile.FullName, $content, $utf8NoBom)
            
            Write-Host "Fixed: $relativePath" -ForegroundColor Green
            $fixed++
        }
        catch {
            Write-Host "Error: $relativePath - $_" -ForegroundColor Red
        }
    }
    else {
        $skipped++
    }
}

Write-Host "`nTotal fixed: $fixed" -ForegroundColor Cyan
Write-Host "Skipped (no source): $skipped" -ForegroundColor Yellow
