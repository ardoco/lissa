# WARC Dataset
See https://github.com/ArDoCo/Replication-Package-REFSQ25_Requirements-TLR-via-RAG/tree/main/datasets/req2req/WARC

Data set:
WARC
Description:
Tools for the WARC file format for storing web archives
Source:
high level reqs. (FRS+NFR)
Target:
low level reqs. (SRS)

Directory structure (this repository)
answer.csv: answer matrix file (136 links, with a `high,low` header row)
high: directory that contains source artifacts (63 files)
low: directory that contains target artifacts (89 files)
cache: recorded embedding and classifier caches for the offline replay (see below)
config.json: evaluation configuration for the offline replay
WARC_*.json: the four prompt-optimizer configurations used by the end-to-end test

Files in the upstream dataset (not vendored here, see the link above)
FRS: directory that contains Functional Requirements Specification artifacts (42 files)
NFR: directory that contains Non-Functional Requirements Specification artifacts (21 files)
SRS: directory that contains Software Requirements Specification artifacts (89 files)
FRStoSRS.txt: Functional Requirements Specification to Software Requirements Specification gold standard
NFRtoSRS.txt: Non-Functional Requirements Specification to Software Requirements Specification gold standard
warc_tools_frs.pdf: original Functional Requirements Specification document
warc_tools_nfr.pdf: original Non-Functional Requirements Specification document
warc_tools_srs.pdf: original Software Requirements Specification document

## Offline replay fixture

`config.json` runs the WARC req2req pipeline entirely from the recorded caches in `cache/`, so the
end-to-end test needs no network access and no real credentials:

- `cache/OpenAiEmbeddingCreator_text-embedding-3-large.json` — recorded `text-embedding-3-large` embeddings.
- `cache/SimpleClassifier_gpt-4o-mini-2024-07-18_133742243.json` — recorded `gpt-4o-mini-2024-07-18`
  classifications (seed 133742243, temperature 0.0).
- `cache/optimizerTests/{simple,iterative,feedback,gradient}/` — caches for the four prompt-optimizer
  configurations `WARC_simple_*.json`, `WARC_iterative_*.json`, `WARC_feedback_*.json`, `WARC_gradient_*.json`.

`Requirement2RequirementE2ETest#testEnd2End` replays `config.json` and asserts precision `0.38`,
recall `0.6985294117647058` and F1 `0.49222797927461137`. `#testEnd2EndOptimizers` replays the four
optimizer configurations against `src/test/resources/expected/*Expectation.txt`.

`src/test/resources/.env-test` supplies placeholder credentials (`OPENAI_API_KEY=DUMMY`). They must be
set even though no request is ever made, because the OpenAI clients validate them at construction
time — and their invalidity is deliberate: it makes an accidental cache miss fail loudly instead of
silently calling the API.

Do not edit the prompts, the `artifact_type` values, the module arguments in `config.json`, or upgrade
langchain4j without re-recording the caches. Every one of those changes the derived cache keys, and a
mismatch is a silent miss rather than an error. See `docs/caching.md`.

Data source:
Kong, W.-K., Hayes, J. H., Dekhtyar, A. und Dekhtyar, O. „Process Improvement for Traceability: A Study of Human Fallibility“. In: 2012 20th IEEE International Requirements Engineering Conference (RE). 2012 20th IEEE International Requirements Engineering Conference (RE). Sep. 2012, p. 31–40.
