using System;
namespace NinjaAssemble.UI.Production
{
    public enum ProductionUiStateKind { Loading, Ready, Empty, Error, Offline }
    public readonly struct ProductionUiState
    {
        public ProductionUiStateKind Kind { get; }
        public string Message { get; }
        public Action Retry { get; }
        public bool CanRetry => Retry != null;
        private ProductionUiState(ProductionUiStateKind kind,string message,Action retry){Kind=kind;Message=message??string.Empty;Retry=retry;}
        public static ProductionUiState Loading(string message="Loading...")=>new ProductionUiState(ProductionUiStateKind.Loading,message,null);
        public static ProductionUiState Ready()=>new ProductionUiState(ProductionUiStateKind.Ready,string.Empty,null);
        public static ProductionUiState Empty(string message)=>new ProductionUiState(ProductionUiStateKind.Empty,message,null);
        public static ProductionUiState Error(string message,Action retry)=>new ProductionUiState(ProductionUiStateKind.Error,message,retry);
        public static ProductionUiState Offline(Action retry)=>new ProductionUiState(ProductionUiStateKind.Offline,"You are offline. Check your connection and retry.",retry);
    }
}
