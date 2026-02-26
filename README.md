# Alpha Bot 📊

Hệ thống tình báo tài chính real-time: Thu thập tin tức, phân tích tâm lý bằng AI, và gửi cảnh báo đến trader.

## Stack
- **Backend:** Spring Boot 3.x + LangChain4j
- **Frontend:** Angular 17+
- **Database:** PostgreSQL + Redis
- **AI:** Groq (cloud) hoặc Ollama (local)

## Khởi động nhanh

```bash
# 1. Cấu hình môi trường
cp .env.example .env
# Điền GROQ_API_KEY hoặc để nguyên nếu dùng Ollama local

# 2. Chạy toàn bộ hệ thống
docker compose up -d

# 3. Truy cập
# Frontend:  http://localhost:4200
# API:       http://localhost:8080/api
# Swagger:   http://localhost:8080/swagger-ui.html
```

## Chạy môi trường Development

```bash
# Backend
cd backend
./mvnw spring-boot:run

# Frontend (terminal khác)
cd frontend
npm install
ng serve
```
