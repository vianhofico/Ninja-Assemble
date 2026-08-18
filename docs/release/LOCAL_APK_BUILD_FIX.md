# Local Android APK build fix

This hardening change keeps the Unity runtime code compatible with the project's C# 9 compiler configuration. `LocalizationService.Entry` is implemented as a readonly struct rather than a C# 10 `record struct`, preserving the existing immutable value semantics required by localization lookup while allowing the Android development APK workflow to compile under Unity 6000.0.42f1.

The purpose of this branch is limited to producing a verified local/playtest APK. It does not claim production release, store signing, physical-device certification, or M77 production evidence completion.
