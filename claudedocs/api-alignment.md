# API 정합성 분석 및 정렬 가이드

> 대상
> - **백엔드**: `2026-Bridge-quadS` (Spring Boot, `me.sogom.bridge`)
> - **부모 앱**: `Quad-S-Team12-App-Parent` (Flutter, Dio)
> - **자녀 앱**: `Quad-S-Team12-App-Child` (Flutter, Dio)
>
> 작성일: 2026-06-01
> 목적: 앱과 백엔드 API 불일치를 정리하고, **변경을 최소화하면서(앱 우선) 오류 가능성이 낮은 방향**으로 정렬 방안을 문서화.

---

## 0. 현재 상태 요약

- 두 앱 모두 `repository 패턴`으로 `Api*`(실통신) ↔ `Mock*`(가짜데이터) 구현을 분리해 둠.
- `environment.dart`의 dev 기본값이 **`useMocks: true`** → 현재는 백엔드와 실통신하지 않고 mock으로만 동작.
- `baseUrl`은 전부 placeholder (`api.dev.bridge-p.example.com`, `api.dev.bridge-k.example.com`).
- `useMocks: false` + 실제 baseUrl로 전환하는 순간 **대부분의 엔드포인트가 경로/구조 불일치로 깨짐**.

판단 근거: 앱은 "합의된(이상적인) REST 스펙" 기준으로 미리 구현됐고, 백엔드는 도메인 주도(`/api/v1`, 역할별 auth, mission performance, schedule 분리) 구조로 별도 구현됨. 두 스펙이 아직 정렬되지 않음.

---

## 1. 전역(공통) 차이 — 여기서 80%가 해결됨

개별 엔드포인트를 건드리기 전에, **모든 호출에 공통으로 적용되는 3가지**를 먼저 처리하면 작업량과 오류가 크게 줄어든다.

### S1. 응답 래퍼 언래핑 (가장 중요) — 앱 `dio_config` 인터셉터 1곳 수정

| | 형태 |
|---|---|
| 백엔드 성공 | `{ "isSuccess": true, "code": "...", "message": "...", "data": <실제값> }` |
| 백엔드 에러 | `{ "isSuccess": false, "code": "...", "message": "...", "data": null }` |
| 앱이 기대(성공) | `data`를 **래퍼 없이 직접** (예: `response.data['accessToken']`) |
| 앱이 기대(에러) | `{ "error": { "code": "...", "message": "..." } }` |

**CoT**: 모든 엔드포인트에 영향을 주지만, 개별 repository를 다 고치는 건 비효율적이고 오류가 많다. 백엔드 래퍼를 없애는 것도 백엔드 전역 변경이라 위험. → **앱의 `dio_config.dart` response/error 인터셉터에서 한 번만 변환**하는 게 최소 변경·최소 오류.

조치(앱):
```dart
// onResponse 인터셉터 추가 (양 앱 dio_config.dart)
onResponse: (response, handler) {
  final body = response.data;
  if (body is Map && body.containsKey('isSuccess')) {
    response.data = body['data']; // data 언래핑 → 기존 repository 코드 그대로 동작
  }
  handler.next(response);
}

// onError 인터셉터에서 백엔드 에러 → 앱이 읽는 {error:{code,message}} 형태로 정규화
// body = { isSuccess:false, code, message } → { error: { code, message } }
```

⚠️ 예외: **자녀 앱의 목록 파싱**은 `{ "missions": [...] }`, `{ "notifications": [...] }`처럼 자체 키를 가정함. 백엔드 `data`는 **배열 그 자체**. 언래핑 후엔 배열이 되므로, 자녀 앱의 해당 파싱부는 개별 수정 필요(아래 표 참고).
부모 앱 목록은 `response.data`를 배열로 직접 기대 → 언래핑 후 일치 ✅.

### S2. `/api/v1` prefix — 앱 경로 수정

