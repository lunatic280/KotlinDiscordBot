# Raspberry Pi Discord 봇 컨테이너 운영 가이드

기준일: 2026-07-26

이 문서는 최초 설치가 끝난 뒤 Raspberry Pi에서 Discord 봇 컨테이너를 점검하고 배포·재시작·롤백하는 방법을 설명한다.

## 1. 기본 원칙

운영 명령은 Raspberry Pi에 SSH로 접속한 뒤 실행한다.

```bash
cd /opt/discordbot
```

이 문서에서 사용하는 Compose 명령의 공통 형태는 다음과 같다.

```bash
sudo docker compose --env-file .env -f compose.prod.yaml <명령>
```

`.env`에는 Discord 토큰, Supabase 비밀번호, PUBG API 키 등 비밀값이 있으므로 화면 공유 중 출력하거나 Git에 올리지 않는다.

## 2. 현재 상태 확인

### 컨테이너 상태

```bash
sudo docker compose --env-file .env -f compose.prod.yaml ps
```

정상 상태라면 `bot` 컨테이너가 `Up` 상태로 표시된다.

실행 중인 전체 컨테이너를 확인하려면 다음 명령을 사용한다.

```bash
sudo docker ps
```

### 현재 사용 중인 이미지

```bash
sudo docker inspect \
  --format='{{.Config.Image}}' \
  "$(sudo docker compose --env-file .env -f compose.prod.yaml ps -q bot)"
```

출력 예시는 다음과 같다.

```text
ghcr.io/lunatic280/kotlindiscordbot-ops:0123456789abcdef0123456789abcdef01234567
```

### CPU와 메모리 사용량

```bash
sudo docker stats --no-stream
```

봇 컨테이너에는 2GB 메모리 제한이 적용되어 있다. 메모리가 제한에 계속 가까워지거나 재시작 횟수가 증가하면 JVM 메모리 설정과 누수 여부를 확인한다.

### 디스크 사용량

```bash
sudo docker system df
```

microSD 용량이 부족해지기 전에 이미지와 로그 사용량을 정기적으로 확인한다.

## 3. 로그 확인

### 최근 로그 200줄

```bash
sudo docker compose --env-file .env -f compose.prod.yaml logs --tail 200 bot
```

### 실시간 로그

```bash
sudo docker compose --env-file .env -f compose.prod.yaml logs -f bot
```

종료하려면 `Ctrl+C`를 누른다. 컨테이너 자체는 종료되지 않는다.

### 최근 15분 로그

```bash
sudo docker compose --env-file .env -f compose.prod.yaml logs --since 15m bot
```

### 준비 완료 및 오류 확인

```bash
sudo docker compose --env-file .env -f compose.prod.yaml logs --tail 500 bot \
  | grep -E 'Discord JDA ready|ERROR|Exception|FATAL'
```

`Discord JDA ready.`가 보이면 Discord 연결과 JDA 초기화가 완료된 것이다. 이 메시지가 없다고 해서 원인이 하나로 확정되는 것은 아니므로 그 앞의 오류 로그를 함께 확인한다.

## 4. 재시작과 설정 반영

### 같은 설정으로 단순 재시작

```bash
sudo docker compose --env-file .env -f compose.prod.yaml restart bot
```

단순 재시작은 현재 컨테이너를 다시 시작할 뿐이다. `.env`, 이미지 태그 또는 `compose.prod.yaml` 변경사항은 완전히 반영되지 않을 수 있다.

### `.env` 또는 Compose 변경 후 재생성

먼저 설정 문법을 검사한다.

```bash
sudo docker compose --env-file .env -f compose.prod.yaml config --quiet
```

오류가 없으면 컨테이너를 다시 만든다.

```bash
sudo docker compose --env-file .env -f compose.prod.yaml \
  up -d --force-recreate bot
```

그다음 상태와 로그를 확인한다.

```bash
sudo docker compose --env-file .env -f compose.prod.yaml ps
sudo docker compose --env-file .env -f compose.prod.yaml logs --tail 200 bot
```

## 5. 중지와 시작

### 컨테이너만 중지

```bash
sudo docker compose --env-file .env -f compose.prod.yaml stop bot
```

### 중지한 컨테이너 시작

```bash
sudo docker compose --env-file .env -f compose.prod.yaml start bot
```

### 컨테이너와 Compose 네트워크 제거

```bash
sudo docker compose --env-file .env -f compose.prod.yaml down
```

현재 Compose에는 로컬 데이터베이스 볼륨이 없으므로 Supabase 데이터는 영향을 받지 않는다. 그래도 `down`은 컨테이너를 제거하는 명령이므로 단순 재시작 목적이라면 `restart` 또는 `up -d --force-recreate`를 우선 사용한다.

