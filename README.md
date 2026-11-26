# 상담 예약 서비스

1시간 1:1 상담 예약 서비스 (상담실 1개 → 한 시간대 1건). 배포: https://kmj.recruit.kayple.com

| 화면 | 경로 |
|---|---|
| 예약(날짜·시간 → 정보 입력 → 완료) | `/` → `/reserve` → `/done` |
| 내 예약 확인·취소 | `/my` |
| 관리자 | `/admin` |

## 구조

```
backend/   Spring Boot 4.2 (JDK 25), JPA, MySQL 8  — /api/**
frontend/  React 19 + Vite + TypeScript + Tailwind v4 (pnpm)
deploy/    nginx 설정, systemd 유닛, 서버 초기화·배포 스크립트
docs/      과제 명세(assignment.md), 구현 계획
```

```
브라우저 ─HTTPS─▶ nginx(443) ─┬─ /, /admin, /my → /var/www/kayple (React 빌드)
                              └─ /api/**        → 127.0.0.1:8080 (Spring Boot, systemd)
                                                     └─ MySQL app (localhost)
```

### 중복 예약 방지
`reservation.slot_key` 에 UNIQUE 제약. 예약 중이면 `yyyy-MM-dd-HH`, 취소되면 `NULL`.
- 선조회 후 저장하면 동시 요청 두 건이 모두 통과할 수 있어 **DB 제약으로 막고** 위반을 409 로 변환
- UNIQUE 는 NULL 을 중복으로 보지 않으므로 **취소된 시간은 다시 예약 가능**, 취소 이력은 남음

### API
| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/dates` | 예약 가능 날짜(서버 KST 기준 오늘~+6일) |
| GET | `/api/slots?date=` | 시간별 예약 가능 여부(예약됨·지난 시간은 `available:false`) |
| POST | `/api/reservations` | 예약 생성. 400(입력 오류, `field` 포함) / 409(중복) |
| POST | `/api/reservations/lookup` | `{phone}` 로 조회(연락처 마스킹, URL·로그에 연락처 미노출). 지난 예약은 `cancellable:false` |
| POST | `/api/reservations/{id}/cancel` | `{phone}` 일치 + 아직 지나지 않은 예약만 취소, 그 외 404 |
| POST | `/api/admin/login`, `/api/admin/logout` | 세션 로그인 |
| GET | `/api/admin/me`, `/api/admin/reservations?date=` | 비로그인 시 401 |

## 실행

```bash
# 백엔드 테스트 (H2, MySQL 불필요)
cd backend && ./gradlew test

# 백엔드 로컬 실행 (로컬 MySQL 필요)
DB_PASSWORD=... ADMIN_PASSWORD=... ./gradlew bootRun

# 프론트 (/api 는 localhost:8080 으로 프록시)
cd frontend && pnpm install && pnpm dev
```

## 배포

```bash
# 최초 1회: 서버에 nginx·systemd 설치, /etc/kayple.env 생성(DB 비밀번호는 ~/.my.cnf 에서, 관리자 비밀번호는 랜덤 생성)
scp deploy/{nginx.conf,kayple.service,setup-server.sh} 서버:/tmp/  # 파일명은 setup-server.sh 참고
ssh 서버 'bash /tmp/setup-server.sh'

