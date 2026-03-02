# Script kiểm tra Video API endpoints

Write-Host "=== Test Video API Endpoints ===" -ForegroundColor Cyan

# Test 1: Check if backend is running
Write-Host "`n1. Kiểm tra backend đang chạy..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/swagger-ui.html" -Method GET -TimeoutSec 5 -ErrorAction Stop
    Write-Host "✓ Backend đang chạy" -ForegroundColor Green
} catch {
    Write-Host "✗ Backend không chạy hoặc không truy cập được" -ForegroundColor Red
    Write-Host "Vui lòng start backend trước khi test" -ForegroundColor Yellow
    exit 1
}

# Test 2: Check public videos endpoint (không cần auth)
Write-Host "`n2. Test GET /api/videos/public (không cần auth)..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/videos/public?page=0&size=10" -Method GET -ErrorAction Stop
    Write-Host "✓ Endpoint /api/videos/public hoạt động" -ForegroundColor Green
    Write-Host "Response: $($response | ConvertTo-Json -Depth 2)" -ForegroundColor Gray
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 404) {
        Write-Host "✗ Endpoint không tồn tại (404)" -ForegroundColor Red
        Write-Host "Có thể VideoController chưa được load. Hãy restart backend!" -ForegroundColor Yellow
    } elseif ($statusCode -eq 401 -or $statusCode -eq 403) {
        Write-Host "✗ Endpoint cần authentication ($statusCode)" -ForegroundColor Red
        Write-Host "Cần kiểm tra SecurityConfig" -ForegroundColor Yellow
    } else {
        Write-Host "✗ Lỗi: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Test 3: Check upload endpoint (cần auth - sẽ trả về 401)
Write-Host "`n3. Test POST /api/videos/upload (cần auth - expect 401)..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/videos/upload" -Method POST -ErrorAction Stop
    Write-Host "✗ Không nên trả về success khi chưa auth" -ForegroundColor Red
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 404) {
        Write-Host "✗ Endpoint không tồn tại (404)" -ForegroundColor Red
        Write-Host "VideoController chưa được load. Hãy restart backend!" -ForegroundColor Yellow
    } elseif ($statusCode -eq 401 -or $statusCode -eq 403) {
        Write-Host "✓ Endpoint tồn tại và yêu cầu authentication ($statusCode)" -ForegroundColor Green
    } elseif ($statusCode -eq 400) {
        Write-Host "✓ Endpoint tồn tại (400 - thiếu data)" -ForegroundColor Green
    } else {
        Write-Host "? Status code: $statusCode" -ForegroundColor Yellow
    }
}

# Test 4: List all endpoints from Swagger
Write-Host "`n4. Kiểm tra danh sách endpoints từ Swagger..." -ForegroundColor Yellow
try {
    $swagger = Invoke-RestMethod -Uri "http://localhost:8080/api/v3/api-docs" -Method GET -ErrorAction Stop
    $videoPaths = $swagger.paths.PSObject.Properties | Where-Object { $_.Name -like "*video*" }
    
    if ($videoPaths.Count -gt 0) {
        Write-Host "✓ Tìm thấy $($videoPaths.Count) video endpoints:" -ForegroundColor Green
        foreach ($path in $videoPaths) {
            Write-Host "  - $($path.Name)" -ForegroundColor Gray
        }
    } else {
        Write-Host "✗ Không tìm thấy video endpoints trong Swagger" -ForegroundColor Red
        Write-Host "VideoController chưa được load. Hãy restart backend!" -ForegroundColor Yellow
    }
} catch {
    Write-Host "✗ Không thể lấy Swagger docs: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n=== Kết luận ===" -ForegroundColor Cyan
Write-Host "Nếu thấy lỗi 404, hãy:" -ForegroundColor Yellow
Write-Host "1. Stop backend" -ForegroundColor White
Write-Host "2. Clean và rebuild: mvn clean install" -ForegroundColor White
Write-Host "3. Start lại backend" -ForegroundColor White
Write-Host "4. Chạy lại script này để verify" -ForegroundColor White
