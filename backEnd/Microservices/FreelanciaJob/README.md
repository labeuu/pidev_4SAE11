# FreelanciaJob Microservice

Job board service for the Smart Freelance Platform. Clients post jobs; freelancers search, filter, and apply. Includes AI-powered job-draft generation, AI profile-fit scoring, email notifications, file attachment uploads, and job statistics.

## Overview

| Property | Value |
|---|---|
| Spring application name | `FreelanciaJob` |
| Port | **8097** |
| Database | `freelancia_job_db` (MySQL 8) |
| Eureka registration | Yes |
| Config Server | Enabled |

## Features

- **Job CRUD** — create, update, delete, and list job postings with budget ranges, deadlines, categories, required skills, and location type
- **Advanced search & filter** — keyword search, category, budget range, location type, required skill; server-side paginated `POST /jobs/filter` endpoint using JPA Specifications
- **Recommended jobs** — returns jobs matching a freelancer's skill set (Feign call to Portfolio service)
- **AI job-draft generator** — client provides a one-sentence idea; GPT returns a structured job draft (title, description, skills, budget, duration, category, location type)
- **AI profile fit score** — computes a 0-100 score with tier (`STRONG_MATCH` / `GOOD_MATCH` / `PARTIAL_MATCH` / `LOW_MATCH`), matched/missing skills, and improvement recommendations by comparing a freelancer's profile against a job
- **Job applications** — CRUD for applications with `PENDING` / `ACCEPTED` / `REJECTED` status; one application per job-freelancer pair enforced at DB level
- **File attachments** — freelancers attach up to 5 files (PDF/PNG/JPG/DOC/DOCX, max 10 MB each) when applying; files served back via download endpoint
- **Email notifications** — Thymeleaf HTML templates sent on: job posted, application submitted (to client and freelancer), application accepted
- **Job statistics** — counts by status, monthly job creation trend, per-job application stats
- **Translation** — MyMemory free API integration for on-the-fly text translation

## Tech Stack

- Java 17, Spring Boot 4.0.2
- Spring Cloud 2025.1.0 (Eureka Client, Config Client, OpenFeign, Resilience4j circuit breaker)
- Spring Data JPA + MySQL, JPA Specifications
- Spring Boot Mail + Thymeleaf (HTML email templates)
- Spring Boot Actuator (`/actuator/refresh` for live config reload)
- Lombok, Jackson, java-dotenv (for local AI key loading)

## Project Structure

```
src/main/java/tn/esprit/freelanciajob/
├── FreelanciaJobApplication.java
├── Client/
│   ├── SkillClient.java              # Feign → Portfolio service (skills by user)
│   ├── SkillClientFallback.java
│   ├── ExperienceClient.java         # Feign → Portfolio service (experiences)
│   ├── ExperienceClientFallback.java
│   ├── UserClient.java               # Feign → User service (user info for emails)
│   └── UserClientFallback.java
├── Config/
│   ├── AsyncConfig.java              # @EnableAsync thread pool
│   ├── CacheConfig.java
│   └── WebConfig.java                # Static resource handler for uploaded files
├── Controller/
│   ├── JobController.java            # /jobs endpoints
│   ├── JobApplicationController.java # /job-applications endpoints
│   └── JobStatsController.java       # /jobs/stats endpoints
├── Dto/
│   ├── request/                      # JobRequest, JobApplicationRequest, JobSearchRequest,
│   │                                 # GenerateJobRequest, TranslationRequest
│   ├── response/                     # JobResponse, JobApplicationResponse, ApplyJobResponse,
│   │                                 # AttachmentResponse, FitScoreResponse, GeneratedJobDraft,
│   │                                 # JobStatsDTO, MonthlyCount, TranslationResponse, UserDto
│   └── projection/                   # StatusCountProjection, MonthlyJobProjection
├── Entity/
│   ├── Job.java
│   ├── JobApplication.java
│   ├── ApplicationAttachment.java
│   └── Enums/
│       ├── JobStatus.java            # OPEN | CLOSED | CANCELLED
│       ├── ApplicationStatus.java    # PENDING | ACCEPTED | REJECTED
│       ├── ClientType.java           # INDIVIDUAL | COMPANY
│       └── LocationType.java         # REMOTE | ONSITE | HYBRID
├── Event/
│   ├── JobCreatedEvent.java
│   ├── ApplicationSubmittedEvent.java
│   └── ApplicationAcceptedEvent.java
├── Listener/
│   └── JobEventListener.java         # Sends emails asynchronously on events
├── Mapper/
│   └── JobMapper.java
├── Repository/
│   ├── JobRepository.java
│   ├── JobApplicationRepository.java
│   └── ApplicationAttachmentRepository.java
├── Service/
│   ├── IJobService.java / JobServiceImpl (implicit)
│   ├── IJobApplicationService.java / JobApplicationServiceImpl.java
│   ├── AiJobGeneratorService.java    # GPT-based job draft generation
│   ├── ProfileFitScoreService.java   # GPT-based fit score evaluation
│   ├── JobStatsService.java
│   ├── EmailService.java / EmailServiceImpl.java
│   ├── FileStorageService.java / FileStorageServiceImpl.java
│   └── TranslationService.java
└── Specification/
    └── JobSpecification.java         # Dynamic JPA filters for /jobs/filter
```

