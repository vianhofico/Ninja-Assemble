using TMPro;
using UnityEngine;
using UnityEngine.UI;
namespace NinjaAssemble.UI.Production
{
    public sealed class ProductionLegacyScreenBinding : MonoBehaviour
    {
        private TMP_Text sourceBody,sourceStatus,sourceAction;
        private TMP_Text targetBody,targetStatus,targetAction;
        private Button sourceButton,targetButton;
        public void Configure(TMP_Text body,TMP_Text status,Button action,TMP_Text actionLabel,TMP_Text mirroredBody,TMP_Text mirroredStatus,Button productionAction,TMP_Text productionActionLabel)
        {sourceBody=body;sourceStatus=status;sourceButton=action;sourceAction=actionLabel;targetBody=mirroredBody;targetStatus=mirroredStatus;targetButton=productionAction;targetAction=productionActionLabel;if(targetButton!=null)targetButton.onClick.AddListener(ForwardAction);Refresh();}
        private void OnDestroy(){if(targetButton!=null)targetButton.onClick.RemoveListener(ForwardAction);}
        private void Update()=>Refresh();
        private void ForwardAction(){if(sourceButton!=null&&sourceButton.interactable)sourceButton.onClick.Invoke();}
        private void Refresh(){if(targetBody!=null&&sourceBody!=null)targetBody.text=sourceBody.text;if(targetStatus!=null&&sourceStatus!=null){targetStatus.text=sourceStatus.text;targetStatus.gameObject.SetActive(!string.IsNullOrWhiteSpace(sourceStatus.text));}if(targetAction!=null&&sourceAction!=null&&!string.IsNullOrWhiteSpace(sourceAction.text))targetAction.text=sourceAction.text;if(targetButton!=null&&sourceButton!=null)targetButton.interactable=sourceButton.interactable;}
    }
}
