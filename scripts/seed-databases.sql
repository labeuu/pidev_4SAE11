-- =============================================================================
-- Smart Freelance - Global MySQL seed script
-- Run: mysql -u root -p < scripts/seed-databases.sql
-- =============================================================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- Row target guide (all <= 30 rows/table):
-- userdb.users: 5
-- portfolio_db.skills: 6 | skill_domains: 11 | experiences: 6 | evaluation_tests: 6 | evaluations: 9
-- projectdb.project: 8 | project_skill_ids: 17 | project_application: 10
-- gestion_offre_db.offers: 6 | offer_applications: 9
-- gestion_contract_db.contracts: 7 | conflicts: 6
-- reviewdb.reviews: 10 | review_responses: 8
-- planningdb.progress_update: 8 | progress_comment: 12
-- taskdb.task: 12 | subtask: 12 | task_comment: 15
-- freelancia_job_db.jobs: 8 | job_required_skills: 15 | job_applications: 9 | application_attachments: 9
-- meetingdb.meetings: 8
-- ticketdb.tickets: 8 | ticket_replies: 12
-- gestion_vendor_db.vendor_approvals: 5 | vendor_approval_audits: 8 | freelancer_match_profiles: 3 | match_recommendations: 8
-- gestion_subcontracting_db.subcontracts: 5 | subcontract_deliverables: 10 | subcontract_audits: 10
-- gamificationdb.achievement: 8 | user_level: 3 | user_achievement: 10
-- chatdb.chat_messages: 20

-- -----------------------------------------------------------------------------
-- 1) USER DB (userdb)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS userdb;
USE userdb;

-- Required users: password = 15961596 (BCrypt)
INSERT INTO users (id, email, password_hash, first_name, last_name, role, phone, avatar_url, is_active, created_at, updated_at) VALUES
(1, 'hanen@hotmail.com', '$2b$10$8XLQGMLPa/QeqxDSD8i5luP3q.QZeoG6zEApSglhf1rq23XxSIEuu', 'Hanen', 'Freelancer', 'FREELANCER', '+21620100101', NULL, 1, NOW(), NOW()),
(2, 'omar@hotmail.com',  '$2b$10$8XLQGMLPa/QeqxDSD8i5luP3q.QZeoG6zEApSglhf1rq23XxSIEuu', 'Omar',  'Client',     'CLIENT',     '+21620100102', NULL, 1, NOW(), NOW()),
(3, 'ridha@hotmail.com', '$2b$10$8XLQGMLPa/QeqxDSD8i5luP3q.QZeoG6zEApSglhf1rq23XxSIEuu', 'Ridha', 'Admin',      'ADMIN',      '+21620100103', NULL, 1, NOW(), NOW()),
(4, 'demo.user1@example.com', '$2b$10$8XLQGMLPa/QeqxDSD8i5luP3q.QZeoG6zEApSglhf1rq23XxSIEuu', 'Demo', 'One', 'FREELANCER', '+21620100104', NULL, 1, NOW(), NOW()),
(5, 'demo.user2@example.com', '$2b$10$8XLQGMLPa/QeqxDSD8i5luP3q.QZeoG6zEApSglhf1rq23XxSIEuu', 'Demo', 'Two', 'CLIENT', '+21620100105', NULL, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
email = VALUES(email),
password_hash = VALUES(password_hash),
first_name = VALUES(first_name),
last_name = VALUES(last_name),
role = VALUES(role),
phone = VALUES(phone),
is_active = VALUES(is_active),
updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 2) PORTFOLIO DB (portfolio_db)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS portfolio_db;
USE portfolio_db;

INSERT INTO skills (id, name, description, user_id, experience_id, created_at, updated_at) VALUES
(1, 'Java', 'Spring Boot microservices', 1, NULL, NOW(), NOW()),
(2, 'Angular', 'Angular dashboards and SPA', 1, NULL, NOW(), NOW()),
(3, 'DevOps', 'Docker and CI/CD setup', 1, NULL, NOW(), NOW()),
(4, 'System Design', 'Distributed architecture and scalability', 1, NULL, NOW(), NOW()),
(5, 'Project Governance', 'Roadmapping, reporting and risk tracking', 2, NULL, NOW(), NOW()),
(6, 'Quality Assurance', 'Acceptance criteria validation and UAT', 3, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE
name = VALUES(name),
description = VALUES(description),
user_id = VALUES(user_id),
experience_id = VALUES(experience_id),
updated_at = NOW();

INSERT INTO skill_domains (skill_id, domain) VALUES
(1, 'WEB_DEVELOPMENT'),
(1, 'DATABASE_ADMINISTRATION'),
(2, 'WEB_DEVELOPMENT'),
(2, 'UI_UX_DESIGN'),
(3, 'DEVOPS'),
(3, 'CLOUD_COMPUTING'),
(4, 'SOFTWARE_ARCHITECTURE'),
(4, 'CLOUD_COMPUTING'),
(5, 'PROJECT_MANAGEMENT'),
(5, 'BUSINESS_ANALYSIS'),
(6, 'QUALITY_ASSURANCE')
ON DUPLICATE KEY UPDATE domain = VALUES(domain);

INSERT INTO experiences (id, user_id, title, type, description, start_date, end_date, company_or_client_name) VALUES
(1, 1, 'Freelance Backend Missions', 'JOB', 'Delivered backend APIs for multiple clients.', '2022-01-01', NULL, 'Independent'),
(2, 1, 'Project Delivery for Omar', 'PROJECT', 'Implemented platform module end to end.', '2024-02-01', '2024-05-01', 'Omar Client'),
(3, 1, 'Performance Rescue Sprint', 'PROJECT', 'Reduced latency on critical APIs by 45 percent.', '2024-06-01', '2024-07-15', 'Smart Ops Team'),
(4, 2, 'Digital Product Owner', 'JOB', 'Managed backlog and release cadence across teams.', '2021-03-01', NULL, 'Client Operations'),
(5, 2, 'Marketplace Modernization', 'PROJECT', 'Coordinated migration to multi-service architecture.', '2023-10-01', '2024-01-20', 'Internal Program'),
(6, 3, 'Platform Governance Lead', 'JOB', 'Audited delivery quality, compliance and SLA adherence.', '2020-05-10', NULL, 'Smart Freelance Admin')
ON DUPLICATE KEY UPDATE title = VALUES(title), type = VALUES(type), description = VALUES(description), company_or_client_name = VALUES(company_or_client_name);

INSERT INTO evaluation_tests (id, skill_id, title, passing_score, duration_minutes, created_at, updated_at) VALUES
(1, 1, 'Java Core Assessment', 70.0, 45, NOW(), NOW()),
(2, 2, 'Angular Frontend Assessment', 70.0, 45, NOW(), NOW()),
(3, 3, 'DevOps Automation Assessment', 75.0, 50, NOW(), NOW()),
(4, 4, 'System Design Foundations', 72.0, 60, NOW(), NOW()),
(5, 5, 'Project Governance Essentials', 70.0, 40, NOW(), NOW()),
(6, 6, 'QA Process and Acceptance', 68.0, 35, NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), passing_score = VALUES(passing_score), duration_minutes = VALUES(duration_minutes), updated_at = NOW();

INSERT INTO evaluations (id, freelancer_id, skill_id, score, passed, test_result, evaluated_at) VALUES
(1, 1, 1, 92.5, 1, 'Excellent backend skills', NOW()),
(2, 1, 2, 88.0, 1, 'Strong frontend delivery', NOW()),
(3, 1, 3, 90.0, 1, 'Reliable CI/CD practices', NOW()),
(4, 1, 4, 87.5, 1, 'Good architecture trade-off analysis', NOW()),
(5, 2, 5, 84.0, 1, 'Strong planning and roadmap definition', NOW()),
(6, 3, 6, 89.0, 1, 'Rigorous QA and review standards', NOW()),
(7, 1, 6, 78.0, 1, 'Good test coverage discipline', NOW()),
(8, 2, 4, 74.5, 1, 'Client understands architecture constraints', NOW()),
(9, 3, 5, 81.0, 1, 'Admin process governance validated', NOW())
ON DUPLICATE KEY UPDATE score = VALUES(score), passed = VALUES(passed), test_result = VALUES(test_result), evaluated_at = VALUES(evaluated_at);

-- -----------------------------------------------------------------------------
-- 3) PROJECT DB (projectdb)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS projectdb;
USE projectdb;

INSERT INTO project (id, client_id, title, description, budget, deadline, status, category, created_at, updated_at) VALUES
(1, 2, 'Smart Freelance MVP Upgrade', 'Extend platform modules and improve performance.', 8000.00, DATE_ADD(NOW(), INTERVAL 45 DAY), 'OPEN', 'Web Platform', NOW(), NOW()),
(2, 2, 'Admin Analytics Dashboard', 'Build operational dashboard for admin insights.', 3500.00, DATE_ADD(NOW(), INTERVAL 30 DAY), 'IN_PROGRESS', 'Analytics', NOW(), NOW()),
(3, 2, 'API Stabilization Sprint', 'Fix bugs and add API monitoring.', 2500.00, DATE_ADD(NOW(), INTERVAL 20 DAY), 'OPEN', 'Backend', NOW(), NOW()),
(4, 2, 'Client Workspace Revamp', 'Refactor client dashboard flows and data widgets.', 4200.00, DATE_ADD(NOW(), INTERVAL 35 DAY), 'IN_PROGRESS', 'Frontend', NOW(), NOW()),
(5, 2, 'Messaging Reliability Upgrade', 'Improve chat retries and delivery tracking.', 2800.00, DATE_ADD(NOW(), INTERVAL 28 DAY), 'OPEN', 'Communication', NOW(), NOW()),
(6, 2, 'Helpdesk Automation Rollout', 'Automate ticket classification and SLA tagging.', 3100.00, DATE_ADD(NOW(), INTERVAL 40 DAY), 'OPEN', 'Support', NOW(), NOW()),
(7, 2, 'Freelancer Discovery Engine', 'Enhance matching and ranking relevance.', 5200.00, DATE_ADD(NOW(), INTERVAL 50 DAY), 'IN_PROGRESS', 'Marketplace', NOW(), NOW()),
(8, 2, 'Legacy Cleanup Sprint', 'Retire old endpoints and complete migration checks.', 1900.00, DATE_SUB(NOW(), INTERVAL 10 DAY), 'COMPLETED', 'Maintenance', NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), description = VALUES(description), budget = VALUES(budget), deadline = VALUES(deadline), status = VALUES(status), updated_at = NOW();

INSERT INTO project_skill_ids (project_id, skill_id) VALUES
(1, 1), (1, 2), (1, 3),
(2, 2), (2, 3),
(3, 1), (3, 3),
(4, 2), (4, 4),
(5, 1), (5, 3),
(6, 1), (6, 5),
(7, 4), (7, 5), (7, 6),
(8, 1), (8, 6)
ON DUPLICATE KEY UPDATE skill_id = VALUES(skill_id);

INSERT INTO project_application (id, freelance_id, cover_letter, proposed_price, proposed_duration, status, applied_at, responded_at, project_id) VALUES
(1, 1, 'I can deliver this with a clean architecture and tested APIs.', 7600.00, 40, 'ACCEPTED', DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), 1),
(2, 1, 'Dashboard delivery in two iterations with weekly demos.', 3300.00, 28, 'ACCEPTED', DATE_SUB(NOW(), INTERVAL 5 DAY), NOW(), 2),
(3, 1, 'Ready to start immediately and improve reliability fast.', 2400.00, 18, 'PENDING', NOW(), NULL, 3),
(4, 1, 'I can optimize client portal UX with reusable Angular modules.', 4000.00, 30, 'ACCEPTED', DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY), 4),
(5, 1, 'I will redesign message delivery with clear observability.', 2600.00, 24, 'PENDING', DATE_SUB(NOW(), INTERVAL 7 DAY), NULL, 5),
(6, 1, 'Ticket automation can be delivered with staged rollout.', 2950.00, 34, 'PENDING', DATE_SUB(NOW(), INTERVAL 3 DAY), NULL, 6),
(7, 1, 'Matching relevance and ranking logic are my core strengths.', 5000.00, 45, 'ACCEPTED', DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 17 DAY), 7),
(8, 1, 'I can finish legacy cleanup without downtime.', 1800.00, 14, 'ACCEPTED', DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 25 DAY), 8),
(9, 3, 'Admin-supported execution with compliance focus.', 5100.00, 44, 'REJECTED', DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 13 DAY), 7),
(10, 3, 'Can provide quality gate and final validation support.', 2100.00, 20, 'PENDING', DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, 6)
ON DUPLICATE KEY UPDATE cover_letter = VALUES(cover_letter), proposed_price = VALUES(proposed_price), proposed_duration = VALUES(proposed_duration), status = VALUES(status), responded_at = VALUES(responded_at);

