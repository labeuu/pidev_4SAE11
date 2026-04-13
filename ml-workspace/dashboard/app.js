function parseCsv(text) {
  const lines = text.trim().split("\n");
  const headers = lines[0].split(",");
  return lines.slice(1).map((line) => {
    const values = line.split(",");
    const row = {};
    headers.forEach((h, i) => {
      const v = values[i];
      const n = Number(v);
      row[h] = Number.isNaN(n) || v === "" ? v : n;
    });
    return row;
  });
}

function fmt(n, digits = 4) {
  if (typeof n !== "number" || Number.isNaN(n)) return String(n);
  return n.toFixed(digits);
}

function pct(n) {
  if (typeof n !== "number" || Number.isNaN(n)) return String(n);
  return `${(n * 100).toFixed(2)} %`;
}

function buildPlaceholders(summary) {
  const c = summary.classification_best;
  const r = summary.regression_best;
  const cl = summary.clustering_best;
  return {
    "{{CLS_BEST_ALGO}}": c.algorithm,
    "{{CLS_BEST_F1}}": fmt(c.f1),
    "{{CLS_BEST_ROC}}": fmt(c.roc_auc),
    "{{CLS_BEST_CV_F1}}": fmt(c.cross_validation_f1_mean),
    "{{CLS_BEST_CV_STD}}": fmt(c.cross_validation_f1_std),
    "{{REG_BEST_ALGO}}": r.algorithm,
    "{{REG_BEST_R2}}": fmt(r.r2),
    "{{REG_BEST_RMSE}}": fmt(r.rmse),
    "{{REG_BEST_MAE}}": fmt(r.mae),
    "{{CLU_BEST_ALGO}}": cl.algorithm,
    "{{CLU_BEST_SIL}}": fmt(cl.silhouette),
    "{{CLU_BEST_K}}": String(cl.num_clusters),
    "{{CLU_NOISE}}": pct(cl.noise_ratio),
  };
}

function interpolate(template, placeholders) {
  if (!template) return "";
  return template.replace(/\{\{([^}]+)\}\}/g, (match, key) => {
    const k = `{{${key}}}`;
    return Object.prototype.hasOwnProperty.call(placeholders, k) ? placeholders[k] : match;
  });
}

