# Final Writeup Draft

## Title

Grounded Technical Support QA with a Local Citation-Aware RAG System

## Team

- Yifan Tao
- Letong Yi

## 1. Objectives and Introduction

We build a local retrieval-augmented generation system for technical support QA. The system retrieves evidence from a TechQA-style document collection, answers user questions with a locally served LLM, and exposes passage-level citations so users can verify the response.

Reuse from the proposal:
- objective
- motivation
- why retrieval reliability is the main bottleneck

Add final-project contributions:
- local deployment
- citation-aware answer generation
- UI evidence inspection
- reproducible ingestion pipeline

## 2. Background and Related Work

- RAG for knowledge-intensive QA
- document retrieval for technical QA
- retrieval reliability and hallucination risk
- TechQA dataset and why it is a realistic benchmark

Need to cite:
- TechQA paper
- RAG paper
- DPR
- Lost in the Middle
- any reranking or abstention paper actually used

## 3. Approach and Implementation

### System overview

- frontend
- backend
- PostgreSQL full-text search index
- optional pgvector fallback for generic uploads
- Redis
- local Ollama model
- corpus ingestion pipeline

### Retrieval pipeline

- corpus preprocessing
- TechQA JSON to JSONL conversion
- import into `rag_corpus_documents`
- PostgreSQL full-text retrieval with `websearch_to_tsquery`
- optional fallback to vector search for non-TechQA uploaded files

### Generation pipeline

- citation-aware prompt
- answer generation
- evidence card rendering

### Reproducibility

- Docker compose stack
- startup steps
- model pulls
- corpus preparation scripts

Repository README:
- link the final GitHub URL here

## 4. Data and Data Analysis

### Data source

- official TechQA source
- final demo uses 10,000 IBM technotes
- 10,000 indexed PostgreSQL rows in the stable final path
- each row stores title, URL, document ID, and evidence text

### Data analysis

- document length distribution
- question length distribution
- sample retrieved passages
- any cleaning or deduplication decisions

## 5. Results and Evaluation

### Retrieval metrics

- Recall@k
- MRR
- evidence hit rate on prepared demo queries

### End-to-end behavior

- exact match / token F1 if used
- latency
- qualitative examples
- failure analysis
- comparison between retrieval-only mode and answer-generation mode

### Corner cases

- unsupported question
- weak evidence
- lexically similar but wrong doc

## 6. Conclusions

- what worked
- what still fails
- future improvements

## 7. Contributions

- Yifan Tao:
- Letong Yi:

## Figures to Add

- system diagram
- retrieval pipeline figure
- screenshot of UI with citations
- table of evaluation metrics
