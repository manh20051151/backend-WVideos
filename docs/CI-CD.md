# CI/CD Backend WVideos

Luồng tự động mỗi khi push lên nhánh `master`:

```
push code → CI build jar (GitHub) → Docker build + push Docker Hub (GitHub) → deploy trên máy chủ (self-hosted runner)
```

- Pull request chỉ chạy CI (GitHub-hosted), **không** chạy trên máy của bạn → an toàn.
- Job deploy chạy trực tiếp trên máy chủ 24/7 nhờ **self-hosted runner** (không cần SSH).

## 1. File trong repo

| File | Vai trò |
|---|---|
| `Dockerfile` | Image multi-stage: Maven build jar → JRE 17 Alpine chạy app, user không đặc quyền |
| `.dockerignore` | Loại `.env`, `target/`, IDE config khỏi image |
| `docker-compose.prod.yml` | Chạy backend trên máy chủ (MySQL/Redis ngoài, inject env từ file `.env`) |
| `.github/workflows/ci-cd.yml` | Workflow GitHub Actions |

## 2. Cài self-hosted runner trên máy chủ (chạy 1 LẦN)

Máy chủ = máy Windows chạy 24/7 (cần Docker Desktop chạy nền).

1. Vào repo GitHub → **Settings → Actions → Runners → New self-hosted runner → Windows / x64**.
2. Làm theo hướng dẫn tải + giải nén, ví dụ vào `C:\actions-runner`:
   ```powershell
   mkdir C:\actions-runner; cd C:\actions-runner
   # giải nén file zip runner vào đây
   ./config.cmd --url https://github.com/manh20051151/backend-WVideos --token <TOKEN> --labels wvideos
   ./svc.cmd install   # cài thành Windows service
   ./svc.cmd start     # chạy nền 24/7
   ```
   Quan trọng: thêm **label `wvideos`** (workflow chỉ chạy job deploy trên runner có label này).
3. Kiểm tra: repo → Settings → Actions → Runners thấy runner **Idle**.

## 3. Cấu hình GitHub Secrets

**Repo → Settings → Secrets and variables → Actions → New repository secret**:

| Secret | Giá trị | Lấy ở đâu |
|---|---|---|
| `DOCKERHUB_USERNAME` | Tài khoản Docker Hub (VD: `manh20051151`) | hub.docker.com |
| `DOCKERHUB_TOKEN` | Access token | hub.docker.com → Account Settings → Personal access tokens → Generate (Read/Write) |
| `DEPLOY_PATH` | Thư mục deploy trên máy chủ (VD: `E:\deploy\wvideos`) | |

## 4. Chuẩn bị thư mục deploy trên máy chủ (chạy 1 lần)

```powershell
mkdir E:\deploy\wvideos\backend
cd E:\deploy\wvideos\backend
# Copy docker-compose.prod.yml từ repo vào đây
# Tạo file .env với cấu hình thật, ví dụ:
notepad .env
```

Nội dung `.env` (giống `.env` đang dùng ở local — MySQL/Redis trên chính máy này nên dùng `host.docker.internal`):

```env
DB_URL=jdbc:mysql://host.docker.internal:3306/db_wvideos?createDatabaseIfNotExist=true
DB_USERNAME=root
DB_PASSWORD=mat-khau-mysql
REDIS_HOST=host.docker.internal
JWT_SIGNER_KEY=...
DOODSTREAM_API_KEY=...
STREAMTAPE_API_LOGIN=...
STREAMTAPE_API_KEY=...
SEPAY_ACCOUNT_NUMBER=...
SEPAY_API_TOKEN=...
MAIL_USERNAME=...
MAIL_PASSWORD=...
```

## 5. Tạo Docker Hub repo

hub.docker.com → **Create repository** → tên `wvideos-backend` → Public/Private.

> Nếu đặt Private: trên máy chủ chạy 1 lần `docker login` để cho phép pull image.

## 6. Kiểm tra

1. Commit + push lên `master`.
2. Tab **Actions**: 3 job lần lượt Build → Docker → **Deploy trên máy chủ**.
3. Trên máy chủ: `docker ps` thấy container `wvideos-backend` chạy.

## Rollback

```powershell
cd E:\deploy\wvideos\backend
docker pull manh20051151/wvideos-backend:<ma-sha-cu>   # mỗi commit có 1 tag SHA
$env:IMAGE = "manh20051151/wvideos-backend:<ma-sha-cu>"
docker compose -f docker-compose.prod.yml up -d
```
