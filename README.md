# Ninja Assemble — Bản triển khai Clean-Room mở rộng

Ninja Assemble là dự án game mobile được xây dựng lại theo phương pháp **clean-room**, tập trung tái hiện các hệ thống gameplay có thể quan sát của Ninja Assemble / Ninja Rebirth và mở rộng đội hình nhân vật dựa trên Naruto + Naruto Shippuden.

> Repository không chứa asset trích xuất từ APK, source code độc quyền, logic server dịch ngược hoặc tài nguyên gốc được phân phối lại. Gameplay được triển khai lại độc lập; production art được quản lý bằng các package contract có file/evidence rõ ràng.

## Bản Android chơi thử

Dự án hiện đã build thành công **development APK** bằng Unity trên GitHub Actions. Đây là bản dành cho cài đặt và chơi thử local trên thiết bị Android, **không phải bản phát hành Google Play**.

- Workflow đã xác minh: `Android Playtest Build #19`
- Artifact: `NinjaAssemble-playtest-apk-19`
- SHA-256 artifact: `99a6c2db4b7fecc0b4dcba2bcdc2a6425c8fcce90b066ad4a85793e941babd8f`
- Source đã được merge vào `main` qua PR #102.

Khi có GitHub Release playtest, hãy tải file APK trong phần **Releases** của repository. Nếu chỉ cần artifact CI, có thể mở workflow Android Playtest Build tương ứng trong GitHub Actions.

## Trạng thái hiện tại

Các nền tảng chính đã có:

- **189 nhân vật gốc / 427 biến thể playable** trong census;
- **120 kỹ năng song ngữ EN/VI**, 44 kit profile tái sử dụng và mapping kit cho toàn bộ nhân vật gốc;
- Server Java 21 / Spring Boot với player state, wallet/energy, hero ownership, formation, deterministic battle/replay, progression/evolution, campaign, resource PvE, Arena/Shadow Arena, summon/pity, shop, inventory/equipment, guild, daily/events và mail;
- Unity mobile client với Bootstrap, các màn hình gameplay chính và vertical slice cho battle/summon/level-up;
- hệ thống presentation dựa trên Addressables;
- art production gate cho portrait/icon/chibi/animation/VFX/SFX/regression capture/review;
- parity gate có evidence cho combat stats, damage formula, summon profile và level cost;
- pipeline build Android APK/AAB và contract kiểm thử thiết bị.

### Những phần chưa được tuyên bố production-ready

APK chơi thử đã build thành công không đồng nghĩa game đã đạt production release gate. Release audit vẫn yêu cầu evidence thật cho production art, reference/balance parity và kiểm thử hiệu năng/smoke test trên thiết bị Android vật lý. Xem `docs/12-RELEASE-STATUS.md` để biết trạng thái chi tiết.

## Cấu trúc repository

```text
.
├── client-unity/          # Unity 6000.0 mobile client + editor/build automation
├── server/                # Java 21 / Spring Boot game server
├── game-data/             # roster, variants, skills, localization, balance evidence
├── art/                   # manifests, package schema, regression/review contracts
├── docs/                  # luật chơi, kiến trúc, milestone và release documentation
├── scripts/               # validation, generation, release/build helpers
├── .github/workflows/     # CI và Android build
└── docker-compose.yml     # PostgreSQL + Redis
```

## Chạy hạ tầng local

```bash
docker compose up -d postgres redis
```

## Kiểm tra server

```bash
bash scripts/validate-core.sh
mvn -f server/pom.xml test
```

## Kiểm tra content / release

```bash
python scripts/validate-content.py
python scripts/validate-art-packages.py
python scripts/validate-production-assets.py
python scripts/validate-reference-evidence.py
python scripts/validate-unity-shell.py
python scripts/validate-mobile-build-source.py
python scripts/validate-mobile-release-evidence.py
python scripts/release-audit.py --markdown
```

Một số strict release check được thiết kế để fail khi evidence production thực tế chưa đầy đủ; không được biến các gate này thành pass giả.

## Mở project Unity

Mở thư mục `client-unity` bằng Unity 6000.0. Để tạo lại mobile scene shell, chạy menu:

`Ninja Assemble → Mobile → Generate Complete Scene Shell`

## Build Android local

Cần cài Unity Android Build Support. Development build tạo APK để cài trực tiếp lên Android:

```bash
UNITY_PATH=/path/to/Unity ./scripts/build-mobile.sh development
```

Release build tạo AAB và chỉ cần thiết khi chuẩn bị phát hành store:

```bash
UNITY_PATH=/path/to/Unity ./scripts/build-mobile.sh release
```

Output được ghi vào `builds/android/` và không commit vào Git.

## Quy tắc production art

Một biến thể playable không được coi là release-ready chỉ vì CSV đánh dấu trạng thái. Component `READY` phải có descriptor thật tại:

`art/packages/<character_id>/<variant-slug>/package.json`

và phải có các file/evidence tương ứng trong repository. Final review `READY` cũng cần review evidence thực tế.

## Tài liệu chính

Xem thêm `docs/00-MASTER-PLAN.md`, `docs/12-RELEASE-STATUS.md`, `docs/100-PERCENT-COMPLETION-PLAN.md` và `docs/IMPLEMENTATION-MERGE-POLICY.md`.
