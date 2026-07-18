# Phase 9: Evidence-Backed Project Graph

## Goal
Turn the Atlas decision field into a real, inspectable graph whose links are derived exclusively from persisted decision evidence.

## Graph contract
- Nodes are stable `document-{id}`, `decision-{id}`, and `evidence-{id}` identifiers.
- Every `supports` edge is `decision -> evidence`; every `sources` edge is `evidence -> document` and carries its persisted block ID.
- Documents and decisions without edges may still appear as persisted entities, but the graph never infers a relationship from title, tag, or embedding similarity.
- Repository queries and service assembly use ascending database IDs for deterministic output.

## UI behavior
- The status filter selects decision nodes and keeps their evidence/document path visible.
- Selecting a decision opens existing evidence. Selecting evidence or a document opens the source document inspector.
- The node-link view remains keyboard-operable and scrollable on narrow screens.

## Validation
- Unit test deterministic node/edge assembly and evidence-only relationships.
- Run API tests, web lint/build, and GitHub Actions.

## Non-goals
- Editable topology, persisted coordinates, inferred semantic links, and collaborative graph editing.
