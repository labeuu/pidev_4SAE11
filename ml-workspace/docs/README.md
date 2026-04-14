# ML Workspace

This workspace contains end-to-end machine learning assets for the Smart Freelance platform:

- Synthetic CSV generation based on `Project`, `Task`, and `Review`-inspired structures
- Three notebooks:
  - `01_project_success_classification.ipynb`
  - `02_client_satisfaction_regression.ipynb`
  - `03_client_segmentation_clustering.ipynb`
- Exported metrics and plots used by a single-page dashboard

## Folder Map

- `data/raw`: generated CSV files (`projects.csv`, `tasks.csv`, `reviews.csv`, `clients.csv`, `modeling_dataset.csv`)
- `data/processed`: cleaned dataset snapshots
- `notebooks`: ML notebooks
- `results/metrics`: metrics JSON/CSV files
- `results/plots`: generated PNG plots
- `dashboard`: static website showing all outcomes
- `src`: generation, notebook creation, and model execution scripts

## Setup

```bash
python -m pip install -r ml-workspace/requirements.txt
```

## Generate Data

```bash
python ml-workspace/src/generate_synthetic_data.py
```

## Create Notebooks

```bash
python ml-workspace/src/create_notebooks.py
```

## Produce Metrics/Plots

```bash
python ml-workspace/src/run_all_models.py
```

## Open Dashboard

Open `ml-workspace/dashboard/index.html` in a browser.

The dashboard reads files from:
- `ml-workspace/results/metrics/*.json|*.csv`
- `ml-workspace/results/plots/*.png`
