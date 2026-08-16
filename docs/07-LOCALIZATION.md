# Localization — English + Vietnamese

## Requirement

English and Vietnamese are first-class languages from the start. Language switching must not require reinstalling or rebuilding the game.

## Rules

- code never contains player-facing text when a localization key can be used;
- hero names, variants, skill names/descriptions, item names, progression labels, quests, events, tutorials and error messages receive keys;
- content definitions reference keys rather than embedding UI prose;
- English is the fallback language;
- missing keys fail validation before release;
- Vietnamese text uses UTF-8 throughout server, game-data and Unity.

## Client

`LocalizationService` loads the shared CSV key table and exposes runtime language switching. UI components subscribe to a language-changed event and refresh labels.

## Release gate

The mobile build is incomplete while any required key lacks either an English or Vietnamese value.
