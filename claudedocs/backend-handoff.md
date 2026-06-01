# 백엔드 핸드오프 — 앱 정렬 후 백엔드에서 처리해야 할 항목 (도메인별)

> 선행 문서: [api-alignment.md](./api-alignment.md)
> 맥락: 앱(Parent/Child) API 레이어는 백엔드 스펙에 맞춰 정렬 완료(경로/prefix/필드/응답 언래핑/enum/알림 파싱). 두 앱 모두 `flutter analyze` 클린.
> 이 문서는 **앱만으로는 해결 불가**하여 백엔드 변경/결정이 필요한 항목만 정리한다.
> 작성일: 2026-06-01

## 우선순위 표기
- **P1 (시연 필수)**: 핵심 시연 흐름을 막음. 백엔드 없이는 해당 화면이 동작 안 함.
- **P2 (기능)**: 기능은 필요하나 시연 시나리오에 따라 선택.
- **P3 (선택)**: 없어도 앱이 우아하게 degrade. 여유 시 처리.

## 앱에서 이미 완화한 것 (백엔드 작업 불필요 / 참고만)
- ✅ 응답 래퍼 `{isSuccess,code,message,data}` → 앱 인터셉터가 자동 언래핑. **백엔드는 현행 래퍼 유지하면 됨.**
- ✅ 미션 enum 대소문자: 앱이 **UPPER_CASE로 전송 + 대소문자 무시 디코드**. 백엔드 enum 명칭만 아래대로 유지하면 됨.
- ✅ 알림 `notificationId`(숫자)·`notificationType` 파싱: 앱이 방어적으로 처리(크래시/누락 제거).
- ✅ 알림 미읽음 수: 앱이 목록에서 로컬 계산.
- ✅ 에러 코드 매핑(§8.1): 앱 `errorCodeOf` 가 백엔드 `MEMBER*/MISSION*` 코드를 앱 표준 코드로 변환. **백엔드 코드 현행 유지.**
- ✅ 미션 승인/거절(§3.3): 앱이 기존 엔드포인트 라운드트립으로 처리. **백엔드 무변경.**
- ✅ 자녀 등록(§2.1) 요청 필드: 앱이 `{childrenName,childrenCode,profileImageUrl}` 로 정렬 전송(단, 백엔드의 birth 완화 + 응답 반환 2가지는 §2.1 P1로 필요).

---

## 1. Member / Auth 도메인

| # | 항목 | 우선 | 내용 / 제안 |
|---|---|---|---|
| 1.1 | **AuthResponse에 `memberId` 추가** | **P1** | 현재 `{accessToken, refreshToken}`만 반환. 앱이 로그인 후 세션에 사용자 식별자를 저장해야 함(앱은 이미 `json['memberId']`를 방어적으로 읽음). `MemberResDTO.AuthResponse`에 `memberId`(가능하면 `name`도) 추가. ⚠️ **주의**: JWT 토큰 claim에는 `memberId`가 없고 `subject=email`, `role`, `email`만 있음(`JwtUtil`). 따라서 토큰이 아니라 `AuthService.issueTokens`에서 `Member` 엔티티의 `getId()`/`getName()`을 직접 응답에 담아야 함. 변경 작음. |
| 1.2 | 부모 프로필 조회 | P2 | 앱: `GET /parents/{parentId}` → `{memberId,email,name,status}`. 백엔드 부재. 제안: `GET /api/v1/members/me`(토큰 식별) 신규. |
| 1.3 | 부모 이름 수정 | P2 | 앱: `PATCH /parents/{parentId}` `{name}`. 백엔드 부재. 제안: `PATCH /api/v1/members/me` `{name}`. |
| 1.4 | 부모 휴면 전환 | P3 | 앱: `PATCH /parents/{parentId}/status` `{status}`. 백엔드 부재. |
| 1.5 | 자녀 프로필 조회 | P2 | 자녀앱: `GET /user/profile` → `{username,accountType,childCode}`. 백엔드 부재. 제안: `GET /api/v1/members/me` 를 역할 공통으로 사용하고 자녀는 `childCode` 포함. |
| 1.6 | **자녀 로그인/가입 식별자 결정** | **P1(결정)** | 자녀앱은 `username` 기반(`{username,password}`), 백엔드는 `email` 기반(`{name,email,password}`). 또한 자녀-부모 **연결 코드**가 가입 흐름에 필요한데 `SignUpRequest`에 코드 필드 없음. **결정 필요**: 자녀는 username vs email 중 무엇으로 로그인? 가입 시 부모 코드 연결을 어디서? |

---

## 2. 자녀 관리 도메인 (parents 하위)

