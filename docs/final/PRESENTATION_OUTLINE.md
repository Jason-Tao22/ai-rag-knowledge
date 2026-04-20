# Presentation Outline

## Slide 1. Title

- project title
- team members
- one-sentence value proposition

## Slide 2. Problem

- technical support QA is retrieval-heavy
- hallucinations are dangerous when evidence is weak

## Slide 3. Our System

- local Ollama model
- PostgreSQL full-text retrieval over TechQA
- optional retrieval-only evidence mode
- citation-aware answer generation
- UI with clickable evidence

## Slide 4. Data

- TechQA corpus source
- 10,000 technotes used in the final demo
- title, URL, and passage metadata stored for each source

## Slide 5. Demo Flow

- user asks question
- retrieve top passages
- generate grounded answer
- show citations and passage evidence

## Slide 6. Results

- retrieval metric table
- latency
- one good example
- one failure case

## Slide 7. Future Work

- reranking
- better abstention
- larger corpus scale

## 3-minute Delivery Script

1. Introduce the problem and why grounding matters.
2. Explain the architecture in one sentence.
3. Show the UI, ask a prepared question, and point to the retrieved evidence.
4. End with one metric and one concrete future improvement.
