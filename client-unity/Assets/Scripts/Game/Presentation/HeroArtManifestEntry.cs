using System;

namespace NinjaAssemble.Presentation
{
    [Serializable]
    public sealed class HeroArtManifestEntry
    {
        public string CharacterId;
        public string Variant;
        public string PortraitAddress;
        public string IconAddress;
        public string PrefabAddress;
        public string AnimationSet;
        public string VfxSet;
        public string SfxSet;
        public string Status;

        public string Key => $"{CharacterId}::{Variant}";

        public bool HasRuntimeAddresses =>
            !string.IsNullOrWhiteSpace(PortraitAddress) &&
            !string.IsNullOrWhiteSpace(IconAddress) &&
            !string.IsNullOrWhiteSpace(PrefabAddress) &&
            !string.IsNullOrWhiteSpace(AnimationSet) &&
            !string.IsNullOrWhiteSpace(VfxSet) &&
            !string.IsNullOrWhiteSpace(SfxSet);

        public bool IsReleaseReady =>
            string.Equals(Status, "READY", StringComparison.OrdinalIgnoreCase) && HasRuntimeAddresses;
    }
}