-- -----------------------------------------------------------------------------
-- 4) OFFER DB (gestion_offre_db)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS gestion_offre_db;
USE gestion_offre_db;

INSERT INTO offers (id, freelancer_id, title, domain, description, price, duration_type, offer_status, deadline, category, rating, communication_score, tags, views_count, is_featured, is_active, created_at, updated_at, published_at, expired_at) VALUES
(1, 1, 'Backend API Development', 'Backend', 'Design and implement scalable APIs.', 65.00, 'hourly', 'AVAILABLE', DATE_ADD(CURDATE(), INTERVAL 90 DAY), 'Development', 4.9, 4.8, 'java,spring,mysql', 120, 1, 1, NOW(), NOW(), NOW(), NULL),
(2, 1, 'Fullstack Feature Delivery', 'Fullstack', 'End-to-end feature delivery from UI to DB.', 3200.00, 'fixed', 'AVAILABLE', DATE_ADD(CURDATE(), INTERVAL 60 DAY), 'Web', 4.8, 4.9, 'angular,java,api', 90, 0, 1, NOW(), NOW(), NOW(), NULL),
(3, 1, 'DevOps Setup and Release Automation', 'DevOps', 'CI/CD pipelines, observability and deployments.', 2800.00, 'fixed', 'AVAILABLE', DATE_ADD(CURDATE(), INTERVAL 75 DAY), 'Infrastructure', 4.7, 4.8, 'docker,ci,monitoring', 71, 0, 1, NOW(), NOW(), NOW(), NULL),
(4, 1, 'Performance Tuning Package', 'Backend', 'Profiling and optimization of critical API paths.', 1800.00, 'fixed', 'AVAILABLE', DATE_ADD(CURDATE(), INTERVAL 40 DAY), 'Optimization', 4.9, 4.9, 'profiling,jvm,mysql', 110, 1, 1, NOW(), NOW(), NOW(), NULL),
(5, 1, 'Technical Audit and Remediation', 'Architecture', 'Architecture review with actionable fixes.', 1400.00, 'fixed', 'AVAILABLE', DATE_ADD(CURDATE(), INTERVAL 45 DAY), 'Consulting', 4.8, 4.7, 'audit,architecture,security', 57, 0, 1, NOW(), NOW(), NOW(), NULL),
(6, 1, 'Mentored Delivery Sprint', 'Fullstack', 'Feature delivery with weekly demos and mentoring.', 2400.00, 'fixed', 'AVAILABLE', DATE_ADD(CURDATE(), INTERVAL 55 DAY), 'Delivery', 4.8, 4.9, 'mentoring,delivery,quality', 64, 0, 1, NOW(), NOW(), NOW(), NULL)
ON DUPLICATE KEY UPDATE title = VALUES(title), description = VALUES(description), price = VALUES(price), offer_status = VALUES(offer_status), updated_at = NOW();

INSERT INTO offer_applications (id, offer_id, client_id, message, proposed_budget, portfolio_url, attachment_url, estimated_duration, status, is_read, applied_at, responded_at, accepted_at) VALUES
(1, 1, 2, 'We need backend ownership for the next release cycle.', 5200.00, 'https://portfolio.local/omar', NULL, 60, 'PENDING', 0, NOW(), NULL, NULL),
(2, 2, 2, 'Scope fits our roadmap. Let us discuss milestones.', 3000.00, NULL, NULL, 45, 'ACCEPTED', 1, DATE_SUB(NOW(), INTERVAL 4 DAY), NOW(), NOW()),
(3, 3, 2, 'Need CI/CD plus release monitoring for Q2.', 2700.00, NULL, NULL, 40, 'ACCEPTED', 1, DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY)),
(4, 4, 2, 'Performance tuning is urgent for the next launch.', 1750.00, NULL, NULL, 20, 'ACCEPTED', 1, DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
(5, 5, 2, 'Requesting architecture audit and mitigation report.', 1300.00, NULL, NULL, 18, 'PENDING', 1, DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, NULL),
(6, 6, 2, 'Mentored sprint requested for junior internal team.', 2300.00, NULL, NULL, 30, 'PENDING', 0, NOW(), NULL, NULL),
(7, 1, 2, 'Follow-up engagement for support window extension.', 2100.00, NULL, NULL, 21, 'REJECTED', 1, DATE_SUB(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 22 DAY), NULL),
(8, 2, 2, 'Second fullstack stream requested for admin portal.', 3150.00, NULL, NULL, 42, 'ACCEPTED', 1, DATE_SUB(NOW(), INTERVAL 16 DAY), DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_SUB(NOW(), INTERVAL 13 DAY)),
(9, 3, 2, 'Maintenance agreement for release pipeline.', 2400.00, NULL, NULL, 35, 'PENDING', 0, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, NULL)
ON DUPLICATE KEY UPDATE message = VALUES(message), proposed_budget = VALUES(proposed_budget), estimated_duration = VALUES(estimated_duration), status = VALUES(status), is_read = VALUES(is_read), responded_at = VALUES(responded_at), accepted_at = VALUES(accepted_at);

-- -----------------------------------------------------------------------------
-- 5) CONTRACT DB (gestion_contract_db)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS gestion_contract_db;
USE gestion_contract_db;

INSERT INTO contracts (id, client_id, freelancer_id, project_application_id, offer_application_id, title, description, terms, amount, start_date, end_date, status, signed_at, created_at) VALUES
(1, 2, 1, 1, NULL, 'MVP Upgrade Contract', 'Execution of project MVP upgrade scope.', 'Milestones every 2 weeks.', 7600.00, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 45 DAY), 'ACTIVE', NOW(), NOW()),
(2, 2, 1, NULL, 2, 'Feature Delivery Contract', 'Delivery of fullstack feature package.', 'One final handover and docs.', 3000.00, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY), 'PENDING_SIGNATURE', NULL, NOW()),
(3, 2, 1, 4, NULL, 'Client Workspace Revamp Contract', 'Revamp workflow and implement reusable UI modules.', 'Bi-weekly demos and QA checkpoints.', 4000.00, DATE_SUB(CURDATE(), INTERVAL 8 DAY), DATE_ADD(CURDATE(), INTERVAL 22 DAY), 'ACTIVE', DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),
(4, 2, 1, 7, NULL, 'Discovery Engine Contract', 'Build ranking improvements and matching refinements.', 'Delivery in three iterative milestones.', 5000.00, DATE_SUB(CURDATE(), INTERVAL 12 DAY), DATE_ADD(CURDATE(), INTERVAL 33 DAY), 'ACTIVE', DATE_SUB(NOW(), INTERVAL 12 DAY), NOW()),
(5, 2, 1, 8, NULL, 'Legacy Cleanup Contract', 'Retire deprecated endpoints and finish migration.', 'Fixed scope with final checklist sign-off.', 1800.00, DATE_SUB(CURDATE(), INTERVAL 30 DAY), DATE_SUB(CURDATE(), INTERVAL 9 DAY), 'COMPLETED', DATE_SUB(NOW(), INTERVAL 30 DAY), NOW()),
(6, 2, 1, NULL, 3, 'DevOps Automation Contract', 'CI/CD design and release hardening.', 'Weekly release review and runbook handover.', 2700.00, DATE_SUB(CURDATE(), INTERVAL 11 DAY), DATE_ADD(CURDATE(), INTERVAL 25 DAY), 'ACTIVE', DATE_SUB(NOW(), INTERVAL 11 DAY), NOW()),
(7, 2, 1, NULL, 4, 'Performance Tuning Contract', 'Profile and optimize production bottlenecks.', 'Two optimization cycles and metrics report.', 1750.00, DATE_SUB(CURDATE(), INTERVAL 7 DAY), DATE_ADD(CURDATE(), INTERVAL 16 DAY), 'ACTIVE', DATE_SUB(NOW(), INTERVAL 7 DAY), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), description = VALUES(description), terms = VALUES(terms), amount = VALUES(amount), status = VALUES(status), signed_at = VALUES(signed_at);

INSERT INTO conflicts (id, contract_id, raised_by_id, reason, description, evidence_url, status, created_at, resolved_at, resolution) VALUES
(1, 1, 2, 'Timeline clarification', 'Need confirmation on sprint cut-off date.', NULL, 'IN_REVIEW', NOW(), NULL, NULL),
(2, 2, 1, 'Awaiting signature', 'Freelancer requested legal wording update.', NULL, 'OPEN', NOW(), NULL, NULL),
(3, 3, 1, 'Scope creep warning', 'Additional UX requests need scope update.', NULL, 'OPEN', NOW(), NULL, NULL),
(4, 4, 2, 'Data quality concern', 'Client requested benchmark evidence before milestone approval.', NULL, 'IN_REVIEW', NOW(), NULL, NULL),
(5, 5, 2, 'Final payment released', 'Minor delay accepted after clear communication.', NULL, 'RESOLVED', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), 'Completed with adjusted acceptance date.'),
(6, 6, 3, 'Compliance checklist', 'Admin requested audit logs before final sign-off.', NULL, 'OPEN', NOW(), NULL, NULL)
ON DUPLICATE KEY UPDATE reason = VALUES(reason), description = VALUES(description), status = VALUES(status), resolved_at = VALUES(resolved_at), resolution = VALUES(resolution);

-- -----------------------------------------------------------------------------
-- 6) REVIEW DB (reviewdb)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS reviewdb;
USE reviewdb;

