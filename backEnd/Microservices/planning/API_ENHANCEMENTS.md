# Planning Service – API Enhancements Summary

This document lists APIs that would enhance the Planning microservice. Use it as a backlog for future improvements.

---

## 1. Bulk / Summary APIs

| API | Purpose |
|-----|--------|
| **GET /api/progress-updates/summary** (or **/api/progress-updates/stats/summary**) | Return lightweight summary for **multiple** projects/contracts in one call (e.g. `?projectIds=1,2,3` or `?contractIds=1,2`). Response: list of `{ projectId/contractId, currentProgress%, lastUpdateAt }`. Reduces round-trips for project lists or dashboards. |
| **GET /api/progress-updates/latest** | For a project (or freelancer): return only the **latest** progress update (current % and date). Useful for “current status” badges without loading full history. |

---

## 2. Health / Readiness for Planning

| API | Purpose |
|-----|--------|
| **GET /api/planning/health** or **GET /actuator/health** (with planning-specific details) | Expose that Planning is up and optionally that it can reach DB/Eureka. Helps CI/CD and API Gateway health checks. |

---

## 3. Comments and UX

| API | Purpose |
|-----|--------|
| **GET /api/progress-comments?userId=** (or **/by-user/{userId}**) | List comments **by commenter** (userId). Supports “all my comments” or moderation. |
| **PATCH /api/progress-comments/{id}** | Partial update (e.g. only `message`). Keeps compatibility if you later add more fields. |
| **GET /api/progress-updates/{id}/with-comments** (or include comments in GET by id) | Return one progress update **with its comments** in one call. Avoids separate call to `/progress-comments/progress-update/{id}`. |

---

## 4. Export and Reporting

| API | Purpose |
|-----|--------|
| **GET /api/progress-updates/export** (e.g. CSV/Excel) | Same filters as the list API, but return **export** (e.g. `Accept: text/csv` or `?format=csv`). Useful for client reports and audits. |
| **GET /api/progress-updates/stats/report?from=&to=&projectId=** | Time-bounded **report** (e.g. progress over period, number of updates, comment count). Complements existing stats with an explicit “report” endpoint. |

---

## 5. Notifications and Deadlines

| API | Purpose |
|-----|--------|
| **GET /api/progress-updates/due-or-overdue** (or **/reminders**) | Projects/contracts with **no progress update** in X days or past a “next update due” date (if you add such a field). Feeds reminder/notification features. |
| **POST /api/progress-updates/{id}/remind** (optional) | Mark “reminder sent” for an update (if you track that). Keeps notification logic out of other services. |

---

## 6. Validation and Consistency

| API | Purpose |
|-----|--------|
| **GET /api/progress-updates/next-allowed-percentage?projectId=** | Return the **minimum allowed** progress % for the next update (your “cannot decrease” rule). Frontend can pre-fill or validate before submitting. |
| **POST /api/progress-updates/validate** | Request body same as create/update; response: `{ valid, minAllowed, errors }` without persisting. Safe client-side validation aligned with backend rules. |

---

## 7. Multi-Entity Views

| API | Purpose |
|-----|--------|
| **GET /api/progress-updates/contracts/summary?contractIds=1,2,3** | For contract list/dashboard: current progress and last update per contract in one call. |
| **GET /api/progress-updates/freelancer/{freelancerId}/projects-summary** | Per freelancer: list of projects they have updates on, with latest % and date. Good for “my active work” views. |

---

## 8. Pagination and Performance

| API | Purpose |
|-----|--------|
| **GET /api/progress-comments** with **pagination** (`page`, `size`) | You currently have “list all comments”. Adding pagination (and optional sort) avoids large payloads and improves performance. |

---

## Recommended Priorities (Highest Impact First)

1. **Summary/bulk** – e.g. **GET summary for multiple projects/contracts** and **GET latest** per project/freelancer.
2. **Validation** – **GET next-allowed-percentage** and/or **POST validate** for progress.
3. **Comments** – **GET comments by user**, **GET progress-update with comments**, **paginated list comments**.
4. **Export** – **GET export** (CSV/Excel) with same filters as list.
5. **Reminders** – **GET due-or-overdue** (and optionally **POST remind**) for notifications.

---

*Document created for reference; update as APIs are implemented.*
