# ===== Giai đoạn 1: Build jar bằng Maven =====
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml trước để cache layer dependency (chạy lại khi pom đổi)
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
# Bỏ qua test trong image (test cần DB/Redis thật, chạy ở máy dev)
RUN mvn -B -ntp package -DskipTests

# ===== Giai đoạn 2: Runtime chỉ cần JRE nhẹ =====
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Tạo user không đặc quyền để chạy app (an toàn hơn root)
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/*.jar app.jar
RUN chown -R app:app /app
USER app

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
