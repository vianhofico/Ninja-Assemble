using System;
using System.Collections.Generic;
namespace NinjaAssemble.UI.Production
{
    public static class CoreScreenCatalog
    {
        public sealed class Spec { public string Key; public string Eyebrow; public string Title; public string Subtitle; public string ActionLabel; public Spec(string key,string eyebrow,string title,string subtitle,string action){Key=key;Eyebrow=eyebrow;Title=title;Subtitle=subtitle;ActionLabel=action;} }
        private static readonly Spec[] Specs={
            new Spec("home","HIDDEN VILLAGE","Village Command","Your roster, resources and next objective at a glance.","CONTINUE"),
            new Spec("ninja","ROSTER","Ninja Roster","Review owned Hero Versions and prepare your next upgrade.","OPEN NINJA"),
            new Spec("herodetail","NINJA PROFILE","Ninja Detail","Inspect progression, Awakening identity and combat presentation.","UPGRADE"),
            new Spec("formation","FORMATION","Team Formation","Build the active five-ninja squad used by Campaign and Arena offense.","SAVE TEAM"),
            new Spec("adventure","CAMPAIGN","Adventure","Advance through Normal, Elite and Heroic release stages.","BATTLE"),
            new Spec("summon","SUMMON","Summoning","Spend the configured banner currency and track pity transparently.","SUMMON"),
            new Spec("inventory","INVENTORY","Inventory & Equipment","Manage earned items and equipped progression without hidden state.","MANAGE")
        };
        public static IReadOnlyList<Spec> All=>Specs;
        public static Spec Resolve(string sceneName){string value=(sceneName??string.Empty).ToLowerInvariant();foreach(Spec spec in Specs)if(value.Contains(spec.Key))return spec;return null;}
    }
}
