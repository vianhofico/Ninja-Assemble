using TMPro;
using UnityEngine;
using UnityEngine.SceneManagement;
using UnityEngine.UI;

namespace NinjaAssemble.UI.Production
{
    public static class ProductionLiveScreenInstaller
    {
        private static bool registered;

        [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.BeforeSceneLoad)]
        private static void Register(){if(registered)return;registered=true;SceneManager.sceneLoaded+=OnSceneLoaded;}

        [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.AfterSceneLoad)]
        private static void InstallInitial()=>Install(SceneManager.GetActiveScene());

        private static void OnSceneLoaded(Scene scene,LoadSceneMode mode)=>Install(scene);

        private static void Install(Scene scene)
        {
            LiveScreenCatalog.Spec spec=LiveScreenCatalog.Resolve(scene.name);if(spec==null)return;
            GameObject safeObject=GameObject.Find("SafeArea");if(safeObject==null)return;Transform safe=safeObject.transform;
            if(safe.Find("ProductionLivePanel")!=null)return;
            Transform legacy=safe.Find("BodyPanel");if(legacy==null)return;

            TMP_Text sourceBody=legacy.Find("Body")?.GetComponent<TMP_Text>();
            TMP_Text sourceStatus=legacy.Find("Status")?.GetComponent<TMP_Text>();
            Transform sourceActionTransform=legacy.Find("PrimaryAction");
            Button sourceButton=sourceActionTransform?.GetComponent<Button>();
            TMP_Text sourceAction=sourceActionTransform?.Find("Label")?.GetComponent<TMP_Text>();

            Image panel=ProductionUiFactory.Panel(safe,"ProductionLivePanel",ProductionUiTokens.Surface);
            RectTransform panelRect=panel.rectTransform;ProductionUiFactory.Anchor(panelRect,new Vector2(.025f,.17f),new Vector2(.975f,.88f));
            VerticalLayoutGroup stack=ProductionUiFactory.Vertical(panel.transform,"ContentStack",ProductionUiTokens.SpaceMd);ProductionUiFactory.Stretch(stack.GetComponent<RectTransform>());
            TMP_Text eyebrow=ProductionUiFactory.Text(stack.transform,"Eyebrow",spec.Eyebrow,17f,TextAlignmentOptions.Left,ProductionUiTokens.Accent);SetHeight(eyebrow.gameObject,34f);
            TMP_Text title=ProductionUiFactory.Text(stack.transform,"ProductionTitle",spec.Title,38f,TextAlignmentOptions.Left,ProductionUiTokens.Text);title.fontStyle=FontStyles.Bold;SetHeight(title.gameObject,56f);
            TMP_Text subtitle=ProductionUiFactory.Text(stack.transform,"Subtitle",spec.Subtitle,20f,TextAlignmentOptions.Left,ProductionUiTokens.Muted);SetHeight(subtitle.gameObject,60f);
            Image card=ProductionUiFactory.Panel(stack.transform,"LiveStateCard",ProductionUiTokens.SurfaceRaised);LayoutElement cardLayout=card.gameObject.AddComponent<LayoutElement>();cardLayout.flexibleHeight=1f;cardLayout.minHeight=220f;
            TMP_Text body=ProductionUiFactory.Text(card.transform,"LiveBody","Loading live state...",23f,TextAlignmentOptions.TopLeft,ProductionUiTokens.Text);RectTransform bodyRect=body.rectTransform;ProductionUiFactory.Anchor(bodyRect,new Vector2(.035f,.20f),new Vector2(.965f,.94f));
            TMP_Text status=ProductionUiFactory.Text(card.transform,"LiveStatus",string.Empty,18f,TextAlignmentOptions.BottomLeft,ProductionUiTokens.Muted);RectTransform statusRect=status.rectTransform;ProductionUiFactory.Anchor(statusRect,new Vector2(.035f,.035f),new Vector2(.72f,.20f));
            Button action=ProductionUiFactory.Button(stack.transform,"ProductionPrimaryAction",spec.ActionLabel,null,220f);TMP_Text actionLabel=action.transform.Find("Label")?.GetComponent<TMP_Text>();SetHeight(action.gameObject,ProductionUiTokens.TouchMin+8f);

            MobileScreenRoot root=safe.GetComponent<MobileScreenRoot>();ScreenId id=root!=null?root.ScreenId:ScreenId.Home;
            if(id==ScreenId.ResourcePve||id==ScreenId.Progression||id==ScreenId.Settings)
            {
                var binding=panel.gameObject.AddComponent<ProductionLiveFeatureBinding>();binding.Configure(id,body,status,action,actionLabel);
            }
            else
            {
                var binding=panel.gameObject.AddComponent<ProductionLegacyScreenBinding>();binding.Configure(sourceBody,sourceStatus,sourceButton,sourceAction,body,status,action,actionLabel);
            }
            legacy.gameObject.SetActive(false);
        }

        private static void SetHeight(GameObject go,float height){LayoutElement layout=go.GetComponent<LayoutElement>();if(layout==null)layout=go.AddComponent<LayoutElement>();layout.preferredHeight=height;layout.minHeight=height;}
    }
}