| # | 항목 | 우선 | 내용 / 제안 |
|---|---|---|---|
| 2.1 | **자녀 등록 — `childrenBirth` 선택화 + 생성결과 반환** | **P1** | 📌 **결정 확정(pinned)** & **앱 적용 완료**. 앱은 이제 `POST /api/v1/parents/children` 에 `{childrenName, childrenCode, profileImageUrl?}` 를 전송(parentId·childrenBirth 미전송). **백엔드 할 일 2가지**: (1) `RegisterChildRequest.childrenBirth` 를 **optional(nullable)** 로 완화, (2) 등록 응답을 `Void` 대신 **생성된 자녀 객체** `{childrenId, name, profileImageUrl}` 로 반환(앱이 `ChildSummary` 로 파싱). 이 둘만 하면 자녀 등록이 그대로 동작. |
| 2.2 | 자녀 코드 사전 검증 | P2 | 앱: `POST /children/validate-code` `{code}` → `{valid}`. 백엔드는 등록 시점에만 검증. 제안: 경량 검증 엔드포인트 추가 **또는** 앱이 사전 검증 단계 제거(등록 결과로 처리). |
| 2.3 | 자녀 연결 해제(삭제) | P2 | 앱: `DELETE /children/{childrenId}`. 백엔드 부재. 제안: `DELETE /api/v1/parents/children/{childrenId}`. |
| 2.4 | 자녀 목록 응답 필드 | P3 | 백엔드 `GET /api/v1/parents/children` → `{childrenId,name,profileImageUrl}`. 앱이 쓰던 `childCode`는 미제공(앱은 빈값 처리). 자녀 코드 표시가 필요하면 응답에 `childCode` 포함 검토. `profileImageUrl`은 URL(앱이 URL로 처리, base64 아님). |

---

## 3. Mission 도메인 (가장 영향 큼)

| # | 항목 | 우선 | 내용 / 제안 |
|---|---|---|---|
| 3.1 | **자녀 미션 조회 권한** | **P1** | `GET /api/v1/missions`가 `authMember.asParent()`로 식별 → **자녀 토큰으로 호출 시 거부**. 자녀가 자기 미션 목록을 못 봄(자녀앱 핵심 화면). 제안: 이 엔드포인트를 CHILDREN 역할도 허용(토큰의 childId로 본인 미션 조회) **또는** 자녀 전용 조회 추가. |
| 3.2 | **미션 사진 제출 방식 정렬** | **P1(결정)** | 자녀앱: 2단계(`POST /uploads/photo`→URL, `POST /missions/{id}/submit {photoUrls}`). 백엔드: 1단계 multipart `POST /api/v1/missions/{id}/performances`(`image,category,prompt`), `/uploads/photo` 없음. **결정**: (a) 앱을 multipart 직접 업로드로 전환 / (b) 백엔드가 업로드 엔드포인트+`photoUrls` 방식 추가. 또한 `prompt`는 클라이언트 필수 파라미터인데 `MissionPromptProvider`가 서버에 있으므로 **서버 내부 생성으로 빼는 것을 권장**(앱이 프롬프트를 알 이유 없음). |
| 3.3 | 미션 승인/거절 | ✅ **해결됨(앱 처리, 백엔드 무변경)** | 앱이 **기존 백엔드 엔드포인트만으로** 동작하도록 적용 완료: `GET /api/v1/missions?childId` → missionId → `GET /api/v1/missions/{missionId}/performance` → performanceId → `PATCH /api/v1/missions/performances/{performanceId}/approve\|reject` 라운드트립. **백엔드 추가 작업 불필요.** (선택 최적화: 미션 목록 응답에 `performanceId`/`status` 를 포함하면 라운드트립 2회 → 0회로 줄일 수 있음 — P3) |
| 3.4 | 미션 수정 | P2 | 앱: 미션 편집(PUT). 백엔드 **update 엔드포인트 없음**. 제안: `PUT /api/v1/missions/{missionId}`. |
| 3.5 | 미션 삭제 | P2 | 앱: 미션 삭제. 백엔드 부재. 제안: `DELETE /api/v1/missions/{missionId}`. |
| 3.6 | enum 명칭 고정 | 참고 | 앱이 맞춰둔 기준 — 변경 금지: `MissionCategory{CLEANING,STUDY,EXERCISE,ERRAND,ROUTINE}`, `ResetCycle{DAILY,WEEKLY,MONTHLY}`, `VerificationType{AI,CHILD,PARENT}`. |

> 참고: 미션 **일괄 저장**(앱의 `PUT .../missions` bulk)은 백엔드 추가 대신 **앱이 개별 호출로 전환**할 항목(앱측). 백엔드 작업 불필요.

---

## 4. Notification 도메인