- 백엔드: `/auth/**`를 **제외한** 모든 경로가 `/api/v1/...`.
- `/auth/**`는 백엔드도 루트(prefix 없음) → auth는 prefix 영향 없음.

조치(앱): auth를 제외한 repository 경로 문자열에 `/api/v1` 추가.
> dio `baseUrl`에 `/api/v1`를 넣는 방식은 절대경로(`/auth/...`) 결합 동작이 헷갈려 오류 위험이 있으므로 **각 경로 문자열에 명시적으로 붙이는 방식**을 권장.

### S3. 사용자 식별 (`parentId`) — 대부분 그대로 둬도 됨

**CoT**: 부모 앱은 거의 모든 호출에 `parentId`를 query/body로 보낸다. 백엔드는 이를 받지 않고 **JWT 토큰(`@AuthenticationPrincipal`)으로 식별**한다.
- query의 unknown 파라미터, body의 unknown 필드는 Spring/Jackson 기본 설정상 **무시되어 에러를 내지 않음**.
- 따라서 query/body에 들어간 `parentId`는 **굳이 제거하지 않아도 동작**한다. (오류 최소화 관점에서 작업 생략 가능)
- 단 **경로에 박힌** `parentId`(`/parents/{parentId}`)는 경로 자체가 달라 문제 → 경로 변경 필요.
- 진짜 이슈: 로그인 응답에 `parentId`가 없어서(아래 1-S4) 앱이 query에 넣을 값이 사라짐 → 빈 문자열을 넣어도 백엔드가 무시하므로 무방.

### S4. 로그인 응답 필드 — 백엔드 소변경 1건 권장

- 앱(부모) 로그인은 응답에서 `parentId`, `name`을 읽어 세션에 저장. 자녀 앱은 `username`을 읽음.
- 백엔드 `AuthResponse`는 `{ accessToken, refreshToken }`만 반환.

**CoT**: 앱이 토큰만으로 동작하게 바꾸려면 세션 저장·UI 표시 로직을 광범위하게 고쳐야 함(오류↑). 반면 백엔드 `AuthResponse`에 `memberId`(+선택적으로 `name`) 한 필드를 추가하는 건 **매우 작은 변경**이고 양 앱이 받는 형태와 잘 맞음.
→ **권장: 백엔드 `MemberResDTO.AuthResponse`에 `memberId`(, `name`) 추가.** (예외적으로 백엔드를 손대는 게 전체 오류를 줄이는 케이스)

---

## 2. 등급 정의

| 등급 | 의미 | 위험도 |
|---|---|---|
| 🟢 **A** | 앱에서 경로/prefix/필드명만 수정. 거의 무위험 | 낮음 |
| 🟡 **B** | 앱 로직 변경 필요(식별자 흐름, ID 기반 전환, 응답 필드 매핑 등) | 중간 |
| 🔴 **C** | 구조적 불일치 — 백엔드에 엔드포인트가 없거나 설계가 다름. 팀 결정/백엔드 변경 필요 | 높음 |

> 전제: S1(언래핑)·S2(prefix)는 전 항목에 공통 적용되므로 아래 표에서는 **그 외 추가 조치**만 표기.

---

## 3. 부모 앱(Parent) 엔드포인트 매핑

### 3.1 인증/계정

