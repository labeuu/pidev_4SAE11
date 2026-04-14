# Preprocessing Guide

This guide explains cleaning and transformation choices used across notebooks.

## 1) Missing Values

- Numerical features: median imputation (`SimpleImputer(strategy="median")`)
- Categorical features: mode imputation (`SimpleImputer(strategy="most_frequent")`)

Reason:
- Median is robust to outliers.
- Most frequent is simple and stable for low/medium-cardinality categories.

## 2) Encoding

- Categorical columns are transformed using one-hot encoding:
  - `OneHotEncoder(handle_unknown="ignore")`

Reason:
- Maintains full category information
- Avoids ordinal assumptions
- Handles unseen values during inference

## 3) Normalization and Standardization

Regression notebook:
- Numeric pipeline includes:
  - `MinMaxScaler` (normalization to [0,1])
  - followed by `StandardScaler` (zero mean / unit variance)

Classification/clustering:
- Numeric pipeline uses `StandardScaler` directly.

Reason:
- Linear models benefit from normalized/scaled magnitudes.
- Distance-based methods (e.g., KMeans) need standardized scales to avoid dominance by high-range features.

## 4) Outlier Handling

- Regression notebook applies IQR clipping on sensitive features:
  - `deadline_overrun_days`
  - `avg_task_delay_days`
  - `budget_usd`

Reason:
- Reduces extreme-value instability while preserving most observations.

## 5) Leakage Prevention

- Split train/test before fitting model-specific transformations.
- Use `Pipeline` + `ColumnTransformer` so preprocessing is fit only on training data.
- Apply same fitted transformations to test data automatically.

## 6) Reproducibility

- Fixed random seed (`42`) is used for data generation and model splits.
- Exported metrics/plots are regenerated deterministically from scripts.
