from pathlib import Path

root = Path(__file__).resolve().parents[1]
test = (root / 'client-unity/Assets/Tests/Editor/OfflineStandaloneSmokeTests.cs').read_text(encoding='utf-8')
bootstrap = (root / 'client-unity/Assets/Scripts/Game/Bootstrap/MobileGameBootstrap.cs').read_text(encoding='utf-8')
progression = (root / 'client-unity/Assets/Scripts/Game/Heroes/HeroProgressionPlayableBridge.cs').read_text(encoding='utf-8')
seed = (root / 'client-unity/Assets/Scripts/Game/Playable/OfflineSeedFactory.cs').read_text(encoding='utf-8')
repo = (root / 'client-unity/Assets/Scripts/Game/Playable/OfflineSaveRepository.cs').read_text(encoding='utf-8')
workflow = (root / '.github/workflows/offline-runtime-smoke.yml').read_text(encoding='utf-8')

for required in [
    'OfflineService_FullHappyPath_PersistsWithoutNetwork',
    'GeneratedMobileScenes_HaveRequiredBindings_AndOfflineBootstrapDefault',
    'FightArenaAsync', 'FightShadowArenaAsync', 'PurchaseShopAsync', 'ClaimQuestAsync',
    'ClaimMailAsync', 'SummonAsync', 'LevelUpAsync', 'OFFLINE_TEST'
]:
    assert required in test, f'missing runtime smoke assertion: {required}'

assert 'runtimeMode = MobileRuntimeMode.OfflinePlaytest' in bootstrap
assert 'OFFLINE TEST (no economy charge)' in progression
assert 'SaveFormationAsync' in progression, 'offline frame advance must persist through local service state'
assert 'offline-awakening-' in seed and 'Offline Awakening Test' in seed
assert 'data.readMailIds = data.readMailIds ??' in repo
assert 'game-ci/unity-test-runner@v4' in workflow
assert 'testMode: EditMode' in workflow
assert 'UNITY_LICENSE' in workflow and 'UNITY_EMAIL' in workflow and 'UNITY_PASSWORD' in workflow
print('offline runtime smoke contract: OK')
