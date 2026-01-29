# Script để loại bỏ BOM từ tất cả các file Java

Write-Host "Fixing BOM in Java files..." -ForegroundColor Cyan

$files = Get-ChildItem -Path "src\main\java" -Recurse -Filter "*.java"
$fixed = 0

foreach ($file in $files) {
    try {
        # Đọc file với UTF8 encoding
        $content = Get-Content $file.FullName -Raw -Encoding UTF8
        
        # Kiểm tra và loại bỏ BOM nếu có
        if ($content[0] -eq [char]0xFEFF) {
            $content = $content.Substring(1)
            
            # Ghi lại file không có BOM
            [System.IO.File]::WriteAllText($file.FullName, $content, (New-Object System.Text.UTF8Encoding $false))
            
            Write-Host "Fixed: $($file.Name)" -ForegroundColor Green
            $fixed++
        }
    }
    catch {
        Write-Host "Error fixing $($file.Name): $_" -ForegroundColor Red
    }
}

Write-Host "`nTotal files fixed: $fixed" -ForegroundColor Yellow
