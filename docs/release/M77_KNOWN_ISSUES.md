# M77 Known Issues and Release Blockers

The items below are intentionally visible and must not be downgraded to warnings for RC certification.

1. **Reference/parity evidence is incomplete.** M74 strict verification remains blocked until all ten reference profiles are VERIFIED, feature census rows are PARITY_PASS and full-roster identity/mechanics/balance review is complete.
2. **Real E2E evidence is missing.** M75 requires a PASS report for the exact candidate SHA covering all 20 journey steps and ten reliability cases with retained artifacts.
3. **Physical Android evidence is missing.** M76 requires at least two passing physical Android models across at least two device classes on the exact candidate SHA.
4. **Unity build credentials are not configured in repository Actions.** The Android development APK lane currently stops at `UNITY_LICENSE`, `UNITY_EMAIL` and `UNITY_PASSWORD` preflight; release AAB signing additionally requires Android keystore secrets.
5. **Production art is incomplete.** M70–M73 assign all 427 packages but assignment does not equal READY. Release requires every package to have real portrait/icon/chibi/animation/VFX/SFX/regression/review evidence.
6. **Third-party character/IP rights are not documented.** The current Naruto-derived character identities/techniques are tracked as `RIGHTS_NOT_DOCUMENTED`; public/commercial distribution is blocked until rights are documented or the content is replaced with original/licensed IP.
7. **Store/operator evidence is incomplete.** Public privacy/support URLs, store content-rating/data-safety review, signed AAB reproducibility evidence and database backup/restore evidence remain pending.
8. **Spring Boot 4 migration debt remains.** Persisted JSON services currently use the temporary `spring-boot-jackson2` compatibility bridge. It is tested and operational, but should be migrated to the Boot 4 Jackson 3 stack before that compatibility module is removed upstream.

No release tag should be created while any item above remains unresolved.
