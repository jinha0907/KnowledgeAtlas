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
  const [graph, setGraph] = useState({ nodes: [], edges: [] });
  const [selectedDocument, setSelectedDocument] = useState(null);
  const [documentDetail, setDocumentDetail] = useState(null);
  const [selectedDecision, setSelectedDecision] = useState(null);
  const [highlightedBlockId, setHighlightedBlockId] = useState(null);
  const [filter, setFilter] = useState("proposed");
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResult, setSearchResult] = useState(null);
  const [searchLoading, setSearchLoading] = useState(false);
  const [embeddingStatus, setEmbeddingStatus] = useState(null);
  const [reindexConfirmed, setReindexConfirmed] = useState(false);
  const [supersedingDecisionId, setSupersedingDecisionId] = useState("");
  const [statusUpdating, setStatusUpdating] = useState(false);
  const [notice, setNotice] = useState("Connecting to your local knowledge base...");
  const [loading, setLoading] = useState(true);

  const loadAtlas = async () => {
    setLoading(true);
    try {
      const [documentRows, decisionRows, graphData, embeddingData] = await Promise.all([
        request("/api/documents"),
        request("/api/decisions"),
        request("/api/project-graph"),
        request("/api/embeddings/status"),
      ]);
      setDocuments(documentRows);
      setDecisions(decisionRows);
      setGraph(graphData);
      setEmbeddingStatus(embeddingData);
      setSelectedDocument((current) => (
        documentRows.find((document) => document.id === current?.id) || documentRows[0] || null
      ));
      setSelectedDecision((current) => (
        decisionRows.find((decision) => decision.id === current?.id) || decisionRows[0] || null
      ));
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

  useEffect(() => {
    setSupersedingDecisionId("");
  }, [selectedDecision?.id]);

  const graphLayout = useMemo(() => {
    const decisionNodes = graph.nodes.filter((node) => (
      node.type === "decision" && (filter === "all" || node.status === filter)
    ));
    const decisionIds = new Set(decisionNodes.map((node) => node.id));
    const supportsEdges = graph.edges.filter((edge) => edge.type === "supports" && decisionIds.has(edge.sourceId));
    const evidenceIds = new Set(supportsEdges.map((edge) => edge.targetId));
    const sourceEdges = graph.edges.filter((edge) => edge.type === "sources" && evidenceIds.has(edge.sourceId));
    const documentIds = new Set(sourceEdges.map((edge) => edge.targetId));
    if (filter === "all") {
      graph.nodes.filter((node) => node.type === "document").forEach((node) => documentIds.add(node.id));
    }

    const nodes = graph.nodes.filter((node) => (
      decisionIds.has(node.id) || evidenceIds.has(node.id) || documentIds.has(node.id)
    ));
    const edges = [...supportsEdges, ...sourceEdges].filter((edge) => (
      nodes.some((node) => node.id === edge.sourceId) && nodes.some((node) => node.id === edge.targetId)
    ));
    const lanes = {
      document: nodes.filter((node) => node.type === "document"),
      decision: nodes.filter((node) => node.type === "decision"),
      evidence: nodes.filter((node) => node.type === "evidence"),
    };
    const height = Math.max(350, Math.max(...Object.values(lanes).map((items) => items.length), 1) * 112 + 72);
    const positions = new Map();
    const x = { document: 14, decision: 50, evidence: 86 };
    Object.entries(lanes).forEach(([type, nodesInLane]) => {
      nodesInLane.forEach((node, index) => positions.set(node.id, {
        x: x[type],
        y: 58 + index * 112,
      }));
    });
    return { nodes, edges, positions, height };
  }, [filter, graph]);

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

  const reindexEmbeddings = () => {
    if (!reindexConfirmed) {
      setNotice("Confirm embedding replacement before re-indexing.");
      return;
    }
    runAction("Embedding re-index", "/api/embeddings/reindex", {
      body: JSON.stringify({ confirm: true }),
    });
  };

  const updateSelectedDecisionStatus = async (status) => {
    if (!selectedDecision) {
      return;
    }
    if (status === "accepted" && !selectedDecision.evidence?.length) {
      setNotice("Attach and review source evidence before accepting this decision.");
      return;
    }

    setStatusUpdating(true);
    setNotice(`Updating decision to ${status}...`);
    try {
      const updated = await request(`/api/decisions/${selectedDecision.id}/status`, {
        method: "PATCH",
        body: JSON.stringify({
          status,
          supersedesDecisionId: status === "obsolete" && supersedingDecisionId
            ? Number(supersedingDecisionId)
            : null,
        }),
      });
      setSupersedingDecisionId("");
      setSelectedDecision(updated);
      setNotice(`Decision marked ${status}.`);
      await loadAtlas();
    } catch (error) {
      setNotice(`Decision status update failed: ${error.message}`);
    } finally {
      setStatusUpdating(false);
    }
  };

  const openEvidenceSource = (documentId, blockId = null) => {
    const document = documents.find((item) => item.id === documentId);
    if (!document) {
      setNotice("The source document is no longer available locally.");
      return;
    }
    setSelectedDocument(document);
    setHighlightedBlockId(blockId);
    setNotice(`Opened source evidence from ${document.title || "Untitled"}.`);
  };

  const searchKnowledge = async (event) => {
    event.preventDefault();
    const query = searchQuery.trim();
    if (!query) {
      setSearchResult(null);
      setNotice("Enter a search query first.");
      return;
    }
    setSearchLoading(true);
    try {
      const result = await request("/api/search", {
        method: "POST",
        body: JSON.stringify({ query, topK: 6 }),
      });
      setSearchResult(result);
      setNotice(result.citations.length ? "Evidence found in your local knowledge base." : result.answer);
    } catch (error) {
      setNotice(`Knowledge search failed: ${error.message}`);
    } finally {
      setSearchLoading(false);
    }
  };

  const selectGraphNode = (node) => {
    if (node.type === "decision") {
      const decision = decisions.find((item) => item.id === node.decisionId);
      if (decision) {
        setSelectedDecision(decision);
      }
      return;
    }
    openEvidenceSource(node.documentId, node.blockId);
  };

  const previewBlocks = useMemo(() => {
    if (!documentDetail) {
      return [];
    }
    const highlighted = documentDetail.blocks.find((block) => block.blockId === highlightedBlockId);
    const remaining = documentDetail.blocks.filter((block) => block.blockId !== highlightedBlockId);
    return highlighted ? [highlighted, ...remaining.slice(0, 3)] : remaining.slice(0, 4);
  }, [documentDetail, highlightedBlockId]);

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

      <section className="search-dock panel" aria-labelledby="knowledge-search-title">
        <div className="search-copy">
          <p className="eyebrow">Citation-first retrieval</p>
          <h3 id="knowledge-search-title">Ask the stored project record.</h3>
          <p>Keyword search is always available. When embeddings are configured, the same API adds semantic retrieval without changing the cited source.</p>
        </div>
        <form className="search-form" onSubmit={searchKnowledge}>
          <label htmlFor="knowledge-query">Search synced knowledge</label>
          <div>
            <input
              id="knowledge-query"
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
              placeholder="e.g. release timing, launch plan"
            />
            <button type="submit" disabled={searchLoading}>{searchLoading ? "Searching" : "Search"}</button>
          </div>
        </form>
        {searchResult && (
          <div className="search-results" aria-live="polite">
            <p className="search-answer">{searchResult.answer}</p>
            {searchResult.citations.map((citation) => (
              <button
                key={`${citation.documentId}-${citation.blockId}`}
                className="search-citation"
                onClick={() => openEvidenceSource(citation.documentId, citation.blockId)}
              >
                <span>{citation.title || "Untitled"} <b>BLOCK {citation.blockId}</b></span>
                <strong>{citation.text}</strong>
                <small>relevance {citation.score.toFixed(3)} · open local source</small>
              </button>
            ))}
          </div>
        )}
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
                onClick={() => {
                  setHighlightedBlockId(null);
                  setSelectedDocument(document);
                }}
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
                {previewBlocks.map((block) => (
                  <p key={block.blockId} className={block.blockId === highlightedBlockId ? "block-highlight" : ""}><b>{block.blockId}</b>{block.text}</p>
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
            <section className="embedding-status" aria-label="Embedding readiness">
              <p className="eyebrow">Retrieval readiness</p>
              {!embeddingStatus && <p>Loading embedding status...</p>}
              {embeddingStatus && (
                <>
                  <strong className={`embedding-state ${embeddingStatus.status}`}>{embeddingStatus.status.replaceAll("_", " ")}</strong>
                  <p>
                    {embeddingStatus.activeIdentity
                      ? `${embeddingStatus.activeIdentity.provider}/${embeddingStatus.activeIdentity.model} · ${embeddingStatus.activeIdentity.dimensions}d`
                      : "Keyword retrieval only"}
                  </p>
                  <small>{embeddingStatus.embeddedChunks}/{embeddingStatus.eligibleChunks} chunks embedded</small>
                  {embeddingStatus.reindexRequired && <small className="warning">Stored vectors use another model. Re-index before semantic search.</small>}
                </>
              )}
            </section>
            <button className="quiet-button" onClick={() => runAction("Embedding backfill", "/api/embeddings/backfill")}>Backfill embeddings</button>
            <label className="reindex-confirm">
              <input type="checkbox" checked={reindexConfirmed} onChange={(event) => setReindexConfirmed(event.target.checked)} />
              I understand re-indexing replaces every stored embedding.
            </label>
            <button className="outline-button" disabled={!reindexConfirmed || embeddingStatus?.status === "disabled"} onClick={reindexEmbeddings}>Re-index embeddings</button>
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
            <div className="graph-legend" aria-hidden="true">
              <span className="document">Document</span><span className="decision">Decision</span><span className="evidence">Evidence block</span>
            </div>
            {graphLayout.nodes.length === 0 && <p className="empty-state">No evidence-backed decisions in this view yet.</p>}
            {graphLayout.nodes.length > 0 && (
              <div className="graph-canvas" style={{ minHeight: `${graphLayout.height}px` }}>
                <svg viewBox={`0 0 100 ${graphLayout.height}`} preserveAspectRatio="none" aria-hidden="true">
                  {graphLayout.edges.map((edge) => {
                    const source = graphLayout.positions.get(edge.sourceId);
                    const target = graphLayout.positions.get(edge.targetId);
                    return source && target && <line key={edge.id} x1={source.x} y1={source.y} x2={target.x} y2={target.y} />;
                  })}
                </svg>
                {graphLayout.nodes.map((node) => {
                  const position = graphLayout.positions.get(node.id);
                  const selected = node.type === "decision" && selectedDecision?.id === node.decisionId;
                  return position && (
                    <button
                      key={node.id}
                      className={`graph-node ${node.type} ${selected ? "selected" : ""}`}
                      style={{ left: `${position.x}%`, top: `${position.y}px` }}
                      onClick={() => selectGraphNode(node)}
                      aria-label={`${node.type}: ${node.label}`}
                    >
                      <span>{node.type === "evidence" ? node.blockId : node.type}</span>
                      <strong>{node.label}</strong>
                    </button>
                  );
                })}
              </div>
            )}
          </div>
        </section>

        <aside className="evidence-panel panel">
          <div className="section-heading">
            <div><p className="eyebrow">Evidence drawer</p><h3>{selectedDecision?.title || "Select a decision"}</h3></div>
          </div>
          {selectedDecision ? (
            <div className="evidence-content">
              <section className="decision-review" aria-label="Decision review controls">
                <div>
                  <p className="eyebrow">Review status</p>
                  <strong className={`decision-status ${selectedDecision.status}`}>{statusLabels[selectedDecision.status]}</strong>
                </div>
                {selectedDecision.status === "proposed" && (
                  <button
                    className="accept-button"
                    disabled={statusUpdating || !selectedDecision.evidence?.length}
                    onClick={() => updateSelectedDecisionStatus("accepted")}
                  >
                    {statusUpdating ? "Updating" : "Accept decision"}
                  </button>
                )}
              </section>
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
                    <button className="evidence-link" onClick={() => openEvidenceSource(evidence.documentId, evidence.blockId)}>
                      Open source document
                    </button>
                  </article>
                ))}
                {!selectedDecision.evidence?.length && <p className="empty-state">No evidence has been attached.</p>}
              </div>
              {selectedDecision.status === "proposed" && !selectedDecision.evidence?.length && (
                <p className="review-hint">Acceptance is unavailable until this candidate has source evidence.</p>
              )}
              {selectedDecision.status === "accepted" && (
                <section className="obsolete-action" aria-label="Obsolete decision">
                  <label htmlFor="superseding-decision">Superseded by (optional)</label>
                  <select
                    id="superseding-decision"
                    value={supersedingDecisionId}
                    onChange={(event) => setSupersedingDecisionId(event.target.value)}
                  >
                    <option value="">No replacement decision</option>
                    {decisions.filter((decision) => (
                      decision.status === "accepted" && decision.id !== selectedDecision.id
                    )).map((decision) => (
                      <option key={decision.id} value={decision.id}>{decision.title}</option>
                    ))}
                  </select>
                  <button
                    className="obsolete-button"
                    disabled={statusUpdating}
                    onClick={() => updateSelectedDecisionStatus("obsolete")}
                  >
                    {statusUpdating ? "Updating" : "Mark obsolete"}
                  </button>
                </section>
              )}
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
