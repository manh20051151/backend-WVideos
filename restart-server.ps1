# Script để restart Spring Boot server
Write-Host "Đang dừng server hiện tại..." -ForegroundColor Yellow

# Tìm và kill process Java đang chạy trên port 8080
$processes = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess
if ($processes) {
    foreach ($pid in $processes) {
        Write-Host "Đang dừng process ID: $pid" -ForegroundColor Red
        Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
    }
    Start-Sleep -Seconds 3
}

Write-Host "Đang compile code..." -ForegroundColor Green
./mvnw compile

Write-Host "Đang khởi động server..." -ForegroundColor Green
Start-Process -FilePath "powershell" -ArgumentList "-Command", "./mvnw spring-boot:run" -WindowStyle Normal

Write-Host "Server đang khởi động... Vui lòng đợi 30 giây" -ForegroundColor Cyan
Start-Sleep -Seconds 30

# Kiểm tra server đã sẵn sàng chưa
$maxRetries = 10
$retryCount = 0
do {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/api/categories" -Method GET -UseBasicParsing -TimeoutSec 5
        if ($response.StatusCode -eq 200) {
            Write-Host "✅ Server đã sẵn sàng!" -ForegroundColor Green
            break
        }
    }
    catch {
        $retryCount++
        Write-Host "Đang chờ server khởi động... ($retryCount/$maxRetries)" -ForegroundColor Yellow
        Start-Sleep -Seconds 5
    }
} while ($retryCount -lt $maxRetries)

if ($retryCount -eq $maxRetries) {
    Write-Host "❌ Server không thể khởi động. Vui lòng kiểm tra logs." -ForegroundColor Red
} else {
    Write-Host "🎉 Server đã sẵn sàng tại http://localhost:8080" -ForegroundColor Green
}