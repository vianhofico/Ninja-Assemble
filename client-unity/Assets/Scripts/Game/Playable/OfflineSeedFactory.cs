using NinjaAssemble.Network;

namespace NinjaAssemble.Playable
{
    public static class OfflineSeedFactory
    {
        private static readonly string[] Names =
        {
            "Naruto Uzumaki", "Sasuke Uchiha", "Sakura Haruno", "Kakashi Hatake", "Hinata Hyuga",
            "Shikamaru Nara", "Gaara", "Rock Lee", "Neji Hyuga", "Might Guy",
            "Jiraiya", "Tsunade", "Minato Namikaze", "Itachi Uchiha", "Killer Bee"
        };

        public static OfflineSaveData Create()
        {
            var heroes = new OwnedHeroDto[Names.Length];
            for (int i = 0; i < heroes.Length; i++)
            {
                string slug = Names[i].ToLowerInvariant().Replace(' ', '-');
                heroes[i] = new OwnedHeroDto
                {
                    id = "offline-hero-" + (i + 1),
                    characterId = slug,
                    heroId = slug + "-base",
                    displayName = Names[i],
                    level = i < 5 ? 20 : 10,
                    exp = 0,
                    frameTier = i < 5 ? "PURPLE" : "BLUE",
                    awakened = false,
                    currentVariant = "BASE",
                    awakeningLevel = 0
                };
            }

            return new OfflineSaveData
            {
                playerId = "offline-player",
                displayName = "Offline Ninja",
                gold = 1000000,
                diamond = 10000,
                energy = 999,
                energyCap = 999,
                heroes = heroes,
                formationIds = new[] { heroes[0].id, heroes[1].id, heroes[2].id, heroes[3].id, heroes[4].id }
            };
        }
    }
}
