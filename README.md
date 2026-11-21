# 11th-neordinary-hackathon-server
11기 너디너리 해커톤 서버 레포입니다.

## 로컬 개발

```bash
docker compose up -d      # 로컬 MySQL 8.4 실행 (localhost:3306)
./gradlew bootRun         # 기본 프로필 dev → 로컬 MySQL 연결 (JDK 25 필요)
```

| 항목 | 값 |
|---|---|
| Host / Port | `localhost` / `3306` |
| Database | `hackathon` |
| User / Password | `hackathon` / `hackathon` (로컬 전용) |

- 데이터 초기화: `docker compose down -v`
- 운영 서버: https://neo8.duckdns.org ([Swagger](https://neo8.duckdns.org/swagger-ui/index.html))
- 브랜치: `develop`에서 개발 → `main` 머지 시 Cloud Run 자동 배포
