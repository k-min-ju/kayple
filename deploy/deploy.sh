#!/usr/bin/env bash
# 로컬에서 실행: 빌드 → scp → 재시작
set -euo pipefail
cd "$(dirname "$0")/.."
KEY=${KEY:-$HOME/Downloads/recruit-kmj.pem}
HOST=ubuntu@kmj.recruit.kayple.com

(cd backend && ./gradlew bootJar -x test)
(cd frontend && pnpm install --frozen-lockfile && pnpm build)

scp -i "$KEY" backend/build/libs/app.jar "$HOST:/opt/kayple/app.jar"
rsync -az --delete -e "ssh -i $KEY" frontend/dist/ "$HOST:/var/www/kayple/"
ssh -i "$KEY" "$HOST" 'sudo systemctl restart kayple && sleep 1 && systemctl is-active kayple'