| 앱 호출 | 백엔드 실제 | 등급 | 조치 |
|---|---|---|---|
| `POST /auth/login` `{email,password}` | `POST /auth/parent/login` `{email,password}` | 🟢→🟡 | 경로만 변경. body 일치 ✅. 단 응답 `parentId/name` 없음 → **S4 적용** |
| `POST /auth/signup` `{email,name,password}` | `POST /auth/parent/signup` `{name,email,password}` | 🟢 | 경로 변경. body 필드 일치 ✅ |
| `POST /auth/refresh` `{refreshToken}` | `POST /auth/token/refresh` `{refreshToken}` | 🟢 | 경로 변경. ⚠️ **`dio_config.dart`의 401 refresh 호출 경로(`/auth/refresh`)도 함께 수정** |
| `POST /auth/logout` `{refreshToken?}` | `POST /auth/logout` `{refreshToken}` | 🟢 | 경로 일치 ✅. ⚠️ 백엔드 `refreshToken` `@NotBlank` → 앱이 null/빈값 보내면 400. 로그아웃 시 토큰 항상 채워 보내도록 보장 |
| `PUT /auth/password` `{parentId,currentPassword,newPassword}` | `PATCH /api/v1/members/password` `{oldPassword,newPassword}` | 🟡 | 경로+메서드(PUT→PATCH), `currentPassword`→`oldPassword`, `parentId` 제거 |
| `DELETE /auth/account` `{parentId}` | `DELETE /api/v1/members` (no body) | 🟡 | 경로 변경, body 제거 |

### 3.2 프로필 (자녀 정보 외)

| 앱 호출 | 백엔드 실제 | 등급 | 조치 |
|---|---|---|---|
| `GET /parents/{parentId}` → `{parentId,email,name,status}` | ❌ 없음 | 🔴 | 부모 프로필 조회 엔드포인트 없음. **대안A**: 로그인 응답(S4) 정보를 캐시해 화면 표시 / **대안B**: 백엔드 `GET /api/v1/members/me` 추가 |
| `PATCH /parents/{parentId}` `{name}` | ❌ 없음 | 🔴 | 이름 수정 없음 → 백엔드 추가 또는 기능 보류 |
| `PATCH /parents/{parentId}/status` `{status}` | ❌ 없음 | 🔴 | 휴면 전환 미구현 → 백엔드 추가 또는 기능 보류 |

### 3.3 자녀 관리

| 앱 호출 | 백엔드 실제 | 등급 | 조치 |
|---|---|---|---|
| `POST /children/validate-code` `{code}` → `{valid}` | ❌ 없음(등록 시 검증) | 🔴/🟡 | 백엔드는 등록(`POST .../children`) 시 `childrenCode` 검증. **대안**: 앱이 별도 검증 단계를 생략하고 등록 결과로 처리, 또는 백엔드에 코드검증 엔드포인트 추가 |
| `GET /children?parentId` → `[{childrenId,childCode,name,photoBase64}]` | `GET /api/v1/parents/children` → `[{childrenId,name,profileImageUrl}]` | 🟡 | 경로 변경. 응답 필드 매핑: `photoBase64`→`profileImageUrl`(URL), `childCode` 미제공 → 앱 모델/화면 조정 |
| `POST /children` `{parentId,childCode,name,photoBase64}` | `POST /api/v1/parents/children` `{childrenName,childrenBirth,childrenCode,profileImageUrl}` | 🟡 | 경로 변경. 필드명 매핑(`name`→`childrenName`, `childCode`→`childrenCode`). ⚠️ **`childrenBirth` 필수** → 앱에 생년월일 입력 필요. `photoBase64`→`profileImageUrl`은 base64가 아니라 **URL** 요구 → 업로드 방식 결정 필요. 응답: 백엔드 `Void`(앱은 `ChildSummary` 기대) → 등록 후 목록 재조회로 처리 |
| `DELETE /children/{childrenId}?parentId` | ❌ 없음 | 🔴 | 자녀 삭제(연결 해제) 엔드포인트 없음 → 백엔드 추가 또는 보류 |

### 3.4 미션 (부모: 생성/관리/승인)

