# Feature: Message Structure

Every message contains:

## 1. Voice

The original recorded audio. User can tap Play/Pause to hear it.

## 2. Transcribed Text

The speech converted to text in the original (detected) language.

Example: `How are you?`

## 3. Detected/Source Language

Example: `English`

## 4. Translated Text

The translation into the other conversation language.

Example: `你好吗？`

## 5. Pronunciation / Reading (optional, language-dependent)

- Chinese target → provide an English-readable pinyin reading:

  ```text
  你好吗？
  Nǐ hǎo ma?
  ```

- English target → `reading` is not required (`null`).

**Phase 1 constraint:** the logic that decides whether a reading is needed
must be driven by the *target language's* properties, not a special case
for "Chinese". The same field/flag will be reused in Phase 2 for every other
non-Latin-script language.
