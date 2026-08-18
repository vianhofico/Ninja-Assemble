from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = ROOT / '.github/workflows/offline-playtest-release.yml'
README = ROOT / 'README.md'

errors = []

if not WORKFLOW.is_file():
    errors.append('offline playtest release workflow is missing')
else:
    text = WORKFLOW.read_text(encoding='utf-8')
    required = [
        'name: Offline Playtest Release',
        'Run Unity EditMode standalone smoke',
        'Build Android offline APK',
        'NinjaAssemble-offline-playtest.apk',
        'sha256sum NinjaAssemble-offline-playtest.apk',
        "if: github.event_name != 'pull_request'",
        'gh release create',
        '--prerelease',
        'permissions:',
        'contents: write',
    ]
    for marker in required:
        if marker not in text:
            errors.append(f'workflow missing required marker: {marker}')
    if 'ANDROID_KEYSTORE' in text:
        errors.append('offline playtest release must not require Android store signing secrets')

if not README.is_file():
    errors.append('README.md is missing')
else:
    text = README.read_text(encoding='utf-8')
    for marker in [
        '## Tải bản Offline Test',
        'không cần PostgreSQL',
        'không cần Redis',
        'không cần Java/Spring Boot server',
        'Releases',
        'Offline Playtest',
    ]:
        if marker not in text:
            errors.append(f'README missing offline release guidance: {marker}')

if errors:
    for error in errors:
        print(f'ERROR: {error}')
    raise SystemExit(1)

print('Offline playtest release contract OK')
