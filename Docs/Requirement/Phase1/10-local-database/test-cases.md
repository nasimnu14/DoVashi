# Test Cases — 10-local-database

| ID | Title | Steps | Expected | Priority |
|---|---|---|---|---|
| TC-01 | Conversation columns [Auto] | `SchemaConformanceTest` | Exactly the six doc-10 columns | High |
| TC-02 | Message columns + FK [Auto] | same | Ten columns; FK to Conversation | High |
| TC-03 | Status values [Auto] | same | The five names | Medium |
| TC-04 | Unmigrated change is caught [Auto] | Add a column to `Message.sq`, run `verifySqlDelightMigration` | Fails: column ADDED | High |
| TC-05 | Offline browsing (manual) | Airplane mode; open Home, a Conversation, play a Recording | All work; no errors | High |
| TC-06 | Upgrade keeps data (manual, when a v2 exists) | Install old build, add data, install new | Data intact | High |
