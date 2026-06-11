# App Change Phases

작성일: 2026-06-08  
대상: Quad-S Parent/Child Flutter apps  
목표: 백엔드 작업을 PR #53 수준에서 더 늘리지 않고, 부스 시연 core flow를 앱 변경으로 살린다.

## 0. 판단 요약

내부 검토 결과, 시연에 필요한 앱 변경은 "모든 API 완성"이 아니라 아래 흐름을 안정화하는 것이다.

1. 자녀가 MyPage에서 자기 `childCode`를 확인한다.
2. 부모가 `childCode`로 자녀를 등록한다.
3. 부모가 1분 또는 짧은 시간을 설정한다.
4. 자녀 앱이 남은 시간을 표시하고 카운트다운한다.
5. 시간이 0이 되면 앱 차단을 발동한다.
6. 자녀가 미션을 수행하면 보상 시간이 증가한 것을 확인한다.

따라서 리포트, profile update, 자녀 연결 해제, 알림 deeplink, usage ingestion은 이번 시연 MVP에서 제외한다.

## Phase 1. Child Code 저장/표시

목적: 별도 `members/me` API 없이, backend PR #53의 auth 응답만으로 자녀 코드를 MyPage에 표시한다.

### Backend 의존성

- PR #53 배포 필요
- `AuthResponse.childCode`가 child login/signup/refresh 응답에 포함되어야 함
- live Swagger에 아직 없으면 이 phase는 앱 코드를 넣어도 실제 값 확인이 불가

### Child 앱 변경

대상 repo: `/Users/yeongj/Quad-S-Team12-App-Child`

변경 파일 후보:
- `lib/features/auth/data/models/auth_token.dart`
- `lib/features/auth/data/repositories/api_auth_repository.dart`
- `lib/core/auth/auth_session.dart`
- `lib/features/login/presentation/pages/login_page.dart`
- `lib/features/signup/presentation/pages/signup_page.dart`
- `lib/features/my_page/data/repositories/api_my_page_repository.dart`

작업:
- `AuthToken`에 nullable `childCode` 추가
- `_parseTokenResponse()`에서 `childCode` 파싱
- 로그인 성공 시 `AuthSession`에 `childCode` 저장
- 회원가입은 tokenless 결과일 수 있으므로, code 저장은 login success 경로 중심으로 처리
- MyPage repository에서 hardcoded `XY785eZ` 제거
- 저장된 code가 없으면 `'-'` 또는 "다시 로그인 필요" 수준의 안전한 fallback 표시

완료 기준:
- 자녀 로그인 후 MyPage의 `자녀코드`가 backend 응답값을 표시
- 로그아웃 후 재로그인하면 code가 복구
- 기존 mock mode는 mock childCode를 계속 표시

검증:
- `flutter analyze`
- `flutter test`
- PR #53 배포 후 실제 자녀 로그인으로 MyPage 확인

## Phase 2. 시간/차단 시연 루프

목적: 부모가 짧은 시간을 설정하고, 자녀 앱이 카운트다운 후 차단을 발동한다.

### Backend 의존성

추가 backend 작업 없음. 현재 Swagger 경로 사용:
- Parent: `POST /api/v1/parents/time-policy`
- Child: `GET /api/v1/schedules/daily?date=YYYY-MM-DD`
- 선택: `GET /api/v1/children/{childId}/policies`

주의:
- Child 앱이 자기 `childId/memberId`를 알아야 `children/{childId}/policies`를 직접 조회할 수 있음
- PR #53의 `memberId`는 이미 auth response에 있음. Child 앱은 이 값을 세션에 저장해야 함

### Child 앱 변경

변경 파일 후보:
- `lib/features/auth/data/models/auth_token.dart`
- `lib/core/auth/auth_session.dart`
- `lib/features/child_home/presentation/pages/child_home_page.dart`
- `lib/features/time_confirm/data/repositories/api_time_confirm_repository.dart`
- 필요 시 신규 `policy`/`home_time` repository

작업:
- Child auth session에 `memberId` 저장
- Child home의 `_remainingMinutes = 90 + 30` hardcode 제거
- `GET /api/v1/schedules/daily`의 `totalAvailableMinutes`를 홈 시간 카드/차단 판단에 사용
- 시연 모드에서는 서버에서 받은 초기 분 단위 값을 앱 내부 timer로 초 단위 카운트다운
- `remaining <= 0`이면 `DeviceBlockController.instance.applyForRemainingMinutes(0)` 또는 `setBlocked(true)` 호출
- 권한이 없으면 permission flow를 먼저 유도

### Parent 앱 변경

대상 repo: `/Users/yeongj/Quad-S-Team12-App-Parent`

현재 `saveMonthlyTotal()`은 `POST /api/v1/parents/time-policy`에 붙어 있음. 시연에서 1분 설정이 자연스럽게 가능하도록 UI 또는 입력 제한만 확인한다.

변경 후보:
- `lib/features/today_time/presentation/pages/monthly_time_setup_page.dart`
- `lib/data/repositories/api_time_plan_repository.dart`

작업:
- 1분 같은 짧은 값이 UI에서 입력/저장 가능한지 확인
- backend `@Positive` 때문에 0분은 금지, 1분 이상만 허용

완료 기준:
- 부모 앱에서 연결된 자녀에게 1분 정책 저장 성공
- 자녀 앱 홈에서 1분 또는 설정값 기반 카운트다운 시작
- 0초 도달 시 native block method 호출

검증:
- `flutter analyze`, `flutter test`
- iOS/Android simulator 또는 실제 기기에서 blocker permission path 확인
- 실제 차단은 platform capability가 필요하므로, 최소한 method channel 호출 성공/실패 로그를 확인

