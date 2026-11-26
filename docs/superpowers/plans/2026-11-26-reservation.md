# 상담 예약 서비스 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 1시간 안에 상담 예약(사용자·관리자) 서비스를 만들어 https://kmj.recruit.kayple.com 에 배포한다.

**Architecture:** 단일 서버. nginx(443)가 React 빌드 정적 파일을 서빙하고 `/api/` 를 `127.0.0.1:8080` Spring Boot jar로 프록시. Spring Boot는 로컬 MySQL `app` DB 사용. 배포 플랫폼(Vercel 등) 없음: 로컬에서 빌드 → `scp` → systemd 재시작.

**Tech Stack:** 모노레포(`backend/` Gradle, `frontend/` pnpm), React + Vite + TypeScript + Tailwind v4 + react-router (SPA), API 클라이언트는 손으로 쓴 `api.ts`(orval 미사용: 엔드포인트 7개), Spring Boot 3 (Java 21 타깃), Spring Data JPA, Bean Validation, MySQL 8, 테스트는 JUnit5 + H2(MySQL 모드) + MockMvc.

**Spec:** `docs/assignment.md`

## Global Constraints
- 시간대: 모든 날짜 계산은 `Asia/Seoul`. 예약 가능 날짜 = 오늘 ~ 오늘+6일. 시간 = `[10,11,13,14,15,16,17]`.
- 서브도메인 금지. 사용자 `/`, 관리자 `/admin`, API `/api/**`.
- 관리자 비밀번호·DB 비밀번호는 서버 `/etc/kayple.env`(root 600)에만. 코드·Git·AI 대화 출력에 남기지 않는다.
- 서버 기본 JDK 21 → Gradle `java.toolchain`/`release` 21.
- 오류 문구(시안 고정): 중복 `이미 예약된 시간입니다. 다른 시간을 선택해 주세요.` / 연락처 `연락처 형식이 올바르지 않습니다` / 로그인 `아이디 또는 비밀번호가 올바르지 않습니다` / 빈 결과 `예약 내역이 없습니다`.
- 오류 응답 형식: `{ "message": string, "field"?: string }`.

## Review Focus
1. **취소 후 같은 시간 재예약** — 취소된 행이 유니크 제약을 막으면 안 된다 → Task 2 테스트 `cancelFreesSlot`.
2. **동시 요청 두 건이 같은 슬롯** — 앱 레벨 선조회만으로는 둘 다 통과 → DB 유니크 제약 + 409 변환, Task 2 테스트 `concurrentBookingOnlyOneSucceeds`.
3. **범위 밖 입력(8일 뒤, 12시, 오늘 이미 지난 시간)** — 400 → Task 2 테스트 `rejectsOutOfRangeSlot`.
4. **남의 예약 취소** — id만 알면 취소 가능하면 안 됨. 연락처 일치 시에만 → Task 3 테스트 `cancelRequiresMatchingPhone`.
5. **비로그인 관리자 API** — 401, 데이터 미노출 → Task 4 테스트 `adminApiRequiresLogin`.

---

### Task 0: 배포 경로 먼저 뚫기 (위험 최우선)
**Files:** Create `deploy/nginx.conf`, `deploy/kayple.service`, `deploy/deploy.sh`
- [ ] nginx server 블록: 80→443 리다이렉트, 443 ssl(인증서 경로 위), `root /var/www/kayple; try_files $uri /index.html;`, `location /api/ { proxy_pass http://127.0.0.1:8080; }`
- [ ] systemd: `EnvironmentFile=/etc/kayple.env`, `ExecStart=/usr/bin/java -jar /opt/kayple/app.jar`
- [ ] `/etc/kayple.env` 생성은 서버에서 직접: `DB_PASSWORD`는 `~/README-server.txt`에서 읽어 넣고, `ADMIN_PASSWORD`는 `openssl rand` 생성. 화면에 출력하지 않는다(사용자가 나중에 `sudo cat`으로 확인).
- [ ] `deploy.sh`: `./gradlew bootJar` + `npm run build` → scp → `systemctl restart kayple`
- [ ] 검증: 빈 index.html 배포 후 `curl -sI https://kmj.recruit.kayple.com` → 200

