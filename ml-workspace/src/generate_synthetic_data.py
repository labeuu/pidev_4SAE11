import json
from pathlib import Path
import numpy as np
import pandas as pd


RANDOM_SEED = 42
N_CLIENTS = 220
N_PROJECTS = 900


def ensure_dirs(root: Path) -> None:
    for rel in [
        "data/raw",
        "data/processed",
        "results/metrics",
    ]:
        (root / rel).mkdir(parents=True, exist_ok=True)


def bounded(value, min_v, max_v):
    return max(min_v, min(max_v, value))


def generate_clients(rng: np.random.Generator) -> pd.DataFrame:
    client_ids = np.arange(1, N_CLIENTS + 1)
    client_types = rng.choice(["startup", "sme", "enterprise"], size=N_CLIENTS, p=[0.45, 0.35, 0.20])
    communication_score = rng.normal(6.8, 1.4, N_CLIENTS).clip(1, 10)
    strictness_score = rng.normal(5.5, 1.7, N_CLIENTS).clip(1, 10)
    payment_delay_days = rng.normal(7.0, 4.0, N_CLIENTS).clip(0, 30)
    repeat_hiring_rate = rng.beta(3, 2, N_CLIENTS).clip(0, 1)
    avg_budget_usd = rng.normal(6200, 2200, N_CLIENTS).clip(1200, 18000)
    project_count_history = rng.integers(3, 40, N_CLIENTS)

    clients = pd.DataFrame(
        {
            "client_id": client_ids,
            "client_type": client_types,
            "communication_score": communication_score.round(2),
            "strictness_score": strictness_score.round(2),
            "payment_delay_days": payment_delay_days.round(2),
            "repeat_hiring_rate": repeat_hiring_rate.round(3),
            "avg_budget_usd": avg_budget_usd.round(2),
            "project_count_history": project_count_history,
        }
    )
    return clients


def generate_projects_and_tasks(rng: np.random.Generator, clients: pd.DataFrame):
    categories = ["web", "mobile", "ai", "data", "design", "devops", "qa"]
    statuses = ["OPEN", "IN_PROGRESS", "COMPLETED", "CANCELLED"]
    project_rows = []
    task_rows = []
    project_id_seq = 1
    task_id_seq = 1

    for _ in range(N_PROJECTS):
        client = clients.iloc[rng.integers(0, len(clients))]
        created_day = int(rng.integers(0, 365))
        planned_duration = int(rng.integers(20, 170))
        budget = float(rng.normal(client["avg_budget_usd"], 1600))
        budget = bounded(budget, 800, 25000)
        category = rng.choice(categories)
        complexity = bounded(
            0.35 + 0.06 * categories.index(category) + float(rng.normal(0.3, 0.15)),
            0.2,
            1.0,
        )

        n_tasks = int(rng.integers(4, 26))
        complete_count = int(bounded(round(n_tasks * rng.uniform(0.35, 1.0)), 0, n_tasks))
        delayed_count = int(bounded(round(n_tasks * rng.uniform(0.0, 0.45)), 0, n_tasks))
        blocked_count = int(bounded(round(n_tasks * rng.uniform(0.0, 0.25)), 0, n_tasks))
        avg_task_delay = float(rng.normal(4 + complexity * 5 + client["strictness_score"] * 0.4, 3))
        avg_task_delay = bounded(avg_task_delay, 0, 25)
        deadline_overrun_days = bounded(float(rng.normal(avg_task_delay * 2, 10)), 0, 90)

        completion_ratio = complete_count / n_tasks if n_tasks else 0.0
        risk_signal = (
            (1 - completion_ratio) * 0.42
            + (delayed_count / n_tasks) * 0.25
            + complexity * 0.13
            + (client["strictness_score"] / 10) * 0.08
            + (deadline_overrun_days / 90) * 0.12
        )
        success_score = 1.0 - risk_signal + float(rng.normal(0.0, 0.08))
        success_flag = 1 if success_score >= 0.52 else 0

        if success_flag == 1:
            status = rng.choice(["COMPLETED", "IN_PROGRESS"], p=[0.80, 0.20])
        else:
            status = rng.choice(["IN_PROGRESS", "CANCELLED", "OPEN"], p=[0.55, 0.30, 0.15])
        if status not in statuses:
            status = "IN_PROGRESS"

        project_rows.append(
            {
                "project_id": project_id_seq,
                "client_id": int(client["client_id"]),
                "title": f"Project {project_id_seq}",
                "category": category,
                "status": status,
                "budget_usd": round(budget, 2),
                "deadline_days": planned_duration,
                "created_day_index": created_day,
                "complexity_score": round(complexity, 3),
                "task_count": n_tasks,
                "task_completed_count": complete_count,
                "task_delayed_count": delayed_count,
                "task_blocked_count": blocked_count,
                "avg_task_delay_days": round(avg_task_delay, 2),
                "deadline_overrun_days": round(deadline_overrun_days, 2),
                "completion_ratio": round(completion_ratio, 3),
                "success_flag": success_flag,
            }
        )

        for i in range(n_tasks):
            if i < complete_count:
                task_status = "DONE"
            elif i < complete_count + delayed_count:
                task_status = "DELAYED"
            elif i < complete_count + delayed_count + blocked_count:
                task_status = "BLOCKED"
            else:
                task_status = rng.choice(["TODO", "IN_PROGRESS"], p=[0.45, 0.55])

            task_rows.append(
                {
                    "task_id": task_id_seq,
                    "project_id": project_id_seq,
                    "priority": rng.choice(["LOW", "MEDIUM", "HIGH"], p=[0.2, 0.55, 0.25]),
                    "status": task_status,
                    "due_in_days": int(rng.integers(1, planned_duration + 20)),
                    "delay_days": round(float(bounded(rng.normal(avg_task_delay, 3), 0, 30)), 2),
                }
            )
            task_id_seq += 1

        project_id_seq += 1

    return pd.DataFrame(project_rows), pd.DataFrame(task_rows)


