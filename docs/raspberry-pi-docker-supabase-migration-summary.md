# Raspberry Pi Docker·Supabase 전환 작업 정리

기준일: 2026-07-26

## 1. 전환 결과

Discord 봇을 Raspberry Pi의 호스트 환경에서 직접 실행하는 방식 대신, GitHub Actions가 ARM64 Docker 이미지를 만들고 Raspberry Pi에 자동 배포하는 구조로 전환했다.

데이터베이스는 Raspberry Pi의 microSD에 MySQL 컨테이너를 두는 초기 계획에서 Supabase PostgreSQL을 사용하는 방식으로 변경했다. 따라서 Raspberry Pi에서는 현재 봇 컨테이너만 실행하며, 데이터베이스 컨테이너는 실행하지 않는다.

현재 구성은 다음과 같다.

```text
GitHub main 브랜치
  └─ GitHub Actions
      ├─ Gradle bootJar 빌드
      ├─ linux/arm64 Docker 이미지 빌드
      ├─ GHCR에 SHA/Latest 태그 푸시
      ├─ Tailscale로 Raspberry Pi 접속
      └─ SSH로 배포 스크립트 실행
          └─ Raspberry Pi의 Discord 봇 컨테이너 교체

Discord 봇 컨테이너
  └─ TLS를 사용해 Supabase PostgreSQL Session Pooler에 접속
```

## 2. 서버 및 실행 환경

- 서버: Raspberry Pi
- 운영체제: Ubuntu Server 26.04 LTS
- CPU 아키텍처: `aarch64`/ARM64
- 메모리: 8GB
- 저장장치: microSD
- 애플리케이션 런타임: Java 21
- 컨테이너 이미지: `ghcr.io/lunatic280/kotlindiscordbot-ops:<commit-sha>`

음성 기능에서 사용하는 libdave 네이티브 라이브러리가 ARM64에서 동작하도록 다음 런타임 의존성을 추가했다.

```kotlin
runtimeOnly("moe.kyokobot.libdave:natives-linux-aarch64:$libdaveVersion")
```

## 3. Supabase 연결 전환

### 3.1 직접 연결 실패

Supabase의 직접 연결 주소는 IPv6 주소로 해석되었지만 Raspberry Pi 네트워크에 IPv6 경로가 없어 다음 오류가 발생했다.

```text
Network is unreachable
```

이는 비밀번호나 PostgreSQL 설정 문제가 아니라, 서버에서 Supabase IPv6 주소까지 도달할 수 없었던 네트워크 문제였다.

### 3.2 Session Pooler 연결

IPv4를 지원하는 Supabase Session Pooler로 변경한 뒤 TCP 연결과 PostgreSQL 인증이 모두 성공했다.

- 호스트: `aws-1-ap-northeast-2.pooler.supabase.com`
- 포트: `5432`
- 데이터베이스: `postgres`
- 사용자: `postgres.uomrpfkorbrvucsviibw`
- SSL: `sslmode=require`
- 애플리케이션 스키마: `discordbot`

비밀번호는 이 문서나 Git 저장소에 저장하지 않는다. 실제 연결 정보는 Raspberry Pi의 `/opt/discordbot/.env`에만 보관한다.

애플리케이션의 JDBC 연결은 다음 형태를 사용한다.

```text
jdbc:postgresql://aws-1-ap-northeast-2.pooler.supabase.com:5432/postgres?sslmode=require&currentSchema=discordbot&ApplicationName=kotlin-discord-bot
```

데이터베이스 연결 풀은 외부 Pooler와 Raspberry Pi의 제한된 자원을 고려해 최대 5개로 설정했다.

### 3.3 스키마 초기화

Supabase에 `discordbot` 스키마를 만들고 초기 실행 시 Hibernate DDL 모드를 `update`로 사용해 필요한 테이블을 생성했다.

확인 대상 테이블은 다음과 같다.

- `members`
- `positions`
- `pubgplays`
- `wallets`

테이블 생성이 끝난 운영 환경에서는 DDL 모드를 `validate`로 바꿔 애플리케이션이 임의로 스키마를 변경하지 않도록 운영한다.

## 4. Docker 구성

### 4.1 이미지

`Dockerfile`은 Java 21 JRE 기반으로 봇을 실행한다. 컨테이너 내부에서는 root가 아닌 UID `10001` 사용자를 사용한다.

### 4.2 Compose

운영용 `compose.prod.yaml`에는 Discord 봇 서비스만 정의되어 있다.

적용된 주요 보호 설정은 다음과 같다.

- 메모리 제한: 2GB
- PID 제한: 256
- Linux capability 전체 제거
- `no-new-privileges` 적용
- 컨테이너 로그 크기와 파일 수 제한
- 외부 공개 포트 없음
- 비정상 종료 후 자동 재시작을 위한 `unless-stopped`

