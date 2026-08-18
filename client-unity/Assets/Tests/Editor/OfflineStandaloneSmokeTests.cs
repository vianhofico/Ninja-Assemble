using System;
using System.IO;
using System.Linq;
using System.Reflection;
using NinjaAssemble.Bootstrap;
using NinjaAssemble.EditorTools;
using NinjaAssemble.Playable;
using NinjaAssemble.UI;
using NUnit.Framework;
using TMPro;
using UnityEditor;
using UnityEditor.SceneManagement;
using UnityEngine;
using UnityEngine.UI;

namespace NinjaAssemble.Tests.Editor
{
    public sealed class OfflineStandaloneSmokeTests
    {
        private string savePath;

        [SetUp]
        public void SetUp()
        {
            savePath = Path.Combine(Path.GetTempPath(), "ninjaassemble-offline-smoke-" + Guid.NewGuid().ToString("N") + ".json");
        }

        [TearDown]
        public void TearDown()
        {
            DeleteIfExists(savePath);
            DeleteIfExists(savePath + ".tmp");
            DeleteIfExists(savePath + ".bak");
            MobileGameBootstrap.ResetStaticStateForTests();
        }

        [Test]
        public void OfflineService_FullHappyPath_PersistsWithoutNetwork()
        {
            var repository = new OfflineSaveRepository(savePath);
            var service = new OfflinePlayableGameService(repository);
            var store = new PlayableGameStore(service);

            store.LoginAndBootstrapAsync("offline-smoke", "Smoke Ninja").GetAwaiter().GetResult();
            Assert.AreEqual("offline-player", store.PlayerId);
            Assert.GreaterOrEqual(store.Heroes.Length, 15);
            Assert.AreEqual(5, store.Formation.heroes.Length);
            Assert.IsNotNull(store.RecommendedStage);
            Assert.IsTrue(File.Exists(savePath));

            var awakening = service.GetAwakeningAsync(store.PlayerId, store.Heroes[0].id).GetAwaiter().GetResult();
            Assert.IsTrue(awakening.available, "Seed profile must expose an explicit offline awakening fixture.");
            var awakened = service.AwakenAsync(store.PlayerId, store.Heroes[0].id).GetAwaiter().GetResult();
            Assert.IsTrue(awakened.awakened);
            Assert.AreEqual("OFFLINE_TEST", awakened.visual.status);

            var battle = store.BattleCampaignAsync("c1-s1").GetAwaiter().GetResult();
            Assert.IsNotNull(battle.battle);
            Assert.AreEqual("TEAM_A", battle.battle.outcome);
            Assert.IsNotNull(battle.battle.events);
            Assert.Greater(battle.battle.events.Length, 0);

            var arena = store.FightArenaAsync("offline-bot").GetAwaiter().GetResult();
            Assert.IsTrue(arena.training);
            Assert.Greater(arena.ratingAfter, arena.ratingBefore);

            var shadow = store.FightShadowArenaAsync("offline-shadow-bot").GetAwaiter().GetResult();
            Assert.IsTrue(shadow.training);
            Assert.AreEqual(3, shadow.squads.Length);

            long goldBeforeShop = store.Gold;
            var purchase = store.PurchaseShopAsync("offline-general", "ramen-pack").GetAwaiter().GetResult();
            Assert.AreEqual(1000, purchase.charged);
            Assert.Less(store.Gold, goldBeforeShop);

            store.RefreshQuestsAsync().GetAwaiter().GetResult();
            Assert.IsNotNull(store.ClaimableQuest);
            var quest = store.ClaimQuestAsync(store.ClaimableQuest.questId).GetAwaiter().GetResult();
            Assert.IsFalse(quest.replayed);

            store.RefreshMailAsync().GetAwaiter().GetResult();
            Assert.IsNotNull(store.ClaimableMail);
            var mail = store.ClaimMailAsync(store.ClaimableMail.mailId).GetAwaiter().GetResult();
            Assert.IsFalse(mail.replayed);
            Assert.AreEqual(1, mail.grants.Length);

            long diamondBeforeSummon = store.Diamond;
            var summon = store.SummonAsync().GetAwaiter().GetResult();
            Assert.IsFalse(string.IsNullOrWhiteSpace(summon.heroId));
            Assert.AreEqual(diamondBeforeSummon - PlayableGameStore.CompleteRosterSummonCost, store.Diamond);

            int levelBefore = store.Heroes[0].level;
            var upgrade = store.LevelUpAsync(store.Heroes[0].id).GetAwaiter().GetResult();
            Assert.AreEqual(levelBefore + 1, upgrade.hero.level);

            var reloadedService = new OfflinePlayableGameService(new OfflineSaveRepository(savePath));
            var reloadedStore = new PlayableGameStore(reloadedService);
            reloadedStore.LoginAndBootstrapAsync("offline-smoke", "Smoke Ninja").GetAwaiter().GetResult();
            Assert.IsTrue(reloadedStore.Heroes[0].awakened);
            Assert.AreEqual(upgrade.hero.level, reloadedStore.Heroes[0].level);
            Assert.IsTrue(reloadedStore.Campaign.stages.First(stage => stage.stageId == "c1-s1").clearCount > 0);
            Assert.AreEqual(store.Gold, reloadedStore.Gold);
            Assert.AreEqual(store.Diamond, reloadedStore.Diamond);
        }

