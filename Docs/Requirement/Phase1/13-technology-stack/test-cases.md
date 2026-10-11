# Test Cases — 13-technology-stack

| ID | Title | Expected | Priority |
|---|---|---|---|
| TC-01 | Mandated stack present [Auto] | `TechStackTest.sharedModuleUsesTheMandatedStack` passes | High |
| TC-02 | Both platforms' driver and engine [Auto] | passes | Medium |
| TC-03 | No Room [Auto] | Adding `room` to the catalog fails the test | High |
| TC-04 | Guards re-run on change [Auto] | Catalog/`androidApp` edits re-run the host tests (mutation checked) | High |
| TC-05 | Both apps build (regression) | `assembleDebug`, Xcode simulator build succeed | Medium |