### 4.3 Raspberry Pi에 설치한 파일

```text
/opt/discordbot/compose.prod.yaml
/opt/discordbot/.env
/usr/local/bin/deploy-discordbot-container
```

권한이 필요한 설정과 비밀값은 다음 원칙으로 관리한다.

- `/opt/discordbot/.env`: `root:root`, 권한 `0600`
- Compose 파일: `root:root`, 권한 `0644`
- 배포 스크립트: `root:root`, 실행 권한 포함
- `.env`는 Git에 커밋하지 않음

## 5. GitHub Actions 자동 배포

`main` 브랜치에 애플리케이션 변경이 푸시되면 다음 절차가 실행된다.

1. Java 21 환경에서 Gradle `bootJar`를 빌드한다.
2. `linux/arm64` Docker 이미지를 빌드한다.
3. GHCR에 커밋 SHA 태그와 `latest` 태그를 푸시한다.
4. GitHub Actions Runner가 Tailscale에 연결한다.
5. Tailscale IP를 통해 Raspberry Pi에 SSH 접속한다.
6. `/usr/local/bin/deploy-discordbot-container`를 실행한다.
7. 새 컨테이너에서 `Discord JDA ready.` 로그가 나오는지 확인한다.
8. 제한 시간 안에 정상 상태가 되지 않으면 이전 이미지로 자동 롤백한다.

배포 스크립트에는 다음 검증이 적용되어 있다.

- 허용된 GHCR 이미지 저장소만 사용
- 40자리 Git 커밋 SHA 태그만 허용
- `.env`의 `APP_IMAGE`를 원자적으로 변경
- 새 컨테이너 강제 재생성
- 최대 90초 동안 JDA 준비 로그 확인
- 실패 시 이전 이미지 복구

GitHub 저장소 정책이 Marketplace Action에 전체 커밋 SHA 고정을 요구해, 워크플로에서 사용하는 모든 Action을 전체 SHA로 고정했다.

## 6. GitHub 환경 설정

GitHub의 `prod_bot` Environment에 다음 값을 설정했다.

Secrets:

- `TS_OAUTH_CLIENT_ID`
- `TS_OAUTH_SECRET`
- `SSH_PRIVATE_KEY`

Variables:

- `DEPLOY_HOST`
- `DEPLOY_PORT`
- `DEPLOY_USER`

GitHub Secrets 값은 저장 후 화면에서 다시 표시되지 않는 것이 정상이다.

SSH 개인 키는 줄바꿈이 유지된 OpenSSH 형식 전체를 저장해야 한다. 키 형식이 깨지면 다음 오류가 발생할 수 있다.

```text
Load key ".../id_ed25519": error in libcrypto
```

또한 이 개인 키와 짝이 맞는 공개 키가 Raspberry Pi 사용자의 `~/.ssh/authorized_keys`에 등록되어 있어야 한다.

## 7. 확인된 배포 상태와 남은 문제

확인된 내용:

- ARM64 Docker 이미지 빌드와 GHCR 푸시는 성공했다.
- Supabase Session Pooler에 PostgreSQL 클라이언트로 접속했다.
- 일반 Discord 명령어는 동작한다.

아직 해결되지 않은 내용:

- PUBG 관련 명령어만 응답하지 않으며 관련 로그도 충분히 남지 않는다.

코드 검토를 바탕으로 한 가능성 높은 원인은 다음과 같다.

1. PUBG API 호출부가 비동기 이벤트 스레드에서 블로킹 호출을 수행한다.
2. 처리 시간이 긴 명령에서 `deferReply()`가 없어 Discord 응답 제한 시간을 넘길 수 있다.
3. 명령 진입 로그와 예외 처리, HTTP 타임아웃이 부족해 실패 원인이 로그에 드러나지 않는다.
4. Raspberry Pi의 `/opt/discordbot/.env`에 저장된 `PUBG_API_KEY`가 누락되었거나 유효하지 않을 수 있다.

위 항목은 현재까지의 근거를 바탕으로 한 추론이며, 아직 확정된 원인은 아니다. 별도의 로그 보강과 API 응답 확인이 필요하다.

## 8. 관련 파일

- `.github/workflows/deploy.yml`: ARM64 이미지 빌드 및 Raspberry Pi 자동 배포
- `Dockerfile`: 봇 컨테이너 이미지 정의
- `compose.prod.yaml`: 운영 컨테이너 설정
- `env.example`: 필요한 환경 변수 예시
- `scripts/deploy-discordbot-container`: 서버 배포 및 롤백 스크립트
- `docs/supabase-docker-deployment.md`: 최초 설치 및 설정 절차
- `docs/docker-container-operations-guide.md`: 배포 후 컨테이너 운영 절차
