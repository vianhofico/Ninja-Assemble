using UnityEngine;

namespace NinjaAssemble.UI
{
    public static class MobileTheme
    {
        public static readonly Color Background = new(0.055f, 0.065f, 0.09f, 1f);
        public static readonly Color Panel = new(0.11f, 0.125f, 0.16f, 0.96f);
        public static readonly Color PanelAlt = new(0.16f, 0.18f, 0.22f, 0.96f);
        public static readonly Color Accent = new(0.94f, 0.48f, 0.12f, 1f);
        public static readonly Color Gold = new(0.96f, 0.77f, 0.28f, 1f);
        public static readonly Color Text = new(0.97f, 0.95f, 0.9f, 1f);
        public static readonly Color MutedText = new(0.72f, 0.74f, 0.78f, 1f);
        public static readonly Color Danger = new(0.85f, 0.23f, 0.2f, 1f);

        public const float HeaderHeight = 92f;
        public const float BottomNavHeight = 116f;
        public const float CornerRadiusHint = 18f;
    }
}
