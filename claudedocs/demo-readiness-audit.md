# QuadS 부스 시연 검수 문서

> 작성일: 2026-06-07  
> 범위: Parent 앱(`Quad-S-Team12-App-Parent`), Child 앱(`Quad-S-Team12-App-Child`), 백엔드(`2026-Bridge-quadS`)  
> 기준: 실출시 품질이 아니라 부스에서 "시간 설정", "앱 차단", "미션" 흐름을 안정적으로 보여줄 수 있는지 검수

---

## 1. 결론

### Mock 앱 시연

현재 상태로는 **양 앱 모두 mock 모드 시연은 가능**하다.

- Parent/Child 앱의 dev 환경은 `useMocks: true`라서 기본 실행 시 실백엔드를 호출하지 않는다.
- 두 앱 모두 `flutter analyze`와 `flutter test`가 통과했다.
- 화면 이동, mock 미션, mock 시간 설정 화면은 데모용으로 안정적인 편이다.

다만 **앱 차단은 mock 화면만으로는 자동 시연되지 않는다.** Child 앱에 Android 접근성 서비스 기반 차단 구현은 있지만, 현재 홈 화면의 남은 시간이 하드코딩 `120분`이라 자동 차단 조건(`remainingMinutes <= 0`)에 들어가지 않는다.

### 실백엔드 연동 시연

현재 코드 그대로 `useMocks: false`로 전환하면 **핵심 흐름이 일부 막힌다.**

가장 중요한 차단점은 아래 5개다.

1. Parent 로그인 응답에 `memberId`가 없어 Parent 홈이 실데이터를 로드하지 못한다.
2. Parent의 자녀 등록 요청과 백엔드 계약이 다르다. 앱은 `childrenBirth`를 보내지 않고 생성된 자녀 객체를 기대하지만, 백엔드는 `childrenBirth`를 필수로 받고 `Void`를 반환한다.
3. Child 미션 목록 조회가 막힌다. Child 앱은 `GET /api/v1/missions`를 호출하지만, 백엔드는 Parent 권한과 `childId` query를 요구한다.
4. Child 미션 제출이 막힌다. 앱은 multipart `image`만 보내지만, 백엔드는 `childId`, `category`, `prompt`도 필수로 요구한다.
5. 앱 차단/화이트리스트는 앱-백엔드 모델이 연결되어 있지 않다. Parent 앱은 whitelist 저장 API를 호출하지만 백엔드에는 해당 쓰기 API가 없고, Child 앱 차단 서비스는 백엔드 `blockedApps`를 사용하지 않는다.

따라서 **빠른 부스 시연 목표라면 mock 모드 중심으로 시연하고, 앱 차단만 별도 데모 브랜치/수동 토글로 준비하는 것을 권장**한다. 실백엔드까지 붙여 보여주려면 아래 "최소 조치"가 필요하다.

---

## 2. 실행 검증 결과

| 대상 | 명령 | 결과 |
|---|---|---|
| Parent 앱 | `flutter analyze && flutter test` | 통과 |
| Child 앱 | `flutter analyze && flutter test` | 통과 |
| 백엔드 | `bash ./gradlew test` | 실패. 코드 실패가 아니라 로컬 JDK 21 미설치로 Gradle toolchain 해석 실패 |

백엔드 테스트 실패 사유:

- `build.gradle`은 Java toolchain 21을 요구한다.
- 현재 로컬 `java -version`은 OpenJDK 17이다.
- Gradle 메시지: `Cannot find a Java installation ... matching: {languageVersion=21}`.

백엔드 테스트를 실제로 돌리려면 JDK 21 설치와 `gradlew` 실행 권한 정리가 필요하다.

---

## 3. Parent 앱 검수

### 인증/세션

- 앱은 `/auth/parent/login`, `/auth/parent/signup`, `/auth/token/refresh`로 백엔드 경로에 맞춰져 있다.
- 응답 래퍼 `{isSuccess, code, message, data}` 언래핑도 Dio 인터셉터에 반영되어 있다.
- 하지만 백엔드 `AuthResponse`는 현재 `accessToken`, `refreshToken`만 반환한다.
- Parent 앱은 로그인 성공 후 `data.parentId`를 `AuthSession.current_parent_id`에 저장하고, 홈에서 이 값이 비어 있으면 자녀/미션 로딩을 하지 않는다.