INSERT INTO reviews (id, reviewer_id, reviewee_id, project_id, rating, comment, created_at) VALUES
(1, 2, 1, 1, 5, 'Excellent quality and communication throughout delivery.', NOW()),
(2, 1, 2, 2, 5, 'Clear requirements and fast feedback from client side.', NOW()),
(3, 3, 1, 1, 5, 'Admin validation: delivery matched all acceptance criteria.', NOW()),
(4, 2, 1, 4, 5, 'Strong UX improvements and timely demos.', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(5, 1, 2, 4, 4, 'Requirements changed often but communication stayed productive.', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(6, 3, 2, 7, 4, 'Client maintained excellent documentation quality.', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(7, 2, 1, 7, 5, 'Matching quality improved significantly after delivery.', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(8, 1, 3, 8, 5, 'Admin review process was clear and fair.', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(9, 3, 1, 8, 5, 'Migration checklist completed with zero regressions.', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(10, 2, 3, 2, 4, 'Admin support for release decisions was very helpful.', NOW())
ON DUPLICATE KEY UPDATE rating = VALUES(rating), comment = VALUES(comment), created_at = VALUES(created_at);

INSERT INTO review_responses (id, review_id, respondent_id, message, responded_at) VALUES
(1, 1, 1, 'Thank you, looking forward to the next collaboration.', NOW()),
(2, 2, 2, 'Appreciate your professionalism and quick turnaround.', NOW()),
(3, 4, 1, 'Great collaboration, happy to continue with phase two.', NOW()),
(4, 5, 2, 'Thanks, we will lock scope earlier in the next sprint.', NOW()),
(5, 6, 2, 'Thanks for the governance support from admin side.', NOW()),
(6, 7, 1, 'Glad the relevance improvements delivered impact quickly.', NOW()),
(7, 8, 3, 'Thank you for maintaining complete evidence and logs.', NOW()),
(8, 10, 3, 'Happy to keep supporting release governance.', NOW())
ON DUPLICATE KEY UPDATE message = VALUES(message), responded_at = VALUES(responded_at);

-- -----------------------------------------------------------------------------
-- 7) PLANNING DB (planningdb)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS planningdb;
USE planningdb;

INSERT INTO progress_update (id, project_id, contract_id, freelancer_id, title, description, progress_percentage, created_at, updated_at) VALUES
(1, 1, 1, 1, 'Architecture and setup', 'Prepared project structure and initial API modules.', 35, NOW(), NOW()),
(2, 2, 2, 1, 'Dashboard sprint', 'Built dashboard widgets and integrated API data.', 60, NOW(), NOW()),
(3, 4, 3, 1, 'Client workspace refactor', 'Introduced reusable components and unified data loading.', 48, NOW(), NOW()),
(4, 7, 4, 1, 'Ranking model implementation', 'Added weighted matching heuristics and score normalization.', 55, NOW(), NOW()),
(5, 8, 5, 1, 'Legacy cleanup execution', 'Deprecated routes removed and compatibility shim completed.', 100, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY)),
(6, 5, 6, 1, 'Pipeline setup', 'Release pipeline and rollback checks are now active.', 62, NOW(), NOW()),
(7, 5, 7, 1, 'Performance profiling', 'Collected flamegraphs and prioritized optimization tasks.', 43, NOW(), NOW()),
(8, 1, 1, 1, 'Milestone 2 planning', 'Prepared backlog and estimates for next sprint.', 52, NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), description = VALUES(description), progress_percentage = VALUES(progress_percentage), updated_at = NOW();

INSERT INTO progress_comment (id, progress_update_id, user_id, message, created_at) VALUES
(1, 1, 2, 'Great start, please share updated endpoint list.', NOW()),
(2, 1, 3, 'Admin note: include monitoring metrics in this sprint.', NOW()),
(3, 2, 2, 'Dashboard is clear. Add export button next.', NOW()),
(4, 3, 2, 'Please keep table filters consistent with legacy version.', NOW()),
(5, 3, 3, 'Capture before/after UX metrics for report.', NOW()),
(6, 4, 2, 'Can you include explainability hints for ranking?', NOW()),
(7, 4, 3, 'Audit note: preserve match score traceability.', NOW()),
(8, 5, 2, 'Legacy retirement looked smooth from client perspective.', NOW()),
(9, 6, 2, 'Pipeline status widgets are useful.', NOW()),
(10, 6, 3, 'Please attach rollback drill logs.', NOW()),
(11, 7, 2, 'Share top three bottlenecks with ETA.', NOW()),
(12, 8, 3, 'Approved for milestone 2 execution.', NOW())
ON DUPLICATE KEY UPDATE message = VALUES(message), created_at = VALUES(created_at);

-- -----------------------------------------------------------------------------
-- 8) TASK DB (taskdb)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS taskdb;
USE taskdb;

INSERT INTO task (id, project_id, contract_id, title, description, status, priority, assignee_id, due_date, order_index, created_by, created_at, updated_at) VALUES
(1, 1, 1, 'Implement auth integration', 'Connect gateway auth with microservices.', 'IN_PROGRESS', 'HIGH', 1, DATE_ADD(CURDATE(), INTERVAL 7 DAY), 1, 2, NOW(), NOW()),
(2, 1, 1, 'Finalize DB seed validation', 'Verify all mock datasets and edge cases.', 'TODO', 'MEDIUM', 1, DATE_ADD(CURDATE(), INTERVAL 10 DAY), 2, 3, NOW(), NOW()),
(3, 2, 2, 'Build KPI widgets', 'Implement trend cards and status counters.', 'DONE', 'MEDIUM', 1, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 1, 2, NOW(), NOW()),
(4, 4, 3, 'Unify client table filters', 'Align dashboard filters across modules.', 'IN_PROGRESS', 'HIGH', 1, DATE_ADD(CURDATE(), INTERVAL 6 DAY), 2, 2, NOW(), NOW()),
(5, 7, 4, 'Implement rank scoring strategy', 'Code weighted scoring and tie-breakers.', 'IN_PROGRESS', 'HIGH', 1, DATE_ADD(CURDATE(), INTERVAL 9 DAY), 1, 2, NOW(), NOW()),
(6, 8, 5, 'Remove deprecated routes', 'Delete old APIs and update docs.', 'DONE', 'HIGH', 1, DATE_SUB(CURDATE(), INTERVAL 9 DAY), 1, 3, NOW(), NOW()),
(7, 5, 6, 'Set up CI quality gate', 'Add static checks and integration stage.', 'DONE', 'MEDIUM', 1, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 1, 3, NOW(), NOW()),
(8, 5, 7, 'Profile slow endpoints', 'Measure p95 latency and identify SQL hotspots.', 'IN_PROGRESS', 'HIGH', 1, DATE_ADD(CURDATE(), INTERVAL 4 DAY), 2, 2, NOW(), NOW()),
(9, 6, 3, 'Tag tickets by SLA', 'Apply SLA tags and escalation labels.', 'TODO', 'MEDIUM', 1, DATE_ADD(CURDATE(), INTERVAL 5 DAY), 1, 2, NOW(), NOW()),
(10, 6, 3, 'Implement auto-priority rules', 'Route urgent support requests to fast lane.', 'TODO', 'MEDIUM', 1, DATE_ADD(CURDATE(), INTERVAL 8 DAY), 2, 3, NOW(), NOW()),
(11, 3, 1, 'Add API health dashboard', 'Display service health and failure trends.', 'IN_PROGRESS', 'MEDIUM', 1, DATE_ADD(CURDATE(), INTERVAL 12 DAY), 3, 2, NOW(), NOW()),
(12, 1, 1, 'Prepare release notes', 'Summarize shipped tasks and pending risks.', 'TODO', 'LOW', 1, DATE_ADD(CURDATE(), INTERVAL 13 DAY), 4, 3, NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), description = VALUES(description), status = VALUES(status), priority = VALUES(priority), assignee_id = VALUES(assignee_id), due_date = VALUES(due_date), updated_at = NOW();

INSERT INTO subtask (id, parent_task_id, project_id, title, description, status, priority, assignee_id, due_date, order_index, created_at, updated_at) VALUES
(1, 1, 1, 'Configure token relay', 'Propagate user context between services.', 'IN_PROGRESS', 'HIGH', 1, DATE_ADD(CURDATE(), INTERVAL 3 DAY), 1, NOW(), NOW()),
(2, 1, 1, 'Add auth tests', 'Cover login and role checks.', 'TODO', 'MEDIUM', 1, DATE_ADD(CURDATE(), INTERVAL 5 DAY), 2, NOW(), NOW()),
(3, 3, 2, 'Wire trend API', 'Bind widgets to aggregated endpoint.', 'DONE', 'MEDIUM', 1, DATE_SUB(CURDATE(), INTERVAL 4 DAY), 1, NOW(), NOW()),
(4, 4, 4, 'Standardize filter schema', 'Use same query params in all views.', 'IN_PROGRESS', 'HIGH', 1, DATE_ADD(CURDATE(), INTERVAL 3 DAY), 1, NOW(), NOW()),
(5, 5, 7, 'Tune score weights', 'Calibrate relevance and recency impact.', 'IN_PROGRESS', 'HIGH', 1, DATE_ADD(CURDATE(), INTERVAL 4 DAY), 1, NOW(), NOW()),
(6, 6, 8, 'Archive route docs', 'Move deprecated references to archive.', 'DONE', 'LOW', 1, DATE_SUB(CURDATE(), INTERVAL 8 DAY), 1, NOW(), NOW()),
(7, 7, 5, 'Fail build on lint', 'Enforce code quality in CI stage.', 'DONE', 'MEDIUM', 1, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 1, NOW(), NOW()),
(8, 8, 5, 'Collect SQL plans', 'Capture explain plans for slow queries.', 'IN_PROGRESS', 'HIGH', 1, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 1, NOW(), NOW()),
(9, 9, 6, 'Map ticket categories', 'Define category dictionary and thresholds.', 'TODO', 'MEDIUM', 1, DATE_ADD(CURDATE(), INTERVAL 4 DAY), 1, NOW(), NOW()),
(10, 10, 6, 'Implement escalation cron', 'Auto-escalate overdue high priority tickets.', 'TODO', 'MEDIUM', 1, DATE_ADD(CURDATE(), INTERVAL 6 DAY), 1, NOW(), NOW()),
(11, 11, 3, 'Surface outage alerts', 'Show critical incidents with context.', 'IN_PROGRESS', 'MEDIUM', 1, DATE_ADD(CURDATE(), INTERVAL 6 DAY), 1, NOW(), NOW()),
(12, 12, 1, 'Draft milestone summary', 'Document delivered outcomes and blockers.', 'TODO', 'LOW', 1, DATE_ADD(CURDATE(), INTERVAL 9 DAY), 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), description = VALUES(description), status = VALUES(status), priority = VALUES(priority), assignee_id = VALUES(assignee_id), due_date = VALUES(due_date), updated_at = NOW();

INSERT INTO task_comment (id, task_id, user_id, message, created_at) VALUES
(1, 1, 2, 'Please keep backward compatibility with existing APIs.', NOW()),
(2, 1, 3, 'Admin review scheduled tomorrow.', NOW()),
(3, 2, 2, 'Edge cases should include partially populated databases.', NOW()),
(4, 3, 2, 'KPI cards are ready for demo.', NOW()),
(5, 4, 3, 'Please track regressions after filter changes.', NOW()),
(6, 5, 2, 'Ranking quality must improve for niche skills too.', NOW()),
(7, 6, 3, 'Deprecated routes removal approved.', NOW()),
(8, 7, 2, 'Quality gate helped reduce merge issues.', NOW()),
(9, 8, 3, 'Attach profiling screenshots in report.', NOW()),
(10, 9, 2, 'SLA category mapping should be configurable.', NOW()),
(11, 10, 3, 'Escalation logic needs audit logging.', NOW()),
(12, 11, 2, 'Health dashboard should include historical trend.', NOW()),
(13, 11, 3, 'Add admin filter by service domain.', NOW()),
(14, 12, 2, 'Release notes should mention unresolved risks.', NOW()),
(15, 12, 3, 'Schedule publication after QA sign-off.', NOW())
ON DUPLICATE KEY UPDATE message = VALUES(message), created_at = VALUES(created_at);

-- -----------------------------------------------------------------------------
-- 9) FREELANCIA JOB DB (freelancia_job_db)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS freelancia_job_db;
USE freelancia_job_db;

INSERT INTO jobs (id, client_id, client_type, company_name, title, description, budget_min, budget_max, currency, deadline, category, location_type, status, created_at, updated_at) VALUES
(1, 2, 'INDIVIDUAL', NULL, 'Backend Optimization Mission', 'Improve API performance and reliability.', 1500.00, 2500.00, 'TND', DATE_ADD(NOW(), INTERVAL 20 DAY), 'Backend', 'REMOTE', 'OPEN', NOW(), NOW()),
(2, 2, 'INDIVIDUAL', NULL, 'UI Enhancement Sprint', 'Refine admin and client portal UX.', 1000.00, 1800.00, 'TND', DATE_ADD(NOW(), INTERVAL 15 DAY), 'Frontend', 'HYBRID', 'OPEN', NOW(), NOW()),
(3, 2, 'INDIVIDUAL', NULL, 'Ticket Workflow Automation', 'Automate triage and SLA routing.', 1200.00, 2200.00, 'TND', DATE_ADD(NOW(), INTERVAL 18 DAY), 'Support', 'REMOTE', 'OPEN', NOW(), NOW()),
(4, 2, 'INDIVIDUAL', NULL, 'Vendor Compliance Dashboard', 'Build vendor approval monitoring dashboard.', 1800.00, 2600.00, 'TND', DATE_ADD(NOW(), INTERVAL 22 DAY), 'Governance', 'REMOTE', 'OPEN', NOW(), NOW()),
(5, 2, 'INDIVIDUAL', NULL, 'Meeting Orchestration API', 'Improve scheduling and conflict detection.', 1400.00, 2300.00, 'TND', DATE_ADD(NOW(), INTERVAL 25 DAY), 'Collaboration', 'HYBRID', 'OPEN', NOW(), NOW()),
(6, 2, 'INDIVIDUAL', NULL, 'Subcontract Evidence Module', 'Track deliverables and audit trails.', 1300.00, 2100.00, 'TND', DATE_ADD(NOW(), INTERVAL 19 DAY), 'Compliance', 'REMOTE', 'OPEN', NOW(), NOW()),
(7, 2, 'INDIVIDUAL', NULL, 'Portfolio Assessment Toolkit', 'Provide skill assessments and scoring summaries.', 1100.00, 1700.00, 'TND', DATE_ADD(NOW(), INTERVAL 14 DAY), 'Portfolio', 'REMOTE', 'OPEN', NOW(), NOW()),
(8, 2, 'INDIVIDUAL', NULL, 'Gamification Milestone Engine', 'Drive engagement with role-based achievements.', 1250.00, 1900.00, 'TND', DATE_ADD(NOW(), INTERVAL 16 DAY), 'Engagement', 'REMOTE', 'OPEN', NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), description = VALUES(description), budget_min = VALUES(budget_min), budget_max = VALUES(budget_max), status = VALUES(status), updated_at = NOW();

INSERT INTO job_required_skills (job_id, skill_id) VALUES
(1, 1), (1, 3), (2, 2),
(3, 1), (3, 5),
(4, 4), (4, 6),
(5, 1), (5, 2),
(6, 1), (6, 6),
(7, 2), (7, 5),
(8, 1), (8, 5), (8, 6)
ON DUPLICATE KEY UPDATE skill_id = VALUES(skill_id);

INSERT INTO job_applications (id, job_id, freelancer_id, proposal_message, expected_rate, availability_start, status, created_at, updated_at) VALUES
(1, 1, 1, 'I can optimize backend bottlenecks and provide profiling reports.', 55.00, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'SHORTLISTED', NOW(), NOW()),
(2, 2, 1, 'Ready to improve UX with maintainable Angular components.', 48.00, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'PENDING', NOW(), NOW()),
(3, 3, 1, 'Can automate ticket flow and SLA queues quickly.', 50.00, DATE_ADD(CURDATE(), INTERVAL 3 DAY), 'ACCEPTED', NOW(), NOW()),
(4, 4, 1, 'I will provide vendor dashboard with approval timeline analytics.', 58.00, DATE_ADD(CURDATE(), INTERVAL 4 DAY), 'SHORTLISTED', NOW(), NOW()),
(5, 5, 1, 'Meeting orchestration and reminder reliability can be improved fast.', 53.00, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'PENDING', NOW(), NOW()),
(6, 6, 1, 'I can enforce traceable subcontract evidence lifecycle.', 52.00, DATE_ADD(CURDATE(), INTERVAL 3 DAY), 'PENDING', NOW(), NOW()),
(7, 7, 1, 'Portfolio scoring module can be delivered with clean API contracts.', 46.00, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'ACCEPTED', NOW(), NOW()),
(8, 8, 1, 'Gamification logic with role-aware achievements is in my expertise.', 49.00, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'SHORTLISTED', NOW(), NOW()),
(9, 1, 3, 'Admin co-delivery for observability and compliance reporting.', 60.00, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'REJECTED', NOW(), NOW())
ON DUPLICATE KEY UPDATE proposal_message = VALUES(proposal_message), expected_rate = VALUES(expected_rate), availability_start = VALUES(availability_start), status = VALUES(status), updated_at = NOW();

INSERT INTO application_attachments (id, job_application_id, file_name, file_type, file_url, file_size, uploaded_at) VALUES
(1, 1, 'hanen-cv.pdf', 'application/pdf', '/uploads/applications/1/hanen-cv.pdf', 254000, NOW()),
(2, 2, 'hanen-portfolio.pdf', 'application/pdf', '/uploads/applications/2/hanen-portfolio.pdf', 312000, NOW()),
(3, 3, 'ticket-automation-case-study.pdf', 'application/pdf', '/uploads/applications/3/ticket-automation-case-study.pdf', 275000, NOW()),
(4, 4, 'vendor-dashboard-wireframes.pdf', 'application/pdf', '/uploads/applications/4/vendor-dashboard-wireframes.pdf', 298000, NOW()),
(5, 5, 'meeting-api-spec.pdf', 'application/pdf', '/uploads/applications/5/meeting-api-spec.pdf', 201000, NOW()),
(6, 6, 'subcontract-audit-flow.pdf', 'application/pdf', '/uploads/applications/6/subcontract-audit-flow.pdf', 229000, NOW()),
(7, 7, 'portfolio-assessment-plan.pdf', 'application/pdf', '/uploads/applications/7/portfolio-assessment-plan.pdf', 247000, NOW()),
(8, 8, 'gamification-rules-draft.pdf', 'application/pdf', '/uploads/applications/8/gamification-rules-draft.pdf', 239000, NOW()),
(9, 9, 'admin-observability-outline.pdf', 'application/pdf', '/uploads/applications/9/admin-observability-outline.pdf', 192000, NOW())
ON DUPLICATE KEY UPDATE file_name = VALUES(file_name), file_type = VALUES(file_type), file_url = VALUES(file_url), file_size = VALUES(file_size), uploaded_at = VALUES(uploaded_at);

-- -----------------------------------------------------------------------------
-- 10) MEETING DB (meetingdb)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS meetingdb;
USE meetingdb;

INSERT INTO meetings (id, client_id, freelancer_id, title, agenda, start_time, end_time, meeting_type, status, google_event_id, meet_link, calendar_id, project_id, contract_id, cancellation_reason, created_at, updated_at) VALUES
(1, 2, 1, 'Kickoff Meeting', 'Scope and milestone alignment', DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 1 DAY), INTERVAL 1 HOUR), 'VIDEO_CALL', 'ACCEPTED', 'evt-001', 'https://meet.google.com/demo-001', 'primary', 1, 1, NULL, NOW(), NOW()),
(2, 2, 1, 'Weekly Sync', 'Progress and blockers review', DATE_ADD(NOW(), INTERVAL 8 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 8 DAY), INTERVAL 1 HOUR), 'VIDEO_CALL', 'PENDING', NULL, NULL, NULL, 1, 1, NULL, NOW(), NOW()),
(3, 2, 1, 'Contract Review Session', 'Discuss contract #3 deliverables and risks', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 4 DAY), INTERVAL 45 MINUTE), 'VIDEO_CALL', 'COMPLETED', 'evt-003', 'https://meet.google.com/demo-003', 'primary', 4, 3, NULL, NOW(), NOW()),
(4, 2, 1, 'Ranking Design Workshop', 'Finalize scoring model assumptions', DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 3 DAY), INTERVAL 50 MINUTE), 'VIDEO_CALL', 'ACCEPTED', 'evt-004', 'https://meet.google.com/demo-004', 'primary', 7, 4, NULL, NOW(), NOW()),
(5, 2, 1, 'Legacy Cleanup Retrospective', 'Post-delivery review for completed contract', DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 6 DAY), INTERVAL 40 MINUTE), 'VIDEO_CALL', 'COMPLETED', 'evt-005', 'https://meet.google.com/demo-005', 'primary', 8, 5, NULL, NOW(), NOW()),
(6, 2, 1, 'Pipeline Governance Checkpoint', 'Review CI quality gates and deployment policy', DATE_ADD(NOW(), INTERVAL 5 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 5 DAY), INTERVAL 1 HOUR), 'VIDEO_CALL', 'PENDING', NULL, NULL, NULL, 5, 6, NULL, NOW(), NOW()),
(7, 2, 1, 'Performance Progress Review', 'Evaluate optimization outcomes and next actions', DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 2 DAY), INTERVAL 45 MINUTE), 'VIDEO_CALL', 'ACCEPTED', 'evt-007', 'https://meet.google.com/demo-007', 'primary', 5, 7, NULL, NOW(), NOW()),
(8, 2, 1, 'Admin Compliance Briefing', 'Admin-led compliance and audit handover', DATE_ADD(NOW(), INTERVAL 9 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 9 DAY), INTERVAL 35 MINUTE), 'VIDEO_CALL', 'PENDING', NULL, NULL, NULL, 6, 3, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), agenda = VALUES(agenda), start_time = VALUES(start_time), end_time = VALUES(end_time), status = VALUES(status), meet_link = VALUES(meet_link), updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 11) TICKET DB (ticketdb)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS ticketdb;
USE ticketdb;

