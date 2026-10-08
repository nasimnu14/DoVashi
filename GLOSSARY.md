# DoVashi

A mobile app that lets two people hold a natural voice conversation in two
languages, translating each spoken message into the other language.

## Language

**Conversation**:
A voice exchange between two people, each of whom may speak either Language of
its Language Pair.

**Language**:
A human language a Conversation can be held in, identified by its Language Code.

**Language Code**:
The short identifier of a Language, such as `en` or `zh`, by which the rest of
the app refers to it.

**Language Catalog**:
The open-ended set of Languages from which every Language Pair is chosen. Its
size is never assumed to be two.
_Avoid_: language list, supported languages

**Language Pair**:
The two different Languages a Conversation is held in, distinguished as
Language 1 and Language 2.
_Avoid_: Language A / Language B

**Mandarin Chinese**:
Standard Mandarin written in Simplified Chinese characters; the Language whose
Language Code is `zh`.
_Avoid_: Chinese, Mandarin