실백엔드 시연 영향: **Parent 로그인 후 홈이 빈 상태로 보일 가능성이 높다.**

필요 조치:

- 백엔드 `MemberResDTO.AuthResponse`에 `memberId`, 가능하면 `name/email`도 추가.
- `AuthService.issueTokens()`에서 인증된 `Parent/Children` 엔티티의 ID를 응답에 담기.

### 자녀 등록

앱 현재 요청:

- `POST /api/v1/parents/children`
- body: `{childrenName, childrenCode, profileImageKey?}`
- `childrenBirth` 생략
- 응답: 생성된 `ChildSummary` 객체 기대

백엔드 현재 계약:

- `RegisterChildRequest.childrenBirth`가 `@NotBlank` 필수
- `profileImageUrl` 필드만 받음
- `ParentController.registerChild()`는 `ApiResponse<Void>` 반환

실백엔드 시연 영향: **Parent 앱에서 자녀 등록이 실패하거나 응답 파싱이 실패한다.**

필요 조치:

- `childrenBirth` optional로 완화.
- 등록 응답을 `ChildrenInfoResponse`로 반환.
- `profileImageKey`를 받거나, 데모에서는 사진 업로드를 생략하고 `profileImageUrl/profileImageKey`를 모두 optional로 처리.
- 참고: Parent 앱은 `/api/v1/files/photos` 업로드를 best-effort로 호출하지만 백엔드에는 현재 파일 업로드 컨트롤러가 없다. 실패해도 무사진 등록으로 진행되도록 앱은 방어되어 있다.

### 시간 설정

Parent 앱의 월 총량 저장은 백엔드와 비교적 잘 맞는다.

- 앱: `POST /api/v1/parents/time-policy`
- 백엔드: `POST /api/v1/parents/time-policy`
- body: `{childId, yearMonth, baseTime}`

주의점:

- Parent 홈 세션 `parentId`가 비면 시간 설정 플로우 진입/저장에 필요한 선택 자녀가 제대로 잡히지 않는다.
- 일별/주별 시간 규칙은 앱이 로컬 저장만 한다. 백엔드로 보내지 않는다.
- whitelist 저장은 `/children/{childrenId}/time-plan/whitelist`를 호출하지만 백엔드에 없다.

실백엔드 시연 영향:

- "월 총 사용시간 설정"은 `memberId`와 자녀 등록 문제가 해결되면 가능하다.
- "앱 허용/차단 리스트 저장"은 현재 실백엔드로는 불가하다.

### 미션

동작 가능한 부분:

- 미션 생성: 앱 `POST /api/v1/missions`와 백엔드 생성 API가 맞다.
- 미션 목록: Parent 앱은 `GET /api/v1/missions?childId=...`를 호출하고 백엔드도 같은 형태다.
- 승인/거절: 앱이 `missionId -> performanceId` 라운드트립 후 `PATCH /api/v1/missions/performances/{performanceId}/approve|reject`를 호출하므로 백엔드와 맞다.

막히는 부분:

- 미션 수정/삭제/일괄 저장은 앱에 호출 코드가 있지만 백엔드 API가 없다.

부스 기준:

- 미션 "생성 -> 자녀 제출 -> 부모 승인/거절"만 보여주면 충분하다.
- 수정/삭제는 시연에서 빼는 것이 안전하다.

---

## 4. Child 앱 검수

### 인증/가입

- 로그인은 앱의 `username`을 백엔드 `email`로 보내도록 맞춰져 있다.
- 그래서 시연 계정의 아이디 칸에는 이메일 형식 값을 넣어야 한다.
- 회원가입은 앱이 `{email: username, password}`만 보내는데 백엔드 `SignUpRequest`는 `name`, `email`, `password`를 필수로 요구한다.

실백엔드 시연 영향:

- 로그인은 사전 생성된 자녀 계정이 있으면 가능하다.
- Child 앱 회원가입 화면으로 실백엔드 계정을 만드는 것은 현재 어렵다.
- 자녀 코드도 signup 응답에 반환되지 않아서 Parent 연결 시연에는 DB seed 또는 백엔드 응답 추가가 필요하다.

### 시간 설정

Child 앱은 저장 시 백엔드의 3개 API로 분할 호출한다.

