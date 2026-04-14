# Dashboard Guide

The dashboard is a static single page located in `ml-workspace/dashboard/index.html`.

## Data Sources

The page reads:

- Rapport de validation (texte structuré + placeholders métriques) :
  - `results/metrics/validation_report.json` (à ajuster si les objectifs ou colonnes changent)
  - `results/metrics/data_schema.json` (liste des variables pour le rapport)
- Metrics:
  - `results/metrics/classification_metrics.csv`
  - `results/metrics/regression_metrics.csv`
  - `results/metrics/clustering_metrics.csv`
  - `results/metrics/dashboard_summary.json`
- Plots:
  - `results/plots/*.png`

## Refresh Workflow

1. Regenerate data if needed:
   - `python ml-workspace/src/generate_synthetic_data.py`
2. Recompute model outputs:
   - `python ml-workspace/src/run_all_models.py`
3. Reload `dashboard/index.html`.

No manual edits are required for metrics/plot updates. After changing datasets or business objectives, update `validation_report.json` accordingly. Figures `preprocessing_missing_values.png` and `preprocessing_scaling_effect.png` are produced by `run_all_models.py`.

## What the Dashboard Shows

- **Rapport de validation (sections 1 à 6)** : grille MLA (introduction, tableau BO/DSO/données, variables et pré-traitement avec figures, modélisation, métriques et interprétation obligatoire, benchmarking) puis **Annexes** (métriques détaillées et graphiques). Export PDF : bouton *Exporter PDF (impression)* → *Enregistrer au format PDF*.
- Classification section:
  - metric table per algorithm
  - cross-validation score columns (`cross_validation_f1_mean`, `cross_validation_f1_std`)
  - confusion matrix images
  - ROC curves image
  - Precision-Recall curves image (`PR-AUC` included in metrics table)
- Regression section:
  - metric table per algorithm
  - predicted-vs-actual plots per algorithm
  - residual diagnostics per algorithm
- Clustering section:
  - metric table per algorithm
  - cluster projection plots
  - KMeans elbow/silhouette curve figure
- Global best-model summary cards