INSERT INTO tickets (id, user_id, subject, status, priority, created_at, last_activity_at, first_response_at, resolved_at, last_reopened_at, response_time_minutes, reopen_count) VALUES
(1, 2, 'Need help with contract workflow', 'OPEN', 'MEDIUM', NOW(), NOW(), NULL, NULL, NULL, NULL, 0),
(2, 1, 'Question about project assignment', 'CLOSED', 'LOW', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, 120, 0),
(3, 1, 'Unable to attach deliverable evidence', 'OPEN', 'HIGH', DATE_SUB(NOW(), INTERVAL 2 DAY), NOW(), DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, NULL, 45, 1),
(4, 2, 'Offer application accepted but no notification', 'OPEN', 'MEDIUM', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW(), DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, NULL, 38, 0),
(5, 3, 'Need audit export for vendor approvals', 'OPEN', 'MEDIUM', DATE_SUB(NOW(), INTERVAL 5 DAY), NOW(), DATE_SUB(NOW(), INTERVAL 5 DAY), NULL, NULL, 60, 0),
(6, 2, 'Meeting invite timezone mismatch', 'CLOSED', 'LOW', DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), NULL, 90, 0),
(7, 1, 'Gamification points not updated after milestone', 'OPEN', 'MEDIUM', DATE_SUB(NOW(), INTERVAL 2 DAY), NOW(), DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, NULL, 52, 0),
(8, 2, 'Project dashboard export failed', 'OPEN', 'HIGH', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW(), DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, NULL, 35, 0)
ON DUPLICATE KEY UPDATE subject = VALUES(subject), status = VALUES(status), priority = VALUES(priority), last_activity_at = VALUES(last_activity_at), first_response_at = VALUES(first_response_at), resolved_at = VALUES(resolved_at), response_time_minutes = VALUES(response_time_minutes), reopen_count = VALUES(reopen_count);

INSERT INTO ticket_replies (id, ticket_id, message, sender, author_user_id, created_at, read_by_user, read_by_admin) VALUES
(1, 1, 'Can you share your current contract ID so we can verify?', 'ADMIN', 3, NOW(), 0, 1),
(2, 1, 'Contract ID is #2. Thanks for checking.', 'USER', 2, NOW(), 1, 1),
(3, 2, 'Issue resolved after permissions refresh.', 'ADMIN', 3, NOW(), 1, 1),
(4, 3, 'Please share the exact attachment type and size.', 'ADMIN', 3, NOW(), 0, 1),
(5, 3, 'PDF 8MB, upload fails at 90 percent.', 'USER', 1, NOW(), 1, 1),
(6, 4, 'Notification queue has been reprocessed.', 'ADMIN', 3, NOW(), 1, 1),
(7, 5, 'Audit export is now available from admin panel.', 'ADMIN', 3, NOW(), 1, 1),
(8, 6, 'Timezone setting fixed to UTC+1 for your org.', 'ADMIN', 3, NOW(), 1, 1),
(9, 7, 'Gamification recalculation job has been triggered.', 'ADMIN', 3, NOW(), 1, 1),
(10, 7, 'Points are now visible, thanks.', 'USER', 1, NOW(), 1, 1),
(11, 8, 'Export service restarted; please retry.', 'ADMIN', 3, NOW(), 0, 1),
(12, 8, 'Retried successfully, issue resolved.', 'USER', 2, NOW(), 1, 1)
ON DUPLICATE KEY UPDATE message = VALUES(message), sender = VALUES(sender), author_user_id = VALUES(author_user_id), read_by_user = VALUES(read_by_user), read_by_admin = VALUES(read_by_admin);

-- -----------------------------------------------------------------------------
-- 12) VENDOR DB (gestion_vendor_db)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS gestion_vendor_db;
USE gestion_vendor_db;