function escapeHtml(s) {
  return String(s)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function explanatoryFeatures(schema, task) {
  const name = (task.name || "").toLowerCase();
  if (name.includes("clustering") && task.exclude_from_features?.length === 1 && task.exclude_from_features[0] === "client_id") {
    return (schema.clients || []).filter((c) => c !== "client_id");
  }
  const cols = schema.modeling_dataset || [];
  return cols.filter((c) => !(task.exclude_from_features || []).includes(c));
}

function renderFeatureTable(features) {
  if (!features.length) return "<p><em>Aucune colonne listée.</em></p>";
  const cols = 3;
  const rows = [];
  for (let i = 0; i < features.length; i += cols) {
    const chunk = features.slice(i, i + cols);
    const tds = chunk.map((f) => `<td><code>${escapeHtml(f)}</code></td>`).join("");
    const pad = cols - chunk.length;
    rows.push(`<tr>${tds}${pad > 0 ? `<td colspan="${pad}"></td>` : ""}</tr>`);
  }
  return `<table class="feature-grid"><tbody>${rows.join("")}</tbody></table>`;
}

function setupPdfExport() {
  const btn = document.getElementById("export-pdf-btn");
  if (!btn) return;
  btn.addEventListener("click", () => {
    globalThis.print();
  });
}

function renderTable(targetId, rows) {
  const el = document.getElementById(targetId);
  if (!rows.length) {
    el.innerHTML = "<p>No data available.</p>";
    return;
  }
  const headers = Object.keys(rows[0]);
  const thead = `<tr>${headers.map((h) => `<th>${h}</th>`).join("")}</tr>`;
  const tbody = rows
    .map(
      (r) =>
        `<tr>${headers
          .map((h) => `<td>${typeof r[h] === "number" ? r[h].toFixed(4) : r[h]}</td>`)
          .join("")}</tr>`
    )
    .join("");
  el.innerHTML = `<table><thead>${thead}</thead><tbody>${tbody}</tbody></table>`;
}

function renderPlots(targetId, items) {
  const container = document.getElementById(targetId);
  container.innerHTML = items
    .map(
      (it) => `
      <article class="plot-card">
        <h4>${it.title}</h4>
        <img src="${it.src}" alt="${it.title}" loading="lazy">
      </article>
    `
    )
    .join("");
}

function renderSummary(summary) {
  const card = (title, obj) => `
    <article class="card">
      <h3>${title}</h3>
      ${Object.entries(obj)
        .map(([k, v]) => `<div><strong>${k}:</strong> ${typeof v === "number" ? v.toFixed(4) : v}</div>`)
        .join("")}
    </article>`;
  document.getElementById("summary-cards").innerHTML =
    card("Best Classification", summary.classification_best) +
    card("Best Regression", summary.regression_best) +
    card("Best Clustering", summary.clustering_best);
}

function renderBenchmarkRecap(_summary, ph) {
  return `
    <table class="benchmark-recap">
      <thead>
        <tr><th>Tâche</th><th>Modèle retenu (score)</th><th>Métriques clés</th></tr>
      </thead>
      <tbody>
        <tr>
          <td>Classification (succès projet)</td>
          <td><code>${ph["{{CLS_BEST_ALGO}}"]}</code></td>
          <td>F1 = ${ph["{{CLS_BEST_F1}}"]}, ROC-AUC = ${ph["{{CLS_BEST_ROC}}"]}, F1 CV = ${ph["{{CLS_BEST_CV_F1}}"]} ± ${ph["{{CLS_BEST_CV_STD}}"]}</td>
        </tr>
        <tr>
          <td>Régression (satisfaction)</td>
          <td><code>${ph["{{REG_BEST_ALGO}}"]}</code></td>
          <td>R² = ${ph["{{REG_BEST_R2}}"]}, RMSE = ${ph["{{REG_BEST_RMSE}}"]}, MAE = ${ph["{{REG_BEST_MAE}}"]}</td>
        </tr>
        <tr>
          <td>Clustering (clients)</td>
          <td><code>${ph["{{CLU_BEST_ALGO}}"]}</code></td>
          <td>Silhouette = ${ph["{{CLU_BEST_SIL}}"]}, k = ${ph["{{CLU_BEST_K}}"]}, bruit (DBSCAN ref.) = ${ph["{{CLU_NOISE}}"]}</td>
        </tr>
      </tbody>
    </table>`;
}

function sortRowsBy(rows, key, descending = true) {
  return [...rows].sort((a, b) => {
    let va = a[key];
    let vb = b[key];
    const na = typeof va !== "number" || Number.isNaN(va);
    const nb = typeof vb !== "number" || Number.isNaN(vb);
    if (na && nb) return 0;
    if (na) va = descending ? Number.NEGATIVE_INFINITY : Number.POSITIVE_INFINITY;
    if (nb) vb = descending ? Number.NEGATIVE_INFINITY : Number.POSITIVE_INFINITY;
    return descending ? vb - va : va - vb;
  });
}

function formatMetricCell(headerKey, value) {
  if (value === "" || value === null || value === undefined) return "—";
  if (typeof value === "number" && Number.isNaN(value)) return "—";
  if (typeof value !== "number") return escapeHtml(String(value));
  if (headerKey === "noise_ratio") return pct(value);
  return fmt(value, 4);
}

/**
 * Full algorithm × metrics table for PDF (section 6).
 * @param {object[]} rows
 * @param {string[]} columnOrder
 * @param {Record<string, string>} headerLabels
 * @param {string} bestAlgorithm - value of `algorithm` column for highlight
 */
function renderPdfComparisonTable(rows, columnOrder, headerLabels, bestAlgorithm) {
  if (!rows.length) return "<p><em>Aucune ligne métrique.</em></p>";
  const thead = `<tr>${columnOrder.map((h) => `<th>${escapeHtml(headerLabels[h] || h)}</th>`).join("")}</tr>`;
  const tbody = rows
    .map((row, idx) => {
      const isBest = row.algorithm === bestAlgorithm;
      const trc = isBest ? ' class="metric-row-best"' : "";
      const tds = columnOrder
        .map((h) => {
          const cell = h === "rang" ? String(idx + 1) : formatMetricCell(h, row[h]);
          return `<td>${cell}</td>`;
        })
        .join("");
      return `<tr${trc}>${tds}</tr>`;
    })
    .join("");
  return `<table class="pdf-metrics-table"><thead>${thead}</thead><tbody>${tbody}</tbody></table>`;
}

function buildDetailedComparisonBlock(clsRows, regRows, cluRows, summary) {
  const clsSorted = sortRowsBy(clsRows, "f1", true);
  const regSorted = sortRowsBy(regRows, "r2", true);
  const cluSorted = sortRowsBy(cluRows, "silhouette", true);

  const clsCols = ["rang", "algorithm", "accuracy", "precision", "recall", "f1", "roc_auc", "pr_auc", "cross_validation_f1_mean", "cross_validation_f1_std"];
  const clsLabels = {
    rang: "Rang",
    algorithm: "Algorithme",
    accuracy: "Exactitude",
    precision: "Précision",
    recall: "Rappel",
    f1: "F1",
    roc_auc: "ROC-AUC",
    pr_auc: "PR-AUC",
    cross_validation_f1_mean: "F1 CV (moy.)",
    cross_validation_f1_std: "F1 CV (σ)",
  };

  const regCols = ["rang", "algorithm", "mae", "rmse", "r2"];
  const regLabels = {
    rang: "Rang",
    algorithm: "Algorithme",
    mae: "MAE",
    rmse: "RMSE",
    r2: "R²",
  };

  const cluCols = ["rang", "algorithm", "num_clusters", "noise_ratio", "silhouette", "calinski_harabasz", "davies_bouldin"];
  const cluLabels = {
    rang: "Rang",
    algorithm: "Algorithme",
    num_clusters: "Clusters (k)",
    noise_ratio: "Part bruit",
    silhouette: "Silhouette",
    calinski_harabasz: "Calinski–Harabasz",
    davies_bouldin: "Davies–Bouldin",
  };

  const clsTable = renderPdfComparisonTable(clsSorted, clsCols, clsLabels, summary.classification_best.algorithm);
  const regTable = renderPdfComparisonTable(regSorted, regCols, regLabels, summary.regression_best.algorithm);
  const cluTable = renderPdfComparisonTable(cluSorted, cluCols, cluLabels, summary.clustering_best.algorithm);

  const crossRows = [
    {
      task: "Classification",
      crit: "F1 (test)",
      best: summary.classification_best.algorithm,
      v1: fmt(clsSorted[0]?.f1),
      v2: fmt(clsSorted[1]?.f1),
      v3: fmt(clsSorted[2]?.f1),
      a1: clsSorted[0]?.algorithm,
      a2: clsSorted[1]?.algorithm,
      a3: clsSorted[2]?.algorithm,
    },
    {
      task: "Régression",
      crit: "R² (test)",
      best: summary.regression_best.algorithm,
      v1: fmt(regSorted[0]?.r2),
      v2: fmt(regSorted[1]?.r2),
      v3: fmt(regSorted[2]?.r2),
      a1: regSorted[0]?.algorithm,
      a2: regSorted[1]?.algorithm,
      a3: regSorted[2]?.algorithm,
    },
    {
      task: "Clustering",
      crit: "Silhouette",
      best: summary.clustering_best.algorithm,
      v1: fmt(cluSorted[0]?.silhouette),
      v2: fmt(cluSorted[1]?.silhouette),
      v3: fmt(cluSorted[2]?.silhouette),
      a1: cluSorted[0]?.algorithm,
      a2: cluSorted[1]?.algorithm,
      a3: cluSorted[2]?.algorithm,
    },
  ];

  const crossBody = crossRows
    .map(
      (r) => `
    <tr>
      <td>${escapeHtml(r.task)}</td>
      <td>${escapeHtml(r.crit)}</td>
      <td><code>${escapeHtml(r.best)}</code></td>
      <td>1. <code>${escapeHtml(r.a1 || "—")}</code><br><strong>${escapeHtml(r.v1)}</strong></td>
      <td>2. <code>${escapeHtml(r.a2 || "—")}</code><br>${escapeHtml(r.v2)}</td>
      <td>3. <code>${escapeHtml(r.a3 || "—")}</code><br>${escapeHtml(r.v3)}</td>
    </tr>`
    )
    .join("");

  return `
    <h3 class="rubric-h2">Tableaux comparatifs détaillés (tous algorithmes)</h3>
    <p class="muted pdf-table-note">Classement par métrique principale ; la ligne du modèle retenu dans <code>dashboard_summary.json</code> est surlignée.</p>

    <h4 class="pdf-subtable-title">Classification — performances sur le jeu de test (+ validation croisée F1 sur le train)</h4>
    ${clsTable}

    <h4 class="pdf-subtable-title">Régression — erreur et coefficient de détermination</h4>
    ${regTable}

    <h4 class="pdf-subtable-title">Clustering — qualité de partition et structure</h4>
    ${cluTable}

    <h3 class="rubric-h2">Vue transversale — podium par tâche</h3>
    <table class="pdf-metrics-table pdf-cross-table">
      <thead>
        <tr>
          <th>Tâche</th>
          <th>Critère de classement</th>
          <th>Modèle retenu</th>
          <th>1<sup>er</sup></th>
          <th>2<sup>e</sup></th>
          <th>3<sup>e</sup></th>
        </tr>
      </thead>
      <tbody>${crossBody}</tbody>
    </table>`;
}

function renderValidationReport(report, schema, summary, clsRows, regRows, cluRows) {
  const el = document.getElementById("validation-report");
  if (!el || !report) return;

  const ph = buildPlaceholders(summary);
  const s1 = report.section_1_introduction;
  const s2 = report.section_2_business_table;
  const s3 = report.section_3_data;
  const s4 = report.section_4_modeling;
  const s5 = report.section_5_evaluation;
  const s6 = report.section_6_benchmarking;

  const boHeader = s2.columns.map((c) => `<th>${escapeHtml(c)}</th>`).join("");
  const boRows = s2.rows
    .map(
      (row) =>
        `<tr><td>${escapeHtml(row.bo)}</td><td>${escapeHtml(row.dso)}</td><td>${escapeHtml(row.datasets)}</td></tr>`
    )
    .join("");

  const tasksHtml = s3.tasks
    .map((task, idx) => {
      const feats = explanatoryFeatures(schema, task);
      const prep = (task.preprocessing_points || []).map((p) => `<li>${escapeHtml(p)}</li>`).join("");
      return `
        <section class="rubric-subblock">
          <h3 class="rubric-h2">${escapeHtml(task.name)}</h3>
          <p><strong>Variable cible :</strong> ${escapeHtml(task.target)}</p>
          <p><strong>Variables explicatives (liste opérationnelle) :</strong></p>
          ${renderFeatureTable(feats)}
          <p><strong>Pré-traitement (bloc ${idx + 1}) :</strong></p>
          <ol class="numbered-sub">${prep}</ol>
        </section>`;
    })
    .join("");

  const visuals = (s3.visual_proof_images || [])
    .map(
      (img) => `
      <figure class="report-figure">
        <img src="${escapeHtml(img.src)}" alt="${escapeHtml(img.caption)}" loading="lazy" onerror="this.style.display='none'">
        <figcaption>${escapeHtml(img.caption)}</figcaption>
      </figure>`
    )
    .join("");

  const modelsHtml = (s4.models || [])
    .map(
      (m) => `
      <article class="model-card">
        <h4>${escapeHtml(m.label)}</h4>
        <p><strong>Choix / pertinence :</strong> ${escapeHtml(m.rationale)}</p>
        <p><strong>Références théoriques :</strong> ${escapeHtml(m.theoretical_notes)}</p>
        <p><strong>Complexité (ordre de grandeur) :</strong> ${escapeHtml(m.complexity_notes)}</p>
      </article>`
    )
    .join("");

  const metricsHtml = (s5.metric_choices || [])
    .map((m) => `<li><strong>${escapeHtml(m.metric)} :</strong> ${escapeHtml(m.justification)}</li>`)
    .join("");

  const interp = interpolate(s5.interpretation_template, ph);

  const deployHtml = (s6.deployment_choices || [])
    .map(
      (d) => `
      <li>
        <strong>${escapeHtml(d.task)} :</strong> modèle retenu <code>${escapeHtml(interpolate(d.chosen_model_placeholder, ph))}</code>.
        ${escapeHtml(d.justification)}
      </li>`
    )
    .join("");

  const swHtml = (s6.strengths_weaknesses || [])
    .map(
      (x) => `
      <li>
        <strong>${escapeHtml(x.approach)} —</strong>
        <em>Forces :</em> ${escapeHtml(x.forces)}
        <em>Faiblesses :</em> ${escapeHtml(x.faiblesses)}
      </li>`
    )
    .join("");

  const detailedCompareHtml = buildDetailedComparisonBlock(clsRows, regRows, cluRows, summary);

  el.innerHTML = `
    <header class="doc-cover">
      <h1 class="doc-title">${escapeHtml(report.document_title)}</h1>
      <p class="doc-subtitle">${escapeHtml(report.document_subtitle)}</p>
    </header>

    <section class="rubric-section" data-rubric="1">
      <h2 class="rubric-h1">${escapeHtml(s1.heading)}</h2>
      <h3 class="rubric-h2">Contexte</h3>
      <p>${escapeHtml(s1.contexte)}</p>
      <h3 class="rubric-h2">Problématique</h3>
      <p>${escapeHtml(s1.problematique)}</p>
    </section>

    <section class="rubric-section" data-rubric="2">
      <h2 class="rubric-h1">${escapeHtml(s2.heading)}</h2>
      <p>${escapeHtml(s2.intro)}</p>
      <div class="table-wrap">
        <table class="bo-table">
          <thead><tr>${boHeader}</tr></thead>
          <tbody>${boRows}</tbody>
        </table>
      </div>
    </section>

    <section class="rubric-section" data-rubric="3">
      <h2 class="rubric-h1">${escapeHtml(s3.heading)}</h2>
      <p>${escapeHtml(s3.variables_intro)}</p>
      ${tasksHtml}
      <h3 class="rubric-h2">Preuve visuelle du pré-traitement</h3>
      <div class="report-figures">${visuals}</div>
      <p class="muted">${escapeHtml(s3.preprocessing_reference)}</p>
    </section>

    <section class="rubric-section" data-rubric="4">
      <h2 class="rubric-h1">${escapeHtml(s4.heading)}</h2>
      <p>${escapeHtml(s4.intro)}</p>
      <div class="model-grid">${modelsHtml}</div>
    </section>

    <section class="rubric-section" data-rubric="5">
      <h2 class="rubric-h1">${escapeHtml(s5.heading)}</h2>
      <h3 class="rubric-h2">Choix des métriques adaptées</h3>
      <ol class="metric-list">${metricsHtml}</ol>
      <h3 class="rubric-h2">Tableaux et graphiques (voir annexes)</h3>
      <p>Les matrices de confusion, courbes ROC et PR, graphiques de régression (prédit vs réel, résidus) et projections de clustering sont regroupés après ce rapport dans les sections <em>Annexe</em>.</p>
      <h3 class="rubric-h2">Interprétation des résultats <span class="badge-obligatoire">obligatoire</span></h3>
      <div class="interpretation-box">${interp.replace(/\*\*(.*?)\*\*/g, "<strong>$1</strong>")}</div>
    </section>

    <section class="rubric-section" data-rubric="6">
      <h2 class="rubric-h1">${escapeHtml(s6.heading)} <span class="badge-obligatoire">obligatoire</span></h2>
      <p>${escapeHtml(s6.comparative_summary)}</p>
      <h3 class="rubric-h2">Tableau comparatif synthétique</h3>
      ${renderBenchmarkRecap(summary, ph)}
      ${detailedCompareHtml}
      <h3 class="rubric-h2">Justification des modèles retenus pour un déploiement pilote</h3>
      <ol class="numbered-sub">${deployHtml}</ol>
      <h3 class="rubric-h2">Forces et faiblesses par approche</h3>
      <ul class="sw-list">${swHtml}</ul>
    </section>
  `;
}

async function loadCsv(path) {
  const res = await fetch(path);
  if (!res.ok) throw new Error(`Failed to fetch ${path}`);
  return parseCsv(await res.text());
}

async function loadJson(path) {
  const res = await fetch(path);
  if (!res.ok) throw new Error(`Failed to fetch ${path}`);
  return res.json();
}

async function main() {
  setupPdfExport();
  try {
    const [clsRows, regRows, cluRows, summary, report, schema] = await Promise.all([
      loadCsv("../results/metrics/classification_metrics.csv"),
      loadCsv("../results/metrics/regression_metrics.csv"),
      loadCsv("../results/metrics/clustering_metrics.csv"),
      loadJson("../results/metrics/dashboard_summary.json"),
      loadJson("../results/metrics/validation_report.json"),
      loadJson("../results/metrics/data_schema.json"),
    ]);

    renderValidationReport(report, schema, summary, clsRows, regRows, cluRows);

    renderTable("classification-table", clsRows);
    renderTable("regression-table", regRows);
    renderTable("clustering-table", cluRows);
    renderSummary(summary);

    renderPlots("classification-plots", [
      { title: "ROC Curves (All Algorithms)", src: "../results/plots/classification_roc_all_algorithms.png" },
      { title: "PR Curves (All Algorithms)", src: "../results/plots/classification_pr_all_algorithms.png" },
      { title: "Confusion - Logistic Regression", src: "../results/plots/classification_confusion_logistic_regression.png" },
      { title: "Confusion - Random Forest", src: "../results/plots/classification_confusion_random_forest_classifier.png" },
      { title: "Confusion - Gradient Boosting", src: "../results/plots/classification_confusion_gradient_boosting_classifier.png" },
    ]);

    renderPlots("regression-plots", [
      { title: "Linear: Predicted vs Actual", src: "../results/plots/regression_pred_vs_actual_linear_regression.png" },
      { title: "Linear: Residual Diagnostics", src: "../results/plots/regression_residuals_linear_regression.png" },
      { title: "RF: Predicted vs Actual", src: "../results/plots/regression_pred_vs_actual_random_forest_regressor.png" },
      { title: "RF: Residual Diagnostics", src: "../results/plots/regression_residuals_random_forest_regressor.png" },
      { title: "GB: Predicted vs Actual", src: "../results/plots/regression_pred_vs_actual_gradient_boosting_regressor.png" },
      { title: "GB: Residual Diagnostics", src: "../results/plots/regression_residuals_gradient_boosting_regressor.png" },
    ]);

    renderPlots("clustering-plots", [
      { title: "Projection - KMeans", src: "../results/plots/clustering_projection_kmeans.png" },
      { title: "Projection - Agglomerative", src: "../results/plots/clustering_projection_agglomerative.png" },
      { title: "Projection - DBSCAN", src: "../results/plots/clustering_projection_dbscan.png" },
      { title: "KMeans Curves (Elbow + Silhouette)", src: "../results/plots/clustering_kmeans_curves.png" },
    ]);

    document.getElementById("conclusion").innerHTML = `
      <p><strong>Classification best:</strong> ${summary.classification_best.algorithm} (F1=${summary.classification_best.f1.toFixed(4)})</p>
      <p><strong>Regression best:</strong> ${summary.regression_best.algorithm} (R2=${summary.regression_best.r2.toFixed(4)})</p>
      <p><strong>Clustering best:</strong> ${summary.clustering_best.algorithm} (Silhouette=${summary.clustering_best.silhouette.toFixed(4)})</p>
    `;
  } catch (err) {
    console.error(err);
    document.body.insertAdjacentHTML(
      "afterbegin",
      `<div style="background:#fee2e2;color:#7f1d1d;padding:10px;margin:12px;border-radius:8px;">
        Failed loading dashboard data. Run this in local server mode from <code>ml-workspace</code>:
        <code>python -m http.server</code>
      </div>`
    );
  }
}

main();
