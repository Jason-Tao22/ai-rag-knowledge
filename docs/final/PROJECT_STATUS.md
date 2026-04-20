# Project Status

## Current project story

We are building a local, citation-grounded RAG system for technical support QA using a 10k-document TechQA corpus. The final demo path is:

1. user asks a question in the frontend
2. backend retrieves supporting passages from PostgreSQL full-text search
3. local Ollama generates a grounded answer
4. frontend shows clickable citations and retrieved evidence cards

## What is already done

- Dockerized local stack for PostgreSQL, Redis, Ollama, backend, and nginx
- `techqa` knowledge-base tag is loaded and queryable
- 10,000 TechQA documents were prepared for the final demo path
- grounded answer endpoint: `GET /api/v1/rag/query`
- retrieval-only endpoint: `GET /api/v1/rag/retrieve`
- frontend supports normal QA mode and retrieval-only mode
- final writeup draft, system diagram draft, and presentation outline draft

## What is verified

- retrieval-only mode is very fast and returns correct citations
- example query:
  - `AppScan Source not a supported operating system`
  - correct IBM support article is returned
- full answer mode works with local `qwen2.5:0.5b`
- typical answer latency is about 16 to 22 seconds on the current CPU-only setup

## Current architecture

- frontend: static nginx app under `docs/tag/v1.0/nginx/html`
- backend: Spring Boot app
- retrieval: PostgreSQL full-text search over `rag_corpus_documents`
- generation: local Ollama
- fallback: legacy `pgvector` path still exists for uploaded files

## Known limitations

- answer generation is slower than retrieval because we are running locally on CPU
- retrieval quality is good for prepared technical-support queries, but broader evaluation still needs to be written up
- writeup and slides are still drafts, not final polished artifacts

## Most important next steps

1. finalize the writeup using the draft in `docs/final/FINAL_WRITEUP_DRAFT.md`
2. fill the actual slide deck using `docs/final/SLIDE_CONTENT_DRAFT.md`
3. prepare 5 to 8 presentation-day demo queries with expected citations
4. add one compact evaluation table for retrieval examples, latency, and failure cases

## Quick commands

Start services:

```bash
docker compose -f docker-compose.final.yml up --build -d
```

Pull models:

```bash
docker exec -it techqa-ollama ollama pull qwen2.5:0.5b
docker exec -it techqa-ollama ollama pull deepseek-r1:1.5b
docker exec -it techqa-ollama ollama pull nomic-embed-text
```

Retrieval-only test:

```bash
curl "http://localhost/api/v1/rag/retrieve?ragTag=techqa&message=AppScan%20Source%20not%20a%20supported%20operating%20system&topK=2"
```

Grounded answer test:

```bash
curl "http://localhost/api/v1/rag/query?ragTag=techqa&message=AMQ9208%20AMQ6048%20AMQ9492%20data%20conversion&model=qwen2.5:0.5b&topK=1"
```