1. `POST /api/v1/schedules/weekly-budgets`
2. `PUT /api/v1/schedules/templates`
3. `GET/DELETE/POST /api/v1/schedules/routines`

백엔드도 이 API들을 제공하고, 모두 Child JWT의 `asChildren()`로 childId를 식별한다.

주의점:

- 저장 전에 Parent가 같은 `yearMonth`로 `time-policy.baseTime`을 먼저 설정해야 한다.
- Child 앱의 `allowedHours`는 "사용 가능 시간 격자"에 가깝지만, 백엔드 `routines`는 "학원/고정 일정" 의미다. 현재 앱 코드는 allowedHours를 routine으로 저장한다.
- `GET /time-setup/previous-week`, `GET /time-setup/current`는 아직 백엔드에 없다. v2/현재 스케줄 조회 실데이터는 막힌다.

부스 기준:

- mock 시연은 안정적.
- 실백엔드 저장 시연은 Parent 월 총량 선설정과 Child 계정/토큰이 준비되어야 한다.
- 의미 충돌 때문에 "정확한 시간 정책"보다는 "설정값 저장 API 호출 데모" 수준으로 보는 것이 맞다.

### 앱 차단

구현된 것:

- Android `AccessibilityService`가 foreground package를 감시한다.
- blocking flag가 true면 자기 앱, 시스템 UI, 설정, 기본 전화, 기본 SMS, 홈 런처 외 앱을 HOME으로 튕긴다.
- Flutter `DeviceBlockController.applyForRemainingMinutes()`가 남은 시간이 0 이하일 때 차단을 켠다.

현재 막히는 것:

- Child 홈의 남은 시간은 `90 + 30`으로 하드코딩되어 있어 차단이 자동으로 켜지지 않는다.
- 백엔드 `GET /api/v1/children/{childId}/policies`는 `blockedApps`를 반환하지만 Child 앱이 이 값을 읽어 접근성 allow/block 목록에 반영하지 않는다.
- Parent 앱 whitelist와 백엔드 `AppBlock`은 연결되어 있지 않다. AppBlock 쓰기 API도 없다.

부스 기준 권장:

- Android 실기기에서 접근성 권한을 미리 켜둔다.
- 데모용으로 남은 시간을 0으로 만드는 별도 빌드/토글을 준비하면 앱 차단은 보여줄 수 있다.
- 백엔드 기반 패키지별 차단까지 보여주는 것은 현재 범위를 넘는다.

### 미션

현재 Child 앱 호출:

- 목록: `GET /api/v1/missions`
- 상세: `GET /api/v1/missions/{id}`
- 제출: `POST /api/v1/missions/{id}/performances` multipart `image` 1개

백엔드 현재 계약:

- 목록은 `childId` query와 Parent 권한을 요구한다.
- 제출은 `childId`, `image`, `category`, `prompt`를 모두 `@RequestParam`으로 요구한다.

실백엔드 시연 영향:

- Child 미션 목록 조회가 실패한다.
- Child 미션 사진 제출도 앱 요청만으로는 실패한다.

필요 조치:

- `GET /api/v1/missions`에서 CHILDREN 권한이면 JWT의 childId로 본인 미션을 반환하도록 분기.
- `POST /api/v1/missions/{missionId}/performances`에서 childId는 JWT에서, category/prompt는 mission setting 또는 `MissionPromptProvider`에서 파생하도록 변경.

---

## 5. 백엔드 정합성 검수

### 현재 제공 중인 핵심 API

- Auth: `/auth/parent/login`, `/auth/children/login`, signup, refresh, logout
- Parent: `/api/v1/parents/children`, `/api/v1/parents/time-policy`
- Child policy read: `/api/v1/children/{childId}/policies`
- Mission: create/list/detail/performances/approve/reject
- Schedule: daily/extend/settle/templates/routines/weekly-budgets
- Notification/FCM: 기본 API 존재

### 데모를 막는 계약 차이