# 이후 배포: 로컬 빌드 → scp/rsync → systemctl restart
KEY=~/Downloads/recruit-kmj.pem ./deploy/deploy.sh
```
- 시크릿(DB·관리자 비밀번호)은 서버 `/etc/kayple.env`(600)에만 있고 코드·Git 에 없음
- 로컬·서버 모두 JDK 25 (systemd 에서 `/usr/lib/jvm/java-25-openjdk-amd64` 지정)

## 한 것 / 못 한 것

**한 것**
- 요구 기능 1~7 전부: 날짜·시간 선택(마감 표시), 입력 검증(클라이언트+서버), 서버 중복 차단(409 → 화면 배너), 연락처 조회·취소(확인 모달), 관리자 로그인·날짜별 목록·API 보호, 테스트, HTTPS 배포
- 시안 10개 화면
- 테스트 13개(`./gradlew test`): 지난 예약 취소 거부, 없는 API 404, KST 자정 날짜 전환 포함: 중복 409, **동시 요청 2건 중 1건만 성공**, 취소 후 재예약, 범위 밖(8일째·어제·12시·오늘 지난 시간), 연락처 불일치 취소 거부, 조회·마감 표시, 관리자 비로그인 401·잘못된 비밀번호·로그인/로그아웃
- 운영 URL 에서 curl 로 수용 기준 13개 항목 확인

**못 한 것 / 한계**
- 프론트 자동화 테스트(E2E) 없음 — 화면은 운영 URL 에서 수동·스크린샷 확인
- 로그인·예약 API 레이트 리밋 없음(관리자 비밀번호는 랜덤 16자)
- 관리자 날짜 선택은 예약 가능 7일만(과거 날짜 조회 불가)
- 배포 시 JVM 재시작 동안 약 10초 502
- 연락처만으로 조회·취소(과제 요구사항). 연락처를 아는 타인도 조회·취소 가능
- 스키마는 `ddl-auto: update` (마이그레이션 도구 없음)

## AI 가 만든 코드에서 직접 판단해 고친 부분
- **JDK 버전 불일치 제거**: AI 는 로컬 JDK 25 로 개발하고 서버 기본 JDK 21 에 맞춰 바이트코드만 21 로 내리려 했음 → 로컬·배포 환경을 같게 하라고 지시해 서버도 설치된 JDK 25 로 실행하도록 변경
- **orval 제외**: 처음엔 OpenAPI + orval 도입을 고려했으나 엔드포인트 7개에 springdoc 설정·버전 호환 리스크가 1시간 안에서 손해라 판단 → 타입을 붙인 `api.ts` 직접 작성
- **스타일링**: AI 가 순수 CSS 를 제안 → 시안을 빠르게 맞추기 위해 Tailwind v4 로 변경
- **Next.js 대신 React(Vite)**: SSR·BFF 가 필요 없는 예약/관리 화면이고 API 는 Spring 이 담당 → Node 프로세스를 늘리지 않도록 정적 빌드 + nginx
- **중복 방지 방식**: 선조회 방식은 동시 요청에서 뚫림 → UNIQUE 제약 + 취소 시 NULL 로 바꾸는 `slot_key` 설계, 동시성 테스트로 검증
- **AI 가 생성한 잘못된 코드 수정**: 연락처 포맷 함수가 의미 없는 인덱스 계산으로 생성됨 → 단순 `substring` 으로 재작성
- **시크릿 노출 사고 대응**: 서버 안내 파일을 마스킹해 읽으려던 정규식이 한국어 표기를 놓쳐 DB 비밀번호가 AI 대화에 출력됨 → 이후 시크릿은 서버 안에서만 파일로 옮기고, 제출용 대화 내보내기에서 마스킹

## 팀원에게 넘길 작업 지시 3개
1. **남용·도용 방지**: nginx `limit_req` 로 `/api/admin/login`(분당 5회)·`POST /api/reservations`·`/lookup`(IP당 분당 10회) 제한. 연락처만 알면 남의 예약을 조회·취소할 수 있으므로 예약번호(또는 SMS 인증)를 함께 확인하도록 변경. 완료 기준: 초과 시 429, 예약번호 불일치 취소 404 테스트.
2. **배포 안정화**: `ddl-auto: update` 를 Flyway `V1__init.sql`(slot_key UNIQUE 포함)로 교체하고 `validate` 로 전환. GitHub Actions 로 `./gradlew test` + `pnpm build` 를 PR 마다 실행. 완료 기준: 빈 DB 에서 마이그레이션만으로 기동, CI 녹색.
3. **프론트 회귀 테스트**: Playwright 로 예약→중복 409 배너→내 예약 조회→취소→관리자 로그인/목록 E2E 1개 시나리오 작성. springdoc + orval 로 API 타입 자동 생성 도입 검토. 완료 기준: CI 에서 E2E 통과.
