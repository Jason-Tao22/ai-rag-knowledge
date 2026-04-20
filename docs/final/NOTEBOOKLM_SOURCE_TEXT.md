# NotebookLM Source Text for Final Slides and Report

Use this document as the source material for NotebookLM. The goal is to generate:

1. one required master-deck slide for the in-class elevator pitch
2. a concise 1-2 minute presentation script
3. a final report/writeup draft in PDF-ready structure
4. an optional simple system diagram for the slide/report

Do not overclaim results that are not listed here. Keep the language clear, technical, and easy for instructors/TAs to verify during a live demo.

## Course Presentation Requirements

The instructor-provided presentation deck says:

- Elevator pitches happen from 4:15pm to 5:00pm.
- Each team should prepare a 1-2 minute presentation.
- The master deck needs one slide with links to:
  - the public endpoint
  - the GitHub repository
  - the writeup in the repository
- The slide should include:
  - problem and motivation
  - data used
  - specific queries that instructors/TAs can issue

The project page also says the elevator pitch should be at most three minutes and that the course staff will be strict about timelines. Therefore, prepare a 1.5-2 minute talk, never longer than three minutes.

## Final Submission Requirements

The final submission needs:

- Project writeup in PDF format.
- Slides linked to the master presentation deck, especially the one-slide elevator pitch.
- GitHub repository link.
- README.md explaining how to set up and run the project.
- Docker/containerized solution.
- Publicly accessible demonstration endpoint or IP address on presentation day. DNS is not required.
- Frontend, for example a Streamlit or web UI. This project uses an nginx-served web frontend.
- Database with no fewer than 10,000 entries.
- LLMs served locally/native on the project infrastructure rather than calling a hosted commercial API.
- Clickable citations to the relevant data source/article and passage.
- A system diagram in the writeup.
- Contributions section explaining what each team member worked on.

Rubric summary:

- 40% documentation: written report and oral delivery.
- 60% technical delivery: real-time demo, accessibility, performance, data scale, accuracy, code/data provenance, reproducibility, and verifiability.

## Project Identity

Title:

Grounded Technical Support QA with a Local Citation-Aware RAG System

One-sentence pitch:

We built a local RAG system over a 10,000-document technical-support corpus that answers troubleshooting questions with retrieved evidence and clickable citations, so users can verify where each answer comes from.

Team:

- Yifan Tao
- Letong Yi

Public demo endpoint:

- http://35.247.103.164/

GitHub repository:

- https://github.com/Jason-Tao22/ai-rag-knowledge

Current project branch / PR:

- https://github.com/Jason-Tao22/ai-rag-knowledge/tree/codex/final-rag-demo-handoff
- https://github.com/Jason-Tao22/ai-rag-knowledge/pull/1

Writeup draft in repository:

- docs/final/FINAL_WRITEUP_DRAFT.md

Slide draft in repository:

- docs/final/SLIDE_CONTENT_DRAFT.md

Important deployment note:

- The GCP homepage is publicly reachable.
- The TechQA corpus has been imported into the public GCP endpoint under the `techqa` tag.
- A presentation smoke test passed on 2026-04-20.
- Smoke-test result: tag list includes `techqa`; AppScan retrieval returns `swg21512700`; MQ grounded-answer query returns `swg1SE46234`.
- Current observed public-endpoint latency: retrieval-only about 135 ms; full local generation about 31.5 seconds on the tested CPU-only environment.
- See `docs/final/GCP_DEMO_STATUS.md` for the exact smoke test result and reimport commands.

## Problem and Motivation

Technical-support question answering is retrieval-heavy. Users often ask about product-specific errors, installation failures, support notes, and configuration issues. A general LLM may answer fluently but hallucinate or miss the exact product documentation. In a support setting, this is risky because users need actionable, verifiable troubleshooting guidance.

This project addresses that trust problem by combining retrieval and generation:

- retrieve relevant technical-support passages from a database
- generate a concise answer using a locally served LLM
- show clickable citations and evidence passages alongside the answer
- allow a retrieval-only mode for fast verification when generation is slow

The main project value is not just answer generation. The value is verifiability: instructors/TAs can follow the citation to the source article and inspect the passage that grounded the answer.

## Data Used

Dataset:

- TechQA-style IBM technical-support corpus / technote collection.

Final demo data scale:

- 10,000 indexed technical-support documents.

Stored document fields:

- document ID
- title
- source URL
- source name
- passage/body text
- knowledge-base tag

