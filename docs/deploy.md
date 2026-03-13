## 🚀 실행 방법

### 로컬 실행
```bash
# 프로젝트 클론 후 AuthServerPlatform 디렉토리로 이동
cd AuthServerPlatform

# Gradle 빌드
./gradlew build

# 애플리케이션 실행 (dev 프로필)
./gradlew bootRun --args='--spring.profiles.active=dev'
```

## 🐳 Docker 배포

### Docker Compose 실행
```bash
# 환경 변수 파일 생성
cat > .env << 'EOF'
DB_URL=<DB_URL>
DB_USER=<DB_USER>
DB_PASSWORD=<DB_PASSWORD>
JWT_SECRET=<JWT_SECRET>
DISCORD_CLIENT_ID=<DISCORD_CLIENT_ID>
DISCORD_CLIENT_SECRET=<DISCORD_CLIENT_SECRET>
DISCORD_REDIRECT_URI=<DISCORD_REDIRECT_URI>
DISCORD_TOKEN_URI=<DISCORD_TOKEN_URI>
DISCORD_USER_URI=<DISCORD_USER_URI>
FRONT_REDIRECT_URI=<FRONT_REDIRECT_URI>
REGISTER_URI=[로그인 실패시 채팅방 가입 링크] 
SPRING_DATA_REDIS_HOST=[ex) redis]
SPRING_PROFILES_ACTIVE=[ex) prod]
EOF

# 컨테이너 빌드 및 실행
docker compose up -d --build

# 상태 확인
docker compose ps
```

## 🌐 배포 환경

### 운영 서버 설정
- **포트**: 4003
- **프로필**: prod
- **SSL**: 설정 가능 (현재 비활성화)
- **MySQL**: 외부 서버 연동
- **Redis**: 로컬 또는 외부 서버

### CI/CD 파이프라인
1. **GitHub Push** → main 브랜치
2. **SCP 배포** → 원격 서버 파일 전송
3. **Docker Build** → 컨테이너 이미지 생성
4. **Service Up** → docker-compose로 서비스 실행