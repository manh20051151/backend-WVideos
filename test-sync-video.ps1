# Test Sync Video Info API
# Cần thay VIDEO_ID và TOKEN

$VIDEO_ID = "your-video-id-here"
$TOKEN = "your-jwt-token-here"

Write-Host "🔄 Testing Sync Video Info API..." -ForegroundColor Cyan
Write-Host ""

# Sync video info
Write-Host "1. Syncing video info from DoodStream..." -ForegroundColor Yellow
$syncResponse = Invoke-RestMethod `
    -Uri "http://localhost:8080/api/videos/$VIDEO_ID/sync" `
    -Method POST `
    -Headers @{
        "Authorization" = "Bearer $TOKEN"
        "Content-Type" = "application/json"
    }

Write-Host "✅ Sync Response:" -ForegroundColor Green
$syncResponse | ConvertTo-Json -Depth 10

Write-Host ""
Write-Host "2. Getting video details..." -ForegroundColor Yellow
$videoResponse = Invoke-RestMethod `
    -Uri "http://localhost:8080/api/videos/$VIDEO_ID" `
    -Method GET `
    -Headers @{
        "Authorization" = "Bearer $TOKEN"
    }

Write-Host "✅ Video Details:" -ForegroundColor Green
$videoResponse | ConvertTo-Json -Depth 10

Write-Host ""
Write-Host "📊 Video Info Summary:" -ForegroundColor Cyan
Write-Host "  Title: $($videoResponse.result.title)"
Write-Host "  Status: $($videoResponse.result.status)"
Write-Host "  Views: $($videoResponse.result.views)"
Write-Host "  File Size: $($videoResponse.result.fileSize) bytes"
Write-Host "  Duration: $($videoResponse.result.duration) seconds"
Write-Host "  Thumbnail: $($videoResponse.result.thumbnailUrl)"
Write-Host "  Splash: $($videoResponse.result.splashImageUrl)"
