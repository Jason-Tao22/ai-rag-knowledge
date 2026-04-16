# Demo Queries

These are the prepared queries we should use on presentation day.

## Recommended live-demo order

1. start with retrieval-only mode to prove citation quality instantly
2. then run one grounded-answer example with the local model
3. if latency becomes an issue, switch back to retrieval-only mode and inspect evidence

## Query 1: AppScan installation issue

- Mode: retrieval-only
- Query:
  - `AppScan Source not a supported operating system`
- What to show:
  - the top citation should be the IBM support article about silent install failing on a supported OS
  - point to the exact sentence in the retrieved passage
- Expected top document:
  - `swg21512700`

## Query 2: MQ data conversion error

- Mode: grounded answer or retrieval-only
- Query:
  - `AMQ9208 AMQ6048 AMQ9492 data conversion`
- What to show:
  - retrieved evidence mentions conversion from CCSID 1208 to CCSID 37
  - the answer should stay grounded in the cited passage
- Expected top document:
  - `swg1SE46234`

## Query 3: MQ DBCS error explanation

- Mode: grounded answer
- Query:
  - `What causes the WebSphere MQ AMQ6048 DBCS error?`
- What to show:
  - the answer should mention string data conversion failure
  - citation should still point to the same MQ support article
- Expected top document:
  - `swg1SE46234`

## Query 4: Linux graphical installer issue

- Mode: retrieval-only
- Query:
  - `Graphical installers not supported AppScan Source Linux`
- What to show:
  - the system should retrieve the Linux installation support note
  - this is a good example of citation grounding without generation
- Expected top document:
  - `swg21990930`

## Query 5: weak-evidence / unsupported question

- Mode: retrieval-only first, then optionally grounded answer
- Query:
  - `How do I configure Kubernetes autoscaling for IBM MQ?`
- What to show:
  - the system may retrieve weak or unrelated documents
  - explain that this is a limitation case and why citation visibility matters

## Suggested talking points

- retrieval-only mode demonstrates verifiability and low latency
- grounded-answer mode demonstrates the complete local RAG loop
- when the answer is slow, the evidence is still immediately inspectable
- the project prioritizes trust and reproducibility over flashy generation