Why this dataset is meaningful:

- Technical-support documents contain specific error codes, installation messages, and product symptoms.
- The answers usually cannot be guessed by hand.
- Retrieval quality matters because similar technical phrases can point to different support articles.
- The dataset naturally supports citation-grounded RAG because each row has article metadata and passage text.

## System Architecture

Suggested diagram:

User/browser -> nginx frontend -> Spring Boot RAG API -> PostgreSQL full-text retrieval -> top evidence passages -> local Ollama LLM -> grounded answer + clickable citation cards -> browser

Components:

- Frontend: static web UI served by nginx.
- Backend: Spring Boot API.
- Retrieval database: PostgreSQL table `rag_corpus_documents`.
- Retrieval method: PostgreSQL full-text search over the TechQA corpus.
- Generation: local Ollama-served model, such as `qwen2.5:0.5b`.
- Cache/infrastructure: Redis is included in the Docker stack.
- Verification: frontend citation cards show the article title, URL, and supporting passage.
- Backup demo path: retrieval-only endpoint returns evidence quickly without waiting for LLM generation.

Endpoints:

- Full RAG answer: `GET /api/v1/rag/query`
- Retrieval-only evidence: `GET /api/v1/rag/retrieve`
- Tag list: `GET /api/v1/rag/query_rag_tag_list`

Key design decision:

We prioritize reproducibility and verifiability over using a large hosted model. The project uses local/native model serving and a containerized database-backed RAG pipeline.

## Demo Queries

Use the deployed tag that actually exists on presentation day. If the live server tag list returns `a`, use `ragTag=a`. If the TechQA import uses `techqa`, use `ragTag=techqa`.

Recommended live demo order:

1. First show retrieval-only mode, because it is fast and proves citation quality.
2. Then run one full answer-generation example.
3. If local generation is slow, return to retrieval-only evidence inspection and explain that citation verification remains fast.

Prepared query 1:

- Query: `AppScan Source not a supported operating system`
- Mode: retrieval-only
- Expected evidence: IBM support article about AppScan Source silent install / supported operating system issue.
- Expected document ID in local prepared corpus: `swg21512700`

Prepared query 2:

- Query: `AMQ9208 AMQ6048 AMQ9492 data conversion`
- Mode: full RAG answer or retrieval-only
- Expected evidence: WebSphere MQ / IBM MQ support note mentioning conversion from CCSID 1208 to CCSID 37.
- Expected document ID in local prepared corpus: `swg1SE46234`

Prepared query 3:

- Query: `What causes the WebSphere MQ AMQ6048 DBCS error?`
- Mode: full RAG answer
- Expected behavior: answer should mention string data conversion failure and cite the MQ support passage.
- Expected document ID in local prepared corpus: `swg1SE46234`

Prepared query 4:

- Query: `Graphical installers not supported AppScan Source Linux`
- Mode: retrieval-only
- Expected evidence: Linux/AppScan installation support note.
- Expected document ID in local prepared corpus: `swg21990930`

Prepared query 5:

- Query: `How do I configure Kubernetes autoscaling for IBM MQ?`
- Mode: weak-evidence / unsupported case
- Expected behavior: retrieved evidence may be weak or unrelated; use this to explain why citations and abstention are important future work.

## One Required Master-Deck Slide

Create one clean, dense slide with these blocks:

Title:

Grounded Technical Support QA with Local RAG

Links:

- Demo: http://35.247.103.164/
- Repo: https://github.com/Jason-Tao22/ai-rag-knowledge
- Writeup: docs/final/FINAL_WRITEUP_DRAFT.md in the repo

Problem:

Technical-support QA needs exact, source-backed answers. Generic LLMs can hallucinate product-specific troubleshooting details.

Data:

10,000 TechQA / IBM technical-support documents with article titles, URLs, document IDs, and passages.

System:

nginx frontend + Spring Boot API + PostgreSQL retrieval + local Ollama LLM + clickable citation cards.

Demo queries:

- `AppScan Source not a supported operating system`
- `AMQ9208 AMQ6048 AMQ9492 data conversion`
- `What causes the WebSphere MQ AMQ6048 DBCS error?`

Why it matters:

The answer is not a black box: the UI shows the retrieved passage and clickable source so graders can verify whether the answer is grounded.

## Suggested 1-2 Minute Script

We built a local citation-grounded RAG system for technical-support question answering. The motivation is that support questions often depend on exact product documents, error codes, and troubleshooting articles, so a fluent LLM answer is not enough unless the user can verify the source.