## 6. 자동 배포

애플리케이션 코드나 배포 설정을 `main` 브랜치에 푸시하면 GitHub Actions가 자동으로 다음 작업을 수행한다.

1. ARM64 이미지 빌드
2. GHCR 푸시
3. Tailscale 연결
4. Raspberry Pi SSH 접속
5. 배포 스크립트 실행
6. JDA 준비 상태 확인
7. 실패 시 이전 이미지로 롤백

문서(`docs/**`)만 수정한 커밋은 컨테이너 재배포를 실행하지 않는다. 필요하면 GitHub Actions 화면에서 `build-and-deploy-bot` 워크플로를 수동 실행할 수 있다.

## 7. 수동 배포와 롤백

자동 배포가 아닌 특정 커밋 이미지를 배포하려면 40자리 전체 Git SHA를 사용한다.

```bash
sudo /usr/local/bin/deploy-discordbot-container \
  ghcr.io/lunatic280/kotlindiscordbot-ops \
  0123456789abcdef0123456789abcdef01234567
```

스크립트는 이미지 저장소와 SHA 형식을 검사하고, 이미지를 받은 뒤 컨테이너를 교체한다. 새 버전이 90초 안에 준비되지 않으면 이전 이미지로 자동 복구한다.

### 이전 버전으로 수동 롤백

이전에 정상 동작한 커밋의 40자리 전체 SHA로 같은 명령을 실행한다.

```bash
sudo /usr/local/bin/deploy-discordbot-container \
  ghcr.io/lunatic280/kotlindiscordbot-ops \
  <이전에 정상 동작한 40자리 커밋 SHA>
```

`latest`는 가리키는 버전이 바뀔 수 있으므로 롤백 기준으로 사용하지 않는다.

## 8. 환경 변수 변경

환경 변수 파일을 root 권한으로 편집한다.

```bash
sudo nano /opt/discordbot/.env
```

저장 후 권한과 Compose 설정을 확인한다.

```bash
sudo stat -c '%U:%G %a %n' /opt/discordbot/.env
sudo docker compose --env-file .env -f compose.prod.yaml config --quiet
```

기대 권한은 다음과 같다.

```text
root:root 600 /opt/discordbot/.env
```

변경된 환경 변수를 적용하려면 단순 `restart`가 아니라 컨테이너를 재생성한다.

```bash
sudo docker compose --env-file .env -f compose.prod.yaml \
  up -d --force-recreate bot
```

주의: `docker compose config`를 `--quiet` 없이 실행하면 치환된 비밀값이 터미널에 표시될 수 있으므로 운영 서버에서는 `--quiet`를 사용한다.

## 9. 이미지와 디스크 정리

먼저 사용량을 확인한다.

```bash
sudo docker system df
```

어떤 컨테이너에서도 참조하지 않는 중간 이미지만 정리하려면 다음 명령을 사용한다.

```bash
sudo docker image prune -f
```

이 명령은 일반적으로 dangling 이미지만 제거한다. 롤백에 사용할 과거 SHA 이미지를 무분별하게 삭제하지 않도록 `docker system prune -a`는 사용 전에 제거 대상을 반드시 검토해야 한다. 특히 `--volumes` 옵션은 데이터 볼륨까지 삭제할 수 있으므로 이 운영 절차에서는 사용하지 않는다.

## 10. 재부팅 후 확인

Docker 서비스 상태를 확인한다.

```bash
sudo systemctl is-active docker
sudo systemctl is-enabled docker
```

컨테이너 상태와 준비 로그를 확인한다.

```bash
cd /opt/discordbot
sudo docker compose --env-file .env -f compose.prod.yaml ps
sudo docker compose --env-file .env -f compose.prod.yaml logs --tail 200 bot
```

Compose의 `restart: unless-stopped` 정책으로 인해 정상적으로 실행 중이던 봇은 Docker가 시작될 때 자동 재시작된다. 관리자가 명시적으로 중지한 컨테이너는 자동 시작되지 않을 수 있다.

## 11. 문제별 확인 순서

### 컨테이너가 없음

```bash
sudo docker compose --env-file .env -f compose.prod.yaml config --quiet
sudo docker compose --env-file .env -f compose.prod.yaml up -d bot
sudo docker compose --env-file .env -f compose.prod.yaml ps
```

### 계속 재시작함

```bash
sudo docker compose --env-file .env -f compose.prod.yaml ps
sudo docker compose --env-file .env -f compose.prod.yaml logs --tail 500 bot
sudo docker inspect \
  --format='restart={{.RestartCount}} exit={{.State.ExitCode}} oom={{.State.OOMKilled}} error={{.State.Error}}' \
  "$(sudo docker compose --env-file .env -f compose.prod.yaml ps -q bot)"
```

