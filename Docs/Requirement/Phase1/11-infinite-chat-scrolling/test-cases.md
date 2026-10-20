# Test Cases — 11-infinite-chat-scrolling

| ID | Title | Expected | Priority |
|---|---|---|---|
| TC-01 | SQL limit [Auto] | Only the newest N rows; more with a bigger limit | High |
| TC-02 | Window stays live and slides [Auto] | New Message appears, oldest of the window drops | High |
| TC-03 | VM opens with one page of 100 [Auto] | 30 bubbles, newest first | High |
| TC-04 | VM loads older page by page [Auto] | 60, then all 70; extra calls no-op | High |
| TC-05 | Small Conversation [Auto] | `onLoadOlder` ignored | Medium |
| TC-06 | Arrival keeps older loaded [Auto] | Oldest loaded stays; size +1 | High |
| TC-07 | Reopen resets to newest page [Auto] | 30 bubbles | Medium |
| TC-08 | Scroll trigger (manual) | Seed 200 Messages; scroll up steadily: older pages appear without a visible jump; top reached stops loading | High |
| TC-09 | Opens at newest (manual) | Back out and reopen a long Conversation: newest at bottom | High |
| TC-10 | Rotation (manual) | Scroll position kept | Low |
| TC-11 | New Message while scrolled up (manual) | Record while reading old Messages: view does not jump | Medium |
