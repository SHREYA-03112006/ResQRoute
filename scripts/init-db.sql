-- ResQ-Route database initialisation script
-- Run automatically by Docker on first container start.
-- For manual setup: psql -U resqroute -d resqroute -f scripts/init-db.sql

-- Enable PostGIS extension
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;

-- Enable pg_trgm for fuzzy road-name matching (Phase 3)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Useful check: confirm PostGIS is installed
SELECT PostGIS_version();