| # | 항목 | 우선 | 내용 / 제안 |
|---|---|---|---|
| 4.1 | NotificationType 커버리지 | P2 | 백엔드 `{MISSION_CREATED,MISSION_APPROVED,MISSION_REJECTED,GENERAL}`. 앱은 추가로 주간리포트/시간설정완료 등 종류를 구분(현재 `GENERAL`/근사값으로 매핑). 해당 알림 종류가 필요하면 백엔드 enum 확장 검토. (없어도 크래시는 없음) |
| 4.2 | 미읽음 카운트 엔드포인트 | P3 | 앱이 목록에서 로컬 계산으로 대체함. 서버 카운트가 필요하면 `GET /api/v1/notifications/unread-count` 추가(선택). |

---

## 5. Schedule / Time 도메인 (⚠️ 설계 합의 필요 — 단순 매핑 불가)

> **근본 불일치**: 앱은 *부모가 자녀 시간을 설정*하는 모델, 백엔드는 *부모는 `baseTime`(월 단위)만, 스케줄/루틴은 자녀가 설정*하는 모델. 엔드포인트 1:1 매핑이 안 됨. **별도 설계 회의 안건.**

| # | 앱 기능 | 백엔드 현황 | 갭 |
|---|---|---|---|
| 5.1 | 부모 월 총량 get/put (`time-plan/monthly-total`) | `POST /parents/time-policy`(set만), `GET /children/{id}/policies`(baseTime 읽기) | 조회+수정 parity 없음. P2 |
| 5.2 | 부모 일별/주별 규칙 (`time-plan/daily-rules`, `weekly-rules`) | `schedules/templates`,`routines`,`weekly-budgets` — **모두 자녀 토큰(asChildren)으로만** | 권한·소유 주체 충돌. **결정 필요** |
| 5.3 | 허용앱 화이트리스트 (`time-plan/whitelist`) | `policies.blockedApps` (차단 = 반대 개념), 쓰기 엔드포인트 없음 | 개념 반전 + 설정 API 부재. **결정 필요** |
| 5.4 | 자녀 시간 설정 통합 저장 (`POST /time-setup`) | `templates`+`routines`+`weekly-budgets` 3개 분리 | 통합 vs 분리. 앱 분할 호출 or 백엔드 통합 엔드포인트. **결정 필요** |
| 5.5 | 자녀 시간 확인 흐름 (`time-confirm/current`, `request-modification`, `acknowledge`) | 없음 | 승인/수정요청 플로우 자체가 백엔드에 부재. P2 |
| 5.6 | 자녀 지난주/현재 스케줄 조회 (`time-setup/previous-week`, `current`) | `schedules/daily`,`routines` 부분 | 통합 조회 부재. P2 |

> 참고: 백엔드에 **이미 있는** `GET /api/v1/children/{childId}/policies` → `{totalAvailableTime, baseTime, accumulatedRewardTime, blockedApps[]}` 가 자녀의 "허용 시간/차단 앱 읽기" 경로다. 현재 어느 앱도 이를 호출하지 않음 — 시간 화면(자녀 현재 가용시간 표시)이 이 읽기 엔드포인트를 채택하면 신규 구현을 줄일 수 있다.

→ **권장**: 이 도메인은 코드 작업 전에 *시간 모델 주체/플로우*를 먼저 합의(부모-주도 vs 자녀-주도, 확인 플로우 유무). 합의 후 한쪽 모델로 통일.

---

## 6. Device / FCM 도메인

| # | 항목 | 우선 | 내용 / 제안 |
|---|---|---|---|
| 6.1 | FCM 토큰 삭제 | P3 | 앱: `DELETE /devices/{id}`. 백엔드 부재. 앱은 404를 성공 처리하므로 시연 무해. 로그아웃 시 토큰 정리가 필요하면 `DELETE /api/v1/fcm/token` 추가. |
| 6.2 | 등록 응답 | 참고 | 백엔드는 메시지 문자열 반환, 앱은 `{id}` 기대했으나 폴백 처리됨. 작업 불필요. |

---

## 7. Reports 도메인

| # | 항목 | 우선 | 내용 / 제안 |
|---|---|---|---|
| 7.1 | 주간 사용 리포트 | P2 | 자녀앱: `GET /reports/weekly` → `{weekLabel, plan, dailyRows, compliance, suggestions}`(상세 구조는 api-alignment.md 4.5 참고). 백엔드 **도메인 전무**. 리포트 화면이 시연에 포함되면 신규 구현 필요. 미포함이면 앱 mock 유지. |

---

## 8. 에러 코드 / 응답 계약 (감사로 추가 발견)

> 백엔드 에러 응답 형태는 앱과 **호환됨**(아래 ✅). 단 **에러 코드 문자열**이 앱이 기대하는 값과 달라, 실패 시 "구체적 안내 문구"가 일반 문구로 대체된다.

