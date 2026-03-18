# 🌟 **빗자루 WebUI 프로젝트**
[**빗자루**](https://github.com/ConstellationCafe/DiscordBot)는 섀버 별자리 Cafe에서 개발한 채팅 봇입니다. 이 프로젝트의 목표는 채팅방 운영 도구와 섀도우버스 관련 편의 기능을 제공하는 것입니다.

## 📋 **프로젝트 개요**
이 프로젝트는 [WebUI 프로젝트](https://github.com/ConstellationCafe/web_frontend)의 백엔드 API 서버입니다.  
주요 기능으로는 Discord OAuth 2.0 사용자 인증, CRUD 기반 콘텐츠 관리, 학습 자료 제공, 메뉴 및 음악 추천 시스템 등이 포함되어 있습니다.

### 🎯 **주요 특징**
- **OAuth 2.0**: Discord OAuth 2.0 기반 사용자 인증 구현
- **JWT**: JSON Web Token(JWT)에 기반한 세션 관리
- **Redis**: 고속 캐싱 및 세션 데이터 저장
- **MySQL**: 관계형 데이터 저장소 활용
- **모듈화 아키텍처**: 도메인 모델 중심 설계
- **Docker 컨테이너**: 쉽고 빠른 배포 가능

## 🏗️ [**아키텍처**](./docs/architecture.md)
### **폴더 구조**
```plaintext
AuthServerPlatform/
├── src/main/java/com/help/
│   ├── authserver/        # 인증 서비스 모듈
│   │   ├── api/           # Discord API 통신
│   │   └── domain/user/   # 사용자 도메인 로직
│   │       ├── controller/ # REST 컨트롤러
│   │       ├── service/    # 비즈니스 로직
│   │       ├── repository/ # 데이터 접근 계층
│   │       └── dto/        # 데이터 전송 객체
│   └── backend/           # 백엔드 기능
│       └── domain/        # 도메인별 관리 (Contents, Menu 등)
├── src/main/resources/
    ├── application.yml    # Spring 설정 파일
    ├── application-dev.yml # 개발 환경 설정
    └── application-prod.yml # 운영 환경 설정
```

---

## 🔐 **인증 및 보안**

### OAuth 2.0 인증 플로우
1. **클라이언트(Discord WebUI)**: Discord 로그인 버튼 클릭
2. **OAuth 과정**: Discord 서버와 인증 코드 교환
3. **백엔드**: 인증 코드를 통해 Access Token 획득
4. **Discord 연동 DB 확인**: 사용자 Discord ID와 회원 데이터 일치 여부 검증
5. **JWT 생성 및 리턴**: 세션 유지 목적의 JWT 반환

### 보안 정책
- **JWT 인증 필터**: `/api/**` 경로 보호
- **CORS 허용**: 프론트엔드와 백엔드 분리 운영
- **Redis**: Stateless 상태 관리로 시스템 부하 감소
- **HTTPS 권장**: 민감 데이터 보호를 위한 전송 보안

---

## 🚀 **배포 및 실행 방법**
### **환경 요구사항**
- Java 17+
- Gradle 8.13 이상
- MySQL (8.0+)
- Redis 7.4+

### **도커 컨테이너**
- **백엔드**: `webui_be` (포트: 4003)
- **캐시**: `redis` (포트: 6379)

---

## 📚 **참조 자료**
- [API 명세서](./docs/api.md): 엔드포인트 구성 설명
- [아키텍처 문서](./docs/architecture.md): 시스템 아키텍처 세부사항
- [배포 가이드](./docs/deploy.md): 로컬 및 클라우드 환경 배포 절차

---
