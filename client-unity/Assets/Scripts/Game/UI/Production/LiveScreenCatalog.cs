using System.Collections.Generic;
namespace NinjaAssemble.UI.Production
{
    public static class LiveScreenCatalog
    {
        public sealed class Spec { public string Key; public string Eyebrow; public string Title; public string Subtitle; public string ActionLabel; public Spec(string key,string eyebrow,string title,string subtitle,string actionLabel){Key=key;Eyebrow=eyebrow;Title=title;Subtitle=subtitle;ActionLabel=actionLabel;} }
        private static readonly Spec[] Specs={
            new Spec("shadowarena","COMPETITIVE","Shadow Arena","Manage three squads, defense, season rewards and battle history.","BATTLE"),
            new Spec("arena","COMPETITIVE","Arena","Manage offense/defense, rating, daily attempts and season rewards.","BATTLE"),
            new Spec("guild","SOCIAL","Guild","Coordinate members, progression and guild rewards.","MANAGE"),
            new Spec("shop","ECONOMY","Shops","Review transparent prices, limits, refresh state and currency sinks.","BUY"),
            new Spec("quest","LIVE OPS","Quests","Track claimable objectives and reset windows.","CLAIM"),
            new Spec("events","LIVE OPS","Events","Review active event windows, requirements and rewards.","CLAIM"),
            new Spec("mail","INBOX","Mail","Claim idempotent attachments and review expiration state.","CLAIM"),
            new Spec("settings","SYSTEM","Settings","Language, audio, haptics, accessibility and account controls.","APPLY"),
            new Spec("resourcepve","PVE HUB","Resource PvE","Run nine release resource modes with daily attempt and server-authoritative reward state.","BATTLE"),
            new Spec("progression","GROWTH","Advanced Progression","Upgrade Scroll Mastery, Ninja College and nine Tailed-Beast tracks.","UPGRADE")
        };
        public static IReadOnlyList<Spec> All=>Specs;
        public static Spec Resolve(string sceneName){string value=(sceneName??string.Empty).Replace("_",string.Empty).Replace("-",string.Empty).ToLowerInvariant();foreach(Spec spec in Specs)if(value.Contains(spec.Key))return spec;return null;}
    }

    public static class FeatureHubCatalog
    {
        public sealed class Entry { public string FeatureId; public string Title; public string SceneName; public string Source; public Entry(string id,string title,string sceneName,string source){FeatureId=id;Title=title;SceneName=sceneName;Source=source;} }
        public static readonly Entry ResourcePve=new Entry("resource-pve","Resource PvE","ResourcePve","PlayableGameStore.ResourcePve");
        public static readonly Entry AdvancedProgression=new Entry("advanced-progression","Advanced Progression","Progression","MobileGameBootstrap.AdvancedProgression");
    }
}
