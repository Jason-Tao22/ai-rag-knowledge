# Slide Content Draft

This file is the text draft to paste into the course presentation deck.

## Slide 1. Title

Grounded Technical Support QA with a Local Citation-Aware RAG System

Team:
- Yifan Tao
- Letong Yi

One-sentence pitch:
- We built a local RAG system over a 10k-document TechQA corpus that answers technical-support questions and shows clickable evidence for verification.

## Slide 2. Problem

- Technical-support QA is retrieval-heavy because answers depend on specific product documentation and error-code articles.
- Hallucinations are risky in this setting because users need actionable, source-backed troubleshooting guidance.
- We wanted a final-project system that is local, reproducible, and easy to verify in real time.

## Slide 3. Our System

- Frontend: lightweight web UI for question answering and evidence inspection
- Backend: Spring Boot RAG API
- Retrieval: PostgreSQL full-text search over 10,000 TechQA documents
- Generation: local Ollama model
- Verification: clickable citation cards with article links and supporting passages
- Backup demo mode: retrieval-only endpoint for instant evidence verification

## Slide 4. Data

- Source: IBM TechQA / technote-style technical-support corpus
- Final demo size: 10,000 indexed documents
- Stored metadata:
  - document ID
  - title
  - source URL
  - source name
  - passage text

## Slide 5. Demo Flow

1. User asks a technical-support question.
2. Backend retrieves the top supporting passages.
3. Local model generates an answer grounded in those passages.
4. UI displays the answer plus clickable citations.
5. If needed, we can switch to retrieval-only mode to verify the evidence directly.

## Slide 6. Results

- Retrieval-only mode returns correct support articles quickly.
- Example:
  - Query: `AppScan Source not a supported operating system`
  - Correct IBM support article is retrieved with the exact supporting passage.
- Full grounded answer mode works locally with `qwen2.5:0.5b`.
- Current CPU-only latency:
  - retrieval-only: around tens of milliseconds
  - grounded answering: about 16 to 22 seconds in our current environment

## Slide 7. Limitations and Future Work

- CPU-only local generation is slower than ideal for production
- retrieval evaluation table still needs to be expanded
- future improvements:
  - reranking
  - better abstention when evidence is weak
  - larger local model or GPU-backed serving

## Suggested live demo questions

### Retrieval-only

- `AppScan Source not a supported operating system`
- `silently installing AppScan Source supported OS`

### Full answer mode

- `AMQ9208 AMQ6048 AMQ9492 data conversion`
- `What causes the WebSphere MQ AMQ6048 DBCS error?`

## Suggested 2-minute script

We built a local citation-grounded RAG system for technical-support QA. Our corpus contains 10,000 TechQA documents, and the system retrieves supporting passages from PostgreSQL before generating an answer with a local Ollama model. The main thing we wanted to solve is trust: instead of only showing a generated answer, we also show clickable citations and the exact evidence passage. In the demo, we can either run full grounded QA or switch to retrieval-only mode to prove the right document was retrieved. This gives us a reproducible local system that satisfies the class requirements for a frontend, local model, data scale, and verifiable citations.
