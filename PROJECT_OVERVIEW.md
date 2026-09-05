# DocLock: interview-ready AIML overview

DocLock is a private document-intelligence application built with React, Spring
Boot, PostgreSQL, and Google GenAI.

## RAG pipeline

1. A PDF is validated, stored, and converted to text with Apache Tika.
2. Text is split into overlapping, boundary-aware chunks to preserve context.
3. Each chunk is converted into a dense embedding and stored with its source
   metadata.
4. For a question, DocLock creates a context-aware retrieval query from recent
   chat history, embeds it, and ranks chunks using hybrid retrieval:
   `0.80 × cosine similarity + 0.20 × keyword overlap`.
5. The LLM receives only the highest-ranked, labelled source chunks and is
   instructed to answer from evidence rather than invent facts.

## Structured document intelligence

In addition to semantic chunks, DocLock extracts high-value fields with source
provenance: Aadhaar/ID candidates, certificate and registration numbers,
renewal/expiry dates, PAN, email, and phone numbers. This gives direct factual
questions an evidence-backed structured path while general questions continue
through hybrid RAG. Sensitive Aadhaar and PAN values are masked by default and
can be explicitly requested in full by the authenticated vault owner.

## Concepts to explain in an interview

- **Embeddings:** numeric semantic representations that make paraphrases
  retrievable.
- **Cosine similarity:** compares the direction of vectors to estimate semantic
  relatedness.
- **Hybrid retrieval:** combines dense vector matching with lexical matching,
  which is useful for both paraphrased questions and exact entities such as
  names, IDs, and dates.
- **Grounded generation:** the answer is constrained by retrieved source text;
  the UI shows source document names, chunk numbers, and relevance.
- **Failure-aware product design:** invalid/scanned PDFs, missing server
  configuration, storage errors, and no-match queries return actionable errors.

This version deliberately stores embeddings with document chunks instead of
requiring a manually created `pgvector` table. It remains deployable on a fresh
Render PostgreSQL instance while demonstrating the same RAG and vector-search
fundamentals. For a larger corpus, the next scaling step is PostgreSQL pgvector
with an HNSW index, metadata filtering, and offline evaluation of retrieval
precision/recall.