INSERT INTO vendor_approvals (id, organization_id, freelancer_id, status, domain, valid_from, valid_until, next_review_date, approved_by, approval_notes, rejection_reason, suspension_reason, review_count, created_at, updated_at, status_changed_at, client_signed_at, client_signer_name, freelancer_signed_at, freelancer_signer_name, professional_sector, expiry_reminder_sent_at) VALUES
(1, 2, 1, 'APPROVED', 'Software Engineering', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 365 DAY), DATE_ADD(CURDATE(), INTERVAL 180 DAY), 3, 'Initial approval completed.', NULL, NULL, 1, NOW(), NOW(), NOW(), NOW(), 'Omar Client', NOW(), 'Hanen Freelancer', 'Development', NULL),
(2, 2, 1, 'PENDING', 'Platform Support', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 180 DAY), DATE_ADD(CURDATE(), INTERVAL 90 DAY), NULL, 'Awaiting admin final confirmation.', NULL, NULL, 0, NOW(), NOW(), NOW(), NOW(), 'Omar Client', NOW(), 'Hanen Freelancer', 'Consulting', NULL),
(3, 2, 1, 'APPROVED', 'DevOps and Reliability', DATE_SUB(CURDATE(), INTERVAL 30 DAY), DATE_ADD(CURDATE(), INTERVAL 300 DAY), DATE_ADD(CURDATE(), INTERVAL 120 DAY), 3, 'Reliability scope approved after audit.', NULL, NULL, 2, NOW(), NOW(), NOW(), DATE_SUB(NOW(), INTERVAL 29 DAY), 'Omar Client', DATE_SUB(NOW(), INTERVAL 29 DAY), 'Hanen Freelancer', 'Operations', NULL),
(4, 2, 1, 'SUSPENDED', 'Data Compliance', DATE_SUB(CURDATE(), INTERVAL 150 DAY), DATE_ADD(CURDATE(), INTERVAL 40 DAY), DATE_ADD(CURDATE(), INTERVAL 20 DAY), 3, 'Suspended pending additional policy evidence.', NULL, 'Missing retention policy proof', 3, NOW(), NOW(), NOW(), DATE_SUB(NOW(), INTERVAL 145 DAY), 'Omar Client', DATE_SUB(NOW(), INTERVAL 145 DAY), 'Hanen Freelancer', 'Compliance', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(5, 2, 1, 'APPROVED', 'Support Automation', DATE_SUB(CURDATE(), INTERVAL 60 DAY), DATE_ADD(CURDATE(), INTERVAL 240 DAY), DATE_ADD(CURDATE(), INTERVAL 100 DAY), 3, 'Approved for ticket automation and SLA scope.', NULL, NULL, 1, NOW(), NOW(), NOW(), DATE_SUB(NOW(), INTERVAL 58 DAY), 'Omar Client', DATE_SUB(NOW(), INTERVAL 58 DAY), 'Hanen Freelancer', 'Support', NULL)
ON DUPLICATE KEY UPDATE status = VALUES(status), domain = VALUES(domain), valid_until = VALUES(valid_until), next_review_date = VALUES(next_review_date), approved_by = VALUES(approved_by), approval_notes = VALUES(approval_notes), review_count = VALUES(review_count), updated_at = NOW(), status_changed_at = VALUES(status_changed_at);

INSERT INTO vendor_approval_audits (id, vendor_approval_id, from_status, to_status, action, actor_user_id, detail, created_at) VALUES
(1, 1, 'PENDING', 'APPROVED', 'APPROVED', 3, 'Approved after documentation check.', NOW()),
(2, 2, NULL, 'PENDING', 'CREATED', 2, 'Approval request created by client.', NOW()),
(3, 3, 'PENDING', 'APPROVED', 'APPROVED', 3, 'Reliability controls reviewed and approved.', NOW()),
(4, 4, 'APPROVED', 'SUSPENDED', 'SUSPENDED', 3, 'Suspended due to missing compliance evidence.', NOW()),
(5, 4, 'SUSPENDED', 'SUSPENDED', 'REVIEWED', 3, 'Follow-up review scheduled for next cycle.', NOW()),
(6, 5, 'PENDING', 'APPROVED', 'APPROVED', 3, 'Support automation capability validated.', NOW()),
(7, 2, 'PENDING', 'PENDING', 'COMMENTED', 3, 'Requested additional incident references.', NOW()),
(8, 1, 'APPROVED', 'APPROVED', 'RENEWAL_NOTE', 3, 'Renewal reminder prepared for next quarter.', NOW())
ON DUPLICATE KEY UPDATE from_status = VALUES(from_status), to_status = VALUES(to_status), action = VALUES(action), actor_user_id = VALUES(actor_user_id), detail = VALUES(detail), created_at = VALUES(created_at);

INSERT INTO freelancer_match_profiles (id, freelancer_id, display_name, skill_tags, primary_domain, avg_rating, review_count, completed_contracts, on_time_rate, vendor_trust_score, active_vendor_agreements, avg_response_time_hours, global_score, vendor_boosted, last_computed_at) VALUES
(1, 1, 'Hanen Freelancer', '["Java","Angular","DevOps"]', 'Backend', 4.9, 8, 6, 97.0, 92, 3, 3.5, 94, 1, NOW()),
(2, 2, 'Omar Client Profile', '["Project Governance","Operations"]', 'Client Operations', 4.6, 5, 0, 95.0, 88, 0, 5.0, 86, 0, NOW()),
(3, 3, 'Ridha Admin Profile', '["Quality Assurance","Governance"]', 'Platform Governance', 4.8, 6, 0, 99.0, 96, 0, 2.8, 91, 0, NOW())
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), skill_tags = VALUES(skill_tags), primary_domain = VALUES(primary_domain), avg_rating = VALUES(avg_rating), review_count = VALUES(review_count), completed_contracts = VALUES(completed_contracts), on_time_rate = VALUES(on_time_rate), vendor_trust_score = VALUES(vendor_trust_score), active_vendor_agreements = VALUES(active_vendor_agreements), avg_response_time_hours = VALUES(avg_response_time_hours), global_score = VALUES(global_score), vendor_boosted = VALUES(vendor_boosted), last_computed_at = VALUES(last_computed_at);

INSERT INTO match_recommendations (id, target_type, target_id, freelancer_id, freelancer_name, match_score, match_reasons, status, created_at, viewed_at) VALUES
(1, 'PROJECT', 1, 1, 'Hanen Freelancer', 95, '["skill_match:96","rating:98","vendor_boost:+5"]', 'VIEWED', NOW(), NOW()),
(2, 'OFFER', 2, 1, 'Hanen Freelancer', 91, '["delivery_speed:90","communication:95"]', 'SUGGESTED', NOW(), NULL),
(3, 'PROJECT', 4, 1, 'Hanen Freelancer', 93, '["ui_delivery:92","history:95"]', 'VIEWED', NOW(), NOW()),
(4, 'PROJECT', 5, 1, 'Hanen Freelancer', 90, '["backend_expertise:94","timeline_fit:88"]', 'SUGGESTED', NOW(), NULL),
(5, 'PROJECT', 7, 1, 'Hanen Freelancer', 97, '["domain_fit:97","completion_rate:98"]', 'VIEWED', NOW(), NOW()),
(6, 'OFFER', 4, 1, 'Hanen Freelancer', 92, '["perf_track:95","response_time:90"]', 'SUGGESTED', NOW(), NULL),
(7, 'JOB', 3, 1, 'Hanen Freelancer', 89, '["automation_fit:90","qa_score:88"]', 'SUGGESTED', NOW(), NULL),
(8, 'JOB', 8, 1, 'Hanen Freelancer', 90, '["engagement_fit:89","delivery_speed:91"]', 'SUGGESTED', NOW(), NULL)
ON DUPLICATE KEY UPDATE match_score = VALUES(match_score), match_reasons = VALUES(match_reasons), status = VALUES(status), viewed_at = VALUES(viewed_at);

-- -----------------------------------------------------------------------------
-- 13) SUBCONTRACTING DB (gestion_subcontracting_db)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS gestion_subcontracting_db;
USE gestion_subcontracting_db;

INSERT INTO subcontracts (id, main_freelancer_id, subcontractor_id, project_id, contract_id, title, scope, category, budget, currency, status, start_date, deadline, rejection_reason, cancellation_reason, created_at, updated_at, status_changed_at) VALUES
(1, 1, 1, 1, 1, 'Internal specialization split', 'Split backend tuning and frontend polish into deliverables.', 'DEVELOPMENT', 1200.00, 'TND', 'IN_PROGRESS', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 20 DAY), NULL, NULL, NOW(), NOW(), NOW()),
(2, 1, 1, 4, 3, 'UI component stream', 'Deliver reusable client dashboard components.', 'DEVELOPMENT', 900.00, 'TND', 'IN_PROGRESS', DATE_SUB(CURDATE(), INTERVAL 6 DAY), DATE_ADD(CURDATE(), INTERVAL 12 DAY), NULL, NULL, NOW(), NOW(), NOW()),
(3, 1, 1, 7, 4, 'Ranking tuning stream', 'Refine matching rules and explainability layers.', 'ANALYTICS', 1100.00, 'TND', 'IN_PROGRESS', DATE_SUB(CURDATE(), INTERVAL 8 DAY), DATE_ADD(CURDATE(), INTERVAL 15 DAY), NULL, NULL, NOW(), NOW(), NOW()),
(4, 1, 1, 8, 5, 'Migration validation stream', 'Regression validation for legacy cleanup.', 'QUALITY', 700.00, 'TND', 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 24 DAY), DATE_SUB(CURDATE(), INTERVAL 9 DAY), NULL, NULL, NOW(), NOW(), NOW()),
(5, 1, 1, 5, 7, 'Performance reporting stream', 'Document optimization metrics and next actions.', 'REPORTING', 850.00, 'TND', 'IN_PROGRESS', DATE_SUB(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 11 DAY), NULL, NULL, NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), scope = VALUES(scope), category = VALUES(category), budget = VALUES(budget), status = VALUES(status), deadline = VALUES(deadline), updated_at = NOW(), status_changed_at = VALUES(status_changed_at);

INSERT INTO subcontract_deliverables (id, subcontract_id, title, description, status, deadline, submission_url, submission_note, submitted_at, review_note, reviewed_at, created_at, updated_at) VALUES
(1, 1, 'API latency report', 'Profile and optimize top 5 slow endpoints.', 'SUBMITTED', DATE_ADD(CURDATE(), INTERVAL 7 DAY), '/deliverables/subcontract/1/latency-report.pdf', 'Initial report submitted.', NOW(), 'Review pending by admin.', NULL, NOW(), NOW()),
(2, 1, 'Dashboard widget update', 'Improve KPI rendering and filters.', 'IN_PROGRESS', DATE_ADD(CURDATE(), INTERVAL 12 DAY), NULL, NULL, NULL, NULL, NULL, NOW(), NOW()),
(3, 2, 'Component architecture note', 'Reusable module structure and state strategy.', 'SUBMITTED', DATE_ADD(CURDATE(), INTERVAL 4 DAY), '/deliverables/subcontract/2/component-architecture.pdf', 'Ready for review.', NOW(), 'Looks consistent with UI goals.', NOW(), NOW(), NOW()),
(4, 2, 'Filter UX refinement', 'Improve filter discoverability and defaults.', 'IN_PROGRESS', DATE_ADD(CURDATE(), INTERVAL 9 DAY), NULL, NULL, NULL, NULL, NULL, NOW(), NOW()),
(5, 3, 'Ranking weight matrix', 'Initial score weight matrix and rationale.', 'SUBMITTED', DATE_ADD(CURDATE(), INTERVAL 6 DAY), '/deliverables/subcontract/3/ranking-matrix.pdf', 'Matrix attached.', NOW(), 'Need additional fairness scenarios.', NOW(), NOW(), NOW()),
(6, 3, 'Explainability payload', 'User-facing explanation snippets for ranking.', 'IN_PROGRESS', DATE_ADD(CURDATE(), INTERVAL 10 DAY), NULL, NULL, NULL, NULL, NULL, NOW(), NOW()),
(7, 4, 'Regression checklist', 'Checklist and execution logs for migration.', 'APPROVED', DATE_SUB(CURDATE(), INTERVAL 10 DAY), '/deliverables/subcontract/4/regression-checklist.pdf', 'Completed and approved.', DATE_SUB(NOW(), INTERVAL 10 DAY), 'Validated by admin.', DATE_SUB(NOW(), INTERVAL 9 DAY), NOW(), NOW()),
(8, 4, 'Legacy cleanup report', 'Final migration summary and incidents.', 'APPROVED', DATE_SUB(CURDATE(), INTERVAL 9 DAY), '/deliverables/subcontract/4/cleanup-report.pdf', 'Final report delivered.', DATE_SUB(NOW(), INTERVAL 9 DAY), 'Accepted by client.', DATE_SUB(NOW(), INTERVAL 8 DAY), NOW(), NOW()),
(9, 5, 'Optimization baseline', 'Baseline metrics before final optimization pass.', 'SUBMITTED', DATE_ADD(CURDATE(), INTERVAL 5 DAY), '/deliverables/subcontract/5/perf-baseline.pdf', 'Baseline captured.', NOW(), 'Include endpoint-level charts.', NOW(), NOW(), NOW()),
(10, 5, 'Optimization roadmap', 'Roadmap for phase two performance hardening.', 'IN_PROGRESS', DATE_ADD(CURDATE(), INTERVAL 11 DAY), NULL, NULL, NULL, NULL, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), description = VALUES(description), status = VALUES(status), deadline = VALUES(deadline), submission_url = VALUES(submission_url), submission_note = VALUES(submission_note), submitted_at = VALUES(submitted_at), review_note = VALUES(review_note), reviewed_at = VALUES(reviewed_at), updated_at = NOW();

INSERT INTO subcontract_audits (id, subcontract_id, actor_user_id, action, from_status, to_status, detail, target_entity, target_entity_id, created_at) VALUES
(1, 1, 1, 'STATUS_CHANGE', 'PROPOSED', 'IN_PROGRESS', 'Work execution started.', 'SUBCONTRACT', 1, NOW()),
(2, 1, 3, 'DELIVERABLE_REVIEW', NULL, NULL, 'Admin requested evidence details.', 'SUBCONTRACT_DELIVERABLE', 1, NOW()),
(3, 2, 1, 'STATUS_CHANGE', 'PROPOSED', 'IN_PROGRESS', 'Component stream started.', 'SUBCONTRACT', 2, NOW()),
(4, 2, 3, 'DELIVERABLE_REVIEW', NULL, NULL, 'Architecture note accepted with minor comments.', 'SUBCONTRACT_DELIVERABLE', 3, NOW()),
(5, 3, 1, 'STATUS_CHANGE', 'PROPOSED', 'IN_PROGRESS', 'Ranking stream started.', 'SUBCONTRACT', 3, NOW()),
(6, 3, 3, 'DELIVERABLE_REVIEW', NULL, NULL, 'Requested fairness test expansion.', 'SUBCONTRACT_DELIVERABLE', 5, NOW()),
(7, 4, 1, 'STATUS_CHANGE', 'IN_PROGRESS', 'COMPLETED', 'Migration validation fully completed.', 'SUBCONTRACT', 4, NOW()),
(8, 4, 3, 'DELIVERABLE_REVIEW', NULL, NULL, 'Cleanup report approved.', 'SUBCONTRACT_DELIVERABLE', 8, NOW()),
(9, 5, 1, 'STATUS_CHANGE', 'PROPOSED', 'IN_PROGRESS', 'Performance reporting stream started.', 'SUBCONTRACT', 5, NOW()),
(10, 5, 3, 'DELIVERABLE_REVIEW', NULL, NULL, 'Baseline accepted, roadmap pending finalization.', 'SUBCONTRACT_DELIVERABLE', 9, NOW())
ON DUPLICATE KEY UPDATE actor_user_id = VALUES(actor_user_id), action = VALUES(action), from_status = VALUES(from_status), to_status = VALUES(to_status), detail = VALUES(detail), target_entity = VALUES(target_entity), target_entity_id = VALUES(target_entity_id), created_at = VALUES(created_at);

