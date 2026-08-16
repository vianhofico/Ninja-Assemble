using System;
using System.Collections.Generic;
using System.Linq;
using TMPro;
using UnityEngine;

namespace NinjaAssemble.Localization
{
    public sealed class BilingualRuntimeLocalizationBridge : MonoBehaviour
    {
        private const string LanguageKey = "na.language";
        private static bool installed;
        private readonly Dictionary<int, TextState> states = new Dictionary<int, TextState>();
        private readonly List<Pair> enToVi = new List<Pair>();
        private readonly List<Pair> viToEn = new List<Pair>();
        private string language;
        private float nextScan;

        [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.BeforeSceneLoad)]
        private static void Install()
        {
            if (installed) return;
            installed = true;
            var root = new GameObject("BilingualRuntimeLocalizationBridge");
            DontDestroyOnLoad(root);
            root.AddComponent<BilingualRuntimeLocalizationBridge>();
        }

        private void Awake()
        {
            LoadCatalog();
            AddSupplementalPhrases();
            SortMappings();
            language = CurrentLanguage();
        }

        private void Update()
        {
            string nextLanguage = CurrentLanguage();
            bool changed = !string.Equals(nextLanguage, language, StringComparison.Ordinal);
            if (!changed && Time.unscaledTime < nextScan) return;
            language = nextLanguage;
            nextScan = Time.unscaledTime + 0.20f;
            ApplyToAllText();
        }

        private void LoadCatalog()
        {
            TextAsset asset = Resources.Load<TextAsset>("Localization/strings");
            if (asset == null) throw new InvalidOperationException("missing Resources/Localization/strings.csv");
            string[] lines = asset.text.Replace("\r", string.Empty).Split('\n');
            for (int i = 1; i < lines.Length; i++)
            {
                if (string.IsNullOrWhiteSpace(lines[i])) continue;
                string[] cells = lines[i].Split(new[] { ',' }, 3);
                if (cells.Length != 3 || string.IsNullOrWhiteSpace(cells[1])) continue;
                AddPair(cells[1].Trim(), cells[2].Trim());
            }
        }

        private void AddSupplementalPhrases()
        {
            AddPair("Hidden Village", "Làng Lá");
            AddPair("Owned ninja", "Ninja sở hữu");
            AddPair("Ninja Roster", "Danh sách Ninja");
            AddPair("Formation 5", "Đội hình 5");
            AddPair("Selected candidate", "Ninja đang chọn");
            AddPair("Selected from Ninja Roster", "Đã chọn từ danh sách Ninja");
            AddPair("Battle Debug", "Kiểm thử chiến đấu");
            AddPair("deterministic simulation", "mô phỏng xác định");
            AddPair("Default stage", "Ải mặc định");
            AddPair("battle in progress", "đang chiến đấu");
            AddPair("Battle in progress", "Đang chiến đấu");
            AddPair("replay running", "đang phát lại");
            AddPair("First clear", "Vượt lần đầu");
            AddPair("FIRST CLEAR", "VƯỢT LẦN ĐẦU");
            AddPair("Inventory stacks", "Loại vật phẩm");
            AddPair("STACK ITEMS", "VẬT PHẨM");
            AddPair("GEAR", "TRANG BỊ");
            AddPair("UNEQUIPPED", "CHƯA TRANG BỊ");
            AddPair("EQUIPPED", "ĐÃ TRANG BỊ");
            AddPair("Arena rating", "Điểm Arena");
            AddPair("Daily claims", "Thưởng ngày");
            AddPair("Unread mail", "Thư chưa đọc");
            AddPair("Daily Quests", "Nhiệm vụ hằng ngày");
            AddPair("Weekly objective", "Mục tiêu tuần");
            AddPair("weekly objective", "mục tiêu tuần");
            AddPair("Inbox empty", "Hộp thư trống");
            AddPair("No owned ninja", "Chưa sở hữu Ninja");
            AddPair("No gear", "Chưa có trang bị");
            AddPair("No stack items yet", "Chưa có vật phẩm");
            AddPair("No shop offer is currently purchasable", "Hiện không có vật phẩm cửa hàng có thể mua");
            AddPair("No completed daily quest is waiting to be claimed", "Không có nhiệm vụ ngày đã hoàn thành chờ nhận thưởng");
            AddPair("No mail attachment is waiting to be claimed", "Không có quà thư đang chờ nhận");
            AddPair("No playable evolution path for this ninja", "Ninja này chưa có đường tiến hóa có thể chơi");
            AddPair("No campaign stage is currently unlocked", "Hiện chưa có ải chiến dịch được mở");
            AddPair("No Arena opponent is available yet", "Hiện chưa có đối thủ Arena");
            AddPair("Connecting", "Đang kết nối");
            AddPair("Loading", "Đang tải");
            AddPair("Refreshing", "Đang làm mới");
            AddPair("Purchased", "Đã mua");
            AddPair("Claimed", "Đã nhận");
            AddPair("Selected", "Đã chọn");
            AddPair("Switching variant", "Đang đổi biến thể");
            AddPair("Variant selected", "Đã chọn biến thể");
            AddPair("Training", "Đang huấn luyện");
            AddPair("Advancing", "Đang đột phá");
            AddPair("Evolving", "Đang tiến hóa");
            AddPair("evolution path", "đường tiến hóa");
            AddPair("frame requirement not met", "chưa đạt yêu cầu khung");
            AddPair("hero level requirement not met", "chưa đạt yêu cầu cấp Ninja");
            AddPair("prerequisite variant not unlocked", "chưa mở biến thể tiên quyết");
            AddPair("rating/reward unchanged", "điểm/phần thưởng không đổi");
            AddPair("Training mirror", "Đối luyện mô phỏng");
            AddPair("TRAINING", "ĐỐI LUYỆN");
            AddPair("reset", "đặt lại");
            AddPair("cleared", "đã vượt");
            AddPair("wave", "đợt");
            AddPair("rounds", "lượt");
            AddPair("owned", "sở hữu");
            AddPair("left", "còn lại");
            AddPair("READY", "SẴN SÀNG");
            AddPair("LOCKED", "KHÓA");
            AddPair("NEW", "MỚI");
            AddPair("CLAIMED", "ĐÃ NHẬN");
            AddPair("CLAIM", "NHẬN");
            AddPair("REFRESH", "LÀM MỚI");
            AddPair("FIGHT", "CHIẾN ĐẤU");
            AddPair("SUMMON", "CHIÊU MỘ");
            AddPair("TRAIN", "HUẤN LUYỆN");
            AddPair("BUY", "MUA");
            AddPair("SELECT NEXT", "CHỌN TIẾP");
            AddPair("NEXT VARIANT", "BIẾN THỂ TIẾP");
            AddPair("ADD SELECTED", "THÊM NINJA ĐÃ CHỌN");
            AddPair("FRAME ADVANCE", "ĐỘT PHÁ KHUNG");
            AddPair("EVOLVE", "TIẾN HÓA");
            AddPair("EQUIP NEXT", "TRANG BỊ TIẾP");
            AddPair("ENHANCE GEAR", "CƯỜNG HÓA TRANG BỊ");
            AddPair("HIT GUILD BOSS", "ĐÁNH BOSS BANG");
            AddPair("DONATE", "ĐÓNG GÓP");
            AddPair("CREATE GUILD", "TẠO BANG");
            AddPair("JOIN GUILD", "VÀO BANG");
            AddPair("Choose a destination from the navigation below", "Chọn một tính năng từ thanh điều hướng bên dưới");
            AddPair("Preferences are saved on this device", "Cài đặt được lưu trên thiết bị này");
        }