| 앱 호출 | 백엔드 실제 | 등급 | 조치 |
|---|---|---|---|
| `GET /children/{id}/missions?parentId` → `[TodayMission]` | `GET /api/v1/missions?childId={id}` → `[{missionId,title,category,reward}]` | 🟡 | 경로 변경: `childId`를 path→query로. 응답이 요약(summary) 필드만 → 상세 필드는 `GET /missions/{id}`로 보강하거나 화면 요구 축소 |
| `POST /children/{id}/missions` (단건) `{...}` | `POST /api/v1/missions` `{childId,title,category,resetCycle,verificationType,reward,description}` | 🟡 | 경로 변경. body에 `childId` 추가. 필드명 매핑: `rewardMinutes`→`reward`, `resetPeriod`→`resetCycle`, `confirmationMethod`→`verificationType`. ⚠️ enum 값 표기 일치 확인 필요(`category`/`resetCycle`/`verificationType`) |
| `PUT /children/{id}/missions` (일괄 저장) | ❌ 없음 | 🔴 | 백엔드는 개별 CRUD만. **앱이 일괄→개별 호출로 전환** |
| `PUT /children/{id}/missions/at/{index}` (수정) | ❌ 없음 | 🔴 | 백엔드에 미션 **수정** 엔드포인트 자체 없음 → 백엔드 추가 또는 (삭제+생성)으로 우회 |
| `DELETE /children/{id}/missions/at/{index}` | ❌ 없음 | 🔴 | 미션 삭제 없음 → 백엔드 추가 필요 |
| `POST .../at/{index}/approve` | `PATCH /api/v1/missions/performances/{performanceId}/approve` | 🟡 | 경로+메서드(POST→PATCH). ⚠️ **index→performanceId 전환**: 앱이 수행(performance) ID를 알아야 함. `GET /missions/{id}/performance`로 `performanceId` 확보 후 호출 |
| `POST .../at/{index}/reject` | `PATCH /api/v1/missions/performances/{performanceId}/reject` | 🟡 | 위와 동일 |

> ⚠️ 미션은 **인덱스(at/{index}) 기반 ↔ ID(missionId/performanceId) 기반**이라는 근본 차이가 있음. 앱이 목록 수신 시 `missionId`/`performanceId`를 보관하고 이후 ID로 호출하도록 바꾸는 것이 정석. 가장 작업량이 큰 영역.

### 3.5 시간 정책/계획 (부모) — 구조적 재설계 영역 🔴

| 앱 호출 | 백엔드 후보 | 등급 | 비고 |
|---|---|---|---|
| `GET/PUT /children/{id}/time-plan/monthly-total` `{totalMinutes}` | `POST /api/v1/parents/time-policy` `{childId,yearMonth,baseTime}` / `GET /api/v1/children/{childId}/policies`(`baseTime`) | 🔴 | 백엔드는 set(POST)만, 월(yearMonth) 단위 `baseTime` 개념. 앱은 단순 `totalMinutes` get/put. 부분 매핑 필요 |
| `GET/PUT /children/{id}/time-plan/daily-rules` | `PUT /api/v1/schedules/templates`, `POST /schedules/routines` | 🔴 | ⚠️ **백엔드 `schedules`는 자녀 토큰(`asChildren`)으로 식별** → 부모 앱이 호출 못 함. 설계 충돌 |
| `GET/PUT /children/{id}/time-plan/weekly-rules` | `POST /api/v1/schedules/weekly-budgets` | 🔴 | 위와 동일(자녀 식별) |
| `GET/PUT /children/{id}/time-plan/whitelist` `{appIds}` | `GET /api/v1/children/{childId}/policies`(`blockedApps`) | 🔴 | ⚠️ 개념 반대: 앱은 **허용(whitelist)**, 백엔드는 **차단(AppBlock/blockedApps)**. 설정용 엔드포인트(쓰기)도 백엔드에 없음 |

> **결론(시간 영역)**: "누가 시간을 설정하는가"가 다름 — 앱은 *부모가 자녀 시간을 설정*, 백엔드는 *부모는 `baseTime`(월 기준)만, 나머지 스케줄/루틴은 자녀가 설정*. 이 영역은 단순 매핑 불가, **팀 차원의 설계 합의 필요**(별도 논의 안건).

### 3.6 디바이스/FCM

