using System;
using TMPro;
using UnityEngine;
using UnityEngine.UI;
namespace NinjaAssemble.UI.Production
{
    public sealed class ProductionScreenHost : MonoBehaviour
    {
        private RectTransform safeArea;
        private RectTransform contentRoot;
        private GameObject stateLayer;
        private TMP_Text stateText;
        private Button retryButton;
        private Action retry;

        public RectTransform ContentRoot { get { EnsureBuilt(); return contentRoot; } }

        private void Awake()=>EnsureBuilt();
        private void OnEnable(){EnsureBuilt();ApplySafeArea();}
        private void OnRectTransformDimensionsChange(){if(safeArea!=null)ApplySafeArea();}

        public void SetState(ProductionUiState state)
        {
            EnsureBuilt(); retry=state.Retry; bool ready=state.Kind==ProductionUiStateKind.Ready;
            contentRoot.gameObject.SetActive(ready); stateLayer.SetActive(!ready);
            if(ready)return;
            stateText.text=state.Message; retryButton.gameObject.SetActive(state.CanRetry);
        }

        private void EnsureBuilt()
        {
            if(safeArea!=null)return;
            safeArea=ProductionUiFactory.Rect(transform,"ProductionSafeArea");ProductionUiFactory.Stretch(safeArea);
            Image bg=ProductionUiFactory.Panel(safeArea,"Background",ProductionUiTokens.Background);ProductionUiFactory.Stretch(bg.rectTransform);
            contentRoot=ProductionUiFactory.Rect(safeArea,"Content");ProductionUiFactory.Stretch(contentRoot);
            stateLayer=ProductionUiFactory.Panel(safeArea,"AsyncState",ProductionUiTokens.Background).gameObject;ProductionUiFactory.Stretch(stateLayer.GetComponent<RectTransform>());
            VerticalLayoutGroup stack=ProductionUiFactory.Vertical(stateLayer.transform,"StateStack",ProductionUiTokens.SpaceLg);RectTransform rect=stack.GetComponent<RectTransform>();ProductionUiFactory.Anchor(rect,new Vector2(.25f,.34f),new Vector2(.75f,.66f));
            stateText=ProductionUiFactory.Text(stack.transform,"Message","Loading...",26f,TextAlignmentOptions.Center,ProductionUiTokens.Muted);
            retryButton=ProductionUiFactory.Button(stack.transform,"Retry","RETRY",()=>retry?.Invoke(),180f);retryButton.gameObject.SetActive(false);
            ApplySafeArea();
        }

        private void ApplySafeArea()
        {
            if(safeArea==null||Screen.width<=0||Screen.height<=0)return;
            Rect area=Screen.safeArea;Vector2 min=area.position;Vector2 max=area.position+area.size;min.x/=Screen.width;min.y/=Screen.height;max.x/=Screen.width;max.y/=Screen.height;safeArea.anchorMin=min;safeArea.anchorMax=max;safeArea.offsetMin=Vector2.zero;safeArea.offsetMax=Vector2.zero;
        }
    }
}
