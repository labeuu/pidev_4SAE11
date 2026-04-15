-- Secours manuel si besoin (normalement appliqué au démarrage par
-- SubcontractMissionColumnsInitializer).
-- Erreur : "Column 'project_id' cannot be null" pour une mission « offre » uniquement.
--
-- mysql -h ... -P 3307 -u root -p gestion_subcontracting_db < fix-subcontracts-mission-columns-nullable.sql

ALTER TABLE subcontracts
  MODIFY COLUMN project_id BIGINT NULL;

ALTER TABLE subcontracts
  MODIFY COLUMN offer_id BIGINT NULL;