### Task 1: 백엔드 골격 + 슬롯 조회
**Files:** `backend/` (Spring Initializr: web, data-jpa, validation, mysql, h2(test)), `reservation/Reservation.java`, `ReservationRepository.java`, `SlotPolicy.java`, `ReservationController.java`, `application.yml`
**Interfaces (Produces):**
- Entity `Reservation{ Long id; LocalDate date; int hour; String name; String phone(숫자만); Status status(BOOKED|CANCELLED); String slotKey (BOOKED일 때 "yyyy-MM-dd-HH", 취소 시 null, unique); LocalDateTime createdAt }`
- `SlotPolicy.validate(LocalDate, int)` — 범위 밖이면 `InvalidInputException`
- `GET /api/slots?date=` → `[{hour:int, available:boolean}]`
- [ ] 테스트 `slotsMarkBookedAsUnavailable` → 실패 확인 → 구현 → 통과
- [ ] `application.yml`: datasource는 `${DB_URL:jdbc:mysql://localhost:3306/app}`, `${DB_PASSWORD}`; `ddl-auto: update`; `server.address: 127.0.0.1`

### Task 2: 예약 생성 + 중복 방지 (핵심, TDD)
**Files:** `ReservationService.java`, `ApiExceptionHandler.java`, Test `ReservationServiceTest.java`
**Interfaces:** `POST /api/reservations {date,hour,name,phone}` → 201 `{id,date,hour,name,phoneMasked}` / 400 / 409
- [ ] 테스트 먼저: `duplicateBookingRejected`(두 번째 409·CONFLICT 예외), `cancelFreesSlot`, `concurrentBookingOnlyOneSucceeds`(스레드 2개, 성공 1), `rejectsOutOfRangeSlot`(D+7, 12시, 오늘 지난 시간), `invalidPhoneRejected`(`010-1234`)
- [ ] 실패 확인 → 구현: 선조회 없이 save → `DataIntegrityViolationException` → 409. 연락처 `^01[016789]-?\d{3,4}-?\d{4}$`, 이름 1~20자 trim.
- [ ] 통과 확인 → 커밋

### Task 3: 내 예약 조회·취소
**Interfaces:** `GET /api/reservations?phone=` → `[{id,date,hour,name,status}]` (날짜·시간 내림차순); `POST /api/reservations/{id}/cancel {phone}` → 204 / 404(불일치 포함)
- [ ] 테스트 `cancelRequiresMatchingPhone`, `lookupReturnsBothStatuses` → 구현 → 통과

### Task 4: 관리자 인증 + 목록
**Files:** `admin/AdminController.java`, `admin/AdminAuthInterceptor.java`, Test `AdminApiTest.java`(MockMvc)
**Interfaces:** `POST /api/admin/login {username,password}` → 204 + 세션 / 401; `POST /api/admin/logout`; `GET /api/admin/reservations?date=` → `[{id,hour,name,phone,status,createdAt}]`
- 계정: `ADMIN_USERNAME`(기본 `admin`), `ADMIN_PASSWORD`(필수, 없으면 로그인 항상 실패). 비교는 `MessageDigest.isEqual`. HttpSession + 인터셉터(`/api/admin/**`, login 제외) → 401.
- [ ] 테스트 `adminApiRequiresLogin`(401), `loginThenList`(200), `wrongPasswordRejected`(401) → 구현 → 통과

### Task 5: 프론트 (시안 1→2→2-1→3, 4/4-1/4-2, 5/5-1/5-2)
**Files:** `frontend/` (pnpm, Vite React-TS, Tailwind v4), `src/api.js`, `src/pages/{Booking,BookingForm,BookingDone,MyReservations,AdminLogin,AdminList}.jsx`, `src/styles.css`
- 라우트: `/`, `/reserve`, `/done`, `/my`, `/admin`(세션 없으면 로그인 화면). dev 프록시 `/api → 8080`.
- 공용 포맷: `11월 18일 (수) 14:00`. 오류 응답 `message`를 상단 배너/필드 아래 표시.
- [ ] 검증: 로컬 백엔드 + `npm run dev`로 시나리오 수동 확인 (예약→중복 오류→조회→취소→관리자)

### Task 6: 리뷰·검증·배포·제출물
- [ ] `fullstack-review` 스킬로 변경 리뷰 → 반영 → 커밋
- [ ] `docs/assignment.md` 수용 기준 1~7을 운영 URL에서 하나씩 재검증
- [ ] `deploy.sh` 실행 → 운영 URL에서 Task 5 시나리오 재확인
- [ ] `README.md`: 실행·배포 방법 / 한 것·못 한 것 / AI 코드에서 직접 고친 부분 / 팀원 작업 지시 3개
- [ ] push (저장소는 이미 Public). 작업 단위마다 커밋·푸시
- [ ] AI 대화 내보내기: `~/.claude/projects/-Volumes-development-kayple/*.jsonl` → `docs/ai-conversation.md`(user/assistant 텍스트만, 비밀번호 마스킹 확인) → 제출 양식에 붙여넣기