-- -----------------------------------------------------------------------------
-- 14) GAMIFICATION DB (gamificationdb)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS gamificationdb;
USE gamificationdb;

INSERT INTO achievement (id, title, description, xp_reward, condition_type, condition_threshold, icon_emoji, target_role) VALUES
(1, 'First Delivery', 'Complete first project delivery.', 100, 'FIRST_PROJECT', 1, ':medal:', 'FREELANCER'),
(2, 'Fast Responder', 'Respond quickly to updates and tickets.', 80, 'FAST_RESPONDER', 5, ':zap:', 'ALL'),
(3, 'Project Creator', 'Create multiple projects as a client.', 120, 'PROJECT_CREATED', 3, ':rocket:', 'CLIENT'),
(4, 'Contract Closer', 'Close contracts with positive review outcomes.', 110, 'CONTRACT_COMPLETED', 3, ':briefcase:', 'FREELANCER'),
(5, 'Quality Guardian', 'Perform repeated governance checks.', 90, 'ADMIN_REVIEW', 5, ':shield:', 'ADMIN'),
(6, 'Support Champion', 'Resolve support tickets within SLA.', 95, 'TICKET_RESOLVED', 6, ':headphones:', 'ALL'),
(7, 'Collaboration Pro', 'Attend and complete project meetings.', 70, 'MEETING_COMPLETED', 5, ':handshake:', 'ALL'),
(8, 'Growth Streak', 'Maintain weekly progress updates.', 85, 'PROGRESS_STREAK', 4, ':chart_with_upwards_trend:', 'FREELANCER')
ON DUPLICATE KEY UPDATE title = VALUES(title), description = VALUES(description), xp_reward = VALUES(xp_reward), condition_type = VALUES(condition_type), condition_threshold = VALUES(condition_threshold), icon_emoji = VALUES(icon_emoji), target_role = VALUES(target_role);

INSERT INTO user_level (id, user_id, xp, level, user_role, fast_responder_streak, is_top_freelancer) VALUES
(1, 1, 860, 9, 'FREELANCER', 6, 1),
(2, 2, 430, 5, 'CLIENT', 3, 0),
(3, 3, 390, 4, 'ADMIN', 4, 0)
ON DUPLICATE KEY UPDATE xp = VALUES(xp), level = VALUES(level), user_role = VALUES(user_role), fast_responder_streak = VALUES(fast_responder_streak), is_top_freelancer = VALUES(is_top_freelancer);

INSERT INTO user_achievement (id, user_id, achievement_id, unlocked_at) VALUES
(1, 1, 1, NOW()),
(2, 1, 2, NOW()),
(3, 2, 3, NOW()),
(4, 1, 4, NOW()),
(5, 3, 5, NOW()),
(6, 1, 6, NOW()),
(7, 2, 6, NOW()),
(8, 1, 7, NOW()),
(9, 2, 7, NOW()),
(10, 1, 8, NOW())
ON DUPLICATE KEY UPDATE unlocked_at = VALUES(unlocked_at);

-- -----------------------------------------------------------------------------
-- 15) CHAT DB (chatdb)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS chatdb;
USE chatdb;

INSERT INTO chat_messages (id, sender_id, receiver_id, content, `timestamp`, status) VALUES
(1, 2, 1, 'Hi Hanen, can we confirm this week milestone?', DATE_SUB(NOW(), INTERVAL 2 DAY), 'SEEN'),
(2, 1, 2, 'Yes Omar, milestone one is ready for review.', DATE_ADD(DATE_SUB(NOW(), INTERVAL 2 DAY), INTERVAL 10 MINUTE), 'SEEN'),
(3, 3, 1, 'Admin reminder: upload final delivery note.', DATE_SUB(NOW(), INTERVAL 1 DAY), 'DELIVERED'),
(4, 1, 3, 'Acknowledged, I will upload it today.', DATE_ADD(DATE_SUB(NOW(), INTERVAL 1 DAY), INTERVAL 15 MINUTE), 'SEEN'),
(5, 2, 1, 'Can you also include the ranking improvement summary?', DATE_SUB(NOW(), INTERVAL 1 DAY), 'SEEN'),
(6, 1, 2, 'Sure, I will add benchmark comparison charts.', DATE_ADD(DATE_SUB(NOW(), INTERVAL 1 DAY), INTERVAL 12 MINUTE), 'SEEN'),
(7, 3, 2, 'Please update contract #2 signature timeline.', DATE_SUB(NOW(), INTERVAL 20 HOUR), 'DELIVERED'),
(8, 2, 3, 'Timeline updated. Awaiting final legal confirmation.', DATE_ADD(DATE_SUB(NOW(), INTERVAL 20 HOUR), INTERVAL 9 MINUTE), 'SEEN'),
(9, 1, 2, 'Ticket automation prototype is ready for demo.', DATE_SUB(NOW(), INTERVAL 18 HOUR), 'SEEN'),
(10, 2, 1, 'Great, schedule demo tomorrow at 10am.', DATE_ADD(DATE_SUB(NOW(), INTERVAL 18 HOUR), INTERVAL 7 MINUTE), 'SEEN'),
(11, 1, 3, 'Need admin confirmation on audit export fields.', DATE_SUB(NOW(), INTERVAL 15 HOUR), 'DELIVERED'),
(12, 3, 1, 'Approved. Include actor id and timestamp columns.', DATE_ADD(DATE_SUB(NOW(), INTERVAL 15 HOUR), INTERVAL 11 MINUTE), 'SEEN'),
(13, 2, 1, 'Did the performance contract start successfully?', DATE_SUB(NOW(), INTERVAL 10 HOUR), 'SEEN'),
(14, 1, 2, 'Yes, baseline profiling completed this morning.', DATE_ADD(DATE_SUB(NOW(), INTERVAL 10 HOUR), INTERVAL 14 MINUTE), 'SEEN'),
(15, 3, 2, 'Please close resolved support ticket #6.', DATE_SUB(NOW(), INTERVAL 8 HOUR), 'DELIVERED'),
(16, 2, 3, 'Closed and documented with RCA notes.', DATE_ADD(DATE_SUB(NOW(), INTERVAL 8 HOUR), INTERVAL 6 MINUTE), 'SEEN'),
(17, 1, 3, 'Gamification points sync completed after recalculation.', DATE_SUB(NOW(), INTERVAL 6 HOUR), 'SEEN'),
(18, 3, 1, 'Excellent, this addresses the user complaint.', DATE_ADD(DATE_SUB(NOW(), INTERVAL 6 HOUR), INTERVAL 8 MINUTE), 'SEEN'),
(19, 2, 1, 'Let us lock priorities for next week planning.', DATE_SUB(NOW(), INTERVAL 2 HOUR), 'DELIVERED'),
(20, 1, 2, 'Done. I shared the updated task board and dependencies.', DATE_ADD(DATE_SUB(NOW(), INTERVAL 2 HOUR), INTERVAL 9 MINUTE), 'SENT')
ON DUPLICATE KEY UPDATE content = VALUES(content), `timestamp` = VALUES(`timestamp`), status = VALUES(status);

-- =============================================================================
-- End of global seed script
-- =============================================================================
-- =============================================================================
-- Smart Freelance - Global MySQL seed script
-- Run: mysql -u root -p < scripts/seed-databases.sql
-- =============================================================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- -----------------------------------------------------------------------------
-- 1) USER DB (userdb)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS userdb;
USE userdb;

-- Required users: password = 15961596 (BCrypt)
INSERT INTO users (id, email, password_hash, first_name, last_name, role, phone, avatar_url, is_active, created_at, updated_at) VALUES
(1, 'hanen@hotmail.com', '$2b$10$8XLQGMLPa/QeqxDSD8i5luP3q.QZeoG6zEApSglhf1rq23XxSIEuu', 'Hanen', 'Freelancer', 'FREELANCER', '+21620100101', NULL, 1, NOW(), NOW()),
(2, 'omar@hotmail.com',  '$2b$10$8XLQGMLPa/QeqxDSD8i5luP3q.QZeoG6zEApSglhf1rq23XxSIEuu', 'Omar',  'Client',     'CLIENT',     '+21620100102', NULL, 1, NOW(), NOW()),
(3, 'ridha@hotmail.com', '$2b$10$8XLQGMLPa/QeqxDSD8i5luP3q.QZeoG6zEApSglhf1rq23XxSIEuu', 'Ridha', 'Admin',      'ADMIN',      '+21620100103', NULL, 1, NOW(), NOW()),
(4, 'demo.user1@example.com', '$2b$10$8XLQGMLPa/QeqxDSD8i5luP3q.QZeoG6zEApSglhf1rq23XxSIEuu', 'Demo', 'One', 'FREELANCER', '+21620100104', NULL, 1, NOW(), NOW()),
(5, 'demo.user2@example.com', '$2b$10$8XLQGMLPa/QeqxDSD8i5luP3q.QZeoG6zEApSglhf1rq23XxSIEuu', 'Demo', 'Two', 'CLIENT',     '+21620100105', NULL, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
email = VALUES(email),
password_hash = VALUES(password_hash),
first_name = VALUES(first_name),
last_name = VALUES(last_name),
role = VALUES(role),
phone = VALUES(phone),
is_active = VALUES(is_active),
updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 2) PORTFOLIO DB (portfolio_db)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS portfolio_db;
USE portfolio_db;

INSERT INTO skills (id, name, description, user_id, experience_id, created_at, updated_at) VALUES
(1, 'Java', 'Spring Boot microservices', 1, NULL, NOW(), NOW()),
(2, 'Angular', 'Angular dashboards and SPA', 1, NULL, NOW(), NOW()),
(3, 'DevOps', 'Docker and CI/CD setup', 1, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE
name = VALUES(name),
description = VALUES(description),
user_id = VALUES(user_id),
experience_id = VALUES(experience_id),
updated_at = NOW();

INSERT INTO skill_domains (skill_id, domain) VALUES
(1, 'WEB_DEVELOPMENT'),
(1, 'DATABASE_ADMINISTRATION'),
(2, 'WEB_DEVELOPMENT'),
(2, 'UI_UX_DESIGN'),
(3, 'DEVOPS'),
(3, 'CLOUD_COMPUTING')
ON DUPLICATE KEY UPDATE domain = VALUES(domain);

INSERT INTO experiences (id, user_id, title, type, description, start_date, end_date, company_or_client_name) VALUES
(1, 1, 'Freelance Backend Missions', 'JOB', 'Delivered backend APIs for multiple clients.', '2022-01-01', NULL, 'Independent'),
(2, 1, 'Project Delivery for Omar', 'PROJECT', 'Implemented platform module end to end.', '2024-02-01', '2024-05-01', 'Omar Client')
ON DUPLICATE KEY UPDATE title = VALUES(title), type = VALUES(type), description = VALUES(description), company_or_client_name = VALUES(company_or_client_name);

INSERT INTO evaluation_tests (id, skill_id, title, passing_score, duration_minutes, created_at, updated_at) VALUES
(1, 1, 'Java Core Assessment', 70.0, 45, NOW(), NOW()),
(2, 2, 'Angular Frontend Assessment', 70.0, 45, NOW(), NOW()),
(3, 3, 'DevOps Automation Assessment', 75.0, 50, NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), passing_score = VALUES(passing_score), duration_minutes = VALUES(duration_minutes), updated_at = NOW();

INSERT INTO evaluations (id, freelancer_id, skill_id, score, passed, test_result, evaluated_at) VALUES
(1, 1, 1, 92.5, 1, 'Excellent backend skills', NOW()),
(2, 1, 2, 88.0, 1, 'Strong frontend delivery', NOW()),
(3, 1, 3, 90.0, 1, 'Reliable CI/CD practices', NOW())
ON DUPLICATE KEY UPDATE score = VALUES(score), passed = VALUES(passed), test_result = VALUES(test_result), evaluated_at = VALUES(evaluated_at);

-- -----------------------------------------------------------------------------
-- 3) PROJECT DB (projectdb)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS projectdb;
USE projectdb;

