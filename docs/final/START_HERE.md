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
- `../../scripts/presentation_smoke_test.sh`
  - quick verification script before presentation

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

1. use `SLIDE_CONTENT_DRAFT.md` to fill the slide deck
2. use `FINAL_WRITEUP_DRAFT.md` to write the final report
3. use `PROJECT_STATUS.md` and the README to verify the demo locally
4. use `DEMO_QUERIES.md` and the smoke test before presentation
