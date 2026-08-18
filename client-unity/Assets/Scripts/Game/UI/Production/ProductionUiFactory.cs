using TMPro;
using UnityEngine;
using UnityEngine.Events;
using UnityEngine.UI;
namespace NinjaAssemble.UI.Production
{
    public static class ProductionUiFactory
    {
        public static RectTransform Rect(Transform parent,string name){var go=new GameObject(name,typeof(RectTransform));go.transform.SetParent(parent,false);return go.GetComponent<RectTransform>();}
        public static Image Panel(Transform parent,string name,Color color){var go=new GameObject(name,typeof(RectTransform),typeof(CanvasRenderer),typeof(Image));go.transform.SetParent(parent,false);Image image=go.GetComponent<Image>();image.color=color;return image;}
        public static TMP_Text Text(Transform parent,string name,string value,float size,TextAlignmentOptions alignment=TextAlignmentOptions.Left,Color? color=null){var go=new GameObject(name,typeof(RectTransform),typeof(CanvasRenderer),typeof(TextMeshProUGUI));go.transform.SetParent(parent,false);var text=go.GetComponent<TextMeshProUGUI>();text.text=value??string.Empty;text.fontSize=size;text.alignment=alignment;text.color=color??ProductionUiTokens.Text;text.enableWordWrapping=true;text.raycastTarget=false;return text;}
        public static Button Button(Transform parent,string name,string label,UnityAction action,float minWidth=150f){Image bg=Panel(parent,name,ProductionUiTokens.Accent);Button button=bg.gameObject.AddComponent<Button>();button.targetGraphic=bg;if(action!=null)button.onClick.AddListener(action);LayoutElement layout=bg.gameObject.AddComponent<LayoutElement>();layout.minHeight=ProductionUiTokens.TouchMin;layout.minWidth=minWidth;TMP_Text text=Text(bg.transform,"Label",label,22f,TextAlignmentOptions.Center);Stretch(text.rectTransform);return button;}
        public static VerticalLayoutGroup Vertical(Transform parent,string name,float spacing=ProductionUiTokens.SpaceMd){RectTransform rect=Rect(parent,name);VerticalLayoutGroup group=rect.gameObject.AddComponent<VerticalLayoutGroup>();group.spacing=spacing;group.padding=new RectOffset((int)ProductionUiTokens.SpaceMd,(int)ProductionUiTokens.SpaceMd,(int)ProductionUiTokens.SpaceMd,(int)ProductionUiTokens.SpaceMd);group.childControlHeight=true;group.childControlWidth=true;group.childForceExpandHeight=false;group.childForceExpandWidth=true;return group;}
        public static void Stretch(RectTransform rect){rect.anchorMin=Vector2.zero;rect.anchorMax=Vector2.one;rect.offsetMin=Vector2.zero;rect.offsetMax=Vector2.zero;}
        public static void Anchor(RectTransform rect,Vector2 min,Vector2 max){rect.anchorMin=min;rect.anchorMax=max;rect.offsetMin=Vector2.zero;rect.offsetMax=Vector2.zero;}
    }
}
