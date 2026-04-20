# GCP Demo Status

Public endpoint:

- http://35.247.103.164/

Checked on 2026-04-20:

- Homepage returns HTTP 200 through nginx.
- Initial check showed only `["a"]`, and `ragTag=a` retrieved `project-description (1).pdf`.
- The 38 MB one-shot JSONL upload was blocked by nginx with `413 Request Entity Too Large`.
- The TechQA corpus was then imported successfully through 50 smaller JSONL batch files.
- `GET /api/v1/rag/query_rag_tag_list` now returns `["a", "techqa"]`.

Conclusion:

- The public endpoint is reachable.
- The deployed database is now loaded with the final TechQA demo corpus under the expected `techqa` tag.
- The endpoint passed the presentation smoke test on 2026-04-20.

Smoke-test result:

```text
tags ok: ['a', 'techqa']
retrieve ok: swg21512700 | latency: 135 ms
query ok: swg1SE46234 | latency: 31455 ms
Smoke test passed.
```

## Reimport Command If Needed

On the GCP machine or from any machine that can reach the backend import endpoint, import the processed TechQA JSONL:

```bash
bash scripts/import_jsonl_corpus.sh techqa data/processed/techqa_10k.jsonl http://35.247.103.164
```

If nginx returns `413 Request Entity Too Large`, use the smaller batch files instead:

```bash
for f in data/processed/techqa_batches/batch_*.jsonl; do
  bash scripts/import_jsonl_corpus.sh techqa "$f" http://35.247.103.164
done
```

If `data/processed/techqa_10k.jsonl` does not exist yet, prepare it from the raw TechQA technotes JSON:

```bash
python3 scripts/prepare_techqa_jsonl.py \
  --input data/raw/TechQA/training_and_dev/training_dev_technotes.json \
  --output data/processed/techqa_10k.jsonl \
  --limit 10000
```

Then rerun:

```bash
bash scripts/presentation_smoke_test.sh http://35.247.103.164
```

The endpoint remains presentation-ready as long as:

- the tag list includes `techqa`
- the AppScan retrieval query returns document ID `swg21512700`
- the MQ query returns document ID `swg1SE46234`
- the UI shows clickable citations for the retrieved sources
