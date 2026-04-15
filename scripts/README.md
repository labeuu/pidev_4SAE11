# Repository scripts

Helpers and one-off automation kept at repo root under `scripts/`. Nothing here is required at runtime except when you choose to seed databases or configure a local GitHub token.

## setup-github-token.ps1

Writes your GitHub Personal Access Token to **`githubToken.txt`** at the **repository root** (that file is **gitignored** — never commit it). Used by the **Planning** microservice for GitHub sync when you do not use `application-local.properties`.

### Prerequisites

- PowerShell (Windows, or PowerShell Core on other OSes)
- A GitHub PAT with the scopes expected by Planning (see [credentials/README.md](../credentials/README.md) if documented there)

### Usage

From the repository root:

```powershell
.\scripts\setup-github-token.ps1
```

Or pass the token non-interactively:

```powershell
.\scripts\setup-github-token.ps1 -Token "ghp_your_token"
```

Then either export the token for the shell session or configure Planning’s `application-local.properties` (gitignored). The script prints the exact alternatives when it succeeds.

### Related docs

- [Credentials setup](../credentials/README.md)
- [Planning service](../Documentation/services/planning.md)

---

## seed-databases.sql

Inserts **10 coherent rows** into each main table across all 7 microservice databases so you can test the platform end-to-end.

### Databases and tables

| Database            | Tables seeded |
|---------------------|----------------|
| **userdb**          | `users` (10: clients, freelancers, admin) |
| **portfolio_db**    | `skills`, `experiences`, `evaluation_tests`, `evaluations` (10 each) |
| **projectdb**       | `project`, `project_application` (10 each) |
| **gestion_offre_db** | `offers`, `offer_applications` (10 each) |
| **gestion_contract_db** | `contracts`, `conflicts` (10 each) |
| **reviewdb**         | `reviews`, `review_responses` (10 each) |
| **planningdb**       | `progress_update`, `progress_comment` (10 each) |

IDs and references are aligned across services (e.g. user 1–10 in userdb, same IDs used as `client_id` / `freelancer_id` in other DBs).

### Prerequisites

1. **MySQL** running (e.g. on `localhost:3307`).
2. **Schema already created**: run each microservice at least once with `spring.jpa.hibernate.ddl-auto=update` so tables exist.
3. **Empty or resettable tables**: script does not truncate; if you run it multiple times you will get duplicate key errors unless you clear tables first.

### How to run

From the project root:

```bash
# Default (root with no password)
mysql -u root < scripts/seed-databases.sql

# With password
mysql -u root -p < scripts/seed-databases.sql

# Or open MySQL and source the file
mysql -u root -p
source c:/path/to/Smart Freelance and Project Matching Platform/scripts/seed-databases.sql
```

On Windows PowerShell you can use:

```powershell
Get-Content scripts/seed-databases.sql | mysql -u root
```

### Resetting before re-seeding

To clear data and re-run the script (optional):

```sql
-- Example for one database (repeat for each DB)
USE userdb;
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;
```

Run similar `TRUNCATE` (or `DELETE`) for each table in each database, respecting foreign key order (e.g. truncate child tables before parent).
