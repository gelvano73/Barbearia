#!/bin/sh
set -e

# Render (e similares) fornecem postgres://user:pass@host:port/db
# Spring Boot precisa de jdbc:postgresql:// + user/password separados.
if [ -n "$DATABASE_URL" ]; then
  raw="$DATABASE_URL"
  raw=$(echo "$raw" | sed 's|^postgres://||' | sed 's|^postgresql://||' | sed 's|^jdbc:postgresql://||')
  userpass=${raw%%@*}
  hostpart=${raw#*@}
  user=${userpass%%:*}
  pass=${userpass#*:}
  hostport=${hostpart%%/*}
  dbname=${hostpart#*/}
  dbname=${dbname%%\?*}
  host=${hostport%%:*}
  port=${hostport#*:}
  if [ "$host" = "$port" ]; then
    port=5432
  fi
  export SPRING_DATASOURCE_URL="jdbc:postgresql://${host}:${port}/${dbname}"
  export SPRING_DATASOURCE_USERNAME="$user"
  export SPRING_DATASOURCE_PASSWORD="$pass"
fi

exec java -jar /app/app.jar