INSERT INTO project (id, client_id, title, description, budget, deadline, status, category, created_at, updated_at) VALUES
(1, 2, 'Smart Freelance MVP Upgrade', 'Extend platform modules and improve performance.', 8000.00, DATE_ADD(NOW(), INTERVAL 45 DAY), 'OPEN', 'Web Platform', NOW(), NOW()),
(2, 2, 'Admin Analytics Dashboard', 'Build operational dashboard for admin insights.', 3500.00, DATE_ADD(NOW(), INTERVAL 30 DAY), 'IN_PROGRESS', 'Analytics', NOW(), NOW()),
(3, 2, 'API Stabilization Sprint', 'Fix bugs and add API monitoring.', 2500.00, DATE_ADD(NOW(), INTERVAL 20 DAY), 'OPEN', 'Backend', NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), description = VALUES(description), budget = VALUES(budget), deadline = VALUES(deadline), status = VALUES(status), updated_at = NOW();

INSERT INTO project_skill_ids (project_id, skill_id) VALUES
(1, 1), (1, 2), (1, 3),
(2, 2), (2, 3),
(3, 1), (3, 3)
ON DUPLICATE KEY UPDATE skill_id = VALUES(skill_id);

INSERT INTO project_application (id, freelance_id, cover_letter, proposed_price, proposed_duration, status, applied_at, responded_at, project_id) VALUES
(1, 1, 'I can deliver this with a clean architecture and tested APIs.', 7600.00, 40, 'PENDING', NOW(), NULL, 1),
(2, 1, 'Dashboard delivery in two iterations with weekly demos.', 3300.00, 28, 'ACCEPTED', DATE_SUB(NOW(), INTERVAL 5 DAY), NOW(), 2),
(3, 1, 'Ready to start immediately and improve reliability fast.', 2400.00, 18, 'PENDING', NOW(), NULL, 3)
ON DUPLICATE KEY UPDATE cover_letter = VALUES(cover_letter), proposed_price = VALUES(proposed_price), proposed_duration = VALUES(proposed_duration), status = VALUES(status), responded_at = VALUES(responded_at);

-- -----------------------------------------------------------------------------
-- 4) OFFER DB (gestion_offre_db)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS gestion_offre_db;
USE gestion_offre_db;

INSERT INTO offers (id, freelancer_id, title, domain, description, price, duration_type, offer_status, deadline, category, rating, communication_score, tags, views_count, is_featured, is_active, created_at, updated_at, published_at, expired_at) VALUES
(1, 1, 'Backend API Development', 'Backend', 'Design and implement scalable APIs.', 65.00, 'hourly', 'AVAILABLE', DATE_ADD(CURDATE(), INTERVAL 90 DAY), 'Development', 4.9, 4.8, 'java,spring,mysql', 120, 1, 1, NOW(), NOW(), NOW(), NULL),
(2, 1, 'Fullstack Feature Delivery', 'Fullstack', 'End-to-end feature delivery from UI to DB.', 3200.00, 'fixed', 'AVAILABLE', DATE_ADD(CURDATE(), INTERVAL 60 DAY), 'Web', 4.8, 4.9, 'angular,java,api', 90, 0, 1, NOW(), NOW(), NOW(), NULL)
ON DUPLICATE KEY UPDATE title = VALUES(title), description = VALUES(description), price = VALUES(price), offer_status = VALUES(offer_status), updated_at = NOW();

INSERT INTO offer_applications (id, offer_id, client_id, message, proposed_budget, portfolio_url, attachment_url, estimated_duration, status, is_read, applied_at, responded_at, accepted_at) VALUES
(1, 1, 2, 'We need backend ownership for the next release cycle.', 5200.00, 'https://portfolio.local/omar', NULL, 60, 'PENDING', 0, NOW(), NULL, NULL),
(2, 2, 2, 'Scope fits our roadmap. Let us discuss milestones.', 3000.00, NULL, NULL, 45, 'ACCEPTED', 1, DATE_SUB(NOW(), INTERVAL 4 DAY), NOW(), NOW())
ON DUPLICATE KEY UPDATE message = VALUES(message), proposed_budget = VALUES(proposed_budget), estimated_duration = VALUES(estimated_duration), status = VALUES(status), is_read = VALUES(is_read), responded_at = VALUES(responded_at), accepted_at = VALUES(accepted_at);

-- -----------------------------------------------------------------------------
-- 5) CONTRACT DB (gestion_contract_db)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS gestion_contract_db;
USE gestion_contract_db;

INSERT INTO contracts (id, client_id, freelancer_id, project_application_id, offer_application_id, title, description, terms, amount, start_date, end_date, status, signed_at, created_at) VALUES
(1, 2, 1, 1, NULL, 'MVP Upgrade Contract', 'Execution of project MVP upgrade scope.', 'Milestones every 2 weeks.', 7600.00, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 45 DAY), 'ACTIVE', NOW(), NOW()),
(2, 2, 1, NULL, 2, 'Feature Delivery Contract', 'Delivery of fullstack feature package.', 'One final handover and docs.', 3000.00, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY), 'PENDING_SIGNATURE', NULL, NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), description = VALUES(description), terms = VALUES(terms), amount = VALUES(amount), status = VALUES(status), signed_at = VALUES(signed_at);

INSERT INTO conflicts (id, contract_id, raised_by_id, reason, description, evidence_url, status, created_at, resolved_at, resolution) VALUES
(1, 1, 2, 'Timeline clarification', 'Need confirmation on sprint cut-off date.', NULL, 'IN_REVIEW', NOW(), NULL, NULL),
(2, 2, 1, 'Awaiting signature', 'Freelancer requested legal wording update.', NULL, 'OPEN', NOW(), NULL, NULL)
ON DUPLICATE KEY UPDATE reason = VALUES(reason), description = VALUES(description), status = VALUES(status), resolved_at = VALUES(resolved_at), resolution = VALUES(resolution);

-- -----------------------------------------------------------------------------
-- 6) REVIEW DB (reviewdb)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS reviewdb;
USE reviewdb;

INSERT INTO reviews (id, reviewer_id, reviewee_id, project_id, rating, comment, created_at) VALUES
(1, 2, 1, 1, 5, 'Excellent quality and communication throughout delivery.', NOW()),
(2, 1, 2, 2, 5, 'Clear requirements and fast feedback from client side.', NOW()),
(3, 3, 1, 1, 5, 'Admin validation: delivery matched all acceptance criteria.', NOW())
ON DUPLICATE KEY UPDATE rating = VALUES(rating), comment = VALUES(comment), created_at = VALUES(created_at);

INSERT INTO review_responses (id, review_id, respondent_id, message, responded_at) VALUES
(1, 1, 1, 'Thank you, looking forward to the next collaboration.', NOW()),
(2, 2, 2, 'Appreciate your professionalism and quick turnaround.', NOW())
ON DUPLICATE KEY UPDATE message = VALUES(message), responded_at = VALUES(responded_at);

-- -----------------------------------------------------------------------------
-- 7) PLANNING DB (planningdb)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS planningdb;
USE planningdb;

INSERT INTO progress_update (id, project_id, contract_id, freelancer_id, title, description, progress_percentage, created_at, updated_at) VALUES
(1, 1, 1, 1, 'Architecture and setup', 'Prepared project structure and initial API modules.', 35, NOW(), NOW()),
(2, 2, 2, 1, 'Dashboard sprint', 'Built dashboard widgets and integrated API data.', 60, NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), description = VALUES(description), progress_percentage = VALUES(progress_percentage), updated_at = NOW();

INSERT INTO progress_comment (id, progress_update_id, user_id, message, created_at) VALUES
(1, 1, 2, 'Great start, please share updated endpoint list.', NOW()),
(2, 1, 3, 'Admin note: include monitoring metrics in this sprint.', NOW()),
(3, 2, 2, 'Dashboard is clear. Add export button next.', NOW())
ON DUPLICATE KEY UPDATE message = VALUES(message), created_at = VALUES(created_at);

-- -----------------------------------------------------------------------------
-- 8) TASK DB (taskdb)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS taskdb;
USE taskdb;

INSERT INTO task (id, project_id, contract_id, title, description, status, priority, assignee_id, due_date, order_index, created_by, created_at, updated_at) VALUES
(1, 1, 1, 'Implement auth integration', 'Connect gateway auth with microservices.', 'IN_PROGRESS', 'HIGH', 1, DATE_ADD(CURDATE(), INTERVAL 7 DAY), 1, 2, NOW(), NOW()),
(2, 1, 1, 'Finalize DB seed validation', 'Verify all mock datasets and edge cases.', 'TODO', 'MEDIUM', 1, DATE_ADD(CURDATE(), INTERVAL 10 DAY), 2, 3, NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), description = VALUES(description), status = VALUES(status), priority = VALUES(priority), assignee_id = VALUES(assignee_id), due_date = VALUES(due_date), updated_at = NOW();

INSERT INTO subtask (id, parent_task_id, project_id, title, description, status, priority, assignee_id, due_date, order_index, created_at, updated_at) VALUES
(1, 1, 1, 'Configure token relay', 'Propagate user context between services.', 'IN_PROGRESS', 'HIGH', 1, DATE_ADD(CURDATE(), INTERVAL 3 DAY), 1, NOW(), NOW()),
(2, 1, 1, 'Add auth tests', 'Cover login and role checks.', 'TODO', 'MEDIUM', 1, DATE_ADD(CURDATE(), INTERVAL 5 DAY), 2, NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), description = VALUES(description), status = VALUES(status), priority = VALUES(priority), assignee_id = VALUES(assignee_id), due_date = VALUES(due_date), updated_at = NOW();

INSERT INTO task_comment (id, task_id, user_id, message, created_at) VALUES
(1, 1, 2, 'Please keep backward compatibility with existing APIs.', NOW()),
(2, 1, 3, 'Admin review scheduled tomorrow.', NOW())
ON DUPLICATE KEY UPDATE message = VALUES(message), created_at = VALUES(created_at);

-- -----------------------------------------------------------------------------
-- 9) FREELANCIA JOB DB (freelancia_job_db)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS freelancia_job_db;
USE freelancia_job_db;

INSERT INTO jobs (id, client_id, client_type, company_name, title, description, budget_min, budget_max, currency, deadline, category, location_type, status, created_at, updated_at) VALUES
(1, 2, 'INDIVIDUAL', NULL, 'Backend Optimization Mission', 'Improve API performance and reliability.', 1500.00, 2500.00, 'TND', DATE_ADD(NOW(), INTERVAL 20 DAY), 'Backend', 'REMOTE', 'OPEN', NOW(), NOW()),
(2, 2, 'INDIVIDUAL', NULL, 'UI Enhancement Sprint', 'Refine admin and client portal UX.', 1000.00, 1800.00, 'TND', DATE_ADD(NOW(), INTERVAL 15 DAY), 'Frontend', 'HYBRID', 'OPEN', NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), description = VALUES(description), budget_min = VALUES(budget_min), budget_max = VALUES(budget_max), status = VALUES(status), updated_at = NOW();

INSERT INTO job_required_skills (job_id, skill_id) VALUES
(1, 1), (1, 3), (2, 2)
ON DUPLICATE KEY UPDATE skill_id = VALUES(skill_id);

INSERT INTO job_applications (id, job_id, freelancer_id, proposal_message, expected_rate, availability_start, status, created_at, updated_at) VALUES
(1, 1, 1, 'I can optimize backend bottlenecks and provide profiling reports.', 55.00, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'SHORTLISTED', NOW(), NOW()),
(2, 2, 1, 'Ready to improve UX with maintainable Angular components.', 48.00, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'PENDING', NOW(), NOW())
ON DUPLICATE KEY UPDATE proposal_message = VALUES(proposal_message), expected_rate = VALUES(expected_rate), availability_start = VALUES(availability_start), status = VALUES(status), updated_at = NOW();

INSERT INTO application_attachments (id, job_application_id, file_name, file_type, file_url, file_size, uploaded_at) VALUES
(1, 1, 'hanen-cv.pdf', 'application/pdf', '/uploads/applications/1/hanen-cv.pdf', 254000, NOW()),
(2, 2, 'hanen-portfolio.pdf', 'application/pdf', '/uploads/applications/2/hanen-portfolio.pdf', 312000, NOW())
ON DUPLICATE KEY UPDATE file_name = VALUES(file_name), file_type = VALUES(file_type), file_url = VALUES(file_url), file_size = VALUES(file_size), uploaded_at = VALUES(uploaded_at);

