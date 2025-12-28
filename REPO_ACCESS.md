# 저장소 승인·병합 권한 설정 가이드

다른 사용자는 이슈·PR만 올리고, 승인과 병합은 본인만 할 수 있도록 GitHub 설정을 적용하는 절차입니다.

## 1. CODEOWNERS로 "나만 승인" 강제 (3번)
- 파일 위치: `.github/CODEOWNERS`
- 내용 예시:
  ```
  # 코드 소유자를 리포지토리 관리자 계정으로 지정하세요.
  # 아래 @your-username을 본인 GitHub 사용자명으로 변경하면 모든 PR에 본인 승인(코드 오너 리뷰)을 요구합니다.
  * @your-username
  ```
- 브랜치 보호 규칙에서 **Require review from Code Owners**를 활성화해야 효력이 있습니다.

## 2. 브랜치 보호 규칙 설정 (2번)
GitHub → **Settings → Branches → Add rule**에서 대상 브랜치(예: `main`)에 대해 다음을 켭니다.
- **Require a pull request before merging**: 직접 푸시 금지, 반드시 PR로 병합.
- **Require approvals**: 승인 1명 이상. CODEOWNERS를 쓰면 승인자는 코드 오너로 제한됩니다.
- **Dismiss stale pull request approvals when new commits are pushed**: 새 커밋이 올라오면 이전 승인이 무효.
- **Restrict who can push to matching branches**: 특정 사용자만 푸시 가능하도록 제한 (4번과 연계).

## 3. 병합/푸시 권한 제한 (4번)
- 브랜치 보호 규칙에서 **Restrict who can push to matching branches** 옵션에 **본인 계정만** 추가합니다.
- 다른 사용자는 `Triage`(권장) 또는 `Write` 이하 권한으로 제한해 병합 버튼 노출을 막습니다.
- 이렇게 하면 다른 사람이 Approve하더라도 보호된 브랜치에 직접 푸시하거나 병합할 권한이 없어 실제 머지가 불가능합니다.

## 4. 운영 팁
- 포크 기반 워크플로우를 사용하면 외부 기여자는 포크 → PR만 제출할 수 있습니다.
- 필요 시 PR/Issue 템플릿을 추가해 제출 형식을 표준화하고, CI를 연결해 승인 전에 상태를 확인하세요.