## REST API

### Jobs — `/jobs`

| Method | Path | Description |
|---|---|---|
| `POST` | `/jobs/add` | Create a new job posting |
| `PUT` | `/jobs/update/{id}` | Update a job |
| `DELETE` | `/jobs/{id}` | Delete a job |
| `GET` | `/jobs/list` | List all jobs |
| `GET` | `/jobs/{id}` | Get a job by ID |
| `GET` | `/jobs/client/{clientId}` | Get jobs posted by a client |
| `GET` | `/jobs/recommended?userId=` | Jobs matching a freelancer's skills |
| `POST` | `/jobs/filter` | Paginated filtered search (see below) |
| `GET` | `/jobs/search` | Quick search with query params |
| `GET` | `/jobs/statistics` | Count by status |
| `GET` | `/jobs/application-stats` | Per-job application counts |
| `GET` | `/jobs/{jobId}/fit-score?freelancerId=` | AI fit score |
| `POST` | `/jobs/generate` | AI job draft from a one-sentence prompt |
| `POST` | `/jobs/translate` | Translate text |

#### Filter request body (`POST /jobs/filter`)

```json
{
  "keyword": "React",
  "category": "Web Development",
  "budgetMin": 500,
  "budgetMax": 5000,
  "locationType": "REMOTE",
  "skillId": 12,
  "status": "OPEN",
  "page": 0,
  "size": 10,
  "sortBy": "createdAt",
  "sortDir": "desc"
}
```

All fields are optional. Returns a Spring `Page<JobResponse>` with `totalElements`, `totalPages`, etc.

#### AI job draft request (`POST /jobs/generate`)

```json
{ "prompt": "I need a mobile app that tracks daily water intake" }
```

Response includes `title`, `description`, `requiredSkills`, `budgetMin`, `budgetMax`, `currency`, `estimatedDurationWeeks`, `category`, `locationType`.

#### Fit score response (`GET /jobs/{jobId}/fit-score?freelancerId=3`)

```json
{
  "score": 78,
  "tier": "GOOD_MATCH",
  "summary": "Strong frontend skills but missing CI/CD experience.",
  "matchedSkills": ["React", "TypeScript"],
  "missingSkills": ["Docker", "GitHub Actions"],
  "recommendations": ["Add a Docker project to your portfolio"]
}
```

Tier rules: `STRONG_MATCH` 80-100 · `GOOD_MATCH` 60-79 · `PARTIAL_MATCH` 40-59 · `LOW_MATCH` 0-39

### Job Applications — `/job-applications`

| Method | Path | Description |
|---|---|---|
| `POST` | `/job-applications/add` | Apply (JSON, no files) |
| `POST` | `/job-applications/{jobId}/apply` | Apply with file attachments (multipart) |
| `PUT` | `/job-applications/update/{id}` | Update application |
| `DELETE` | `/job-applications/{id}` | Delete application |
| `GET` | `/job-applications/{id}` | Get by ID |
| `GET` | `/job-applications/list` | All applications |
| `GET` | `/job-applications/job/{jobId}` | Applications for a job |
| `GET` | `/job-applications/freelancer/{freelancerId}` | Applications by freelancer |
| `PATCH` | `/job-applications/{id}/status?value=ACCEPTED` | Update application status |
| `GET` | `/job-applications/{applicationId}/attachments` | List attachment metadata |
| `GET` | `/job-applications/attachments/{attachmentId}/download` | Download a file |

#### Apply with files (`POST /job-applications/{jobId}/apply`)

`Content-Type: multipart/form-data`

| Field | Required | Notes |
|---|---|---|
| `freelancerId` | Yes | Long |
| `proposalMessage` | Yes | Min 20 characters |
| `expectedRate` | No | Decimal |
| `availabilityStart` | No | ISO date `yyyy-MM-dd` |
| `files` | No | Up to 5 files, max 10 MB each, PDF/PNG/JPG/DOC/DOCX |

