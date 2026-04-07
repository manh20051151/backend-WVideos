# AGENTS.md - WVideos Backend

## Commands
```bash
.\mvnw.cmd clean install    # Build project
.\mvnw.cmd spring-boot:run  # Run application (port 8080)
```

## API
- Base URL: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Database: MySQL db_wvideos

## Config
- File: `src/main/resources/application.yaml`
- Env: `.env` file in project root

## Quirks
- Spring Boot 3.2.2, Java 17
- DTOs dùng `@JsonProperty` cho snake_case fields từ external APIs
- Entity relationships dùng `@OneToMany`, `@ManyToOne` với lazy loading

## Skill Files
- `docs/opencode-skill-vietnamese.md` - Comment và log bằng Tiếng Việt

## Code Conventions
- Lombok cho boilerplate (`@Data`, `@Builder`, etc.)
- Service layer xử lý business logic
- Repository cho database operations