| 앱 호출 | 백엔드 실제 | 등급 | 조치 |
|---|---|---|---|
| `POST /devices` `{fcmToken,platform}` → `{id}` | `POST /api/v1/fcm/token` `{fcmToken}` → `String` | 🟡 | 경로 변경. `platform` 불필요(무시됨). 응답 `{id}` 없음 → 앱의 deviceId 의존 제거(또는 더미값 처리. 현재도 `'transferred'` 폴백 존재) |
| `DELETE /devices/{deviceId}` | ❌ 없음 | 🔴 | FCM 토큰 삭제 엔드포인트 없음 → 로그아웃 시 무시 처리(앱은 이미 404를 성공 취급) 또는 백엔드 추가 |

### 3.7 알림 (부모)

| 앱 호출 | 백엔드 실제 | 등급 | 조치 |
|---|---|---|---|
| `GET /notifications?parentId` → `[NotificationItem]` | `GET /api/v1/notifications` → `[{notificationId,title,content,isRead,notificationType,createdAt}]` | 🟡 | 경로 변경. 필드 매핑: `id`←`notificationId`, `message`←`content`, `type`←`notificationType`, `timeAgo`(앱 계산)←`createdAt`로 계산 |
| `GET /notifications/unread-count` → `{unread,count}` | ❌ 없음 | 🔴 | 미읽음 카운트 엔드포인트 없음 → **앱이 목록에서 `isRead==false` 개수 계산**(앱 로직, 무위험) |
| `PATCH /notifications/{id}/read?parentId` | `PATCH /api/v1/notifications/{id}/read` | 🟢 | prefix만 |
| `DELETE /notifications/{id}?parentId` | `DELETE /api/v1/notifications/{id}` | 🟢 | prefix만 |

---

## 4. 자녀 앱(Child) 엔드포인트 매핑

### 4.1 인증

| 앱 호출 | 백엔드 실제 | 등급 | 조치 |
|---|---|---|---|
| `POST /auth/login` `{username,password}` → `{accessToken,refreshToken,username}` | `POST /auth/children/login` `{email,password}` → `{accessToken,refreshToken}` | 🟡 | 경로 변경. ⚠️ **`username` vs `email`**: 백엔드는 `email`로 로그인. 자녀가 username/코드로 로그인하는 UX라면 합의 필요. 응답 username 없음 → S4 |
| `POST /auth/signup` `{username,password}` | `POST /auth/children/signup` `{name,email,password}` | 🟡 | 경로 변경. ⚠️ 필드 불일치(`username` vs `name`+`email`). 또한 자녀가 **부모 코드로 연결**되는 흐름이 백엔드 `SignUpRequest`엔 없음 → 가입/연결 흐름 합의 필요 |
| `POST /auth/refresh` `{refreshToken}` | `POST /auth/token/refresh` | 🟢 | 경로 변경 + ⚠️ `dio_config.dart` refresh 경로 동시 수정 |

### 4.2 미션 (자녀: 조회/제출)

| 앱 호출 | 백엔드 실제 | 등급 | 조치 |
|---|---|---|---|
| `GET /missions` → `{missions:[...]}` | `GET /api/v1/missions?childId=...` | 🔴 | ⚠️ **백엔드 `GET /missions`는 `asParent()`로 식별** → 자녀가 자기 미션을 조회할 엔드포인트가 사실상 없음. 백엔드에 자녀용 미션 목록 조회 추가 필요. 또한 응답 래퍼: 백엔드 `data=배열` vs 앱 `{missions:[]}` → **앱 파싱 개별 수정** |
| `GET /missions/{id}` | `GET /api/v1/missions/{missionId}` | 🟢 | prefix만(응답 필드 차이는 존재) |
| `POST /missions/{id}/submit` `{photoUrls}` | `POST /api/v1/missions/{missionId}/performances` (multipart: `childId,image,category,prompt`) | 🔴 | ⚠️ **방식 완전 상이**: 앱은 ① `POST /uploads/photo`로 사진 업로드 후 URL 획득 → ② `{photoUrls}` JSON 제출(2단계). 백엔드는 사진 파일을 **multipart로 직접** performances에 전송(1단계), `/uploads/photo` 없음. + `category,prompt` 파라미터 요구. **앱이 multipart 직접 업로드로 전환** 권장(백엔드 `prompt`는 서버 내부 생성으로 빼는 백엔드 소변경 검토) |

