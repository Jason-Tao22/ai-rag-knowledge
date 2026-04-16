# Improving Retrieval Reliability in a Lightweight RAG System for Technical Support QA

**Name(s):** Yifan Tao, Letong Yi  
**E-mail(s):** tao.yif@northeastern.edu, yi.le@northeastern.edu

## 1. Proposed Objective

Our objective is to improve the retrieval component of a lightweight retrieval-augmented generation (RAG) system for technical support question answering. Using the public TechQA benchmark and our existing RAG web prototype, we will study how chunking, multi-query retrieval, reranking, and abstention affect evidence retrieval, answer accuracy, and hallucination rates.

## 2. Background and Motivation

Large language models are useful for general conversation, but they are much less reliable when users ask domain-specific technical questions whose answers depend on external documents. This matters for students reading unfamiliar project material, developers onboarding to a new codebase, and technical support users searching long documentation collections. In these settings, a naive RAG pipeline often fails because documents are chunked poorly, retrieved passages are noisy, and the model still answers confidently even when the supporting evidence is weak.

Our project is motivated by both practical and research concerns. Practically, reliable documentation QA can save time and reduce user frustration. From an NLP perspective, the problem combines dense retrieval, passage segmentation, ranking, grounding, and calibration rather than only text generation. Prior work on retrieval-augmented generation and dense passage retrieval shows that retrieved evidence can improve factual question answering, while later work shows that long-context models still fail when relevant evidence is buried or poorly organized. This makes retrieval quality the core bottleneck in many real-world RAG systems.

We will use the public TechQA dataset introduced by Castelli et al. (ACL 2020). TechQA contains real technical support questions collected from developer forums, which makes it more realistic than many synthetic QA benchmarks. According to the official paper, the labeled portion includes 600 training, 310 development, and 490 evaluation question-answer pairs, plus a companion retrieval corpus of 801,998 IBM Technotes. The data can be downloaded from the official IBM baseline repository at https://github.com/IBM/techqa or from the Hugging Face mirror at https://huggingface.co/datasets/PrimeQA/TechQA. For development, we plan to start with the official train/dev questions and a manageable subset of the Technote corpus, then scale toward a larger portion of the 801,998-note collection as indexing becomes stable.

TechQA is a difficult retrieval problem for several reasons. The labeled QA set is relatively small, but the retrieval corpus is very large. The documents are technical, often long, and full of domain-specific terminology. Many Technotes are lexically similar, so retrieving the truly relevant note requires more than simple keyword overlap. In addition, the answer is often supported by only a small part of a long document, which makes chunking and reranking especially important. These properties make TechQA a good fit for our project and for the RAG system that one team member has already built using Spring AI, file/Git ingestion, pgvector retrieval, and local/API LLM backends.

## 3. Proposed Approach / Implementation Details

Our baseline will be the current pipeline from the existing prototype: document extraction, token-based chunking, pgvector top-k dense retrieval, and answer generation from the retrieved chunks. We will adapt that system to TechQA and compare it against a small set of targeted improvements that are realistic within one semester.

- **What pre-processing we will do:** We will convert the Technotes to clean text, remove boilerplate and duplicated content, preserve titles and source metadata, and split documents into chunks. We will compare several chunk sizes and overlap settings because the relevant evidence may appear in only a small section of a long note.
- **What type of retrieval algorithm we will use:** Our main baseline will be dense vector retrieval with pgvector, since that already matches the current prototype. We will compare it with multi-query retrieval, where the user question is rewritten into several semantically related variants and the retrieved results are merged. If time permits, we will also add a lexical BM25 baseline or a hybrid dense-plus-lexical setup.
- **How we will remedy noise:** We will remove boilerplate text, keep metadata for filtering, deduplicate near-identical chunks, and add a lightweight reranking or score-fusion step so that semantically relevant passages are not buried under noisy but similar technical documents.
- **How we will deal with the scale of data:** Because the full TechQA companion corpus contains 801,998 Technotes, we will first build and debug on a smaller subset, cache embeddings offline, and then scale up to larger portions of the corpus. We will use pgvector indexing and batched ingestion so that the system remains feasible on limited compute.
- **What design decisions we will consider:** We will compare chunk sizes, overlap amounts, retrieval depth (for example top-5 vs. top-10), whether metadata filtering helps, and whether reranking improves final answer quality enough to justify its cost.
- **How we will validate and evaluate our results:** For retrieval, we will report Recall@k and MRR on the held-out questions. For end-to-end QA, we will use exact match and token-level F1 where appropriate, plus a small manual error analysis focused on faithfulness and hallucination. We also plan to include some unanswerable or weak-evidence cases to measure whether the system can abstain instead of hallucinating.
- **What LLM we will use:** We plan to use the models already supported by the existing web app, namely a local Ollama model for low-cost experimentation and GPT-4o for a stronger API baseline. This will help us separate retrieval improvements from generation quality.

We expect the final outcome to be both a working prototype and a set of practical recommendations for building more reliable retrieval-centered QA systems over technical documentation.

## References

1. Vittorio Castelli, Rishav Chakravarti, Saswati Dana, et al. "The TechQA Dataset." ACL, 2020.
2. Patrick Lewis, Ethan Perez, Aleksandara Piktus, et al. "Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks." NeurIPS, 2020.
3. Vladimir Karpukhin, Barlas Oguz, Sewon Min, et al. "Dense Passage Retrieval for Open-Domain Question Answering." EMNLP, 2020.
4. Nelson F. Liu, Kevin Lin, John Hewitt, et al. "Lost in the Middle: How Language Models Use Long Contexts." TACL, 2024.
5. Akari Asai, Zeqiu Wu, Yizhong Wang, et al. "Self-RAG: Learning to Retrieve, Generate, and Critique through Self-Reflection." ICLR, 2024.
