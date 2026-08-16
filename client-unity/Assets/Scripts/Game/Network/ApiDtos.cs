using System;

namespace NinjaAssemble.Network
{
    [Serializable] public sealed class HeroCatalogDto { public string id; public string character; public string group; public string scope; }
    [Serializable] public sealed class HeroVariantDto { public string characterId; public string variant; public string status; }
    [Serializable] public sealed class TechniqueDto
    {
        public string id; public string nameEn; public string nameVi; public string descriptionEn; public string descriptionVi;
        public string channel; public string kind; public string tags;
    }
    [Serializable] public sealed class HeroKitDto { public string profileId; public TechniqueDto[] techniques; }
    [Serializable] public sealed class GuestLoginRequest { public string guestKey; public string displayName; }
    [Serializable] public sealed class PlayerDto { public string id; public string displayName; public int level; public long exp; }
    [Serializable] internal sealed class ArrayEnvelope<T> { public T[] items; }
}
