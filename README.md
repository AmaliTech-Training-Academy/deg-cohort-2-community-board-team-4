# CommunityBoard
**AmaliTech Group Project – Full-Stack Teams (Teams 1-5)**

A community notice board where users can post announcements, events, and discussions. Supports categories, comments, and search.

## Tech Stack
- **Backend:** Java 17 + Spring Boot 3.2, Spring Security (JWT), Spring Data JPA, PostgreSQL
- **Frontend:** React 18, React Router, Axios, Chart.js
- **Data Engineering:** Python ETL pipeline, analytics aggregation
- **QA:** REST Assured (API), Selenium WebDriver (UI)
- **DevOps:** Docker, docker-compose, GitHub Actions CI

## Getting Started
```bash
docker-compose up --build
```
- Backend API: http://localhost:8080/swagger-ui.html
- Frontend: http://localhost:3000

## Testing

### Backend (JUnit 5)
Unit tests live under `backend/src/test/java`. They are pure unit tests — they mock all
collaborators (no Spring context, no database), so they run in milliseconds and are safe to
run in CI pipelines. The dependencies (JUnit 5, Mockito, AssertJ) come bundled with
`spring-boot-starter-test`, so no extra setup is required.

```bash
cd backend

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=PostServiceTest

# Run a single test method
mvn test -Dtest=PostServiceTest#returnsResponseWhenFound
```

Test reports are written to `backend/target/surefire-reports/`.

In CI, run the same command from the `backend/` directory:
```yaml
- name: Run backend unit tests
  run: mvn -B test
  working-directory: backend
```

## Default Users (seeded)
| Email | Password | Role |
|---|---|---|
| admin@amalitech.com | password123 | ADMIN |
| user@amalitech.com | password123 | USER |

## Project Structure
```
backend/          - Spring Boot REST API
frontend/         - React 18 SPA
data-engineering/ - Python ETL & analytics
qa/               - API & UI test suites
devops/           - Docker, CI/CD configs
```

## What's Implemented (~30%)
- [x] User authentication (register/login with JWT)
- [x] Basic post CRUD (create, read, update, delete)
- [x] Category management
- [ ] Comments system (TODO)
- [ ] Search & filtering (TODO)
- [ ] User profiles (TODO)
- [ ] Notifications (TODO)
- [ ] Analytics dashboard (TODO)