## Data Model

### `Job`

| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT PK | Auto |
| `client_id` | BIGINT | References user service |
| `client_type` | VARCHAR | `INDIVIDUAL` or `COMPANY` |
| `company_name` | VARCHAR | Populated when `clientType = COMPANY` |
| `title` | VARCHAR | Required |
| `description` | TEXT | Required |
| `budget_min` / `budget_max` | DECIMAL(15,2) | Optional |
| `currency` | VARCHAR(10) | e.g. `USD` |
| `deadline` | DATETIME | Optional |
| `category` | VARCHAR | e.g. `Web Development` |
| `location_type` | VARCHAR | `REMOTE` / `ONSITE` / `HYBRID` |
| `status` | VARCHAR | `OPEN` / `CLOSED` / `CANCELLED` (default `OPEN`) |
| `created_at` / `updated_at` | DATETIME | Auto-managed |

Required skills are stored in the `job_required_skills` join table as foreign skill IDs (integers referencing the Portfolio service).

### `JobApplication`

| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT PK | Auto |
| `job_id` | BIGINT FK | Cascade delete |
| `freelancer_id` | BIGINT | References user service |
| `proposal_message` | TEXT | Freelancer's cover letter |
| `expected_rate` | DECIMAL(15,2) | Optional |
| `availability_start` | DATE | Optional |
| `status` | VARCHAR | `PENDING` / `ACCEPTED` / `REJECTED` (default `PENDING`) |

Unique constraint on `(job_id, freelancer_id)` — one application per job per freelancer.

## Configuration

Key properties in [src/main/resources/application.properties](src/main/resources/application.properties):

| Property / Env var | Default | Description |
|---|---|---|
| `SERVER_PORT` | `8097` | Service port |
| `DB_URL` | `jdbc:mysql://localhost:3306/freelancia_job_db` | MySQL URL |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / `` | DB credentials |
| `EUREKA_URL` | `http://localhost:8420/eureka/` | Eureka endpoint |
| `CONFIG_SERVER_URL` | `http://localhost:8888` | Config Server |
| `API_KEY` | `` | AI API key (GPT) |
| `AI_API_URL` | `https://api.apifree.ai/v1/chat/completions` | AI endpoint |
| `AI_API_MODEL` | `openai/gpt-5.2` | Model name |
| `UPLOAD_DIR` | `uploads` | Root dir for file storage |
| `MAIL_USERNAME` | `iheb.ayed@gmail.com` | Gmail sender address |
| `MAIL_PASSWORD` | `` | Gmail App Password |

### Local credential setup

Create `src/main/resources/application-local.properties` (gitignored) to override secrets without committing them:

```properties
ai.api.key=sk-...
spring.mail.password=your-gmail-app-password
```

Alternatively, set `API_KEY` and `MAIL_PASSWORD` as environment variables. The service also auto-loads an `.env` file from the repo root via java-dotenv as a fallback for the AI key.

## Running

```bash
cd backEnd/Microservices/FreelanciaJob
mvn spring-boot:run
```

Prerequisites: MySQL on port 3306, Eureka on port 8420, Config Server on port 8888.

## Running Tests

```bash
mvn test
```

Tests use H2 in-memory database (`spring-boot-starter-test`, `h2` test scope). The test profile is activated via `src/test/resources/application-test.properties`.

## Inter-Service Dependencies

| Service | Feign Client | Purpose |
|---|---|---|
| Portfolio | `SkillClient` | Fetch freelancer's skills for recommended jobs and fit scoring |
| Portfolio | `ExperienceClient` | Fetch freelancer's work history for fit scoring |
| User | `UserClient` | Fetch user email/name for email notifications |

All Feign clients are protected by Resilience4j circuit breakers with empty-list fallbacks, so the service degrades gracefully when downstream services are unavailable.

## Email Notifications

Emails are sent asynchronously via `@Async` + Spring Application Events:

| Event | Template | Recipients |
|---|---|---|
| Job posted | `job-posted.html` | Client confirmation |
| Application submitted | `application-submitted.html` | Freelancer confirmation |
| Application submitted | `client-application-received.html` | Client notification |
| Application accepted | `application-accepted.html` | Freelancer notification |

Templates are in `src/main/resources/templates/email/`.

## Access via API Gateway

```
http://localhost:8078/FreelanciaJob/jobs/...
http://localhost:8078/FreelanciaJob/job-applications/...
```
