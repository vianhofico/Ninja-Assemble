using System;

namespace NinjaAssemble.Network
{
    [Serializable]
    public sealed class DevStateDto
    {
        public long gold;
        public long diamond;
        public int energy;
    }

    [Serializable]
    public sealed class DevRosterResultDto
    {
        public int newlyGranted;
        public int ownedHeroes;
    }
}
