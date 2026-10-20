# Test Cases — 16-architecture-and-extensibility

| ID | Title | Expected | Priority |
|---|---|---|---|
| TC-01 | Structure [Auto] | Required folders exist in all three source sets | Medium |
| TC-02 | Domain layering [Auto] | A domain file importing `data.*` fails | High |
| TC-03 | Presentation layering [Auto] | A presentation file importing `data.*` fails | High |
| TC-04 | Platform-free common [Auto] | No `android.`/`java.`/`platform.` imports in `commonMain` | High |
| TC-05 | OpenAI behind ports [Auto] | Naming an adapter outside `data/network/openai` and `di` fails | High |
| TC-06 | No hard-coded language [Auto] | A `"zh"`/`"en"` literal outside the catalog metadata fails | High |
| TC-07 | Phase 2 dry run (manual) | Add a third Language to the catalog (e.g. Japanese with its scripts and Reading system): it appears in the pair pickers, a Conversation with it completes the pipeline with no other code change | High |
