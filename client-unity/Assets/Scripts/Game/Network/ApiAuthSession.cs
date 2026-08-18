namespace NinjaAssemble.Network
{
    public static class ApiAuthSession
    {
        public static string BearerToken { get; private set; } = string.Empty;
        public static bool HasToken => !string.IsNullOrWhiteSpace(BearerToken);
        public static void Set(string token) => BearerToken = token ?? string.Empty;
        public static void Clear() => BearerToken = string.Empty;
    }
}
