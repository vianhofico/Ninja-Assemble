from pathlib import Path

root = Path(__file__).resolve().parents[1]
service = (root / 'client-unity/Assets/Scripts/Game/Playable/OfflinePlayableGameService.cs').read_text(encoding='utf-8')
interface = (root / 'client-unity/Assets/Scripts/Game/Playable/IPlayableGameService.cs').read_text(encoding='utf-8')
roster = (root / 'client-unity/Assets/Scripts/Game/Heroes/RosterFormationPlayableBridge.cs').read_text(encoding='utf-8')
progression = (root / 'client-unity/Assets/Scripts/Game/Heroes/HeroProgressionPlayableBridge.cs').read_text(encoding='utf-8')
save = (root / 'client-unity/Assets/Scripts/Game/Playable/OfflineSaveData.cs').read_text(encoding='utf-8')

required_actions = [
    'GetAwakeningAsync', 'AwakenAsync', 'FightArenaAsync', 'FightShadowArenaAsync',
    'PurchaseShopAsync', 'ClaimQuestAsync', 'ReadMailAsync', 'ClaimMailAsync'
]
for action in required_actions:
    assert action in interface, f'missing service contract: {action}'
    assert f' {action}(' in service, f'missing offline implementation: {action}'

for forbidden in [
    'Arena training is delivered in offline playtest 3/5',
    'Shadow Arena training is delivered in offline playtest 3/5',
    'Offline shop actions are delivered in offline playtest 3/5',
    'Offline quest claims are delivered in offline playtest 3/5',
    'Offline mail claims are delivered in offline playtest 3/5'
]:
    assert forbidden not in service, f'stale unsupported action remains: {forbidden}'

assert 'private IPlayableGameService api;' in roster
assert 'as IPlayableGameService' in roster
assert 'private GameApiClient api;' not in roster
assert 'MobileGameBootstrap.IsOffline' in progression
assert 'arenaRating' in save and 'shadowRating' in save and 'readMailIds' in save
assert 'OFFLINE_TEST' in service
print('offline screens integrity: OK')