### 4.3 마이페이지/계정 (자녀)

| 앱 호출 | 백엔드 실제 | 등급 | 조치 |
|---|---|---|---|
| `GET /user/profile` → `{username,accountType,childCode}` | ❌ 없음 | 🔴 | 자녀 프로필 조회 없음 → 로그인 정보 캐시 사용 또는 백엔드 `GET /api/v1/members/me` 추가 |
| `PATCH /user/password` `{currentPassword,newPassword}` | `PATCH /api/v1/members/password` `{oldPassword,newPassword}` | 🟡 | 경로 변경, `currentPassword`→`oldPassword` |
| `DELETE /user/account` | `DELETE /api/v1/members` | 🟢 | 경로 변경만 |

### 4.4 시간 설정/확인 (자녀) — 구조적 재설계 영역 🔴

| 앱 호출 | 백엔드 후보 | 등급 | 비고 |
|---|---|---|---|
| `GET /time-setup/previous-week` → `TimeSchedule` | ❌ 없음 | 🔴 | 지난주 스케줄 조회 없음 |
| `GET /time-setup/current` → `{schedule}` | `GET /api/v1/schedules/routines` (+templates) | 🔴 | 부분 매핑. 통합 조회 없음 |
| `POST /time-setup` `{allowedHours,weeklyTotals,dayAllocations}` | `PUT /schedules/templates` + `POST /schedules/routines` + `POST /schedules/weekly-budgets` (3개 분리) | 🔴 | ⚠️ 앱은 한 번에 저장, 백엔드는 3개 엔드포인트로 분리. 앱이 분할 호출하도록 큰 로직 변경 또는 백엔드에 통합 엔드포인트 추가 |
| `GET /time-confirm/current` | `GET /api/v1/schedules/daily?date=` (개념 다름) | 🔴 | 확인(승인) 플로우 자체가 백엔드에 없음 |
| `POST /time-confirm/request-modification` | ❌ 없음 | 🔴 | 수정 요청 플로우 없음 |
| `POST /time-confirm/acknowledge` | ❌ 없음 | 🔴 | 확인 플로우 없음 |

> 시간 영역은 부모/자녀 양쪽 모두 백엔드 설계(`schedules`, `time-policy`, `policies`)와 앱 설계(`time-plan`, `time-setup`, `time-confirm`)가 근본적으로 다름. **별도 설계 정렬 회의 안건**으로 분리 권장.

### 4.5 리포트 (자녀)

| 앱 호출 | 백엔드 실제 | 등급 | 조치 |
|---|---|---|---|
| `GET /reports/weekly?weekOf` → `UsageReport`(plan/dailyRows/compliance/suggestions) | ❌ 없음 | 🔴 | 주간 사용 리포트 백엔드 전무 → 신규 도메인 구현 필요. 단기적으로는 앱 mock 유지 |

### 4.6 알림 (자녀)

| 앱 호출 | 백엔드 실제 | 등급 | 조치 |
|---|---|---|---|
| `GET /notifications` → `{notifications:[...]}` | `GET /api/v1/notifications` → `[array]` | 🟡 | prefix. ⚠️ 래퍼: 백엔드 `data=배열` → 앱 `{notifications:[]}` 가정 불일치 → **앱 파싱 수정**. 필드 매핑(`id←notificationId`, `message←content`, `type←notificationType`, `createdAt`) |
| `DELETE /notifications/{id}` | `DELETE /api/v1/notifications/{id}` | 🟢 | prefix만 |
| `PATCH /notifications/{id}/read` | `PATCH /api/v1/notifications/{id}/read` | 🟢 | prefix만 |

