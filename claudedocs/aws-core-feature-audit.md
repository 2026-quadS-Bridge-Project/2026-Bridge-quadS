# AWS Core Feature Audit

작성일: 2026-06-08  
대상: live AWS Swagger `https://leyoung.shop/v3/api-docs`, Parent 앱, Child 앱, backend 현재 브랜치

## 1. 결론

두 앱 모두 기본 환경은 `https://leyoung.shop` + `useMocks=false`로 맞춰져 있고, 앱 코드의 직접 HTTP 호출은 현재 Swagger에 있는 `/auth/*`, `/api/v1/*` 경로만 사용한다. 과거 compatibility endpoint 호출은 앱 코드 기준으로 남아 있지 않다.

하지만 "경로"는 맞아도 core flow가 전부 실서비스 수준으로 살아난 상태는 아니다. 가장 큰 문제는 아래 5개다.

| 우선순위 | 영역 | 상태 | 영향 |
| --- | --- | --- | --- |
| P0 | 자녀 코드 노출 | live AWS `AuthResponse`, `ChildrenInfoResponse`에 `childCode` 없음 | 신규 자녀가 자기 코드를 확인할 수 없어 부모-자녀 연결 bootstrap이 막힘 |
| P0 | Child 알림 목록 | Child 앱은 `{ notifications: [...] }` 래퍼를 기대하지만 AWS는 `data: [...]` 배열 | Child 알림 화면이 실 API에서 로드 실패 가능 |
| P1 | 앱 차단/화이트리스트 | 부모 화이트리스트는 로컬 저장, 자녀 blocker는 hardcoded remaining minutes | "앱 차단" core feature가 AWS 정책과 연결되지 않음 |
| P1 | 알림 payload/FCM deeplink | AWS `NotificationResponse`와 FCM payload에 `payload`, `deeplink`, `missionId`, `childCode`가 없음 | 부모/자녀 앱 간 알림 기반 상호작용 라우팅이 약함 |
| P1 | 리포트/실사용량 | Child 리포트는 daily schedule 7건에서 계획 시간만 조합하고 actualMinutes는 0 | 사용 리포트가 실제 사용량 기반이 아님 |

## 2. 검수 기준

- Swagger 수집: `curl -sS https://leyoung.shop/v3/api-docs`
- Child 앱 직접 HTTP 호출: `rg`로 `Dio.get/post/put/patch/delete` 경로 확인
- Parent 앱 직접 HTTP 호출: 동일 방식 확인
- Backend 현재 브랜치 controller/service/DTO 확인
- 정적/테스트 검증:
  - Child: `flutter analyze` 통과, `flutter test` 통과
  - Parent: `flutter analyze` 통과, `flutter test` 통과
  - Backend: Java 21으로 `bash ./gradlew test` 통과

주의: 위 테스트는 live AWS 통합 테스트가 아니라 로컬 정적/단위 테스트다. 실제 계정 생성, 부모-자녀 연결, 미션 제출까지 end-to-end로 AWS DB에 대해 수행한 검증은 아니다.

## 3. Endpoint Coverage

### Child 앱

Child 앱은 현재 Swagger 경로만 호출한다.

| 기능 | 앱 호출 | Swagger 존재 | 판정 |
| --- | --- | --- | --- |
| 자녀 로그인 | `POST /auth/children/login` | 있음 | OK |
| 자녀 회원가입 | `POST /auth/children/signup` | 있음 | 부분 OK, childCode 저장/표시 안 됨 |
| 토큰 갱신 | `POST /auth/token/refresh` | 있음 | OK |
| FCM 토큰 | `POST /api/v1/fcm/token` | 있음 | OK, payload 라우팅은 별도 이슈 |
| 오늘 시간표 | `GET /api/v1/schedules/daily` | 있음 | OK, 부모 정책/템플릿 선행 필요 |
| 루틴 | `GET/POST/DELETE /api/v1/schedules/routines` | 있음 | OK |
| 주차 예산 | `POST /api/v1/schedules/weekly-budgets` | 있음 | OK, parent time-policy 선행 필요 |
| 요일 템플릿 | `PUT /api/v1/schedules/templates` | 있음 | OK |
| 미션 목록/상세 | `GET /api/v1/missions`, `GET /api/v1/missions/{missionId}` | 있음 | OK |
| 미션 인증 | `POST /api/v1/missions/{missionId}/performances` multipart `image` | 있음 | OK |
| 알림 | `GET/PATCH/DELETE /api/v1/notifications...` | 있음 | P0 shape mismatch |
| 비밀번호/탈퇴 | `PATCH/DELETE /api/v1/members...` | 있음 | OK |

