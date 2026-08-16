# Art Direction — Ninja Assemble Chibi Parity

## Target language

The presentation target is a 2D mobile-RPG **super-deformed/chibi** silhouette rather than modern semi-realistic anime rendering.

Character target:

- approximately 2–2.5 heads tall;
- oversized head and eyes;
- short arms/legs;
- simple, readable hands/feet;
- thick/clean anime outline;
- mostly flat cel shading;
- saturated costume colors;
- strongly exaggerated attack poses;
- compact idle loops;
- large facial expression changes.

Combat target:

- side-on 2D arena;
- five-character squad readability;
- VFX can exceed the character silhouette by 2–4× for major skills;
- strong anticipation → hit-stop → impact → recovery timing;
- readable damage numbers/status icons;
- simple camera shake/zoom for ultimates;
- original high-resolution redraws instead of low-resolution ripped sprites.

## Asset package contract

Each hero variant should eventually provide:

```text
hero/<definition-id>/
  portrait
  roster-icon
  body/skeleton
  idle
  enter
  basic-attack
  active-skill(s)
  ultimate
  hit
  stun
  death
  revive
  transform
  victory
  vfx/*
  sfx/*
```

Missing presentation assets must never block battle-domain implementation; placeholder address keys are allowed until the art pass.
