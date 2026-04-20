# Final Project Start Here

This folder is the handoff hub for the final project.

## If you are a teammate joining the repo

Read these files in order:

1. `PROJECT_STATUS.md`
   - what the project currently is
   - what is already working
   - what still needs to be finished

2. `TEAM_COLLAB_PLAN.md`
   - suggested division of labor
   - checklist for the remaining work

3. `README.md` in the repo root
   - how to run the system
   - how to pull models
   - how to test retrieval and grounded QA
   - how to pull raw dataset files with Git LFS
   - how to run the presentation smoke test

## Writeup files

- `NOTEBOOKLM_SOURCE_TEXT.md`
  - copy/paste source packet for NotebookLM to generate the required one-slide pitch, speaking script, system diagram, and final report draft
- `FINAL_PROJECT_WRITEUP.tex`
  - filled LaTeX final report draft based on the instructor template
- `FINAL_WRITEUP_DRAFT.md`
  - draft structure for the final report
- `SYSTEM_DIAGRAM.md`
  - current system diagram

## Slide files

- `SLIDE_CONTENT_DRAFT.md`
  - text draft to paste into the class slide deck
- `PRESENTATION_OUTLINE.md`
  - short speaking outline
- `Final_Presentation_Template.pptx`
  - copy of the presentation deck template we are filling

## Demo prep files

- `DEMO_QUERIES.md`
  - prepared live-demo questions and expected evidence
- `GCP_DEMO_STATUS.md`
  - current public endpoint smoke-test result and reimport steps if the TechQA corpus needs to be restored
- `PUBLIC_ENDPOINT_PLAN.md`
  - how to expose the local demo through a public URL on presentation day
- `../../scripts/presentation_smoke_test.sh`
  - quick verification script before presentation
- `../../scripts/start_public_endpoint.sh`
  - starts the public tunnel for the local frontend
- `../../scripts/stop_public_endpoint.sh`
  - stops the public tunnel

## Reference materials

These are copies of the original course and proposal materials:

- `reference/course_final_project_template.pdf`
- `reference/course_proposal_template.pdf`
- `reference/team_proposal_draft.md`
- `reference/team_proposal_draft.tex`

## Raw data note

The repository now tracks `data/raw` with Git LFS. If you clone the repo and want the original TechQA raw files locally, install Git LFS and run:

```bash
git lfs pull
```

The extracted raw JSON files are in the repo. The duplicate archive file `data/raw/TechQA.tar.gz` is kept local only because GitHub LFS does not accept single files larger than 2 GB.

## Recommended next actions

1. use `NOTEBOOKLM_SOURCE_TEXT.md` to generate the one-slide pitch, talk script, diagram, and report draft
2. use `SLIDE_CONTENT_DRAFT.md` to fill the slide deck
3. use `FINAL_WRITEUP_DRAFT.md` to write the final report
4. use `PROJECT_STATUS.md` and the README to verify the demo locally
5. use `DEMO_QUERIES.md` and the smoke test before presentation