### Parent 앱

Parent 앱도 현재 Swagger 경로만 호출한다.

| 기능 | 앱 호출 | Swagger 존재 | 판정 |
| --- | --- | --- | --- |
| 부모 로그인/회원가입 | `POST /auth/parent/login`, `POST /auth/parent/signup` | 있음 | OK |
| 토큰 갱신/로그아웃 | `POST /auth/token/refresh`, `POST /auth/logout` | 있음 | OK |
| 자녀 목록/등록 | `GET/POST /api/v1/parents/children` | 있음 | OK, childCode가 사용자에게 전달되어야 함 |
| 프로필 사진 | `POST /api/v1/files/photos?category=PROFILE` | 있음 | OK |
| 월 총량 | `POST /api/v1/parents/time-policy` | 있음 | OK |
| 자녀 정책 조회 | `GET /api/v1/children/{childId}/policies` | 있음 | OK |
| 미션 | `GET/POST /api/v1/missions`, performance approve/reject | 있음 | 부분 OK, 수정/삭제는 AWS 경로 없음 |
| 알림 | `GET/PATCH/DELETE /api/v1/notifications...` | 있음 | 목록 파싱 OK, payload 부족 |
| FCM 토큰 | `POST /api/v1/fcm/token` | 있음 | OK |
| 비밀번호/탈퇴 | `PATCH/DELETE /api/v1/members...` | 있음 | OK |

## 4. 부모-자녀 식별자 흐름

| 식별자 | 현재 역할 | 검수 결과 |
| --- | --- | --- |
| `childCode` | 부모가 자녀를 연결할 때 입력하는 bootstrap 코드 | backend 현재 브랜치에는 생성/응답 로직이 있지만 live AWS Swagger에는 아직 없음. Child 앱도 아직 토큰/프로필에 저장하지 않고 MyPage에서 hardcoded `XY785eZ`를 표시함 |
| `childrenId` / `childId` | 연결 후 미션/시간/정책의 실제 DB 식별자 | Parent 앱은 자녀 목록에서 받은 `childrenId`를 후속 호출에 사용한다. 이 방향이 맞다 |
| `parentId` | 앱 내부 세션/로컬 저장 키 | 서버 호출에는 대부분 보내지 않고 JWT로 부모를 식별한다. 이 방향이 맞다 |
| `missionId` | 미션 상세/수행/성능 조회의 실제 식별자 | Child는 `missionId` 기반. Parent는 UI가 index 기반이라 매번 목록 조회 후 `missionId`를 찾아 approve/reject한다 |
| `performanceId` | 부모 승인/반려의 실제 식별자 | Backend가 `GET /missions/{missionId}/performance` 후 `PATCH /performances/{performanceId}` 구조라 Parent 앱 구현과 맞음 |
| `missionIndex` | 과거 Parent 알림 payload용 index | live AWS 알림에는 payload가 없어서 현재는 실효성이 낮음. 가능하면 `missionId`로 통일 필요 |

판정: 자녀 코드는 연결 bootstrap에만 필요하고, 연결 이후 core domain flow는 `childrenId`/`childId` 기반이 맞다. 따라서 후속 작업은 `childCode`를 모든 기능에 퍼뜨리는 것이 아니라, 회원가입/로그인/자녀목록/마이페이지/부모 등록 화면까지만 정확히 노출하는 방향이 좋다.

## 5. 기능별 세부 판정

### Auth

- Parent 로그인은 `email/password`만 보내며 live AWS schema와 맞다.
- Parent 회원가입은 tokenless 응답도 허용하고 로그인 화면으로 넘긴다.
- Child 회원가입은 `name/email/password`로 맞춰졌고 tokenless 응답 크래시는 방지됐다.
- 현재 live AWS에는 `childCode`가 없으므로 Child 회원가입 직후 자녀 코드 표시가 불가능하다.
- Backend 현재 브랜치의 PR #53은 `AuthResponse`, `ChildrenInfoResponse`에 `childCode`를 추가한다. 이 PR이 배포된 뒤 Child 앱이 `childCode`를 저장/표시하도록 후속 수정해야 한다.

