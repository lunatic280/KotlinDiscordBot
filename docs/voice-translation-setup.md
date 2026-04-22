# 실시간 음성 번역 기능 설정 가이드

## 개요

Discord 음성 채널에서 사용자의 음성을 인식하여 한국어 ↔ 영어로 번역한 결과를 텍스트 채널에 출력하는 기능.

## 필요한 외부 서비스

| 서비스 | 용도 | 무료 범위 | 초과 비용 |
|--------|------|----------|----------|
| Google Cloud Speech-to-Text | 음성 → 텍스트 변환 | 매월 60분 | $0.006/15초 |
| DeepL API | 텍스트 번역 (한↔영) | 월 50만 자 | Pro 플랜 필요 |

소규모 디스코드 서버에서는 무료 범위 내에서 충분히 운영 가능.

---

## 1. Google Cloud Speech-to-Text 설정

### 1-1. Google Cloud 프로젝트 생성
1. [Google Cloud Console](https://console.cloud.google.com/) 접속
2. 새 프로젝트 생성 (또는 기존 프로젝트 사용)
3. 신규 가입 시 $300 무료 크레딧 제공

### 1-2. Speech-to-Text API 활성화
1. 콘솔에서 **API 및 서비스** → **라이브러리**
2. "Cloud Speech-to-Text API" 검색 → **사용 설정**

### 1-3. 서비스 계정 키 생성
1. **API 및 서비스** → **사용자 인증 정보**
2. **사용자 인증 정보 만들기** → **서비스 계정**
3. 서비스 계정 생성 후, **키** 탭 → **키 추가** → **새 키 만들기** → JSON 선택
4. 다운로드된 JSON 파일을 안전한 위치에 저장

### 1-4. 환경 변수 설정
```bash
# 서비스 계정 JSON 파일 경로
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/your-service-account-key.json
```

---

## 2. DeepL API 설정

### 2-1. DeepL 계정 생성
1. [DeepL API](https://www.deepl.com/pro-api) 접속
2. **무료로 가입하기** 선택 (DeepL API Free)
3. 월 50만 자까지 무료

### 2-2. API 키 확인
1. 로그인 후 [계정 설정](https://www.deepl.com/account/summary) 접속
2. **Authentication Key for DeepL API** 항목에서 키 복사

### 2-3. 환경 변수 설정
```bash
export DEEPL_API_KEY=your-deepl-api-key-here

# Free 플랜은 아래 URL 사용 (기본값)
export DEEPL_API_URL=https://api-free.deepl.com

# Pro 플랜은 아래 URL 사용
# export DEEPL_API_URL=https://api.deepl.com
```

---

## 3. application.yaml 설정

`application.yaml.example`을 참고하여 `application.yaml`에 추가:

```yaml
google:
  cloud:
    speech:
      credentials-path: ${GOOGLE_APPLICATION_CREDENTIALS:}

deepl:
  api-key: ${DEEPL_API_KEY:}
  api-url: ${DEEPL_API_URL:https://api-free.deepl.com}

translate:
  silence-threshold-ms: ${TRANSLATE_SILENCE_MS:800}
  max-utterance-ms: ${TRANSLATE_MAX_UTTERANCE_MS:30000}
```

| 설정 | 설명 | 기본값 |
|------|------|--------|
| `silence-threshold-ms` | 발화 종료 판단 무음 시간 (ms) | 800 |
| `max-utterance-ms` | 최대 발화 길이 (ms) | 30000 |

---

## 4. 사용 방법

### 봇 음성 채널 참가
```
/translate join
```
- 사용자가 먼저 음성 채널에 접속한 상태에서 실행
- 봇이 같은 음성 채널에 입장하여 번역 시작
- 한국어/영어 자동 감지

### 봇 음성 채널 퇴장
```
/translate leave
```

---

## 5. 완전 무료 대안

Google Cloud / DeepL 무료 한도를 초과하거나, 완전 무료를 원하는 경우:

| 서비스 | 대안 | 장점 | 단점 |
|--------|------|------|------|
| Google STT | **Whisper** (OpenAI, 로컬) | 무료, 높은 정확도 | GPU 필요, 실시간 속도 어려움 |
| Google STT | **Vosk** (로컬) | 무료, CPU 가능 | 한국어 품질 다소 낮음 |
| DeepL | **LibreTranslate** (셀프호스팅) | 완전 무료 | 번역 품질 낮음 |

현재 구현(Google STT + DeepL Free)이 품질과 비용의 최적 균형점.