Our system indexes 10,000 TechQA-style IBM support documents in PostgreSQL. When a user asks a question, the backend retrieves the top supporting passages, sends them to a locally served Ollama model, and returns both a grounded answer and clickable citation cards. We also added a retrieval-only mode, which is useful during the live demo because it lets instructors immediately inspect the exact evidence passage without waiting for generation.

For the demo, we will use queries like `AppScan Source not a supported operating system` and `AMQ9208 AMQ6048 AMQ9492 data conversion`. The important thing to look for is whether the system retrieves the correct article and whether the answer stays tied to the citation. Our current limitation is that CPU-only local generation can be slower than retrieval, but the system is reproducible, Dockerized, publicly accessible, and designed around verifiability.

## Final Report Structure

Use the course template and write the report with these sections.

### 1. Objectives and Introduction

Explain that the project builds a local RAG system for technical-support QA. The objective is to answer domain-specific troubleshooting questions while exposing passage-level citations. Emphasize trust, reproducibility, and real-time demo capability.

### 2. Background and Related Work

Discuss:

- Retrieval-Augmented Generation for knowledge-intensive QA.
- Why RAG is useful compared with retraining or fine-tuning for changing/domain-specific information.
- Technical-support QA as a domain where exact source grounding matters.
- TechQA as a realistic benchmark or source for technical-support question answering.
- Hallucination risks and why citation grounding helps.

Suggested citations to include if the final writer has time:

- Lewis et al., Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks.
- Karpukhin et al., Dense Passage Retrieval.
- TechQA dataset paper/source.
- A paper about context-position or retrieval limitations, such as Lost in the Middle, if relevant.

### 3. Approach and Implementation

Describe the system pipeline:

- ingest TechQA-style JSON documents
- normalize into JSONL/database rows
- store 10,000 documents in PostgreSQL
- retrieve with full-text search
- construct a grounded prompt using top passages
- generate with a locally served Ollama model
- return answer plus citation metadata
- display answer and citations in frontend

Also describe reproducibility:

- Dockerized stack
- README setup instructions
- local model pulls
- import scripts and prepared corpus files

### 4. Data and Data Analysis

Describe the dataset:

- 10,000 indexed technical-support documents
- each document has title, source URL, document ID, and text
- documents are product/support style, with many error-code and troubleshooting terms

Useful analysis to add:

- number of documents
- average passage/document length
- examples of high-value technical terms or error codes
- a small table of sample queries and retrieved document IDs

### 5. Results and Evaluation

Recommended evaluation table columns:

- Query
- Expected article/document ID
- Retrieved top document ID
- Citation correct? yes/no
- Mode used: retrieval-only or full RAG
- Latency
- Notes

Metrics to discuss:

- Recall@k or evidence hit rate on prepared demo queries
- latency for retrieval-only mode
- latency for full answer generation
- qualitative groundedness: whether the answer matches the retrieved passage

Known current behavior:

- retrieval-only mode is fast and good for citation inspection
- full answer generation works with a local Ollama model
- CPU-only generation can be noticeably slower than retrieval

### 6. Conclusions and Future Work

Conclusion:

The project demonstrates a reproducible local RAG system that meets the course requirements for frontend, local model serving, 10k+ database entries, Dockerization, public endpoint, and clickable citations.

Future work:

- add reranking to improve retrieval quality
- add abstention when evidence is weak
- add a larger local model or GPU serving for lower latency
- expand evaluation beyond prepared demo queries
- improve source passage chunking and citation granularity

### 7. Contributions

Include a team contribution section. Fill this in with the true division of labor.

Suggested structure:

- Yifan Tao: project planning, RAG implementation, documentation/report coordination, demo preparation.
- Letong Yi: GCP deployment, infrastructure support, presentation/report preparation, endpoint testing.

Adjust these contribution bullets before final submission so they accurately reflect the real work.

## NotebookLM Output Instructions

When generating the slide:

- Make it one slide, not a full lecture deck, because the instructor's master deck asks for one slide.
- Keep the talk under two minutes.
- Include the endpoint, repository, and writeup link visibly.
- Include three demo queries.
- Include a small system diagram with 5-6 boxes.

When generating the report:

- Use an academic project-report tone.
- Do not invent performance numbers.
- Mark missing metrics as TODO if exact numbers are unavailable.
- Include a system diagram and a small evaluation table.
- Include a contributions section.
- Mention the public endpoint and repository near the top.
