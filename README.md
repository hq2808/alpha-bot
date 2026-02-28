# Alpha Bot 📊

Hệ thống tình báo tài chính real-time: Thu thập tin tức từ RSS, phân tích tâm lý bằng AI, gửi cảnh báo Telegram, và báo cáo cuối ngày.

## Stack

| Layer | Technology |
|---|---|
| **Backend** | Spring Boot 3.x, LangChain4j, Flyway |
| **Frontend** | Angular 17+, lightweight-charts |
| **Database** | PostgreSQL + Redis |
| **AI / LLM** | Groq Cloud hoặc Ollama (local) |
| **Messaging** | WebSocket (live news feed), Telegram Bot API |

---

## Khởi động nhanh (Docker)

```bash
# 1. Cấu hình môi trường
cp .env.example .env
# Điền các giá trị cần thiết (xem mục Biến môi trường bên dưới)

# 2. Chạy toàn bộ hệ thống
docker compose up -d

# 3. Truy cập
# Frontend:  http://localhost:4200
# API:       http://localhost:8080/api
# Health:    http://localhost:8080/api/health
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

---

#### `GET /api/test-alert`


```bash
curl http://localhost:8080/api/test-alert
```

#### `GET /api/test-eod-report`

```bash
curl http://localhost:8080/api/test-eod-report
```

---

## Kiến trúc tổng quan

```
RSS Feeds (DB) ──► NewsCrawlerService ──► SentimentAnalyzerService
                          │                         │
                          ▼                         ▼
                    NewsArticle (DB)         Telegram Alert
                          │
                          ├──► WebSocket /topic/news  (Frontend live feed)
                          │
                          └──► ReportService ──► FinancialAssistant (LLM)
                                                       │
                                                       ▼
                                               EOD Telegram Report (17:00)
```