        private void ApplyToAllText()
        {
            bool vietnamese = string.Equals(language, "VI", StringComparison.Ordinal);
            foreach (TMP_Text text in FindObjectsOfType<TMP_Text>(true))
            {
                if (text == null) continue;
                int id = text.GetInstanceID();
                states.TryGetValue(id, out TextState state);
                string current = text.text ?? string.Empty;
                if (state == null)
                {
                    state = new TextState { EnglishSource = ToEnglish(current), LastOutput = current };
                    states[id] = state;
                }
                else if (!string.Equals(current, state.LastOutput, StringComparison.Ordinal))
                {
                    state.EnglishSource = ToEnglish(current);
                }

                string desired = vietnamese ? ToVietnamese(state.EnglishSource) : state.EnglishSource;
                if (!string.Equals(current, desired, StringComparison.Ordinal)) text.text = desired;
                state.LastOutput = desired;
            }
        }

        private string ToVietnamese(string english) => ReplaceAll(english, enToVi);
        private string ToEnglish(string value) => ReplaceAll(value, viToEn);

        private static string ReplaceAll(string value, List<Pair> pairs)
        {
            string result = value ?? string.Empty;
            foreach (Pair pair in pairs)
                if (!string.IsNullOrEmpty(pair.From) && result.Contains(pair.From)) result = result.Replace(pair.From, pair.To);
            return result;
        }

        private void AddPair(string en, string vi)
        {
            if (string.IsNullOrWhiteSpace(en) || string.IsNullOrWhiteSpace(vi) || string.Equals(en, vi, StringComparison.Ordinal)) return;
            if (!enToVi.Any(pair => pair.From == en)) enToVi.Add(new Pair(en, vi));
            if (!viToEn.Any(pair => pair.From == vi)) viToEn.Add(new Pair(vi, en));
        }

        private void SortMappings()
        {
            enToVi.Sort((a, b) => b.From.Length.CompareTo(a.From.Length));
            viToEn.Sort((a, b) => b.From.Length.CompareTo(a.From.Length));
        }

        private static string CurrentLanguage() => PlayerPrefs.GetString(LanguageKey, "VI").ToUpperInvariant() == "EN" ? "EN" : "VI";

        private sealed class TextState { public string EnglishSource; public string LastOutput; }
        private readonly struct Pair
        {
            public Pair(string from, string to) { From = from; To = to; }
            public string From { get; }
            public string To { get; }
        }
    }
}
