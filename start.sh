#!/bin/bash
cd "$(dirname "$0")"
MYSQL_BIN="/c/Program Files/MySQL/MySQL Server 8.0/bin"

# start MySQL (port 3307) only if it isn't already running
if ! netstat -ano | grep -q ":3307.*LISTENING"; then
  echo "Starting MySQL..."
  "$MYSQL_BIN/mysqld.exe" --datadir=D:/mysql-data --port=3307 --console > mysql.log 2>&1 &
  until "$MYSQL_BIN/mysqladmin.exe" -u root -p1234 -P 3307 --protocol=tcp ping >/dev/null 2>&1; do sleep 1; done
fi

mvn spring-boot:run
