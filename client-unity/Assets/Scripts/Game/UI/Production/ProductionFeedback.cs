using NinjaAssemble.Settings;
using UnityEngine;
namespace NinjaAssemble.UI.Production
{
    public static class ProductionFeedback
    {
        public static void Tap(){ if(AccessibilityPreferences.HapticsEnabled) Handheld.Vibrate(); }
        public static void Success(AudioSource source,AudioClip clip=null){ if(source!=null&&clip!=null)source.PlayOneShot(clip); }
        public static void Error(AudioSource source,AudioClip clip=null){ if(source!=null&&clip!=null)source.PlayOneShot(clip); }
    }
}
