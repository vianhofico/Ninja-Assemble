using UnityEngine;
namespace NinjaAssemble.UI.Production
{
    public enum ProductionWidthClass { Compact, Regular, Wide }
    public static class ResponsiveLayout
    {
        public static ProductionWidthClass WidthClass(float width)
        {
            if(width<1000f)return ProductionWidthClass.Compact;
            if(width<1600f)return ProductionWidthClass.Regular;
            return ProductionWidthClass.Wide;
        }
        public static int GridColumns(float width){return WidthClass(width) switch{ProductionWidthClass.Compact=>2,ProductionWidthClass.Regular=>3,_=>4};}
        public static float ContentMargin(float width){return WidthClass(width)==ProductionWidthClass.Compact?ProductionUiTokens.SpaceMd:ProductionUiTokens.SpaceXl;}
        public static bool IsTabletLike(float width,float height){float shortSide=Mathf.Min(width,height);return shortSide>=900f;}
    }
}
