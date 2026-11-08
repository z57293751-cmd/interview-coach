#!/bin/bash
# AI Interview Coach - one-shot server setup (Ubuntu 20.04 / 22.04)
# ASCII only on purpose: avoids any charset issue on the remote console.
#
# Usage:  sudo bash setup.sh
# Expects in the same folder: interview-coach-0.1.0.jar, schema.sql, seed.sql, mock_schema.sql

set -e

APP_DIR=/opt/coach
ENV_FILE=/etc/coach.env
SERVICE=/etc/systemd/system/coach.service
HERE="$(cd "$(dirname "$0")" && pwd)"

echo ""
echo "=========================================="
echo "  AI Interview Coach - setup"
echo "=========================================="
echo ""

# ---------- 1. packages ----------
echo "[1/6] Installing JDK 17 and MySQL (1-3 min) ..."
export DEBIAN_FRONTEND=noninteractive
apt-get update -qq
apt-get install -y -qq openjdk-17-jdk mysql-server >/dev/null 2>&1
java -version 2>&1 | head -1

# ---------- 2. mysql tuning ----------
echo "[2/6] Tuning MySQL for small RAM ..."
cat > /etc/mysql/mysql.conf.d/zz-coach.cnf <<'CNF'
[mysqld]
innodb_buffer_pool_size = 128M
performance_schema = OFF
max_connections = 50
CNF
systemctl enable mysql >/dev/null 2>&1
systemctl restart mysql
sleep 4

# ---------- 3. database ----------
echo "[3/6] Creating database and user ..."
DB_PASS="coach$(head -c 16 /dev/urandom | md5sum | head -c 10)"
mysql -uroot <<SQL
CREATE DATABASE IF NOT EXISTS coach DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS 'coach'@'localhost' IDENTIFIED BY '$DB_PASS';
ALTER USER 'coach'@'localhost' IDENTIFIED BY '$DB_PASS';
GRANT ALL PRIVILEGES ON coach.* TO 'coach'@'localhost';
FLUSH PRIVILEGES;
SQL

# ---------- 4. schema + seed ----------
echo "[4/6] Loading schema and questions ..."
for f in schema.sql mock_schema.sql seed.sql; do
    if [ -f "$HERE/$f" ]; then
        mysql -uroot coach < "$HERE/$f"
        echo "      loaded $f"
    else
        echo "      [skip] $f not found"
    fi
done
Q_COUNT=$(mysql -uroot -N -B -e "SELECT COUNT(*) FROM coach.question;")
echo "      questions in db: $Q_COUNT"

# ---------- 5. env ----------
echo "[5/6] Configuration"
echo ""
printf "      Paste DEEPSEEK API KEY (input hidden): "
read -rs DS_KEY
echo ""
printf "      Set access password [press Enter for ljt666]: "
read -r ACC_PASS
ACC_PASS=${ACC_PASS:-ljt666}
echo ""

if [ -z "$DS_KEY" ]; then
    echo "      ERROR: API key is empty"
    exit 1
fi

cat > "$ENV_FILE" <<EOF
DEEPSEEK_API_KEY=$DS_KEY
DEEPSEEK_MODEL=deepseek-flash
DEEPSEEK_TIMEOUT_SECONDS=60
ACCESS_PASSWORD=$ACC_PASS
SPRING_DATASOURCE_USERNAME=coach
SPRING_DATASOURCE_PASSWORD=$DB_PASS
SERVER_PORT=8080
EOF
chmod 600 "$ENV_FILE"

# ---------- 6. service ----------
echo "[6/6] Registering service ..."
id -u coach >/dev/null 2>&1 || useradd -r -s /bin/false coach
mkdir -p "$APP_DIR"
cp "$HERE/interview-coach-0.1.0.jar" "$APP_DIR/app.jar"
chown -R coach:coach "$APP_DIR"

cat > "$SERVICE" <<'UNIT'
[Unit]
Description=AI Interview Coach
After=network.target mysql.service
Wants=mysql.service

[Service]
Type=simple
User=coach
EnvironmentFile=/etc/coach.env
ExecStart=/usr/bin/java -Xmx320m -XX:+UseSerialGC -jar /opt/coach/app.jar
Restart=always
RestartSec=5
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
UNIT

systemctl daemon-reload
systemctl enable coach >/dev/null 2>&1
systemctl restart coach
sleep 10

echo ""
if systemctl is-active --quiet coach; then
    IP=$(curl -s --max-time 5 ifconfig.me || echo "YOUR_SERVER_IP")
    echo "=========================================="
    echo "  DEPLOY OK"
    echo "=========================================="
    echo ""
    echo "  URL      : http://$IP:8080/"
    echo "  PASSWORD : $ACC_PASS"
    echo ""
    echo "  systemctl status coach     - status"
    echo "  journalctl -u coach -f     - live logs"
    echo "  systemctl restart coach    - restart"
    echo ""
else
    echo "=========================================="
    echo "  FAILED - last 40 log lines:"
    echo "=========================================="
    journalctl -u coach -n 40 --no-pager
fi
