# 앱에서 못 고치는 항목 (백엔드/설계 필요) — 요약

> 두 앱(부모/자녀) mock·배선 검수 결과. **앱만으로는 불가**한 것만 추렸다.
> 앱 repository factory는 전부 `useMocks`로 정상 전환됨(배선 OK). 아래는 백엔드가 없거나 설계가 미정인 것들.
> 작성: 2026-06-02

## 이번에 앱에서 고친 것 (참고)
- 자녀앱 mock seed 제거 → 실통신 시 실데이터 노출: 알림 목록, 홈 미션 리스트, 미션 상세(`reload()` 추가).
- 부모앱 죽은 화면 `placeholder_page.dart` 삭제.
- (이전) 경로/prefix/enum/알림파싱/에러코드/미션 사진 multipart/미션 승인거절 라운드트립 정렬.

---

## 1. 백엔드 엔드포인트가 없음 → 앱이 호출하면 404 (구현 필요)

| 기능 | 앱 호출(현재) | 비고 |
|---|---|---|
| 주간 리포트 | `GET /reports/weekly` | 백엔드 리포트 도메인 자체 없음. 신규 구현 |
| 부모 프로필 조회/수정 | `GET/PATCH /parents/{id}` | 없음 |
| 자녀 프로필 조회 | `GET /user/profile` | 자녀 read 미제공 |
| 미션 수정/삭제/일괄저장 | `PUT/DELETE /children/{id}/missions[/at/{idx}]` | 백엔드는 개별 생성/조회만 |
| 자녀 삭제 | `DELETE /children/{id}` | 없음 |
| 자녀 코드 검증 | `POST /children/validate-code` | 없음(등록 시 검증) |
| FCM 토큰 삭제 | `DELETE /devices/{id}` | 없음(앱은 404 무해 처리 중) |
| 부모 일별/주별 규칙 | `/children/{id}/time-plan/*` | 불필요(부모는 월 baseTime만) — §3 참고 |

> 위 화면을 실통신 시연에 넣지 않으면 백엔드 작업 없이도 나머지는 동작.

## 2. 인증 / 회원가입 (배선 정상, 백엔드 1건만)

- 부모 `/auth/parent/login·signup`, 자녀 `/auth/children/login·signup`, refresh `/auth/token/refresh`, logout — **앱 정상**.
- 토큰 저장·주입·401 refresh 인터셉터 정상.
- **남은 백엔드 1건**: `AuthResponse`에 `memberId`(+`name`) 추가 → 로그인 후 세션 식별. (기존 핸드오프 §1.1, 토큰 claim엔 없으니 `AuthService`에서 Member 엔티티값 주입)

## 3. 자녀 시간 저장 — 분할호출 (설계 1건 확정 후 앱 작업 가능)

- 앱: 단일 `POST /time-setup` 로 `{allowedHours, weeklyTotals, dayAllocations}` 한 방 전송.
- 백엔드: `weekly-budgets` + `templates` + `routines` 3분할 + 순서검증(부모 baseTime → 주차예산 → 요일템플릿).
- 매핑: `weeklyTotals→weekly-budgets`, `dayAllocations→templates` 는 깔끔(단위/주차/요일 변환만).
- **막힌 1건(설계 결정 필요)**: `allowedHours`(폰 사용 **가능** 시간 격자) ↔ `routine`(학원 등 **고정** 일정 블록) **의미 충돌**. 이거 정하면 앱이 3분할 호출로 전환 작업 가능.

## 4. 자녀 시간 확인 / 현재 가용시간 표시 (백엔드 재배선 필요)

- `time-confirm/*`(앱) → 백엔드 없음. 대안 후보: `GET /api/v1/children/{childId}/policies` + `GET /api/v1/schedules/daily`.
- 자녀 홈의 시간 카드(현재 `01:30`/`00:30`/진행률 하드코딩)는 위 조회 엔드포인트가 정해져야 실데이터 연결 가능.

## 5. 부모 알림 → 미션 연결 (백엔드 payload 필요)

- 부모 알림 화면이 미션 상세로 갈 때 현재 **mock 미션**(`_missionMockForNotification`) + fallback ID(`mock-parent`/`GDG12-1`)를 씀.
- 실데이터 연결하려면: 알림 **payload에 `missionId`/실제 식별자** 포함 + §2의 `memberId`. → 그 후 앱이 mock 제거하고 실 미션 조회로 교체.

---

## 우선순위 (시연 기준)
- **P1(시연 필수)**: §2 `memberId` 추가, §3 allowedHours↔routine 결정.
- **P2**: §4 시간 조회 재배선, §1의 자녀등록/미션조회 권한(기존 핸드오프).
- **P3**: §1 리포트/프로필/수정삭제/디바이스삭제 — 해당 화면을 시연에서 빼면 불필요.