- ✅ 백엔드는 **모든 에러**를 `{isSuccess:false, code, message, data:null}` 래퍼로 일관 반환(Spring 기본 형태로 새지 않음). 앱 인터셉터가 이를 `{error:{code,message}}`로 정규화 → `api_error.dart` 호환. **HTTP status도 정상**(401/403/400/404/500).
- ✅ JWT: 백엔드 `JwtAuthFilter`가 `Authorization: Bearer <token>` 기대 → 앱 주입 형식과 일치. 401→`COMMON401`, 403→`COMMON403`도 동일 래퍼.

| # | 항목 | 우선 | 내용 |
|---|---|---|---|
| 8.1 | 에러 code 문자열 매핑 | ✅ **해결됨(앱 처리, 백엔드 무변경)** | 앱 `api_error.dart` 의 `errorCodeOf` 가 백엔드 코드를 앱 표준 코드로 변환하도록 적용 완료 — Parent: `MEMBER401→INVALID_CREDENTIALS, MEMBER404→USER_NOT_FOUND, MEMBER409→DUPLICATE_EMAIL, MEMBER403_INACTIVE→ACCOUNT_DORMANT, MEMBER404_CHILDREN→INVALID_CHILD_CODE, MEMBER409_CHILDREN→CHILD_ALREADY_LINKED, MISSION404→MISSION_NOT_FOUND, MISSION400→INVALID_MISSION_STATE`; Child: `MEMBER401→INVALID_CREDENTIALS, MEMBER404→USER_NOT_FOUND, MEMBER409→DUPLICATE_USERNAME`. 매핑 안 된 코드는 백엔드 한글 message 로 폴백. **백엔드는 현행 코드 유지하면 됨.** |
| 8.2 | 검증 실패 fieldErrors | P3 | 백엔드 `@Valid` 실패는 `message`에 필드 메시지를 문자열로 합쳐 보냄(구조화된 `fieldErrors` 없음). 앱은 단일 message만 쓰므로 **영향 없음**. 필드별 인라인 표시가 필요해지면 그때 구조화 검토. |

> 결론: 8.1 은 앱측에서 해소 완료(백엔드 무변경). 8.2 는 시연 비차단.

---

## 시연 기준 정리 (요약)

> 앱은 모든 정렬 가능한 부분을 **선반영 완료**(경로/prefix/응답 언래핑/enum/알림 파싱/에러코드 매핑/자녀등록 필드/미션 승인거절 라운드트립). 두 앱 `flutter analyze` 클린. 아래 **백엔드 작업만** 마치면 해당 흐름이 그대로 동작한다.

### 🔴 백엔드에서 해야 시연 흐름이 열리는 항목 (남은 P1)

| 항목 | 백엔드 작업 | 이거 없으면 막히는 시연 흐름 |
|---|---|---|
| **1.1** | `AuthResponse` 에 `memberId`(+`name`) 추가 (`AuthService`에서 `Member` 엔티티값 주입) | 로그인 후 세션 식별 — **모든 인증 후 흐름의 전제** |
| **1.6** | 자녀 로그인/가입 식별자 확정(email 기준 + 부모코드 연결 위치) | 자녀 로그인·가입 |
| **2.1** | `childrenBirth` optional 완화 + 등록 응답을 생성 자녀 객체로 반환 | 부모의 자녀 등록 |
| **3.1** | `GET /api/v1/missions` 를 CHILDREN 역할도 허용(토큰 childId로 본인 미션 조회) | 자녀의 미션 목록 조회 |
| **3.2** | 미션 사진 제출 방식 확정(권장: `/performances` multipart에서 `prompt` 서버 내부 생성, `category`는 path/서버 조회) **+ 해당 앱 흐름 후속 적용 필요** | 자녀의 미션 사진 제출 |

> **3.2 주의**: 사진 제출은 앱의 2단계(업로드→URL) ↔ 백엔드 multipart 라는 **양쪽 변경이 얽힌** 유일한 항목이라, 백엔드 계약 확정 후 **앱 photo 파이프라인 후속 수정**이 필요(현재 선반영하지 않음 — 계약 미확정 상태에서 추측 변경 시 오류 위험). 나머지 항목은 앱 선반영 완료.

### ✅ 앱 선반영으로 백엔드 무변경이 된 항목
- 3.3 미션 승인/거절 (기존 엔드포인트 라운드트립), 8.1 에러코드 매핑, 알림 파싱(id/type), 미읽음 카운트(로컬), 응답 래퍼 언래핑.

### ⚪ 시연 시나리오에서 빼면 백엔드 작업 불필요
- 시간 설정/확인(§5), 주간 리포트(§7.1), 부모/자녀 프로필 조회·수정(§1.2~1.5), 미션 수정/삭제(§3.4~3.5), 자녀 삭제(§2.3), 코드 사전검증(§2.2), FCM 토큰 삭제(§6.1).
