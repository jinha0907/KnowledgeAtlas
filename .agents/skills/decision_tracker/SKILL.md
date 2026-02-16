---
name: decision-tracker
description: Use this when extracting and managing decisions: parse meeting notes into 'discussion + outcome', store decisions, and link evidence blocks. Includes versioning/obsoleting rules.
---

## Decision unit (MVP)
- discussion: what was debated (1-3 bullets)
- outcome: what was decided (1-2 sentences)
- evidence: list of (document_id, block_id, quote)

## Status rules
- proposed -> accepted
- accepted -> obsolete (when superseded by a newer decision)
- Store supersedes_decision_id when applicable.
