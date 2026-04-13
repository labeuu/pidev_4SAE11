# Data Dictionary

## `clients.csv`

- `client_id`: unique client identifier
- `client_type`: startup/sme/enterprise
- `communication_score`: communication quality score (1-10)
- `strictness_score`: strictness level score (1-10)
- `payment_delay_days`: average payment delay in days
- `repeat_hiring_rate`: repeat hiring ratio (0-1)
- `avg_budget_usd`: average historical budget
- `project_count_history`: historical number of projects

## `projects.csv`

- `project_id`: unique project identifier
- `client_id`: owner client id
- `title`: synthetic project title
- `category`: project domain
- `status`: OPEN/IN_PROGRESS/COMPLETED/CANCELLED
- `budget_usd`: project budget
- `deadline_days`: planned duration
- `created_day_index`: synthetic created date index
- `complexity_score`: complexity proxy (0-1)
- `task_count`: total tasks
- `task_completed_count`: completed tasks count
- `task_delayed_count`: delayed tasks count
- `task_blocked_count`: blocked tasks count
- `avg_task_delay_days`: mean delay per task
- `deadline_overrun_days`: overrun duration
- `completion_ratio`: completed tasks ratio
- `success_flag`: target for classification (0/1)

## `tasks.csv`

- `task_id`: unique task identifier
- `project_id`: parent project id
- `priority`: LOW/MEDIUM/HIGH
- `status`: TODO/IN_PROGRESS/DONE/DELAYED/BLOCKED
- `due_in_days`: due horizon
- `delay_days`: realized delay

## `reviews.csv`

- `review_id`: unique review id
- `project_id`: reviewed project
- `reviewer_id`: client reviewer id
- `reviewee_id`: freelancer id
- `rating`: star rating (1-5)
- `comment_length`: review text length proxy

## `modeling_dataset.csv`

Join of project + client + review aggregates with derived ML features.

Includes both targets:
- `success_flag` for classification
- `client_satisfaction_score` for regression
