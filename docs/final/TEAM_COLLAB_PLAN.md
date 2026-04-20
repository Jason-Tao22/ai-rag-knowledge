# Team Collaboration Plan

## Shared goal

Ship one stable final-project repo that includes:

- runnable demo
- final writeup
- slide content
- Dockerized setup
- reproducible citation-grounded TechQA RAG workflow

## Suggested work split

### Track 1: System and demo stability

Focus:
- backend cleanup
- demo queries
- latency sanity checks
- final README polish

Suggested files:
- `xfg-dev-tech-trigger/src/main/java/.../RAGController.java`
- `docs/tag/v1.0/nginx/html/js/index.js`
- `docker-compose.final.yml`
- `README.md`

### Track 2: Writeup and figures

Focus:
- turn draft sections into final prose
- add references
- add evaluation table
- add screenshot and system diagram

Suggested files:
- `docs/final/FINAL_WRITEUP_DRAFT.md`
- `docs/final/SYSTEM_DIAGRAM.md`
- exported PDF when ready

### Track 3: Slides and presentation prep

Focus:
- fill the slide deck
- shorten content to 2 to 3 minute presentation length
- prepare demo script
- prepare corner-case questions

Suggested files:
- `docs/final/SLIDE_CONTENT_DRAFT.md`
- `docs/final/PRESENTATION_OUTLINE.md`
- `docs/final/Final_Presentation_Template.pptx`

## Concrete checklist

- [ ] confirm the final project title
- [ ] confirm team-member contribution lines
- [ ] prepare 5 to 8 demo queries
- [ ] collect 1 screenshot of retrieval-only mode
- [ ] collect 1 screenshot of full answer mode
- [ ] add one evaluation table
- [ ] finish slide text
- [ ] export final PDF writeup
- [ ] test Docker setup from a clean restart
- [ ] confirm public presentation-day endpoint plan

## Recommended branch workflow

1. create a branch from the published collaboration branch
2. each teammate works in a small topic branch if needed
3. open PRs or merge back into the shared branch after checks
4. keep `main` as the stable integration branch for the final submission

## Immediate handoff tasks

### Person A

- polish writeup sections 1 to 3
- add references and citations

### Person B

- fill slide deck from the draft content
- prepare 2-minute script

### Person C

- gather demo screenshots
- verify Docker startup and local commands

### Person D

- prepare evaluation examples
- document failure cases and limitations
