# Script để reset database WVideos
# Chạy script này để xóa và tạo lại database từ đầu

Write-Host "=== Reset Database WVideos ===" -ForegroundColor Cyan

# Tim MySQL executable
$mysqlPaths = @(
    "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
    "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe",
    "C:\xampp\mysql\bin\mysql.exe",
    "mysql" # Nếu có trong PATH
)

$mysqlExe = $null
foreach ($path in $mysqlPaths) {
    if (Test-Path $path -ErrorAction SilentlyContinue) {
        $mysqlExe = $path
        break
    }
    if ($path -eq "mysql") {
        try {
            $null = Get-Command mysql -ErrorAction Stop
            $mysqlExe = "mysql"
            break
        } catch {}
    }
}

if (-not $mysqlExe) {
    Write-Host "ERROR: Khong tim thay MySQL!" -ForegroundColor Red
    Write-Host "Vui long cai dat MySQL hoac them vao PATH" -ForegroundColor Yellow
    exit 1
}

Write-Host "Tim thay MySQL tai: $mysqlExe" -ForegroundColor Green

# Database credentials
$dbUser = "root"
$dbPassword = "sapassword"
$dbName = "db_wvideos"

# Drop va tao lai database
Write-Host "`nDang xoa database cu..." -ForegroundColor Yellow
& $mysqlExe -u $dbUser -p$dbPassword --skip-ssl --skip-column-names -e "DROP DATABASE IF EXISTS $dbName;"

Write-Host "Dang tao database moi..." -ForegroundColor Yellow
& $mysqlExe -u $dbUser -p$dbPassword --skip-ssl --skip-column-names -e "CREATE DATABASE $dbName CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# Chạy init script
$initScript = Join-Path $PSScriptRoot "..\sql\init-database.sql"
if (Test-Path $initScript) {
    Write-Host "Dang chay init script..." -ForegroundColor Yellow
    Get-Content $initScript | & $mysqlExe -u $dbUser -p$dbPassword --skip-ssl --skip-column-names $dbName
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "`nDatabase da duoc reset thanh cong!" -ForegroundColor Green
        Write-Host "Database: $dbName" -ForegroundColor Cyan
        Write-Host "User: $dbUser" -ForegroundColor Cyan
    } else {
        Write-Host "`nLoi khi chay init script!" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "`nKhong tim thay init script tai: $initScript" -ForegroundColor Red
    exit 1
}
