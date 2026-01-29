# WVideos Backend API

Backend API cho hệ thống WVideos với JWT Authentication và Swagger Documentation.

## 🚀 Quick Start

```bash
# Build project
.\mvnw.cmd clean install

# Run application
.\mvnw.cmd spring-boot:run
```

Application sẽ chạy tại: **http://localhost:8080**

## 📚 Documentation

- [Quick Start Guide](docs/guides/QUICK_START.md) - Hướng dẫn khởi động nhanh
- [API Testing Guide](docs/api/API_TESTING_GUIDE.md) - Hướng dẫn test API với Swagger
- [Database Setup](docs/guides/DATABASE_SETUP.md) - Hướng dẫn setup database
- [Copy User API Guide](docs/guides/COPY_USER_API_GUIDE.md) - Chi tiết quá trình copy API

## 🔗 Links

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/v3/api-docs
- **Database**: db_wvideos (MySQL)

## 📁 Project Structure

```
backendWVideos/
├── docs/              # Documentation
│   ├── guides/        # Hướng dẫn chi tiết
│   └── api/           # API documentation
├── scripts/           # Scripts
│   ├── sql/           # SQL scripts
│   └── powershell/    # PowerShell scripts
└── src/               # Source code
```

## 🔑 Default Admin Account

- Username: `admin`
- Email: `admin@wvideos.com`
- Password: `admin123`

## 🛠️ Tech Stack

- Java 17 + Spring Boot 3.2.2
- Spring Security + JWT
- MySQL 8
- Swagger/OpenAPI 3
- Lombok + MapStruct
