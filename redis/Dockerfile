# Redis 기본 이미지 사용
FROM redis:7.4.2-alpine

# 필요한 추가 패키지 설치 (선택사항)
# RUN apk add --no-cache <필요한 패키지>

# 커스텀 설정 파일을 컨테이너 내로 복사
# COPY ./redis.conf /usr/local/etc/redis/redis.conf

# 커스텀 명령어로 Redis 서버 실행
CMD ["redis-server", "/usr/local/etc/redis/redis.conf", "--appendonly", "yes", "--appendfsync", "everysec"]