        [Test]
        public void GeneratedMobileScenes_HaveRequiredBindings_AndOfflineBootstrapDefault()
        {
            MobileSceneBuilder.GenerateCompleteSceneShell();
            MobileSceneBuilder.ValidateSceneShell();

            EditorBuildSettingsScene[] scenes = EditorBuildSettings.scenes.Where(scene => scene.enabled).ToArray();
            Assert.AreEqual(19, scenes.Length, "Bootstrap + 18 mobile screens must be generated.");

            foreach (EditorBuildSettingsScene buildScene in scenes)
            {
                EditorSceneManager.OpenScene(buildScene.path, OpenSceneMode.Single);
                if (buildScene.path.EndsWith("/Bootstrap.unity", StringComparison.OrdinalIgnoreCase))
                {
                    MobileGameBootstrap bootstrap = UnityEngine.Object.FindObjectOfType<MobileGameBootstrap>();
                    Assert.IsNotNull(bootstrap, "Bootstrap scene must contain MobileGameBootstrap.");
                    FieldInfo mode = typeof(MobileGameBootstrap).GetField("runtimeMode", BindingFlags.Instance | BindingFlags.NonPublic);
                    Assert.AreEqual(MobileRuntimeMode.OfflinePlaytest, mode.GetValue(bootstrap));
                    continue;
                }

                MobileVerticalSliceController controller = UnityEngine.Object.FindObjectOfType<MobileVerticalSliceController>();
                Assert.IsNotNull(controller, "Missing MobileVerticalSliceController in " + buildScene.path);
                AssertField<TMP_Text>(controller, "resourceText", buildScene.path);
                AssertField<TMP_Text>(controller, "bodyText", buildScene.path);
                AssertField<TMP_Text>(controller, "statusText", buildScene.path);
                AssertField<Button>(controller, "primaryAction", buildScene.path);
                AssertField<TMP_Text>(controller, "primaryActionLabel", buildScene.path);
            }
        }

        private static void AssertField<T>(object target, string name, string scenePath) where T : class
        {
            FieldInfo field = target.GetType().GetField(name, BindingFlags.Instance | BindingFlags.NonPublic);
            Assert.IsNotNull(field, "Missing field " + name);
            Assert.IsNotNull(field.GetValue(target) as T, "Unassigned " + name + " in " + scenePath);
        }

        private static void DeleteIfExists(string path)
        {
            if (!string.IsNullOrWhiteSpace(path) && File.Exists(path)) File.Delete(path);
        }
    }
}
