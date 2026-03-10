# 🌟 빗자루 WebUI 프로젝트
**빗자루**는 섀버 별자리 Cafe에서 개발 및 운영하는 채팅 봇으로, 섀도우버스 관련 편의 기능부터 채팅방 운영에 필요한 도구들을 제공합니다.

## 📋 프로젝트 개요

해당 프로젝트는 [WebUI 프로젝트](https://github.com/ConstellationCafe/WebUI_BE)의 백엔드 API 서버입니다. Discord OAuth 2.0 인증을 통한 사용자 관리와 콘텐츠, 학습 자료, 메뉴, 음악 추천 등의 CRUD 기능을 제공합니다.

### 🎯 **주요 특징**
- Discord OAuth 2.0 기반 인증 시스템 (회원증 데베 사용)
- JWT 토큰 기반 세션 관리
- Redis를 활용한 세션 저장소
- MySQL 데이터베이스 연동
- 도메인 중심의 모듈화된 아키텍처
- Docker 컨테이너 기반 배포

## 🏗️ 아키텍처

### 시스템 구조
```
┌─────────────────┐    ┌──────────────────┐    
│   Flutter Web   │    │   Spring Boot    │    │     MySQL       │                 │    │                  │ 
│   (Port 1104)   │◄──►│   (Port 4003)    │◄──►│   Database      │                 │    │                  │ 
│                 │    │                  │    
└─────────────────┘    └──────────────────┘    
                                │
                                ▼
                       ┌──────────────────┐
                       │      Redis       │
                       │   (Port 6379)    │
                       │  Session Store   │
                       └──────────────────┘
```

### 폴더 구조
```
AuthServerPlatform/
├── src/main/java/com/help/
│   ├── authserver/         # 인증 서버 모듈
│   │   ├── api/            # 외부 API 통신
│   │   └── domain/user/    # 사용자 도메인
│   │       ├── controller/ # REST 컨트롤러
│   │       ├── service/    # 비즈니스 로직
│   │       ├── repository/ # 데이터 접근
│   │       ├── entity/     # JPA 엔티티
│   │       └── dto/        # 데이터 전송 객체
│   ├── backend/            # 백엔드 API 모듈
│   │   └── domain/
│   │       ├── content/    # 콘텐츠 추천
│   │       ├── learning/   # 학습 자료
│   │       ├── menu/       # 메뉴 추천
│   │       ├── music/      # 음악 추천
│   │       └── metadata/   # 메타데이터
│   └── global/             # 공통 설정
│       ├── config/         # 설정 파일
│       ├── jwt/            # JWT 처리
│       └── common/         # 공통 유틸리티
└── src/main/resources/
    ├── application.yml     # 기본 설정
    ├── application-dev.yml # 개발 환경 설정
    └── application-prod.yml# 운영 환경 설정
```

## 🔐 인증 시스템

### Discord OAuth 2.0 Flow
1. **프론트엔드**: Discord 로그인 버튼 클릭
2. **Discord**: 사용자 인증 후 Authorization Code 발급
3. **백엔드**: Code를 받아 Discord API에서 Access Token 획득
4. **백엔드**: Discord 사용자 정보 조회
5. **백엔드**: 회원증 DB에서 Discord ID 확인
6. **백엔드**: JWT 토큰 발급 및 쿠키 설정
7. **프론트엔드**: 메인 페이지로 리다이렉트

### 보안 설정
- **JWT 필터**: `/auth/**`와 `/api/**` 경로별 분리된 필터
- **CORS 설정**: 프론트엔드 도메인 허용
- **세션 정책**: Stateless (JWT 기반)
- **Redis 세션**: 토큰 관리 및 캐싱

## 📱 API 엔드포인트

### 🔑 인증 API (`/auth`)
| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/auth/discord_login` | Discord OAuth 로그인 |
| `GET` | `/auth/me` | 현재 사용자 정보 |
| `POST` | `/auth/refresh` | 토큰 갱신 |
| `GET` | `/auth/check` | 로그인 상태 확인 |
| `POST` | `/auth/logout` | 로그아웃 |

### 📚 콘텐츠 API (`/api/repository`)
| 도메인 | Endpoint | 설명 |
|--------|----------|------|
| **Content** | `/content/list` | 추천 콘텐츠 목록 |
| | `/content/save_all` | 콘텐츠 일괄 저장 |
| | `/content/delete_all` | 콘텐츠 일괄 삭제 |
| **Learning** | `/learning/list` | 학습 자료 목록 |
| | `/learning/save_all` | 학습 자료 일괄 저장 |
| **Menu** | `/menu/list` | 메뉴 추천 목록 |
| | `/menu/save_all` | 메뉴 일괄 저장 |
| **Music** | `/music/list` | 음악 추천 목록 |
| | `/music/save_all` | 음악 일괄 저장 |

### 👥 멤버십 API
| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/membership/list` | 멤버십 정보 목록 |
| `POST` | `/membership/save_all` | 멤버십 정보 저장 |

## 🗄️ 데이터베이스

### 주요 엔티티
- **User**: 기본 사용자 정보
- **DiscordUser**: Discord 연동 사용자 정보
- **UserProfile**: 사용자 프로필
- **ContentEntity**: 추천 콘텐츠
- **LearningEntity**: 학습 자료
- **MenuEntity**: 메뉴 추천
- **MusicEntity**: 음악 추천
- **MembershipEntity**: 멤버십 정보

### 데이터베이스 설정
- **Primary**: MySQL (DDL auto-update)
- **Secondary**: MySQL (DDL none)
- **Cache**: Redis (세션 및 임시 데이터)

## 🚀 실행 방법

### 환경 요구사항
- **Java**: 17+
- **Gradle**: 8.13+
- **MySQL**: 8.0+
- **Redis**: 7.4+

### 로컬 실행
```bash
# 프로젝트 클론 후 AuthServerPlatform 디렉토리로 이동
cd AuthServerPlatform

# Gradle 빌드
./gradlew build

# 애플리케이션 실행 (dev 프로필)
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 환경 변수
실행 시 다음 환경 변수를 설정해야 합니다:

#### 데이터베이스
- `DB_URL`: MySQL 연결 URL
- `DB_USER`: MySQL 사용자명
- `DB_PASSWORD`: MySQL 비밀번호

#### Discord OAuth
- `DISCORD_CLIENT_ID`: Discord 애플리케이션 클라이언트 ID
- `DISCORD_CLIENT_SECRET`: Discord 애플리케이션 시크릿
- `DISCORD_REDIRECT_URI`: OAuth 리다이렉트 URI
- `DISCORD_TOKEN_URI`: Discord 토큰 엔드포인트
- `DISCORD_USER_URI`: Discord 사용자 정보 엔드포인트

#### 기타
- `FRONT_REDIRECT_URI`: 프론트엔드 리다이렉트 URI
- `REGISTER_URI`: 등록 관련 URI
- `JWT_SECRET`: JWT 서명용 시크릿 키
- `SPRING_PROFILES_ACTIVE`: 활성 프로필 (dev/prod)

## 🐳 Docker 배포

### Docker Compose 실행
```bash
# 환경 변수 파일 생성
cat > .env << 'EOF'
DB_URL=jdbc:mysql://your-mysql-host:3306/database
DB_USER=your-username
DB_PASSWORD=your-password
DISCORD_CLIENT_ID=your-discord-client-id
# ... 기타 환경 변수
EOF

# 컨테이너 빌드 및 실행
docker compose up -d --build

# 상태 확인
docker compose ps
```

### 컨테이너 구성
- **webui_be**: Spring Boot 애플리케이션 (포트 4003)
- **redis-server**: Redis 캐시 서버 (포트 6379)

## 📚 사용된 주요 라이브러리

### 핵심 프레임워크
- `spring-boot-starter-web`: 웹 애플리케이션 기본
- `spring-boot-starter-data-jpa`: JPA 데이터 접근
- `spring-boot-starter-security`: Spring Security
- `spring-boot-starter-validation`: Bean Validation

### 인증 & 보안
- `jjwt-api`, `jjwt-impl`, `jjwt-jackson`: JWT 토큰 처리
- `spring-boot-starter-data-redis`: Redis 연동

### 데이터베이스
- `mysql-connector-j`: MySQL 드라이버
- `h2database`: 테스트용 인메모리 DB

### 유틸리티
- `lombok`: 코드 간소화
- `spring-boot-starter-webflux`: 외부 API 통신
- `spring-boot-starter-mail`: 이메일 발송 (예약)

## 🔧 개발 도구

### 코드 품질
- **Checkstyle**: Naver 코딩 컨벤션 적용
- **EditorConfig**: 일관된 코드 스타일
- **JUnit**: 단위 테스트
- **Spring Boot DevTools**: 개발 중 자동 재시작

### CI/CD
- **GitHub Actions**: 자동 배포 파이프라인
- **SCP**: 프로젝트 파일 전송
- **Docker Compose**: 컨테이너 오케스트레이션

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

## 📝 개발 참고사항

### 인증 시스템 특징
- **회원가입 없음**: Discord OAuth만으로 인증
- **멤버십 확인**: Discord ID가 회원증 DB에 존재하는지 확인
- **JWT 기반**: Stateless 인증으로 확장성 확보

### API 설계 원칙
- **도메인별 분리**: Content, Learning, Menu, Music별 독립적 관리
- **일괄 처리**: `save_all`, `delete_all` 엔드포인트로 효율성 증대
- **권한 기반**: 인증된 사용자만 백엔드 API 접근 가능

### 확장 가능성
- **마이크로서비스**: 도메인별 서비스 분리 가능한 구조
