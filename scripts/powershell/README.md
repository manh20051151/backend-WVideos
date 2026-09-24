# PowerShell Scripts

Các PowerShell scripts hỗ trợ development và deployment.

## 📄 Available Scripts

### 1. copy-files.ps1
Copy files từ DocPro backend sang snha backend.

**Sử dụng:**
```powershell
.\copy-files.ps1
```

### 2. recopy-files.ps1
Copy lại files với package name đã được thay đổi.

**Sử dụng:**
```powershell
.\recopy-files.ps1
```

### 3. fix-bom.ps1
Fix BOM (Byte Order Mark) issues trong một file.

**Sử dụng:**
```powershell
.\fix-bom.ps1 -FilePath "path/to/file.java"
```

### 4. fix-all-bom.ps1
Fix BOM issues trong tất cả files Java đã copy.

**Sử dụng:**
```powershell
.\fix-all-bom.ps1
```

### 5. copy-user-api.ps1
Script tổng hợp để copy toàn bộ User API.

**Sử dụng:**
```powershell
.\copy-user-api.ps1
```

## ⚠️ Lưu ý

- Chạy scripts từ thư mục root của project
- Đảm bảo có quyền execute PowerShell scripts
- Backup code trước khi chạy scripts
