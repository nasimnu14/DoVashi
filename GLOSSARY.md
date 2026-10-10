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

**Message**:
One spoken turn in a Conversation: its Recording, the transcribed text in the
Source Language, the translation into the Target Language, and an optional
Reading.
_Avoid_: chat item, bubble (a bubble is only how a Message is drawn)

**Recording**:
The audio captured for one Message, kept in app-private storage and played back
on request.
_Avoid_: voice note, audio clip

**Reading**:
A Latin-script, English-readable pronunciation of a Message's translation, in
the Target Language's Reading system (for example Hanyu Pinyin with tone marks
for Mandarin Chinese). Present only when the Target Language has a Reading
system.
_Avoid_: romanization (as a field name), pronunciation text

**Message Status**:
Where a Message is in its life: recording, transcribing, translating, then
completed, or failed at any stage. A failed Message can be retried, either from
its Recording or from its saved transcript.
_Avoid_: state (unqualified), progress

**Source Language**:
The Language a Message was spoken in, detected from its Recording and always one
of the Conversation's Language Pair. The user never picks it.

**Target Language**:
The other Language of the Conversation's Language Pair, into which a Message is
translated.
