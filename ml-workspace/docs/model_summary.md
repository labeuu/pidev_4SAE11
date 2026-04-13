# Model Summary

This document summarizes current results exported by `src/run_all_models.py`.

## Classification (`success_flag`)

Compared algorithms:
- `gradient_boosting_classifier`
- `logistic_regression`
- `random_forest_classifier`

Best by F1:
- `gradient_boosting_classifier`
- Accuracy: `0.9747`
- Precision: `1.0000`
- Recall: `0.9688`
- F1: `0.9841`
- ROC-AUC: `0.9980`

## Regression (`client_satisfaction_score`)

Compared algorithms:
- `linear_regression`
- `gradient_boosting_regressor`
- `random_forest_regressor`

Best by R2:
- `linear_regression`
- MAE: `0.3008`
- RMSE: `0.3834`
- R2: `0.8583`

## Clustering (client segmentation)

Compared algorithms:
- `kmeans`
- `agglomerative`
- `dbscan`

Best by silhouette:
- `kmeans`
- Silhouette: `0.0901`
- Calinski-Harabasz: `21.1607`
- Davies-Bouldin: `2.4273`

DBSCAN note:
- Current parameters produced all noise points on this synthetic version (`noise_ratio = 1.0`).
- This can be improved with alternate feature scaling and `eps/min_samples` tuning.

## Limitations

- Data is synthetic and schema-driven, not production telemetry.
- Results should be interpreted as pipeline validation, not business truth.
- Hyperparameter optimization is intentionally lightweight in this phase.