def generate_reviews(rng: np.random.Generator, projects: pd.DataFrame, clients: pd.DataFrame):
    client_map = clients.set_index("client_id").to_dict(orient="index")
    review_rows = []
    review_id_seq = 1

    for _, p in projects.iterrows():
        client = client_map[p["client_id"]]
        base_rating = (
            2.6
            + 1.8 * p["success_flag"]
            + 0.10 * client["communication_score"]
            - 0.08 * client["strictness_score"]
            - 0.04 * (p["deadline_overrun_days"] / 5)
        )
        rating = int(round(bounded(base_rating + float(rng.normal(0, 0.5)), 1, 5)))
        review_rows.append(
            {
                "review_id": review_id_seq,
                "project_id": int(p["project_id"]),
                "reviewer_id": int(p["client_id"]),
                "reviewee_id": int(rng.integers(1000, 2000)),
                "rating": rating,
                "comment_length": int(rng.integers(20, 250)),
            }
        )
        review_id_seq += 1

    return pd.DataFrame(review_rows)


def build_modeling_dataset(projects: pd.DataFrame, clients: pd.DataFrame, reviews: pd.DataFrame):
    review_agg = (
        reviews.groupby("project_id", as_index=False)
        .agg(avg_review_rating=("rating", "mean"), review_count=("review_id", "count"))
    )
    merged = (
        projects.merge(clients, on="client_id", how="left")
        .merge(review_agg, on="project_id", how="left")
    )
    merged["avg_review_rating"] = merged["avg_review_rating"].fillna(3.0)
    merged["review_count"] = merged["review_count"].fillna(0)

    sat = (
        2.8
        + 0.7 * merged["avg_review_rating"]
        + 0.02 * merged["communication_score"]
        - 0.05 * merged["payment_delay_days"]
        - 0.04 * merged["strictness_score"]
        - 0.03 * merged["deadline_overrun_days"]
        + 0.6 * merged["repeat_hiring_rate"]
        + 0.25 * merged["success_flag"]
        + np.random.default_rng(RANDOM_SEED).normal(0, 0.35, len(merged))
    )
    merged["client_satisfaction_score"] = sat.clip(1, 10).round(3)

    for col in ["payment_delay_days", "avg_task_delay_days", "communication_score", "category"]:
        idx = merged.sample(frac=0.03, random_state=RANDOM_SEED).index
        merged.loc[idx, col] = np.nan

    return merged


def save_data(root: Path, clients, projects, tasks, reviews, modeling):
    raw_dir = root / "data/raw"
    proc_dir = root / "data/processed"

    clients.to_csv(raw_dir / "clients.csv", index=False)
    projects.to_csv(raw_dir / "projects.csv", index=False)
    tasks.to_csv(raw_dir / "tasks.csv", index=False)
    reviews.to_csv(raw_dir / "reviews.csv", index=False)
    modeling.to_csv(raw_dir / "modeling_dataset.csv", index=False)

    numeric_cols = modeling.select_dtypes(include=["number"]).columns
    processed = modeling.copy()
    processed[numeric_cols] = processed[numeric_cols].fillna(processed[numeric_cols].median())
    processed.to_csv(proc_dir / "modeling_dataset_cleaned.csv", index=False)

    schema = {
        "clients": list(clients.columns),
        "projects": list(projects.columns),
        "tasks": list(tasks.columns),
        "reviews": list(reviews.columns),
        "modeling_dataset": list(modeling.columns),
    }
    (root / "results/metrics" / "data_schema.json").write_text(json.dumps(schema, indent=2), encoding="utf-8")


def main():
    root = Path(__file__).resolve().parents[1]
    ensure_dirs(root)
    rng = np.random.default_rng(RANDOM_SEED)

    clients = generate_clients(rng)
    projects, tasks = generate_projects_and_tasks(rng, clients)
    reviews = generate_reviews(rng, projects, clients)
    modeling = build_modeling_dataset(projects, clients, reviews)
    save_data(root, clients, projects, tasks, reviews, modeling)
    print("Synthetic CSV generation complete.")


if __name__ == "__main__":
    main()