## Phase 3. 미션 보상 시간 MVP

목적: 자녀가 미션을 수행하면 backend가 보상 시간을 지급하고, 앱에서 증가를 확인한다.

### Backend 의존성

추가 backend 작업 없음. 현재 backend 로직:
- `CHILD` verification: 제출 즉시 `ACCEPTED`, `TimePolicy.addReward(reward)`
- `PARENT` verification: parent approve 시 `TimePolicy.addReward(reward)`
- `AI` verification: AI accepted 시 `TimePolicy.addReward(reward)`

시연 MVP는 가장 안정적인 `CHILD` verification부터 사용한다.

### Child 앱 변경

변경 파일 후보:
- `lib/features/mission/data/repositories/api_mission_repository.dart`
- `lib/features/mission/state/mission_controller.dart`
- `lib/features/mission/data/models/mission.dart`
- `lib/features/child_home/presentation/pages/child_home_page.dart`

작업:
- `submitMission()`이 `AiVerificationResponse`를 버리지 않고 반환하도록 모델링
- `MissionController.submit()`에서 `isAccepted/reason`을 반영
- 자녀 확인 방식은 성공 즉시 completed 화면 표시
- 제출 성공 후 정책/시간 데이터를 refresh하여 보상 시간 증가를 홈 또는 완료 화면에서 확인
- 실패/반려 reason은 Snackbar 또는 제출 완료 화면 subtitle로 표시

완료 기준:
- 자녀 확인 방식 미션 제출 후 성공 화면 표시
- 보상 시간이 backend policy에 추가됨
- 앱에서 보상 시간 증가 또는 총 사용 가능 시간 증가를 확인 가능

검증:
- `flutter analyze`, `flutter test`
- 실제 flow: 부모가 reward 1분 미션 생성 -> 자녀 제출 -> policy `accumulatedRewardTime` 증가 확인

## Phase 4. 부모 승인/반려 화면 보강

목적: 부모 확인 방식 미션도 시연 가능하게 만든다. 단, MVP 이후 단계다.

### Parent 앱 변경

변경 파일 후보:
- `lib/data/repositories/api_mission_repository.dart`
- `lib/data/repositories/mission_repository.dart`
- `lib/features/today_mission/presentation/models/today_mission.dart`
- `lib/features/today_mission/presentation/pages/today_mission_check_page.dart`
- `lib/features/today_mission/presentation/pages/today_mission_list_page.dart`

작업:
- `MissionRepository`에 mission detail/performance fetch 메서드 추가
- `GET /api/v1/missions/{missionId}/performance` 응답의 `status`, `proofImageUrl`를 UI 모델에 매핑
- Parent 미션 확인 화면 진입 시 최신 performance 상태를 조회
- `PENDING`이면 승인/반려 버튼 표시
- `ACCEPTED/REJECTED`이면 버튼 대신 결과 상태 표시
- approve/reject 후 performance와 mission list refresh

주의:
- 현재 Parent 앱은 page-level index 기반이다. backend는 `missionId/performanceId` 기반이므로, 장기적으로는 UI 모델에 `missionId`를 보존하는 구조가 필요하다.
- 단기 시연은 기존 index -> missionId 브릿지를 유지해도 된다.

완료 기준:
- 부모 확인 방식 미션 제출 후 Parent 앱에서 `PENDING` 확인 가능
- 승인 클릭 시 backend approve 호출
- 자녀 policy reward 증가
- 반려 클릭 시 reward 미지급

검증:
- Parent/Child `flutter analyze`, `flutter test`
- 실제 parent-confirm mission E2E

## Phase 5. AI 미션/알림 안정화

목적: AI verification과 notification route를 실서비스에 가깝게 정리한다. 시연 필수는 아니다.

### Child 앱 변경

- AI 미션 제출 결과의 `isAccepted/reason` 즉시 표시
- `ApiMissionApprovalListener` no-op 제거 또는 polling으로 대체
- Child notification repository가 AWS 배열 응답을 받도록 수정

### Parent 앱 변경

- 알림 payload가 없는 현재 backend에서는 알림 클릭 route를 신뢰하지 않음
- backend가 payload를 추가하기 전까지 Parent 알림 action은 fallback routing만 유지

완료 기준:
- AI accepted/rejected 결과가 자녀 화면에 표시
- Child 알림 목록이 live AWS 배열 응답에서 깨지지 않음

검증:
- `flutter analyze`, `flutter test`
- 실제 AI 미션은 외부 AI 응답 변수가 있어 demo 필수 flow로 잡지 않음

## Deferred

이번 시연 범위에서 제외한다.

| 항목 | 제외 이유 |
| --- | --- |
| Usage report / actual usage ingestion | 실제 사용량 수집/정산/리포트는 backend와 native 수집 설계가 필요 |
| Profile update | childCode 표시에는 login response 저장만으로 충분 |
| `GET /api/v1/members/me` | 있으면 좋지만 백엔드 작업량 증가. 이번에는 auth response 저장으로 대체 |
| Child unlink/delete | 부스 core flow 아님 |
| Parent full time-plan draft source of truth | 현재 월 총량 저장만으로 1분 차단 시연 가능 |
| Notification deeplink/payload 통일 | 미션/차단 core flow와 분리 가능 |

## 추천 실행 순서

1. Phase 1: `childCode`/`memberId` 저장 및 MyPage 표시
2. Phase 2: Child home time source + countdown/block trigger
3. Phase 3: 자녀 확인 미션 보상 시간 refresh
4. Phase 4: 부모 확인 미션 approve/reject UI 보강
5. Phase 5: AI/알림 안정화

부스 시연 최소 컷은 Phase 1-3이다. 부모 승인까지 보여주려면 Phase 4까지 진행한다.

