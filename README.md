# Grounded TechQA RAG Demo

This repository contains a local, citation-grounded RAG system for technical support QA. The final-project target is a demo that retrieves evidence from a TechQA-style corpus, generates an answer with a local Ollama model, and shows clickable citations plus supporting passages in the UI.

## What This Demo Includes

- Spring Boot backend with PostgreSQL full-text retrieval over a 10k-document TechQA corpus
- Optional `pgvector` fallback path for legacy uploaded documents
- Local Ollama model inference
- Evidence-bearing RAG endpoint at `/api/v1/rag/query`
- Retrieval-only evidence endpoint at `/api/v1/rag/retrieve`
- Static frontend for asking questions and inspecting retrieved passages
- JSONL corpus import endpoint for large document collections
- Dockerized local stack for PostgreSQL, Redis, Ollama, app, and nginx

## Repository Layout

- `xfg-dev-tech-app`: Spring Boot app and runtime configuration
- `xfg-dev-tech-trigger`: HTTP controllers
- `xfg-dev-tech-api`: shared API and response DTOs
- `docs/tag/v1.0/nginx/html`: frontend demo pages
- `docs/final`: final writeup and slide scaffolding
- `scripts`: dataset preparation and ingestion helpers

## Quick Start

### 1. Start the stack

```bash
docker compose -f docker-compose.final.yml up --build -d
```

### 2. Pull the local models into Ollama

```bash
docker exec -it techqa-ollama ollama pull qwen2.5:0.5b
docker exec -it techqa-ollama ollama pull deepseek-r1:1.5b
docker exec -it techqa-ollama ollama pull nomic-embed-text
```

Recommended for the live demo:
- `qwen2.5:0.5b` for faster local answer generation
- `deepseek-r1:1.5b` when you want a slower but sometimes stronger answer model

### 3. Open the demo

- Frontend: [http://localhost](http://localhost)
- Backend API: [http://localhost:8090](http://localhost:8090)

## Preparing a TechQA Corpus

Download the official IBM TechQA assets separately. The script below converts the official technote corpus JSON into a JSONL file that this app can ingest.

```bash
python3 scripts/prepare_techqa_jsonl.py \
  --input /path/to/training_dev_technotes.json \
  --output data/processed/techqa_corpus.jsonl \
  --limit 10000
```

Import the processed corpus:

```bash
bash scripts/import_jsonl_corpus.sh techqa data/processed/techqa_corpus.jsonl http://localhost:8090
```

The default final-demo setup uses 10,000 TechQA technotes stored in PostgreSQL table `rag_corpus_documents`.

For presentation-day stability, the compose file keeps only one Ollama model loaded at a time. This avoids the large latency spikes that happen when the embedding model and answer model both remain resident in low-memory CPU environments.

## API Endpoints

- `GET /api/v1/rag/query_rag_tag_list`: list indexed knowledge bases
- `POST /api/v1/rag/file/upload`: upload generic files into a knowledge base
- `POST /api/v1/rag/corpus/import_jsonl`: import a JSONL corpus with metadata
- `GET /api/v1/rag/query`: ask a grounded RAG question and receive answer + citations
- `GET /api/v1/rag/retrieve`: retrieve evidence only, without running the local LLM

Example:

```bash
curl "http://localhost:8090/api/v1/rag/query?ragTag=techqa&message=AMQ9208%20AMQ6048%20AMQ9492%20data%20conversion&model=qwen2.5:0.5b&topK=3"
```

Retrieval-only example:

```bash
curl "http://localhost:8090/api/v1/rag/retrieve?ragTag=techqa&message=AppScan%20Source%20not%20a%20supported%20operating%20system&topK=3"
```

## Reproducibility Notes

- The app is configured through environment variables rather than hard-coded hosts.
- Ollama, PostgreSQL, Redis, backend, and nginx can all be started from a single compose file.
- The TechQA raw dataset is not stored in this repository; see the final writeup for attribution and download instructions.
- The main demo retrieval path uses PostgreSQL full-text search plus passage citations, which keeps the 10k-document demo responsive and easy to reproduce.

## Current Final-Project Scope

- TechQA technical-support retrieval over 10,000 indexed source documents
- PostgreSQL full-text search with source-level and passage-level citations
- Local LLM answering with inline citations
- Retrieval-only inspection mode for rapid demo verification
- Evidence cards showing passage-level grounding
- Final report and slide scaffold under `docs/final`

## Team Handoff Docs

These files are intended to help teammates continue the project quickly:

- `docs/final/PROJECT_STATUS.md`
- `docs/final/TEAM_COLLAB_PLAN.md`
- `docs/final/SLIDE_CONTENT_DRAFT.md`
- `docs/final/PRESENTATION_OUTLINE.md`
- `docs/final/FINAL_WRITEUP_DRAFT.md`