### Parent-Child 연결

- Parent 앱의 `POST /api/v1/parents/children` body는 `{ childrenName, childrenCode, profileImageKey? }`로 Swagger와 맞다.
- `childrenBirth`는 backend에서 optional이라 앱이 생략해도 된다.
- 프로필 이미지는 `POST /api/v1/files/photos?category=PROFILE`로 S3 업로드 후 `profileImageKey`를 등록하므로 방향이 맞다.
- live AWS가 자녀 코드를 노출하지 않는 동안에는 신규 자녀가 부모에게 줄 코드를 알 수 없어 연결이 막힌다.

### Mission

- 과거 blocker였던 "Child 미션 목록에 childId query가 필요하다" 문제는 현재 backend 기준으로 해소됐다. 자녀 JWT면 `GET /api/v1/missions`가 자기 미션을 반환한다.
- Child 미션 인증도 현재 backend 기준 `image` multipart만 필요하다. `childId`, `category`, `prompt`는 서버에서 해석한다.
- Parent 미션 생성 payload는 `childId/title/category/resetCycle/verificationType/reward/description`으로 Swagger와 맞다.
- Parent 미션 목록은 AWS `MissionSummaryResponse`가 `missionId/title/category/reward`만 주기 때문에 앱은 resetCycle, verificationType, status를 기본값으로 채운다. 목록 렌더링은 가능하지만 실제 검수 상태 표현은 제한적이다.
- Child 미션 제출 후 `AiVerificationResponse`를 앱이 해석하지 않고 성공만 본다. 또한 실 API용 `ApiMissionApprovalListener`는 no-op이라 AI/부모 승인 후 상태 갱신은 push/polling 전까지 화면에 바로 반영되지 않는다.

### Time / Schedule / Policy

- Parent 월 총량은 `POST /api/v1/parents/time-policy`로 실제 저장된다.
- Child 시간설정은 `weekly-budgets -> templates -> routines` 순서로 저장하며 Swagger schema와 맞다.
- Backend는 Child 주간 예산 저장 시 해당 월 parent `time-policy`를 요구한다. 즉 부모가 월 총량을 먼저 저장해야 Child 시간설정이 성공한다.
- Parent 앱의 일별/주별 규칙과 화이트리스트는 AWS 경로가 없어 로컬 저장이다.
- Backend에는 `GET /api/v1/children/{childId}/policies`가 있고 `blockedApps`를 반환하지만, Parent 앱에서 block app 목록을 쓰는 API가 없다.
- Child 홈의 blocker는 현재 hardcoded remaining minutes를 사용한다. `PolicyResponse`나 `DailyScheduleResponse`와 연결되지 않아 실 앱 차단 동작은 아직 core feature로 완성되지 않았다.

### Notifications / FCM

- Parent 알림 repository는 AWS 배열 응답을 읽을 수 있다.
- Child 알림 repository는 아직 `response.data`가 Map이고 `notifications` 배열을 가진다고 가정한다. live AWS는 `ApiResponse.data`가 바로 배열이므로 수정 필요하다.
- AWS `NotificationResponse`는 `notificationId/title/content/isRead/notificationType/createdAt`만 제공한다. Parent 앱이 기대하는 `payload.childCode`, `payload.missionIndex`도 없고 Child 앱이 선호하는 `deeplink`도 없다.
- Backend FCM 전송 payload는 `title/body/type/targetId` 중심이다. Child의 `deeplink`, `missionId`, Parent의 `childCode`, `missionIndex` 라우팅 계약과 맞지 않는다.
- 결과적으로 알림 목록 자체는 Parent에서 보일 수 있지만, 알림을 눌러 정확한 자녀/미션/화면으로 이동하는 상호작용은 아직 불완전하다.

### Report / Usage

