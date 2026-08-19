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
        'smoke:',
        'name: Unity EditMode standalone smoke',
        'Run Unity EditMode standalone smoke',
        'build-apk:',
        'name: Build standalone offline APK',
        'Build Android offline APK',
        'version: 0.1.${{ github.run_number }}',
        'NinjaAssemble-offline-playtest.apk',
        'sha256sum NinjaAssemble-offline-playtest.apk',
        'Upload offline APK candidate',
        'publish:',
        'needs.smoke.result == \'success\'',
        'needs.build-apk.result == \'success\'',
        'actions/download-artifact@v4',
        'sha256sum -c SHA256SUMS.txt',
        'VERSION="0.1.${GITHUB_RUN_NUMBER}"',
        'gh release create',
        '--prerelease',
        'permissions:',
        'contents: write',
        'checks: write',
    ]
    for marker in required:
        if marker not in text:
            errors.append(f'workflow missing required marker: {marker}')

    # The APK build must not be gated by the smoke job. A failing smoke test must
    # still leave us a candidate APK for diagnosis, while publish remains blocked.
    build_start = text.find('  build-apk:')
    publish_start = text.find('  publish:')
    if build_start < 0 or publish_start < 0 or publish_start <= build_start:
        errors.append('workflow must define build-apk before publish')
    else:
        build_block = text[build_start:publish_start]
        if 'needs: smoke' in build_block or '- smoke' in build_block.split('steps:', 1)[0]:
            errors.append('build-apk must not depend on smoke; candidate APK must still build when smoke fails')
        if 'needs: contract' not in build_block:
            errors.append('build-apk must depend directly on contract')

    smoke_start = text.find('  smoke:')
    if smoke_start < 0 or build_start <= smoke_start:
        errors.append('workflow must define smoke as its own job before build-apk')
    else:
        smoke_block = text[smoke_start:build_start]
        if 'needs: contract' not in smoke_block:
            errors.append('smoke must depend directly on contract')

    publish_block = text[publish_start:] if publish_start >= 0 else ''
    for marker in ['- smoke', '- build-apk', "github.event_name != 'pull_request'"]:
        if marker not in publish_block:
            errors.append(f'publish gate missing required dependency/condition: {marker}')

    if 'continue-on-error:' in text:
        errors.append('offline release workflow must not hide failures with continue-on-error')
    if 'ANDROID_KEYSTORE' in text:
        errors.append('offline playtest release must not require Android store signing secrets')
    if 'inputs.release_version' in text:
        errors.append('release workflow must not read workflow_dispatch-only inputs on PR/push builds')

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
