"use client";

import { useEffect, useMemo, useState } from "react";

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

const statusLabels = {
  proposed: "Review queue",
  accepted: "Accepted",
  obsolete: "Obsolete",
};

async function request(path, options = {}) {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    headers: { "Content-Type": "application/json", ...options.headers },
    ...options,
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(body.error || `Request failed (${response.status})`);
  }
  return body;
}

export default function Home() {
  const [documents, setDocuments] = useState([]);
  const [decisions, setDecisions] = useState([]);
  const [selectedDocument, setSelectedDocument] = useState(null);
  const [documentDetail, setDocumentDetail] = useState(null);
  const [selectedDecision, setSelectedDecision] = useState(null);
  const [filter, setFilter] = useState("proposed");
  const [notice, setNotice] = useState("Connecting to your local knowledge base...");
  const [loading, setLoading] = useState(true);

  const loadAtlas = async () => {
    setLoading(true);
    try {
      const [documentRows, decisionRows] = await Promise.all([
        request("/api/documents"),
        request("/api/decisions"),
      ]);
      setDocuments(documentRows);
      setDecisions(decisionRows);
      setSelectedDocument((current) => (
        documentRows.find((document) => document.id === current?.id) || documentRows[0] || null
      ));
      setSelectedDecision((current) => current || decisionRows[0] || null);
      setNotice(documentRows.length ? "Atlas is in sync." : "No synced documents yet. Run a Notion sync to begin.");
    } catch (error) {
      setNotice(`API unavailable: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAtlas();
  }, []);

  useEffect(() => {
    if (!selectedDocument) {
      setDocumentDetail(null);
      return;
    }

    request(`/api/documents/${selectedDocument.id}`)
      .then(setDocumentDetail)
      .catch((error) => setNotice(`Could not load source blocks: ${error.message}`));
  }, [selectedDocument]);

  const visibleDecisions = useMemo(
    () => decisions.filter((decision) => filter === "all" || decision.status === filter),
    [decisions, filter],
  );

  const runAction = async (label, path, options = {}) => {
    setNotice(`${label} is running...`);
    try {
      const result = await request(path, { method: "POST", ...options });
      if (result.status === "disabled") {
        setNotice(`${label} is disabled. Configure its provider in .env first.`);
      } else {
        setNotice(`${label} completed.`);
      }
      await loadAtlas();
    } catch (error) {
      setNotice(`${label} failed: ${error.message}`);
    }
  };

  const extractSelected = () => {
    if (!selectedDocument) {
      setNotice("Select a synced document before extracting decisions.");
      return;
    }
    runAction("Decision extraction", `/api/documents/${selectedDocument.id}/decisions/extract`);
  };

  const analyzeSelected = () => {
    if (!selectedDocument) {
      setNotice("Select a synced document before running analysis.");
      return;
    }
    runAction("Document analysis", `/api/documents/${selectedDocument.id}/analysis/run`);
  };

  const openEvidenceSource = (documentId) => {
    const document = documents.find((item) => item.id === documentId);
    if (!document) {
      setNotice("The source document is no longer available locally.");
      return;
    }
    setSelectedDocument(document);
    setNotice(`Opened source evidence from ${document.title || "Untitled"}.`);
  };

  return (
    <main className="atlas-shell">
      <header className="topbar">
        <div className="brand-lockup">
          <span className="brand-mark">KA</span>
          <div>
            <p className="eyebrow">Project knowledge atlas</p>
            <h1>Where decisions keep their receipts.</h1>
          </div>
        </div>
        <div className="action-row">
          <button className="quiet-button" onClick={loadAtlas} disabled={loading}>Refresh</button>
          <button onClick={() => runAction("Notion sync", "/api/notion/sync/run", {
            body: JSON.stringify({ pageSize: 20, maxPages: 20 }),
          })}>Sync Notion</button>
        </div>
      </header>

      <section className="signal-strip" aria-live="polite">
        <span className={notice.startsWith("API unavailable") || notice.includes("failed") ? "signal-dot danger" : "signal-dot"} />
        <span>{notice}</span>
      </section>

      <section className="hero-grid">
        <div className="hero-copy">
          <p className="eyebrow">MVP control room</p>
          <h2>Trace a project from raw notes to the decisions that shape it.</h2>
          <p className="hero-description">
            Documents are your terrain. Decisions are the landmarks. Every proposed outcome carries its original block-level evidence.
          </p>
          <div className="stat-row">
            <div><strong>{documents.length}</strong><span>documents</span></div>
            <div><strong>{decisions.length}</strong><span>decisions</span></div>
            <div><strong>{decisions.filter((item) => item.status === "proposed").length}</strong><span>to review</span></div>
          </div>
        </div>
        <div className="constellation" aria-label="Decision evidence constellation">
          <div className="orbit orbit-one" />
          <div className="orbit orbit-two" />
          <span className="star star-main">D</span>
          <span className="star star-one">N</span>
          <span className="star star-two">E</span>
          <p>Decision<br />evidence map</p>
        </div>
      </section>

      <section className="workspace-grid">
        <aside className="document-rail panel">
          <div className="section-heading">
            <div><p className="eyebrow">Source terrain</p><h3>Synced documents</h3></div>
            <span>{documents.length}</span>
          </div>
          <div className="document-list">
            {documents.length === 0 && <p className="empty-state">Your Notion pages will appear here after sync.</p>}
            {documents.map((document) => (
              <button
                key={document.id}
                className={`document-card ${selectedDocument?.id === document.id ? "selected" : ""}`}
                onClick={() => setSelectedDocument(document)}
              >
                <span className="source-pill">{document.sourceType}</span>
                <strong>{document.title || "Untitled"}</strong>
              <small>Synced {new Date(document.lastSyncedAt).toLocaleDateString()}</small>
              {document.analysis?.status === "success" && (
                <span className="tag-count">{document.analysis.tags.length} tags</span>
              )}
              </button>
            ))}
          </div>
          {documentDetail && (
            <section className="source-preview" aria-label="Selected source document">
              <p className="eyebrow">Source inspector</p>
              <strong>{documentDetail.title || "Untitled"}</strong>
              <span>{documentDetail.blocks.length} stored blocks</span>
              <div className="block-list">
                {documentDetail.blocks.slice(0, 4).map((block) => (
                  <p key={block.blockId}><b>{block.blockId}</b>{block.text}</p>
                ))}
                {documentDetail.blocks.length > 4 && <p className="more-blocks">+ {documentDetail.blocks.length - 4} more blocks</p>}
              </div>
              <div className="analysis-preview">
                <p className="eyebrow">Reviewable analysis</p>
                {documentDetail.analysis?.status === "success" && (
                  <>
                    <p>{documentDetail.analysis.summary}</p>
                    <div className="tag-row">
                      {documentDetail.analysis.tags.map((tag) => <span key={tag}>{tag}</span>)}
                    </div>
                  </>
                )}
                {documentDetail.analysis?.status === "running" && <p>Analysis is pending.</p>}
                {documentDetail.analysis?.status === "failed" && <p>Analysis failed. Run it again after reviewing provider settings.</p>}
                {!documentDetail.analysis && <p>No saved analysis. Enable the provider to create a summary and tags.</p>}
              </div>
            </section>
          )}
          <div className="rail-actions">
            <button className="quiet-button" onClick={() => runAction("Embedding backfill", "/api/embeddings/backfill")}>Backfill embeddings</button>
            <button className="quiet-button" onClick={analyzeSelected}>Analyze summary &amp; tags</button>
            <button className="outline-button" onClick={extractSelected}>Extract decisions</button>
          </div>
        </aside>

        <section className="decision-stage panel">
          <div className="section-heading decision-heading">
            <div><p className="eyebrow">Review map</p><h3>Decision field</h3></div>
            <div className="filter-row" role="group" aria-label="Filter decisions">
              {["proposed", "accepted", "obsolete", "all"].map((status) => (
                <button
                  key={status}
                  className={filter === status ? "filter active" : "filter"}
                  onClick={() => setFilter(status)}
                >{status === "all" ? "All" : statusLabels[status]}</button>
              ))}
            </div>
          </div>
          <div className="decision-map">
            {visibleDecisions.length === 0 && <p className="empty-state">No decisions in this view yet.</p>}
            {visibleDecisions.map((decision, index) => (
              <button
                key={decision.id}
                className={`decision-node node-${index % 4} ${selectedDecision?.id === decision.id ? "selected" : ""}`}
                onClick={() => setSelectedDecision(decision)}
              >
                <span className={`status-badge ${decision.status}`}>{decision.status}</span>
                <strong>{decision.title}</strong>
                <small>{decision.evidence?.length || 0} evidence links</small>
              </button>
            ))}
          </div>
        </section>

        <aside className="evidence-panel panel">
          <div className="section-heading">
            <div><p className="eyebrow">Evidence drawer</p><h3>{selectedDecision?.title || "Select a decision"}</h3></div>
          </div>
          {selectedDecision ? (
            <div className="evidence-content">
              {selectedDecision.discussion && <p className="discussion">{selectedDecision.discussion}</p>}
              <p className="outcome">{selectedDecision.outcome}</p>
              {selectedDecision.confidence !== null && selectedDecision.confidence !== undefined && (
                <p className="confidence">Extraction confidence <strong>{Math.round(selectedDecision.confidence * 100)}%</strong></p>
              )}
              <div className="evidence-stack">
                {(selectedDecision.evidence || []).map((evidence) => (
                  <article key={evidence.id} className="evidence-card">
                    <span>BLOCK {evidence.blockId}</span>
                    <blockquote>“{evidence.quote}”</blockquote>
                    {evidence.rationale && <p>{evidence.rationale}</p>}
                    <button className="evidence-link" onClick={() => openEvidenceSource(evidence.documentId)}>
                      Open source document
                    </button>
                  </article>
                ))}
                {!selectedDecision.evidence?.length && <p className="empty-state">No evidence has been attached.</p>}
              </div>
            </div>
          ) : <p className="empty-state">Choose a node to inspect its supporting text.</p>}
        </aside>
      </section>

      <footer>
        <span>Local-first knowledge operations</span>
        <a href={`${apiBaseUrl}/swagger-ui/index.html`} target="_blank" rel="noreferrer">Open API docs</a>
      </footer>
    </main>
  );
}
