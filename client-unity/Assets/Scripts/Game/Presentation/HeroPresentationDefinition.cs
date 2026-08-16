using System;
using UnityEngine;

namespace NinjaAssemble.Game.Presentation
{
    [Serializable]
    public sealed class HeroPresentationDefinition
    {
        [SerializeField] private string heroDefinitionId = string.Empty;
        [SerializeField] private string portraitAddress = string.Empty;
        [SerializeField] private string prefabAddress = string.Empty;
        [SerializeField] private string animationSet = string.Empty;
        [SerializeField] private string vfxSet = string.Empty;
        public string HeroDefinitionId => heroDefinitionId;
        public string PortraitAddress => portraitAddress;
        public string PrefabAddress => prefabAddress;
        public string AnimationSet => animationSet;
        public string VfxSet => vfxSet;
    }
}
