# WebUI_BE 실행과 배포

> 상태: Active  
> 마지막 검토일: 2026-09-28  
> 상위 문서: [README](../README.md) · 관련: [설정](configuration.md), [migration](migrations/README.md), [운영](operations.md)

## 1. 로컬 실행

필요: JDK 17, MySQL 8+(두 datasource), Redis 7.4. 환경 변수는 [설정](configuration.md)을 참고합니다.

```bash
cd AuthServerPlatform

# 실행 (dev profile)
./gradlew bootRun --args='--spring.profiles.active=dev'

# 검증: Spotless·EditorConfig·Checkstyle·테스트·배포 jar
./gradlew clean check bootJar --no-daemon
```

dev profile의 Redis host 기본값은 `redis`입니다. Redis를 로컬에서 띄웠다면 `SPRING_DATA_REDIS_HOST=localhost`를 함께 주입합니다.

## 2. Docker Compose

저장소 루트에 커밋되지 않는 `.env`를 만들고 값을 채웁니다. 아래는 형식 예시이며 실제 값은 넣지 않습니다.

```bash
cat > .env << 'EOF'
SPRING_PROFILES_ACTIVE=prod
DB_URL=<jdbc:mysql://host:3306/database>
DB_CONFIG_URL=<jdbc:mysql://host:3306/config>
DB_USER=<db_user>
DB_PASSWORD=<db_password>
JWT_SECRET=<random_secret>
DISCORD_CLIENT_ID=<discord_client_id>
DISCORD_CLIENT_SECRET=<discord_client_secret>
DISCORD_REDIRECT_URI=<https://.../auth/discord_login>
DISCORD_TOKEN_URI=<discord_token_endpoint>
DISCORD_USER_URI=<discord_user_endpoint>
DISCORD_GUILDS_URI=<discord_guilds_endpoint>
FRONT_REDIRECT_URI=<frontend_uri>
REGISTER_URI=<채팅방_가입_안내_링크>
EOF

docker compose config --quiet   # 필수 변수 누락 시 여기서 실패
docker compose up -d --build
docker compose ps
```

- 서비스: `redis`(6379, AOF 사용, `redis_data` volume) → healthy 후 `webui_be`(4003).
- `SPRING_DATA_REDIS_HOST`는 Compose가 `redis`로 고정합니다.
- 외부 알림 발행 client(`INTEGRATION_CLIENTS_*`)를 쓰려면 `docker-compose.yml`의 `webui_be.environment`에 먼저 추가해야 합니다([설정](configuration.md)).

확인:

```bash
curl -fsS http://localhost:4003/actuator/health/readiness
```

## 3. CI/CD

| workflow | trigger | 내용 |
|---|---|---|
| `CI.yml` | PR → `main`/`develope`, push → `develope` | Gradle wrapper 검증, `./gradlew clean check bootJar --no-daemon`, Docker image build |
| `CD.yml` | push → `main` | SCP로 저장소 파일을 원격 서버에 전송(`rm: true`) → `docker compose -p webui-be config --quiet` → `up -d --build --remove-orphans` → `ps` |

- CD는 동시 실행을 막습니다(`concurrency: webui-be-production`, 진행 중 배포 취소 안 함).
- 원격 서버 접속 정보와 애플리케이션 비밀값은 모두 GitHub Actions secrets입니다.
- 작업 흐름: 기능 브랜치 → `develope` PR(CI 통과) → `main` 병합 시 배포.

## 4. 배포 순서

1. DB backup 확인
2. 필요한 [migration](migrations/README.md)을 순서대로 수동 적용하고 검증 query 실행
3. WebUI_BE `main` 병합 → CD 완료 확인
4. readiness와 핵심 API(로그인, 채팅방 선택) 확인
5. API 계약이 바뀐 릴리스는 WebUI_FE도 같은 시점에 `main`으로 병합·배포

## 5. rollback

- 이전 검증 완료 commit을 `main`에 되돌려(revert) 다시 배포하거나, 이전 image로 되돌린 뒤 readiness를 확인합니다.
- 경로가 바뀐 릴리스는 BE와 FE를 함께 되돌립니다([ADR-0002](adr/0002-admin-api-prefix.md)).
- schema를 줄이는 migration(0004 등) 이후에는 backup 복원이 먼저입니다.

## 6. 운영 서버 메모

- 포트: 4003 (앞단 reverse proxy·TLS는 서버 구성에 따름)
- SSL: 애플리케이션 내부 TLS는 비활성(`application-dev.yml`에 주석으로만 남아 있음)
- SSE를 위한 reverse proxy 설정: [운영](operations.md#실시간-알림sse)
