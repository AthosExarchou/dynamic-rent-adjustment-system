#!/bin/bash
# =============================================================================
# PostgreSQL Database Backup Script for DRAS
# =============================================================================
# Run this on the Oracle VM host to take a backup of the production database.
# Ensure the backup directory exists first: sudo mkdir -p /opt/dras/backups
#
# Usage:
#   ./backup-db.sh
# =============================================================================

BACKUP_DIR="/opt/dras/backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/dras_db_backup_${TIMESTAMP}.sql"
CONTAINER_NAME="dras-db-1"

if [ ! -d "$BACKUP_DIR" ]; then
    echo "Creating backup directory at $BACKUP_DIR..."
    sudo mkdir -p "$BACKUP_DIR"
    sudo chown $USER:$USER "$BACKUP_DIR"
fi

# Load variables from .env if present (fallback to defaults)
if [ -f "/var/lib/jenkins/workspace/dras/.env" ]; then
    export $(grep -v '^#' /var/lib/jenkins/workspace/dras/.env | xargs)
fi

DB_USER=${DATABASE_USERNAME:-dras}
DB_NAME=${DATABASE_NAME:-dras_db}

echo "Starting backup of database '${DB_NAME}' from container '${CONTAINER_NAME}'..."

# Run pg_dump and redirect output to the host file system directly
docker exec -e PGPASSWORD="${DATABASE_PASSWORD}" "$CONTAINER_NAME" pg_dump -U "$DB_USER" -d "$DB_NAME" > "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    echo "Backup successful! Saved to: $BACKUP_FILE"
    
    # Keep only the last 7 backups to save space
    ls -tp "${BACKUP_DIR}"/dras_db_backup_*.sql | grep -v '/$' | tail -n +8 | xargs -I {} rm -- {} 2>/dev/null
else
    echo "Error: pg_dump failed. Check container name and database settings."
    rm -f "$BACKUP_FILE"
    exit 1
fi
