# M66 Production UI Foundation

M66 creates reusable production UI primitives instead of extending the generic mobile scene shell.

Foundation includes design tokens, 52px minimum touch targets, safe-area handling, compact/regular/wide responsive breakpoints, reusable text/panel/button/layout factories, loading/ready/empty/error/offline states with retry, and haptic/audio feedback hooks.

M67 and M68 build product screens on `ProductionScreenHost` and these primitives. The old generic shell remains compatibility scaffolding until all production screens are wired.