### 4.7 디바이스/FCM (자녀)
부모 앱 3.6과 동일. `POST /devices`→`POST /api/v1/fcm/token`(🟡), `DELETE /devices/{id}`→ 백엔드 없음(🔴).

---

## 5. 백엔드에만 존재 / 앱에만 존재 (커버리지 갭)

### 백엔드에 있으나 앱이 호출하지 않음
- `POST /api/v1/fcm/test` (테스트 푸시) — 디버그용, 무시 가능
- `POST /api/v1/notifications` (알림 생성) — 서버 내부/관리용으로 보임
- `POST /api/v1/schedules/settle`, `/extend` (시간 정산/연장) — 앱의 time 플로우와 미연결
- `GET /test` (Gemini 헬스체크) — 운영용

### 앱이 호출하나 백엔드에 없음 (🔴 핵심 갭)
- 부모 프로필 조회/수정, 휴면 전환 (`/parents/{id}`, `/status`)
- 자녀 코드 검증(`/children/validate-code`), 자녀 삭제(`DELETE /children/{id}`)
- 미션 수정/삭제(`PUT/DELETE .../missions/...`), 미션 일괄 저장
- 알림 미읽음 카운트(`/notifications/unread-count`) → 앱 계산으로 대체 가능
- 사진 업로드(`/uploads/photo`)
- 자녀 시간 확인 플로우(`/time-confirm/*`), 시간 설정 통합(`/time-setup/*`)
- 주간 리포트(`/reports/weekly`)
- 디바이스/FCM 토큰 삭제

---

## 6. 권장 작업 순서 (오류 최소화 우선)

1. **[전역·앱]** `dio_config` 인터셉터에 **S1 응답 언래핑 + 에러 정규화** 추가 (양 앱).
2. **[전역·앱]** auth 외 경로에 **`/api/v1` prefix** 추가 (S2). `refresh` 경로(`/auth/token/refresh`)는 repository + **인터셉터 양쪽** 수정.
3. **[백엔드·소변경]** `AuthResponse`에 `memberId`(+`name`) 추가 (S4) — 양 앱 세션 안정화.
4. **[앱·🟢]** auth 경로 정렬(`/auth/parent|children/login` 등), 알림 read/delete, 계정 삭제 등 단순 경로/필드명 변경.
5. **[앱·🟡]** 비밀번호 변경(필드명/메서드), 자녀 목록/등록(필드 매핑), 미션 생성(필드명+childId), FCM 토큰, 알림 목록 파싱.
6. **[앱·🟡]** 미션 **ID 기반 전환**(performanceId 확보 → approve/reject), 자녀 앱 미션 목록 파싱.
7. **[백엔드 결정·🔴]** 미션 수정/삭제, 자녀 삭제, 프로필 조회, 사진 업로드(또는 multipart 직접 전송) — 추가 여부 결정.
8. **[설계 합의·🔴]** 시간(time) 도메인 전체, 자녀 미션 조회 권한, 주간 리포트 — 별도 안건으로 분리.

> 1~6단계까지 진행하면 **인증·미션 핵심·알림·자녀 관리·FCM**가 실통신 가능 상태가 되고, 7~8단계는 백엔드/설계 합의가 필요한 잔여 영역.

---

## 7. 미해결 결정 사항 (팀 합의 필요)

1. **자녀 로그인 식별자**: `username`/코드 vs `email`. 자녀-부모 연결(부모 코드) 가입 흐름.
2. **사진 제출 방식**: 2단계(업로드→URL) vs 1단계(multipart 직접). `prompt`를 서버 내부 생성으로 둘지.
3. **시간(time) 도메인 주체**: 부모가 설정 vs 자녀가 설정 + 부모 baseTime. 앱/백엔드 중 어느 모델로 통일할지.
4. **미션 수정/삭제, 자녀 삭제, 프로필, 주간 리포트** 엔드포인트 백엔드 신규 구현 여부/우선순위.
