# Production Hero Art Packages

Real release art is ingested under:

`art/packages/<character_id>/<variant-slug>/package.json`

The descriptor follows `art/hero-art-package.schema.json` and points to **repo-relative files** that back each component gate.

Example structure:

```text
art/packages/naruto-uzumaki/sage-mode/package.json
client-unity/Assets/GameContent/Heroes/naruto-uzumaki/sage-mode/portrait.png
client-unity/Assets/GameContent/Heroes/naruto-uzumaki/sage-mode/icon.png
client-unity/Assets/GameContent/Heroes/naruto-uzumaki/sage-mode/NarutoSage.prefab
client-unity/Assets/GameContent/Heroes/naruto-uzumaki/sage-mode/animation/NarutoSage.controller
client-unity/Assets/GameContent/Heroes/naruto-uzumaki/sage-mode/vfx/VfxCatalog.asset
client-unity/Assets/GameContent/Heroes/naruto-uzumaki/sage-mode/audio/SfxCatalog.asset
art/regression/naruto-uzumaki/sage-mode/battle-reference.png
art/reviews/naruto-uzumaki/sage-mode/review.md
```

A component may be marked `READY` in `hero-art-component-status.csv` only after its descriptor path points to a file that actually exists. `review_status=READY` additionally requires at least one existing review-evidence file.

Concept/TODO work is allowed to remain external or incomplete; it does not need a descriptor until a component is promoted to READY.
