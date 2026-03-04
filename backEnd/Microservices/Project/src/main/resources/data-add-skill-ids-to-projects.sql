-- Add skill IDs to existing OPEN projects so recommendations work
--
-- OPTION A: Same MySQL server (projectdb + portfolio_db both exist)
-- Assigns first 5 skills from portfolio to each OPEN project (one skill per project for variety)
/*
INSERT INTO project_skill_ids (project_id, skill_id)
SELECT p.id, s.id FROM project p
JOIN portfolio_db.skills s
WHERE p.status = 'OPEN'
AND s.id IN (SELECT id FROM portfolio_db.skills ORDER BY id LIMIT 20)
LIMIT 200;
*/

-- OPTION B: Manual - run these queries first to get IDs, then build INSERTs
-- 1. On portfolio_db: SELECT id, name FROM skills ORDER BY id;
-- 2. On projectdb:  SELECT id, title FROM project WHERE status = 'OPEN';
-- 3. Insert rows (project_id, skill_id). Use skill IDs that match freelancer skills:
--    e.g. JavaScript, Angular, React, Java, Node.js, Python, etc.

INSERT IGNORE INTO project_skill_ids (project_id, skill_id)
SELECT id, 1 FROM project WHERE status = 'OPEN' LIMIT 10;
INSERT IGNORE INTO project_skill_ids (project_id, skill_id)
SELECT id, 2 FROM project WHERE status = 'OPEN' LIMIT 10;
INSERT IGNORE INTO project_skill_ids (project_id, skill_id)
SELECT id, 3 FROM project WHERE status = 'OPEN' LIMIT 10;
