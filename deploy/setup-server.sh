#!/usr/bin/env bash
# 서버에서 1회 실행: nginx/systemd 설치, 시크릿 env 파일 생성(값은 출력하지 않음)
set -euo pipefail
sudo mkdir -p /var/www/kayple /opt/kayple
sudo chown ubuntu:ubuntu /var/www/kayple /opt/kayple

if [ ! -f /etc/kayple.env ]; then
  DB_PW=$(awk -F= '/^[[:space:]]*password/{gsub(/[[:space:]"]/,"",$2); print $2; exit}' ~/.my.cnf)
  [ -n "$DB_PW" ] || { echo "DB password not found in ~/.my.cnf" >&2; exit 1; }
  ADMIN_PW=$(openssl rand -base64 12 | tr -d '/+=')
  sudo install -m 600 -o ubuntu -g ubuntu /dev/null /etc/kayple.env
  printf 'DB_URL=jdbc:mysql://localhost:3306/app?serverTimezone=Asia/Seoul\nDB_USERNAME=app\nDB_PASSWORD=%s\nADMIN_USERNAME=admin\nADMIN_PASSWORD=%s\n' "$DB_PW" "$ADMIN_PW" > /etc/kayple.env
  echo "created /etc/kayple.env (view admin password: cat /etc/kayple.env)"
fi

sudo cp /tmp/kayple-nginx.conf /etc/nginx/sites-available/kayple
sudo ln -sf /etc/nginx/sites-available/kayple /etc/nginx/sites-enabled/kayple
sudo rm -f /etc/nginx/sites-enabled/default   # 파일은 sites-available 에 그대로 둠
sudo nginx -t
sudo systemctl reload nginx

sudo cp /tmp/kayple.service /etc/systemd/system/kayple.service
sudo systemctl daemon-reload
sudo systemctl enable kayple