`oom=true`이면 메모리 제한으로 종료된 것이 확인된 사실이다. 그 외에는 종료 직전 로그와 exit code를 근거로 원인을 판단한다.

### `Discord JDA ready.`가 나오지 않음

가능성이 높은 순서로 확인한다.

1. `DISCORD_TOKEN` 누락 또는 인증 실패
2. Supabase 연결 실패
3. 애플리케이션 초기화 예외
4. 네트워크/DNS 문제

```bash
sudo docker compose --env-file .env -f compose.prod.yaml logs --tail 500 bot
```

### Supabase 접속 실패

먼저 IPv4 Session Pooler 포트가 열리는지 확인한다.

```bash
nc -4 -vz aws-1-ap-northeast-2.pooler.supabase.com 5432
```

그다음 `.env`의 사용자 이름, 비밀번호, JDBC URL, `sslmode=require`를 확인한다. 비밀번호를 명령행이나 로그에 직접 출력하지 않는다.

직접 연결 주소가 IPv6로만 해석되고 `Network is unreachable`가 발생하면 현재 Raspberry Pi 네트워크에서는 Session Pooler IPv4 주소를 사용한다.

### GHCR 이미지 다운로드 실패

```bash
sudo docker pull \
  ghcr.io/lunatic280/kotlindiscordbot-ops:<40자리 커밋 SHA>
```

`unauthorized` 또는 `denied`가 나오면 GHCR 로그인 상태와 토큰의 패키지 읽기 권한을 확인한다.

```bash
sudo docker login ghcr.io -u lunatic280
```

토큰은 프롬프트에서 입력하며 명령행 인수로 넣지 않는다.

### GitHub Actions SSH 실패

- `Permission denied (publickey,password)`: GitHub Secret의 개인 키와 서버 `authorized_keys`의 공개 키가 일치하는지 확인한다.
- `error in libcrypto`: `SSH_PRIVATE_KEY`가 OpenSSH 개인 키 전체 형식이며 줄바꿈이 보존됐는지 확인한다.
- `Host key verification failed`: GitHub에 등록된 known host 정보와 실제 Raspberry Pi SSH 호스트 키를 검증한다.

### PUBG 명령만 동작하지 않음

현재 일반 명령은 동작하지만 PUBG 기능만 응답하지 않는 현상은 아직 원인이 확정되지 않았다. 다음 순서로 확인한다.

1. `/opt/discordbot/.env`에 `PUBG_API_KEY`가 설정되어 있는지 확인한다.
2. 명령 실행 직후 최근 로그를 확인한다.
3. PUBG API 인증 실패, HTTP 상태 코드, 타임아웃 로그를 확인한다.
4. 코드에서 명령 진입 로그, `deferReply()`, 예외 처리와 HTTP 타임아웃을 보강한다.
5. 이벤트 처리 스레드에서 블로킹 호출이 실행되는지 확인한다.

비밀값 자체를 출력하지 않고 변수의 존재 여부만 확인하려면 다음 명령을 사용할 수 있다.

```bash
sudo sh -c 'grep -q "^PUBG_API_KEY=.\+" /opt/discordbot/.env'
```

종료 코드가 `0`이면 비어 있지 않은 설정 줄이 존재한다. 이는 키가 실제로 유효하다는 뜻은 아니다.

## 12. 운영 점검 체크리스트

배포 직후:

- GitHub Actions가 성공했는지 확인
- Compose 상태가 `Up`인지 확인
- 현재 이미지가 배포한 커밋 SHA인지 확인
- `Discord JDA ready.` 로그 확인
- Discord에서 기본 명령 실행
- 변경한 기능의 명령 실행

정기 점검:

- `docker system df`로 microSD 여유 공간 확인
- `docker stats --no-stream`으로 자원 사용 확인
- 반복 오류와 재시작 여부 확인
- Supabase 프로젝트 상태와 사용량 확인
- `/opt/discordbot/.env`와 배포에 필요한 비밀값을 안전한 장소에 별도 백업

## 13. 보안 주의사항

- `.env`, SSH 개인 키, Discord 토큰, Supabase 비밀번호, PUBG API 키를 Git에 커밋하지 않는다.
- Docker 그룹 사용자는 사실상 root 수준의 권한을 가질 수 있으므로 최소 인원만 추가한다.
- 컨테이너는 외부 포트를 공개하지 않는다.
- 배포에는 변경 가능한 `latest`가 아니라 고정된 커밋 SHA를 사용한다.
- 로그나 오류 보고를 공유하기 전에 토큰, 비밀번호, 연결 문자열을 가린다.
