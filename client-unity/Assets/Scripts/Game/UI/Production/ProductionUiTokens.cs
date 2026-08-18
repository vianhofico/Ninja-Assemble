using UnityEngine;
namespace NinjaAssemble.UI.Production
{
    public static class ProductionUiTokens
    {
        public const float SpaceXs=6f, SpaceSm=10f, SpaceMd=16f, SpaceLg=24f, SpaceXl=32f;
        public const float RadiusSm=8f, RadiusMd=14f, RadiusLg=22f;
        public const float TouchMin=52f, HeaderHeight=88f, BottomNavHeight=104f;
        public static readonly Color Background=new Color(0.025f,0.03f,0.045f,1f);
        public static readonly Color Surface=new Color(0.07f,0.085f,0.12f,0.98f);
        public static readonly Color SurfaceRaised=new Color(0.10f,0.12f,0.17f,0.98f);
        public static readonly Color Accent=new Color(0.94f,0.52f,0.12f,1f);
        public static readonly Color AccentSoft=new Color(0.28f,0.16f,0.08f,1f);
        public static readonly Color Text=new Color(0.96f,0.97f,1f,1f);
        public static readonly Color Muted=new Color(0.65f,0.70f,0.80f,1f);
        public static readonly Color Positive=new Color(0.28f,0.86f,0.45f,1f);
        public static readonly Color Warning=new Color(1f,0.73f,0.20f,1f);
        public static readonly Color Negative=new Color(0.95f,0.28f,0.25f,1f);
        public static readonly Vector2 ReferenceResolution=new Vector2(1920f,1080f);
    }
}