-- -----------------------------------------------------------------------------
-- 10) MEETING DB (meetingdb)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS meetingdb;
USE meetingdb;

INSERT INTO meetings (id, client_id, freelancer_id, title, agenda, start_time, end_time, meeting_type, status, google_event_id, meet_link, calendar_id, project_id, contract_id, cancellation_reason, created_at, updated_at) VALUES
(1, 2, 1, 'Kickoff Meeting', 'Scope and milestone alignment', DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 1 DAY), INTERVAL 1 HOUR), 'VIDEO_CALL', 'ACCEPTED', 'evt-001', 'https://meet.google.com/demo-001', 'primary', 1, 1, NULL, NOW(), NOW()),
(2, 2, 1, 'Weekly Sync', 'Progress and blockers review', DATE_ADD(NOW(), INTERVAL 8 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 8 DAY), INTERVAL 1 HOUR), 'VIDEO_CALL', 'PENDING', NULL, NULL, NULL, 1, 1, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), agenda = VALUES(agenda), start_time = VALUES(start_time), end_time = VALUES(end_time), status = VALUES(status), meet_link = VALUES(meet_link), updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 11) TICKET DB (ticketdb)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS ticketdb;
USE ticketdb;

INSERT INTO tickets (id, user_id, subject, status, priority, created_at, last_activity_at, first_response_at, resolved_at, last_reopened_at, response_time_minutes, reopen_count) VALUES
(1, 2, 'Need help with contract workflow', 'OPEN', 'MEDIUM', NOW(), NOW(), NULL, NULL, NULL, NULL, 0),
(2, 1, 'Question about project assignment', 'CLOSED', 'LOW', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, 120, 0)
ON DUPLICATE KEY UPDATE subject = VALUES(subject), status = VALUES(status), priority = VALUES(priority), last_activity_at = VALUES(last_activity_at), first_response_at = VALUES(first_response_at), resolved_at = VALUES(resolved_at), response_time_minutes = VALUES(response_time_minutes), reopen_count = VALUES(reopen_count);

INSERT INTO ticket_replies (id, ticket_id, message, sender, author_user_id, created_at, read_by_user, read_by_admin) VALUES
(1, 1, 'Can you share your current contract ID so we can verify?', 'ADMIN', 3, NOW(), 0, 1),
(2, 1, 'Contract ID is #2. Thanks for checking.', 'USER', 2, NOW(), 1, 1),
(3, 2, 'Issue resolved after permissions refresh.', 'ADMIN', 3, NOW(), 1, 1)
ON DUPLICATE KEY UPDATE message = VALUES(message), sender = VALUES(sender), author_user_id = VALUES(author_user_id), read_by_user = VALUES(read_by_user), read_by_admin = VALUES(read_by_admin);

-- -----------------------------------------------------------------------------
-- 12) VENDOR DB (gestion_vendor_db)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS gestion_vendor_db;
USE gestion_vendor_db;

INSERT INTO vendor_approvals (id, organization_id, freelancer_id, status, domain, valid_from, valid_until, next_review_date, approved_by, approval_notes, rejection_reason, suspension_reason, review_count, created_at, updated_at, status_changed_at, client_signed_at, client_signer_name, freelancer_signed_at, freelancer_signer_name, professional_sector, expiry_reminder_sent_at) VALUES
(1, 2, 1, 'APPROVED', 'Software Engineering', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 365 DAY), DATE_ADD(CURDATE(), INTERVAL 180 DAY), 3, 'Initial approval completed.', NULL, NULL, 1, NOW(), NOW(), NOW(), NOW(), 'Omar Client', NOW(), 'Hanen Freelancer', 'Development', NULL),
(2, 2, 1, 'PENDING', 'Platform Support', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 180 DAY), DATE_ADD(CURDATE(), INTERVAL 90 DAY), NULL, 'Awaiting admin final confirmation.', NULL, NULL, 0, NOW(), NOW(), NOW(), NOW(), 'Omar Client', NOW(), 'Hanen Freelancer', 'Consulting', NULL)
ON DUPLICATE KEY UPDATE status = VALUES(status), domain = VALUES(domain), valid_until = VALUES(valid_until), next_review_date = VALUES(next_review_date), approved_by = VALUES(approved_by), approval_notes = VALUES(approval_notes), review_count = VALUES(review_count), updated_at = NOW(), status_changed_at = VALUES(status_changed_at);

INSERT INTO vendor_approval_audits (id, vendor_approval_id, from_status, to_status, action, actor_user_id, detail, created_at) VALUES
(1, 1, 'PENDING', 'APPROVED', 'APPROVED', 3, 'Approved after documentation check.', NOW()),
(2, 2, NULL, 'PENDING', 'CREATED', 2, 'Approval request created by client.', NOW())
ON DUPLICATE KEY UPDATE from_status = VALUES(from_status), to_status = VALUES(to_status), action = VALUES(action), actor_user_id = VALUES(actor_user_id), detail = VALUES(detail), created_at = VALUES(created_at);

INSERT INTO freelancer_match_profiles (id, freelancer_id, display_name, skill_tags, primary_domain, avg_rating, review_count, completed_contracts, on_time_rate, vendor_trust_score, active_vendor_agreements, avg_response_time_hours, global_score, vendor_boosted, last_computed_at) VALUES
(1, 1, 'Hanen Freelancer', '["Java","Angular","DevOps"]', 'Backend', 4.9, 3, 2, 97.0, 92, 1, 3.5, 94, 1, NOW())
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), skill_tags = VALUES(skill_tags), primary_domain = VALUES(primary_domain), avg_rating = VALUES(avg_rating), review_count = VALUES(review_count), completed_contracts = VALUES(completed_contracts), on_time_rate = VALUES(on_time_rate), vendor_trust_score = VALUES(vendor_trust_score), active_vendor_agreements = VALUES(active_vendor_agreements), avg_response_time_hours = VALUES(avg_response_time_hours), global_score = VALUES(global_score), vendor_boosted = VALUES(vendor_boosted), last_computed_at = VALUES(last_computed_at);

INSERT INTO match_recommendations (id, target_type, target_id, freelancer_id, freelancer_name, match_score, match_reasons, status, created_at, viewed_at) VALUES
(1, 'PROJECT', 1, 1, 'Hanen Freelancer', 95, '["skill_match:96","rating:98","vendor_boost:+5"]', 'VIEWED', NOW(), NOW()),
(2, 'OFFER', 2, 1, 'Hanen Freelancer', 91, '["delivery_speed:90","communication:95"]', 'SUGGESTED', NOW(), NULL)
ON DUPLICATE KEY UPDATE match_score = VALUES(match_score), match_reasons = VALUES(match_reasons), status = VALUES(status), viewed_at = VALUES(viewed_at);

-- -----------------------------------------------------------------------------
-- 13) SUBCONTRACTING DB (gestion_subcontracting_db)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS gestion_subcontracting_db;
USE gestion_subcontracting_db;

INSERT INTO subcontracts (id, main_freelancer_id, subcontractor_id, project_id, contract_id, title, scope, category, budget, currency, status, start_date, deadline, rejection_reason, cancellation_reason, created_at, updated_at, status_changed_at) VALUES
(1, 1, 1, 1, 1, 'Internal specialization split', 'Split backend tuning and frontend polish into deliverables.', 'DEVELOPMENT', 1200.00, 'TND', 'IN_PROGRESS', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 20 DAY), NULL, NULL, NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), scope = VALUES(scope), category = VALUES(category), budget = VALUES(budget), status = VALUES(status), deadline = VALUES(deadline), updated_at = NOW(), status_changed_at = VALUES(status_changed_at);

INSERT INTO subcontract_deliverables (id, subcontract_id, title, description, status, deadline, submission_url, submission_note, submitted_at, review_note, reviewed_at, created_at, updated_at) VALUES
(1, 1, 'API latency report', 'Profile and optimize top 5 slow endpoints.', 'SUBMITTED', DATE_ADD(CURDATE(), INTERVAL 7 DAY), '/deliverables/subcontract/1/latency-report.pdf', 'Initial report submitted.', NOW(), 'Review pending by admin.', NULL, NOW(), NOW()),
(2, 1, 'Dashboard widget update', 'Improve KPI rendering and filters.', 'IN_PROGRESS', DATE_ADD(CURDATE(), INTERVAL 12 DAY), NULL, NULL, NULL, NULL, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), description = VALUES(description), status = VALUES(status), deadline = VALUES(deadline), submission_url = VALUES(submission_url), submission_note = VALUES(submission_note), submitted_at = VALUES(submitted_at), review_note = VALUES(review_note), reviewed_at = VALUES(reviewed_at), updated_at = NOW();

INSERT INTO subcontract_audits (id, subcontract_id, actor_user_id, action, from_status, to_status, detail, target_entity, target_entity_id, created_at) VALUES
(1, 1, 1, 'STATUS_CHANGE', 'PROPOSED', 'IN_PROGRESS', 'Work execution started.', 'SUBCONTRACT', 1, NOW()),
(2, 1, 3, 'DELIVERABLE_REVIEW', NULL, NULL, 'Admin requested evidence details.', 'SUBCONTRACT_DELIVERABLE', 1, NOW())
ON DUPLICATE KEY UPDATE actor_user_id = VALUES(actor_user_id), action = VALUES(action), from_status = VALUES(from_status), to_status = VALUES(to_status), detail = VALUES(detail), target_entity = VALUES(target_entity), target_entity_id = VALUES(target_entity_id), created_at = VALUES(created_at);

-- -----------------------------------------------------------------------------
-- 14) GAMIFICATION DB (gamificationdb)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS gamificationdb;
USE gamificationdb;

INSERT INTO achievement (id, title, description, xp_reward, condition_type, condition_threshold, icon_emoji, target_role) VALUES
(1, 'First Delivery', 'Complete first project delivery.', 100, 'FIRST_PROJECT', 1, ':medal:', 'FREELANCER'),
(2, 'Fast Responder', 'Respond quickly to updates and tickets.', 80, 'FAST_RESPONDER', 5, ':zap:', 'ALL'),
(3, 'Project Creator', 'Create multiple projects as a client.', 120, 'PROJECT_CREATED', 3, ':rocket:', 'CLIENT')
ON DUPLICATE KEY UPDATE title = VALUES(title), description = VALUES(description), xp_reward = VALUES(xp_reward), condition_type = VALUES(condition_type), condition_threshold = VALUES(condition_threshold), icon_emoji = VALUES(icon_emoji), target_role = VALUES(target_role);

INSERT INTO user_level (id, user_id, xp, level, user_role, fast_responder_streak, is_top_freelancer) VALUES
(1, 1, 560, 6, 'FREELANCER', 4, 1),
(2, 2, 260, 3, 'CLIENT', 2, 0),
(3, 3, 180, 2, 'ADMIN', 1, 0)
ON DUPLICATE KEY UPDATE xp = VALUES(xp), level = VALUES(level), user_role = VALUES(user_role), fast_responder_streak = VALUES(fast_responder_streak), is_top_freelancer = VALUES(is_top_freelancer);

INSERT INTO user_achievement (id, user_id, achievement_id, unlocked_at) VALUES
(1, 1, 1, NOW()),
(2, 1, 2, NOW()),
(3, 2, 3, NOW())
ON DUPLICATE KEY UPDATE unlocked_at = VALUES(unlocked_at);

-- -----------------------------------------------------------------------------
-- 15) CHAT DB (chatdb)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS chatdb;
USE chatdb;

INSERT INTO chat_messages (id, sender_id, receiver_id, content, `timestamp`, status) VALUES
(1, 2, 1, 'Hi Hanen, can we confirm this week milestone?', DATE_SUB(NOW(), INTERVAL 2 DAY), 'SEEN'),
(2, 1, 2, 'Yes Omar, milestone one is ready for review.', DATE_ADD(DATE_SUB(NOW(), INTERVAL 2 DAY), INTERVAL 10 MINUTE), 'SEEN'),
(3, 3, 1, 'Admin reminder: upload final delivery note.', DATE_SUB(NOW(), INTERVAL 1 DAY), 'DELIVERED'),
(4, 1, 3, 'Acknowledged, I will upload it today.', DATE_ADD(DATE_SUB(NOW(), INTERVAL 1 DAY), INTERVAL 15 MINUTE), 'SEEN')
ON DUPLICATE KEY UPDATE content = VALUES(content), `timestamp` = VALUES(`timestamp`), status = VALUES(status);

-- =============================================================================
-- End of global seed script
-- =============================================================================