| 영역 | 차이 | 영향 |
|---|---|---|
| Auth | `AuthResponse`에 `memberId` 없음 | Parent 홈 실데이터 로드 막힘 |
| Child 연결 | `childrenBirth` 필수, 응답 `Void` | Parent 자녀 등록 실패/파싱 실패 |
| Child signup | 백엔드는 `name/email/password`, 앱은 `username/password` | Child 앱 회원가입 실통신 실패 |
| Mission list | 백엔드는 Parent + `childId`, Child 앱은 Child 토큰으로 no-query 호출 | Child 미션 홈 실패 |
| Mission submit | 백엔드는 `childId/category/prompt/image`, 앱은 `image`만 | Child 미션 제출 실패 |
| App block | 백엔드 AppBlock 쓰기 API 없음, 앱 native blocker와 미연결 | 실백엔드 기반 앱 차단 불가 |
| Time confirm | `/time-confirm/*` 없음 | Child 시간 확인/수정요청 실데이터 불가 |

---

## 6. 부스 시연 권장 플랜

### 가장 안전한 플랜: Mock 중심

1. Parent/Child 앱 모두 dev 기본값 `useMocks: true` 유지.
2. Parent 앱에서 mock 자녀/시간 설정/미션 생성 화면을 시연.
3. Child 앱에서 mock 시간 설정, mock 미션 수행 흐름을 시연.
4. 앱 차단은 Android 실기기 접근성 권한을 미리 켜고, 별도 데모용으로 남은 시간 0 상태를 만드는 빌드/토글을 준비.

장점:

- 현재 테스트 통과 상태 그대로 시연 가능.
- 백엔드 계정/DB seed/토큰 문제를 피할 수 있다.

주의:

- "실제 백엔드 저장됨"을 보여주기는 어렵다.
- 앱 차단은 현재 기본 앱 상태에서는 자동 발동하지 않는다.

### 실백엔드까지 보여주는 최소 플랜

아래 순서대로만 고치면 "Parent 시간 설정 -> Child 시간 저장 -> Parent 미션 생성 -> Child 제출 -> Parent 승인" 흐름이 열린다.

1. 로컬 백엔드 실행 환경 정리
   - JDK 21 설치
   - `gradlew` 실행 권한 부여 또는 `bash ./gradlew ...` 사용
   - PostgreSQL/local env 또는 test profile 정리

2. Auth 응답 보강
   - `AuthResponse(memberId, name/email)` 추가
   - Parent 홈 로딩의 전제 조건 해결

3. 자녀 연결 보강
   - `childrenBirth` optional
   - `POST /api/v1/parents/children`이 생성된 자녀 객체 반환
   - 자녀 코드 확인을 위해 Child signup 응답 또는 profile/me 응답에 `childCode` 포함

4. Child 미션 API 보강
   - Child 토큰으로 `GET /api/v1/missions` 가능하게 변경
   - performance 제출은 image만 받아도 동작하도록 서버에서 child/category/prompt 파생

5. 앱 차단 데모 보강
   - 빠른 데모용: Child 앱에 "남은 시간 0분" 디버그 토글 또는 빌드 플래그 추가
   - 실정합성용: Parent가 설정한 blocked/whitelist를 백엔드에 저장하고 Child가 policy를 읽어 native blocker에 반영

---

## 7. 최종 판정

| 항목 | Mock 시연 | 실백엔드 시연 | 메모 |
|---|---:|---:|---|
| Parent 시간 설정 화면 | 가능 | 부분 가능 | 월 총량 API는 맞음. whitelist는 백엔드 없음 |
| Child 시간 설정 화면 | 가능 | 부분 가능 | 저장 API는 분할 호출 가능. 조회/확인 플로우는 없음 |
| 앱 차단 | 별도 준비 필요 | 불가에 가까움 | native 구현은 있으나 남은 시간/blockedApps와 미연결 |
| Parent 미션 생성 | 가능 | 가능 | 자녀 연결/세션이 먼저 필요 |
| Child 미션 목록 | 가능 | 불가 | 백엔드 권한/파라미터 구조 변경 필요 |
| Child 미션 제출 | 가능 | 불가 | 백엔드가 image 외 파라미터를 요구 |
| Parent 미션 승인/거절 | 가능 | 부분 가능 | performance가 생성되어 있어야 함 |

**한 줄 정리:** 부스용으로는 mock 시연은 충분히 가능하다. 실백엔드까지 붙여서 보여주려면 Auth/memberId, 자녀 연결, Child 미션 조회/제출, 앱 차단 트리거를 먼저 손봐야 한다.