- AWS에는 `/reports/weekly` 류의 리포트 endpoint가 없다.
- Child 앱은 `GET /api/v1/schedules/daily`를 7회 호출해 계획 시간만 합산하고 `actualMinutes=0`으로 보고서를 만든다.
- 계획표 기반 "껍데기 리포트"는 가능하지만 실제 사용 시간, 초과/미달, AI 제안은 실데이터가 아니다.

### Account / Profile

- 비밀번호 변경과 탈퇴는 `/api/v1/members/password`, `/api/v1/members`로 양 앱 모두 연결되어 있다.
- 부모/자녀 프로필 조회/수정 API는 Swagger에 없다. Parent 프로필은 로컬 account/session snapshot, Child 프로필은 로컬 session + hardcoded childCode에 의존한다.
- 자녀 삭제/연결 해제 endpoint도 Swagger에 없다. Parent 앱은 삭제 시 실패 메시지를 반환한다.

## 6. Backend에 요청할 항목

### 배포 필요

1. PR #53 또는 동등 변경 배포
   - `AuthResponse.childCode`
   - `ChildrenInfoResponse.childCode`
   - 기존 자녀 로그인/refresh 시 code가 없으면 생성해서 반환

### API 추가 또는 schema 확장 필요

1. Child code read path
   - 최소: child login/signup/refresh response에 `childCode`
   - 권장: `GET /api/v1/members/me` 또는 `GET /api/v1/children/me`로 `memberId/name/email/childCode` 조회
2. Notification payload 통일
   - `NotificationResponse`에 `payload` 또는 `deeplink`, `missionId`, `childId` 추가
   - FCM data payload도 같은 키를 포함
   - Parent 알림은 `missionIndex`보다 `missionId`를 권장
3. App block policy write API
   - Parent가 child별 차단 앱 package list를 저장할 수 있어야 함
   - 예: `PUT /api/v1/children/{childId}/policies/blocked-apps`
4. Usage report 또는 actual usage ingestion
   - 실제 사용량 저장/조회 endpoint 필요
   - 최소: daily schedule settle과 앱 사용량 수집 경로를 명확히 연결
5. Parent time-plan rule API
   - 부모 일별/주별 draft rule이 서버 source of truth여야 하면 endpoint 필요
   - 아니라면 앱에서 해당 단계는 "로컬 UI 보조"로 명확히 분리
6. Child unlink/delete mapping
   - 부모가 연결 해제할 수 있는 endpoint 필요
7. Profile read/update
   - 부모/자녀 마이페이지가 서버 값을 보려면 profile endpoint 필요

## 7. Frontend 후속 수정

1. Child 알림 repository를 AWS 배열 응답으로 수정
   - 현재: `data is Map` + `data['notifications']`
   - 변경: `data is List`를 우선 처리
2. Child `AuthToken`/`AuthSession`/`UserProfile`에 `childCode` 저장
   - PR #53 배포 후 live response에서 읽기
   - MyPage hardcoded `XY785eZ` 제거
3. Child 홈 시간/차단 상태를 실데이터로 계산
   - `GET /api/v1/schedules/daily`
   - 필요 시 `GET /api/v1/children/{childId}/policies`
   - `_remainingMinutes` hardcode 제거
4. Child 미션 제출 응답/상태 갱신 처리
   - `AiVerificationResponse` 또는 performance status 반영
   - FCM/polling/SSE 중 하나로 reviewing 이후 상태 갱신
5. Parent 알림 action routing을 `missionId`/`childId` 중심으로 변경
   - backend payload가 준비되면 `missionIndex` fallback 제거
6. Parent home time summary는 로컬 child weekly rules가 아니라 backend policy/schedule에서 계산하도록 재검토

## 8. Demo 관점 권장 순서

1. Backend PR #53 배포
2. Child 앱 childCode 저장/표시 수정
3. Child 알림 배열 파싱 수정
4. 부모 회원가입/로그인 -> 자녀 회원가입 -> 자녀 코드 확인 -> 부모 자녀 등록 E2E 테스트
5. 부모 월 총량 설정 -> 자녀 주차 예산/요일 템플릿 저장 E2E 테스트
6. 부모 미션 생성 -> 자녀 미션 목록/상세 -> 사진 제출 -> 부모 승인 E2E 테스트
7. 앱 차단/리포트/알림 라우팅은 demo에서 설명 범위를 제한하거나 추가 API를 먼저 붙임
