#!/bin/sh
# Backup automático: PostgreSQL (custom format) + uploads
set -eu

TS=$(date +%Y%m%d_%H%M%S)
DEST="${BACKUP_DIR:-/backups}/${TS}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"

mkdir -p "$DEST"

echo "[backup] Iniciando ${TS}"

pg_dump \
  -h "${PGHOST:-postgres}" \
  -p "${PGPORT:-5432}" \
  -U "${PGUSER:-barbearia}" \
  -d "${PGDATABASE:-barbearia_saas}" \
  -Fc \
  -f "${DEST}/db.dump"

if [ -d "${UPLOADS_DIR:-/uploads}" ]; then
  tar -czf "${DEST}/uploads.tar.gz" -C "${UPLOADS_DIR:-/uploads}" . 2>/dev/null || \
    tar -czf "${DEST}/uploads.tar.gz" -C "${UPLOADS_DIR:-/uploads}" --files-from /dev/null
fi

echo "${TS}" > "${DEST}/OK"

# Retenção: remove pastas mais antigas que RETENTION_DAYS
find "${BACKUP_DIR:-/backups}" -mindepth 1 -maxdepth 1 -type d -mtime "+${RETENTION_DAYS}" -exec rm -rf {} + 2>/dev/null || true

echo "[backup] Concluído: ${DEST}"
ls -la "$DEST"
