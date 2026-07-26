# Supabase + Docker 운영 배포

이 문서는 Ubuntu Server 26.04 ARM64 라즈베리파이에 Discord 봇을 배포하는 절차를 설명한다.
데이터베이스는 Supabase PostgreSQL Session pooler를 사용하고, 라즈베리파이에서는 봇 컨테이너만 실행한다.

## 1. 사전 조건

- Docker Engine과 Docker Compose plugin이 설치되어 있어야 한다.
- 라즈베리파이가 Tailscale에 연결되어 있어야 한다.
- Supabase에 `discordbot` 스키마가 생성되어 있어야 한다.
- `psql`로 Supabase Session pooler 연결이 성공해야 한다.

Supabase SQL Editor에서 스키마를 생성한다.

```sql
create schema if not exists discordbot;
```

## 2. 운영 파일 설치

저장소가 있는 디렉터리에서 다음을 실행한다.

```bash
sudo install -d -m 0755 /opt/discordbot
sudo install -m 0644 compose.prod.yaml /opt/discordbot/compose.prod.yaml
sudo install -m 0755 scripts/deploy-discordbot-container \
  /usr/local/bin/deploy-discordbot-container
```

`/opt/discordbot/.env`가 아직 없을 때만 예제 파일을 복사한다.

```bash
sudo cp env.example /opt/discordbot/.env
sudo chown root:root /opt/discordbot/.env
sudo chmod 0600 /opt/discordbot/.env
sudo nano /opt/discordbot/.env
```

필수 설정은 다음과 같다.

```dotenv
APP_IMAGE=ghcr.io/lunatic280/kotlindiscordbot-ops:latest
DB_URL='jdbc:postgresql://aws-1-ap-northeast-2.pooler.supabase.com:5432/postgres?sslmode=require&currentSchema=discordbot&ApplicationName=kotlin-discord-bot'
DB_USERNAME='postgres.uomrpfkorbrvucsviibw'
DB_PASSWORD='실제 Supabase Database Password'
DDL_AUTO='update'
SHOW_SQL='false'

DISCORD_TOKEN='실제 Discord Bot Token'
DISCORD_GUILD_ID=''
PUBG_API_KEY='실제 PUBG API Key'
GEMMA_API_KEY=''
```

비밀번호와 API 키는 저장소나 GitHub Actions 로그에 넣지 않는다.

## 3. 비공개 GHCR 이미지 로그인

GitHub에서 `read:packages` 권한을 가진 Personal Access Token(classic)을 생성한다.
토큰을 명령줄 인수로 전달하지 말고 표준 입력으로 로그인한다.

```bash
sudo docker login ghcr.io -u lunatic280
```

프롬프트가 표시되면 GitHub 계정 비밀번호가 아니라 생성한 토큰을 입력한다.

## 4. 배포 사용자 sudo 권한

GitHub Actions가 사용하는 SSH 사용자를 확인한다.

```bash
whoami
```

`visudo`로 전용 설정을 생성한다.

```bash
sudo visudo -f /etc/sudoers.d/discordbot-deploy
```

아래에서 `DEPLOY_USER`를 실제 SSH 사용자로 바꾼다.

```sudoers
DEPLOY_USER ALL=(root) NOPASSWD: /usr/local/bin/deploy-discordbot-container *
```

설정을 검증한다.

```bash
sudo visudo -cf /etc/sudoers.d/discordbot-deploy
```

배포 스크립트는 GHCR 이미지 이름과 40자리 Git 커밋 SHA만 허용한다.

## 5. GitHub 환경 설정

GitHub 저장소의 `Settings → Environments → prod_bot`에서 설정한다.

Secrets:

- `TS_OAUTH_CLIENT_ID`
- `TS_OAUTH_SECRET`
- `SSH_PRIVATE_KEY`

Variables:

- `DEPLOY_HOST`: 라즈베리파이 Tailscale IPv4
- `DEPLOY_PORT`: 일반적으로 `22`
- `DEPLOY_USER`: SSH 사용자

Supabase와 Discord 비밀값은 GitHub에 저장하지 않고 라즈베리파이의
`/opt/discordbot/.env`에서만 관리한다.

## 6. 최초 배포

서버 준비가 끝난 후 `main` 브랜치에 push하거나 GitHub Actions에서
`build-and-deploy-bot` 워크플로를 수동 실행한다.

워크플로는 다음 작업을 수행한다.

1. Java 21로 실행 JAR 생성
2. `linux/arm64` 이미지 빌드
3. `ghcr.io/lunatic280/kotlindiscordbot-ops:<commit-sha>`에 push
4. Tailscale과 SSH로 라즈베리파이에 접속
5. 커밋 SHA 이미지를 배포
6. 컨테이너가 재시작 없이 실행되고 90초 안에 Discord Ready 상태가 되는지 확인
7. 실패하면 이전 이미지로 롤백

상태와 로그를 확인한다.

```bash
cd /opt/discordbot
sudo docker compose --env-file .env -f compose.prod.yaml ps
sudo docker compose --env-file .env -f compose.prod.yaml logs --tail 200 bot
```

## 7. 최초 스키마 생성 후 잠금

첫 실행에서는 `DDL_AUTO='update'`로 Hibernate가 빈 `discordbot` 스키마에
테이블을 생성한다.

Supabase SQL Editor에서 확인한다.

```sql
select table_name
from information_schema.tables
where table_schema = 'discordbot'
order by table_name;
```

`members`, `positions`, `pubgplays`, `wallets`가 확인되면 서버의 `.env`를 수정한다.

```dotenv
DDL_AUTO='validate'
```

컨테이너를 다시 만든다.

```bash
cd /opt/discordbot
sudo docker compose --env-file .env -f compose.prod.yaml up -d --force-recreate bot
```

## 8. 재부팅 검증

```bash
sudo reboot
```

재접속 후 다음을 확인한다.

```bash
sudo systemctl is-active docker
cd /opt/discordbot
sudo docker compose --env-file .env -f compose.prod.yaml ps
```

`restart: unless-stopped` 정책에 따라 Docker가 시작되면 봇도 다시 실행된다.
