# Planning Service – External API Integration Guide

This document describes how to integrate **Google Calendar**, **Firebase Cloud Messaging (FCM)**, **Slack**, and **GitHub API** into the Planning microservice. All listed services are free to use within their quotas.

---

## Table of contents

1. [Google Calendar API](#1-google-calendar-api)
2. [Firebase Cloud Messaging (FCM)](#2-firebase-cloud-messaging-fcm)
3. [Slack API](#3-slack-api)
4. [GitHub API](#4-github-api)
5. [Configuration summary](#5-configuration-summary)
6. [Security and best practices](#6-security-and-best-practices)

---

## 1. Google Calendar API

### What it does

- Create, read, update, and delete calendar events.
- Use for: **"Next progress update due"** dates, **milestones**, or syncing key dates to a user's calendar.

### Prerequisites

- A **Google Cloud Project** (free).
- **Google Calendar API** enabled in [Google Cloud Console](https://console.cloud.google.com/apis/library/calendar-json.googleapis.com).
- **OAuth 2.0** or **Service Account** credentials:
  - **OAuth 2.0:** If users connect "my calendar" (each user's own calendar).
  - **Service account:** If you use a single shared calendar or calendar per project (server-only, no user login).

### Setup steps

1. Go to [Google Cloud Console](https://console.cloud.google.com/) → create or select a project.
2. Enable **Google Calendar API**: APIs & Services → Library → search "Google Calendar API" → Enable.
3. Create credentials:
   - **Service account (recommended for backend):** APIs & Services → Credentials → Create Credentials → Service Account. Download JSON key. Share the target Google Calendar with the service account email (e.g. `xxx@yyy.iam.gserviceaccount.com`) with "Make changes to events" permission.
   - **OAuth 2.0:** If you need user consent to access their calendar, create OAuth client ID (Web application or Desktop), set redirect URIs, and store `client_id` and `client_secret`.
4. Store credentials securely (e.g. environment variables or secret manager). Do not commit the JSON key to Git.

### Integration in Planning

- **New dependency (Maven):** Google API Client for Calendar (e.g. `com.google.api-client:google-api-client`, `google-api-services-calendar`).
- **Service class:** e.g. `GoogleCalendarService` that:
  - Loads credentials (service account or OAuth).
  - Builds a `Calendar` client.
  - Methods: `createEvent(calendarId, title, startDateTime, endDateTime, description)`, `deleteEvent(calendarId, eventId)`, `listEvents(calendarId, timeMin, timeMax)`.
- **Use cases:**
  - When a progress update is created or a "next update due" date is set → create/update a calendar event.
  - When a milestone (e.g. 100% progress) is reached → create an event or update an existing one.
- **Configuration (e.g. `application.properties`):**
  - `google.calendar.enabled=true/false`
  - `google.calendar.credentials-path` or `google.calendar.credentials-json` (base64 or path)
  - `google.calendar.default-calendar-id` (optional; for service account, use the shared calendar ID).

### Quotas and limits

- Default quota is very high (e.g. 1,000,000 requests/day). No charge; rate limits apply.

### Official docs

- [Google Calendar API overview](https://developers.google.com/calendar/api/guides/overview)
- [Java quickstart](https://developers.google.com/calendar/api/quickstart/java)

---

## 2. Firebase Cloud Messaging (FCM)

### What it does

- Sends **push notifications** to mobile apps and web (via Firebase SDK).
- Use for: notify users when a **progress update is posted**, when someone **comments**, or when a project is **stalled**.

### Prerequisites

- A **Firebase project** (free) at [Firebase Console](https://console.firebase.google.com/).
- For **Android/iOS:** app registered in Firebase; FCM SDK in the app; device tokens stored in your backend (or Firebase Auth).
- For **Web:** Firebase Web SDK; optional "request permission" and token storage in your frontend; send tokens to your backend so Planning can target them.

### Setup steps

1. Create a Firebase project (or use existing).
2. Add your app (Android, iOS, or Web) in Project settings.
3. Get **Server key** (Legacy): Project Settings → Cloud Messaging → "Server key" (for HTTP v1 you use a **service account JSON** and OAuth2 access token instead).
4. **FCM HTTP v1 (recommended):** Project Settings → Service accounts → Generate new private key. Use this JSON to obtain an OAuth2 access token and call the FCM v1 API (`https://fcm.googleapis.com/v1/projects/<projectId>/messages:send`).
5. Store the service account JSON or server key in a secure way (env or secret manager). Do not commit to Git.

### Integration in Planning

- **No mandatory Maven dependency for FCM itself:** use **Spring's `RestTemplate`** or **WebClient** to send HTTP requests to FCM v1 API. For **OAuth2** to get the access token, use Google's libraries (e.g. `google-auth-library-oauth2-http`) or a small HTTP call to get a token from the service account.
- **Service class:** e.g. `FcmNotificationService` with a method `sendNotification(String token, String title, String body, Map<String, String> data)` that:
  - Builds the FCM v1 payload (e.g. `message.token`, `message.notification`, `message.data`).
  - Gets an access token (cached) and calls FCM with `Authorization: Bearer <token>`.
- **When to call:**
  - After creating a progress update → notify project owner or related users (if you have their FCM tokens).
  - After creating a comment → notify the freelancer or comment thread participants.
  - From a scheduled job or API that lists "stalled projects" → send a reminder notification (e.g. "No progress update in 7 days").
- **Configuration:**
  - `fcm.enabled=true/false`
  - `fcm.credentials-path` or `fcm.credentials-json` (service account for FCM v1)
  - Optional: `fcm.default-notification-channel` (Android).

### Quotas and limits

- Free; high throughput. No payment.

### Official docs

- [FCM HTTP v1](https://firebase.google.com/docs/cloud-messaging/http-server-ref)
- [Send to device (token)](https://firebase.google.com/docs/cloud-messaging/send-message)

---

## 3. Slack API

### What it does

- Post messages to a **channel**, **user**, or **DM**.
- Optional: **slash commands** or **incoming webhooks** for simple "post progress" from Slack.
- Use for: post **progress update alerts** or **stalled project** reminders to a dedicated Slack channel.

### Prerequisites

- A **Slack workspace** (free tier is enough).
- Either:
  - **Incoming Webhook:** simple "post to one channel" (no OAuth).
  - **Slack App (Bot):** more control (post as bot, optional slash commands). Create an app at [api.slack.com/apps](https://api.slack.com/apps).

### Setup steps (Incoming Webhook – simplest)

1. Slack workspace → Apps → "Build" or "Create App" → "From scratch".
2. Name the app (e.g. "Planning Alerts"), choose workspace.
3. In the app: **Incoming Webhooks** → On → **Add New Webhook to Workspace** → select channel (e.g. `#planning-alerts`) → copy the **Webhook URL** (e.g. `https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXX`).
4. Store the Webhook URL in configuration (e.g. `slack.webhook.url`). Do not commit to Git.

### Setup steps (Slack App with Bot – more flexible)

1. Create app at [api.slack.com/apps](https://api.slack.com/apps).
2. **OAuth & Permissions:** Scopes → Bot Token Scopes: e.g. `chat:write`, `channels:read` (if needed). Install to workspace and copy **Bot User OAuth Token** (starts with `xoxb-`).
3. Store token in configuration (e.g. `slack.bot-token`).

### Integration in Planning

- **No special Maven dependency required:** use **RestTemplate** or **WebClient** to send JSON to:
  - Incoming Webhook: `POST <webhook_url>` with body `{ "text": "..." }` or rich blocks.
  - Chat API: `POST https://slack.com/api/chat.postMessage` with `Authorization: Bearer <bot-token>` and `channel` (e.g. channel ID).
- **Service class:** e.g. `SlackNotificationService` with a method `postToChannel(String message)` or `postProgressUpdate(ProgressUpdate update)` that formats a short message and sends it.
- **When to call:**
  - New progress update (e.g. "Project X: 75% – Phase 1 done").
  - Stalled project alert (e.g. "Project Y has had no progress update in 7 days").
  - Optional: when a comment is added (if you want comments in Slack).
- **Configuration:**
  - `slack.enabled=true/false`
  - `slack.webhook.url` (for Incoming Webhook), **or** `slack.bot-token` and `slack.default-channel-id` (for Bot).

### Quotas and limits

- Free workspace limits (message history, etc.). API rate limits apply (e.g. tier 2); no payment for typical usage.

### Official docs

- [Incoming Webhooks](https://api.slack.com/messaging/webhooks)
- [chat.postMessage](https://api.slack.com/methods/chat.postMessage)

---

## 4. GitHub API

### What it does

- Read **repositories**, **commits**, **branches**, **issues** (and optionally create issues).
- Use for: **link a progress update to a repo/branch/commit**; show "last commit" or "branch" in the Planning UI; optional "create GitHub issue" when a milestone is missed.

### Prerequisites

- **GitHub account** (free).
- For **public repos:** no token needed for read (subject to stricter rate limits). For **private repos** or higher limits: **Personal Access Token (PAT)** or **GitHub App**.
- Create PAT: GitHub → Settings → Developer settings → Personal access tokens → Generate (e.g. scope `repo` for private repos, or `public_repo` only).

### Setup steps

1. Create a **Personal Access Token** (classic) or **Fine-grained token** with minimal scopes (e.g. read repo, read commit status).
2. Store token in configuration (e.g. `github.token`). Do not commit to Git.

### Integration in Planning

- **No mandatory Maven dependency:** use **RestTemplate** or **WebClient** to call `https://api.github.com/...`. Optional: use a small client like **OkHttp** or **GitHub API Java** libraries if you prefer.
- **Endpoints useful for Planning:**
  - `GET /repos/{owner}/{repo}/commits` – list commits (e.g. last N).
  - `GET /repos/{owner}/{repo}/branches` – list branches.
  - `GET /repos/{owner}/{repo}/commits/{ref}` – single commit (e.g. by branch or SHA).
  - `POST /repos/{owner}/{repo}/issues` – create issue (optional; e.g. "Stalled project" auto-issue).
- **Service class:** e.g. `GitHubApiService` with methods:
  - `getLatestCommit(String owner, String repo, String branch)` – returns last commit info (SHA, message, date).
  - `getBranches(String owner, String repo)` – returns branch names/refs.
  - Optional: `createIssue(String owner, String repo, String title, String body)`.
- **Data model:** If you want to "link" a progress update to GitHub, add optional fields (e.g. `repositoryOwner`, `repositoryName`, `branch`, `commitSha`) to the progress update entity or a separate "link" table. The Planning API can accept these when creating/updating an update and display them in the UI; optionally call GitHub to enrich (e.g. last commit message).
- **When to call:**
  - When displaying a progress update that has repo/branch → call GitHub to show last commit or branch list.
  - Optional: cron or scheduled job for "stalled projects" → create a GitHub issue in a designated repo.
- **Configuration:**
  - `github.enabled=true/false`
  - `github.token` (optional; for private repos or higher rate limit)
  - Optional: `github.default-repo` (owner/repo for creating issues).

### Quotas and limits

- Unauthenticated: 60 requests/hour. With PAT: 5,000 requests/hour. Free.

### Official docs

- [REST API](https://docs.github.com/en/rest)
- [Authentication](https://docs.github.com/en/rest/overview/authenticating-to-the-rest-api)

---

## 5. Configuration summary

Example placeholders for `application.properties` or environment variables:

```properties
# Google Calendar
google.calendar.enabled=false
google.calendar.credentials-path=
google.calendar.default-calendar-id=

# FCM
fcm.enabled=false
fcm.credentials-path=

# Slack
slack.enabled=false
slack.webhook.url=
# OR slack.bot-token= and slack.default-channel-id=

# GitHub
github.enabled=false
github.token=
```

- Use `enabled` flags so the app runs without credentials; enable when you configure each integration.
- Prefer environment variables or a secret manager for tokens and keys in production.

---

## 6. Security and best practices

- **Secrets:** Never commit API keys, tokens, or service account JSON to Git. Use env vars, Spring Cloud Config, or a secret manager.
- **Fail gracefully:** If Google Calendar, FCM, Slack, or GitHub is down or misconfigured, log and skip the integration; do not fail the main flow (e.g. progress update creation).
- **Async:** Consider sending FCM, Slack, and (if needed) Calendar updates **asynchronously** (e.g. `@Async` or a message queue) so the HTTP request that creates the progress update returns quickly.
- **Rate limits:** Respect GitHub and Slack rate limits; add simple in-memory throttling or retry with backoff if you call them frequently.
- **Scopes:** Request minimal OAuth/scopes and minimal token permissions (e.g. GitHub: only needed scopes; Slack: only `chat:write` if you only post).
