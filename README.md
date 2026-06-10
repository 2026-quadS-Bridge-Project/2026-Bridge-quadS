# 2026-Bridge-quadS
GDGoC 연합 프로젝트 quadS General Track Team Bridge의 코드 저장소

## Local Backend

```bash
export LOCAL_DB_PASSWORD=bridge_local
export JWT_SECRET_KEY='<jwt-secret>'
export GEMINI_API_KEY='<gemini-api-key-or-dummy-for-non-ai-flows>'
export AWS_S3_BUCKET='<s3-bucket-or-dummy-for-non-upload-flows>'

docker compose up -d postgres
JAVA_HOME=/opt/homebrew/opt/openjdk@21 PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH bash ./gradlew bootRun
```

The default local profile uses `jdbc:postgresql://localhost:5432/postgres`
with user `postgres`.
