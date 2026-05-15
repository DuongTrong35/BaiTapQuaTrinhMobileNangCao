# 📱 LG TV Remote Control App — Master Build Prompt
### Target: Claude Opus 4.6 via Antigravity | Language: Java | IDE: Android Studio

---

## 🎯 PROJECT OVERVIEW

Build a **production-ready Android application** called **"LG Remote Pro"** — a feature-complete LG TV remote control app **designed primarily for Vietnamese users**. The app must be written entirely in **Java**, structured for **Android Studio**, and follow modern Android architecture patterns. It should feel premium, polished, and exceed the quality of typical remote apps on the Play Store.

**Ngôn ngữ mặc định là tiếng Việt.** Toàn bộ giao diện người dùng — tất cả nhãn nút, thông báo, màn hình onboarding, cài đặt, trạng thái kết nối, thông báo lỗi — phải được viết bằng **tiếng Việt** trong file `values/strings.xml`. Không dùng tiếng Anh cho bất kỳ chuỗi nào hiển thị cho người dùng. Tiếng Anh chỉ dùng cho: tên thương hiệu (Netflix, YouTube, Disney+, Prime Video), tên kỹ thuật (HDMI 1, HDMI 2, AV), địa chỉ IP, và tên model TV.

---

## 🏗️ PROJECT STRUCTURE

Generate a complete Android Studio project with this package and file structure:

```
bai3/
├── MainActivity.java
├── ui/
│   ├── remote/
│   │   ├── RemoteFragment.java
│   │   ├── RemoteViewModel.java
│   │   └── RemoteAdapter.java
│   ├── devices/
│   │   ├── DeviceDiscoveryFragment.java
│   │   ├── DeviceDiscoveryViewModel.java
│   │   └── DeviceListAdapter.java
│   ├── settings/
│   │   ├── SettingsFragment.java
│   │   └── SettingsViewModel.java
│   └── onboarding/
│       └── OnboardingActivity.java
├── network/
│   ├── LGTVClient.java
│   ├── LGWebOSSocket.java
│   ├── CommandBuilder.java
│   └── NetworkScanner.java
├── model/
│   ├── TVDevice.java
│   ├── RemoteCommand.java
│   └── AppPreferences.java
├── repository/
│   ├── DeviceRepository.java
│   └── CommandRepository.java
├── database/
│   ├── AppDatabase.java
│   └── DeviceDao.java
└── utils/
    ├── NetworkUtils.java
    ├── AnimationUtils.java
    └── Constants.java
```

---

## 📋 FULL FEATURE SPECIFICATION

### 1. 🔍 TV Discovery & Pairing

- Scan the local Wi-Fi network for LG TVs using **SSDP (Simple Service Discovery Protocol)** over UDP multicast
- Search for devices responding to `urn:schemas-upnp-org:device:MediaRenderer:1` and LG-specific identifiers
- Display discovered TVs in a card-based list with: TV name, model, IP address, signal strength indicator, and connection status badge
- Implement **WebSocket-based pairing** using the LG TV's WebOS protocol:
  - Connect via `ws://<TV_IP>:3000/`
  - Send a `register` command with a client key
  - Store the returned client key in **SharedPreferences** for reconnection
  - Show a modal pairing dialog instructing the user to accept the prompt on their TV screen
- Support **manual IP entry** for TVs not discovered via SSDP
- Persist paired devices using **Room database** (SQLite) with auto-reconnect on app launch

### 2. 🎮 Remote Control Interface

Design a **full-featured virtual remote** with these button groups:

**Power & System**
- Power On / Power Off (Wake-on-LAN + WebOS command)
- Input Source selector (HDMI 1/2/3/4, AV, Component, USB)
- Home button
- Back button
- Settings/Gear button

**Navigation D-Pad**
- Up / Down / Left / Right arrows (with long-press repeat support at 300ms intervals)
- Center OK/Enter button
- Animated press states with ripple + scale feedback

**Volume & Channel**
- Volume Up / Down
- Mute toggle (show muted state visually)
- Channel Up / Down
- Number pad (0–9) for direct channel entry
- Channel List button

**Media Playback**
- Play / Pause / Stop
- Rewind / Fast Forward
- Record (if supported)
- Previous / Next track

**Smart TV**
- Netflix, YouTube, Prime Video, Disney+ quick-launch buttons with official brand colors and icons
- LG Content Store launcher
- Magic Remote cursor toggle (enable/disable pointer mode)
- Screen Share / Miracast toggle

**Touchpad Mode**
- Swipe-to-navigate virtual touchpad (replaces D-Pad when enabled)
- Tap = OK, Two-finger tap = Back, Two-finger swipe down = Home
- Visual feedback with animated touch ripple

### 3. 🎨 UI / UX Design

**Design Language: Premium Dark Remote**

- **Color Palette:**
  - Background: `#0A0A0F` (near-black with blue undertone)
  - Surface/Card: `#14141E`
  - Primary Accent: `#00C2FF` (electric cyan)
  - Secondary Accent: `#7B2FFF` (deep violet)
  - Button Active: `#1E1E2E`
  - Button Pressed: `#2A2A3E`
  - Text Primary: `#F0F0FF`
  - Text Secondary: `#8A8AA8`
  - Success: `#00E676`
  - Danger: `#FF4757`

- **Typography:**
  - Primary Font: `Rajdhani` (display, headers) — imported via Google Fonts
  - Body Font: `DM Sans` (labels, body text)
  - Monospace: `JetBrains Mono` (IP addresses, technical info)

- **Button Styling:**
  - Circular buttons for D-Pad and media controls
  - Rounded rectangle for volume/channel strips
  - Glass-morphism effect for the main remote card (semi-transparent surface with blur)
  - Neon glow effect (`BoxShadow` via LayerDrawable) on accent-colored buttons
  - All buttons have: normal state, pressed state, and disabled state drawables

- **Animations:**
  - App launch: fade-in + slide-up of remote card (350ms, ease-out)
  - Button press: scale down to 0.92 on touch down, scale back on release (150ms)
  - Successful command: brief cyan flash on button (200ms)
  - Connection lost: shake animation on status indicator
  - Tab switching: shared element transitions between fragments
  - Volume change: animated slider bar (custom view)

- **Bottom Navigation Bar:**
  - Tab 1: Remote (joystick icon)
  - Tab 2: Devices (TV icon)
  - Tab 3: Settings (gear icon)
  - Animated tab selection with sliding indicator and icon color transition

### 4. ⚙️ Architecture & Technical Requirements

**Architecture:** MVVM (Model-View-ViewModel)
- `ViewModel` + `LiveData` for all UI state management
- `Repository` pattern for data access abstraction
- `Room` for local device persistence
- `WorkManager` for background reconnection attempts

**Networking:**
- Use **OkHttp** (version 4.x) for WebSocket connection management
- Implement auto-reconnect with exponential backoff (1s → 2s → 4s → max 30s)
- Connection state machine: `DISCONNECTED → CONNECTING → PAIRING → CONNECTED → ERROR`
- All network calls on background threads via `ExecutorService`, results posted to `LiveData`
- Implement heartbeat ping every 30 seconds to keep WebSocket alive

**WebOS Command Protocol:**
- All commands sent as JSON via WebSocket
- Command format:
```json
{
  "type": "request",
  "id": "command_<uuid>",
  "uri": "ssap://<command_uri>",
  "payload": {}
}
```
- Implement these WebOS URIs:
  - `ssap://system/turnOff` — Power off
  - `ssap://audio/setVolume` — Set volume (payload: `{"volume": int}`)
  - `ssap://audio/volumeUp` — Volume up
  - `ssap://audio/volumeDown` — Volume down
  - `ssap://audio/setMute` — Mute (payload: `{"mute": bool}`)
  - `ssap://tv/switchInput` — Switch input (payload: `{"inputId": "HDMI_1"}`)
  - `ssap://com.webos.service.ime/sendEnterKey` — OK/Enter
  - `ssap://com.webos.service.ime/deleteCharacters` — Backspace
  - `ssap://system.notifications/createToast` — Display toast on TV
  - `ssap://com.webos.applicationManager/launch` — Launch app (Netflix, YouTube, etc.)
  - `ssap://tv/getChannelList` — Fetch channel list
  - `ssap://tv/setChannel` — Change channel
  - `com.webos.service.networkinput/getPointerInputService` — Pointer control

**Key Commands (SSAP button presses via):**
  - `ssap://com.webos.service.ime/sendKeycode` with keycodes for: UP, DOWN, LEFT, RIGHT, HOME, BACK, RED, GREEN, YELLOW, BLUE, PLAY, PAUSE, STOP, REWIND, FASTFORWARD, CHANNELUP, CHANNELDOWN

**Gradle Dependencies (build.gradle :app):**
```gradle
dependencies {
    implementation 'androidx.appcompat:appcompat:1.7.0'
    implementation 'com.google.android.material:material:1.12.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.lifecycle:lifecycle-viewmodel:2.8.0'
    implementation 'androidx.lifecycle:lifecycle-livedata:2.8.0'
    implementation 'androidx.navigation:navigation-fragment:2.7.7'
    implementation 'androidx.navigation:navigation-ui:2.7.7'
    implementation 'androidx.room:room-runtime:2.6.1'
    annotationProcessor 'androidx.room:room-compiler:2.6.1'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'androidx.work:work-runtime:2.9.0'
    implementation 'com.google.code.gson:gson:2.10.1'
    implementation 'androidx.preference:preference:1.2.1'
}
```

**Permissions (AndroidManifest.xml):**
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />
<uses-permission android:name="android.permission.VIBRATE" />
```

**minSdkVersion:** 26 (Android 8.0)  
**targetSdkVersion:** 34 (Android 14)  
**compileSdkVersion:** 34

### 5. 📱 App Screens — Full Specification

#### Màn hình 1: Giới thiệu (Chỉ hiện lần đầu)
- 3-trang ViewPager2 slideshow, toàn bộ nội dung bằng tiếng Việt:
  - Trang 1: **"Điều khiển TV LG của bạn"** — hình minh họa remote có animation, tiêu đề, mô tả ngắn
  - Trang 2: **"Kết nối chỉ trong vài giây"** — animation Wi-Fi, giải thích cách ghép nối
  - Trang 3: **"Mọi thứ trong tầm tay"** — lưới tính năng nổi bật
- Nút **"Bỏ qua"** (góc phải trên), nút **"Tiếp theo"** / **"Bắt đầu ngay"**
- Lưu `onboardingComplete = true` vào SharedPreferences sau khi hoàn tất

#### Màn hình 2: Tìm kiếm thiết bị
- Vòng quét động (animation radar xoay via `ObjectAnimator`) với chữ **"Đang tìm kiếm TV LG..."**
- RecyclerView danh sách TV tìm thấy dạng CardView
- Mỗi card hiển thị: tên TV, model, địa chỉ IP, badge **"Đã ghép nối"** / **"Chưa kết nối"**, nút **"Kết nối"**
- Floating Action Button: **"Thêm thủ công"** → bottom sheet nhập địa chỉ IP với nút **"Xác nhận"** / **"Hủy"**
- Empty state: hình minh họa + chữ **"Không tìm thấy TV nào"** + nút **"Thử lại"**
- Kéo xuống để quét lại

#### Màn hình 3: Remote chính
- Tên TV đang kết nối + chỉ báo trạng thái (chấm xanh = **"Đã kết nối"**, chấm đỏ = **"Mất kết nối"**) trên toolbar
- Layout remote cuộn được, chia theo nhóm rõ ràng:
  - Hàng trên: Nguồn, Đầu vào, Trang chủ, Quay lại, Cài đặt
  - Vùng D-Pad: nút định hướng lớn với nút OK ở giữa
  - Thanh Âm lượng / Kênh
  - Hàng phát media
  - Hàng ứng dụng nhanh (Netflix, YouTube, Prime, Disney+)
  - Bàn phím số (có thể thu/mở)
- Nút bật/tắt touchpad (góc phải dưới)
- Overlay touchpad: panel bán trong suốt toàn màn hình cho điều hướng cử chỉ

#### Màn hình 4: Cài đặt
- Kết nối: **"Quên thiết bị"**, **"Ghép nối lại"**, **"Đổi địa chỉ IP"**
- Tùy chỉnh remote: toggle **"Bố cục gọn / đầy đủ"**, toggle **"Rung khi bấm nút"**
- Giao diện: toggle **"Tự động / Tối / Sáng"**
- Giới thiệu: phiên bản ứng dụng, giấy phép mã nguồn mở, link góp ý
- Toggle thông báo: **"Cảnh báo khi TV mất kết nối"**

### 6. 💎 High-Conversion UX Features

These features drive retention and 5-star reviews:

- **Haptic Feedback:** Use `Vibrator` API — short 15ms buzz on every button press, 50ms on errors
- **Voice Input:** Microphone button → uses Android `SpeechRecognizer` → sends text to TV keyboard via `ssap://com.webos.service.ime/insertText`
- **Widget Support:** Android home screen widget (1x1 and 2x1) with power, volume up/down, and mute controls — implemented as `AppWidgetProvider`
- **Quick Settings Tile:** Android Quick Settings tile for Power toggle — implemented as `TileService`
- **Keyboard Input:** Full keyboard overlay for text fields on TV — opens `InputMethodManager`-aware panel
- **Command History:** Last 10 commands shown as quick-replay chips above the remote
- **Favorites:** Long-press any app to add to favorites strip; drag-to-reorder
- **Smart Auto-Connect:** On app resume, automatically reconnect to last-used TV without user action
- **TV Status Cards:** Show current volume level, current input, mute state as live status chips updated via WebOS subscription (`type: "subscribe"`)
- **Dark/Light Theme:** Full Material3 dynamic theming support

### 7. 🔍 TV Capability Detection & Graceful Degradation

**This is a critical requirement.** Not all LG TVs support every WebOS feature. The app must query the TV's actual capabilities immediately after pairing and **automatically hide or disable any button/feature the connected TV does not support**. Never show a broken button or a failed command to the user.

#### Capability Query on Connect

Immediately after a successful WebSocket connection and pairing, send these discovery requests:

```json
// 1. Get full service list
{ "type": "request", "id": "cap_services", "uri": "ssap://api/getServiceList" }

// 2. Get installed app list (to check Netflix, YouTube, Prime, Disney+, etc.)
{ "type": "request", "id": "cap_apps", "uri": "ssap://com.webos.applicationManager/listApps" }

// 3. Get available input sources
{ "type": "request", "id": "cap_inputs", "uri": "ssap://tv/getExternalInputList" }

// 4. Check pointer/Magic Remote support
{ "type": "request", "id": "cap_pointer", "uri": "ssap://com.webos.service.networkinput/getPointerInputService" }

// 5. Check audio capabilities
{ "type": "request", "id": "cap_audio", "uri": "ssap://audio/getStatus" }

// 6. Check channel/tuner capability
{ "type": "request", "id": "cap_channels", "uri": "ssap://tv/getChannelList" }
```

Parse all responses and store results in a `TVCapabilities.java` model class (not a Room entity — held in memory for the session):

```java
public class TVCapabilities {
    public boolean hasVolumeControl;
    public boolean hasMuteControl;
    public boolean hasChannelControl;
    public boolean hasInputSelection;
    public boolean hasPointerControl;    // Magic Remote cursor
    public boolean hasMediaPlayback;     // Play/Pause/Stop/FF/RW
    public boolean hasScreenShare;
    public boolean hasNetflix;
    public boolean hasYouTube;
    public boolean hasPrimeVideo;
    public boolean hasDisneyPlus;
    public boolean hasLGContentStore;
    public boolean hasVoiceInput;        // ssap://com.webos.service.ime
    public boolean hasRecordButton;
    public List<String> availableInputIds; // e.g. ["HDMI_1", "HDMI_2", "AV"]
}
```

#### UI Behavior Rules — Dimmed Unsupported Buttons

Apply these rules in `RemoteFragment.java` after capabilities are loaded via `RemoteViewModel`.

**Unsupported buttons must remain visible but appear clearly dimmed** — the user can see the full remote layout at all times and understand what their TV does and doesn't support. Do **not** hide or remove any button.

| Condition | UI Action |
|---|---|
| `hasVolumeControl == false` | Dim volume strip: `setEnabled(false)`, alpha `0.35f` |
| `hasMuteControl == false` | Dim mute button: `setEnabled(false)`, alpha `0.35f` |
| `hasChannelControl == false` | Dim channel buttons + number pad: `setEnabled(false)`, alpha `0.35f` |
| `hasInputSelection == false` | Dim Input Source button: `setEnabled(false)`, alpha `0.35f` |
| `hasPointerControl == false` | Dim touchpad FAB: `setEnabled(false)`, alpha `0.35f` |
| `hasMediaPlayback == false` | Dim entire media row: `setEnabled(false)`, alpha `0.35f` |
| `hasScreenShare == false` | Dim Screen Share button: `setEnabled(false)`, alpha `0.35f` |
| `hasRecordButton == false` | Dim Record button: `setEnabled(false)`, alpha `0.35f` |
| `hasNetflix == false` | Dim Netflix button: `setEnabled(false)`, alpha `0.35f` |
| `hasYouTube == false` | Dim YouTube button: `setEnabled(false)`, alpha `0.35f` |
| `hasPrimeVideo == false` | Dim Prime Video button: `setEnabled(false)`, alpha `0.35f` |
| `hasDisneyPlus == false` | Dim Disney+ button: `setEnabled(false)`, alpha `0.35f` |
| `hasLGContentStore == false` | Dim LG Content Store button: `setEnabled(false)`, alpha `0.35f` |
| `hasVoiceInput == false` | Dim microphone button: `setEnabled(false)`, alpha `0.35f` |
| Input source not in `availableInputIds` | Dim that specific input option: `setEnabled(false)`, alpha `0.35f` |
| Supported feature | Full opacity `1.0f`, `setEnabled(true)`, fully interactive |

**Visual styling for dimmed state:**
- Alpha: `0.35f` — clearly muted but still recognizable
- Icon color: desaturated (grayscale tint via `ColorFilter`)
- Background: same shape/size as active buttons but no glow effect
- No strikethrough, no lock icon, no tooltip — just visually faded
- Haptic feedback disabled on dimmed buttons (no buzz when tapped)
- If user taps a dimmed button, do nothing silently — no toast, no error

**Loading state (while capabilities are being fetched):**
- All buttons start at alpha `0.5f` with `setEnabled(false)` and a subtle repeating pulse animation (`ObjectAnimator` on alpha: `0.5f → 0.7f → 0.5f`, 1000ms loop)
- Once capabilities resolve, animate each button to its final state: supported → fade to `1.0f` (250ms), unsupported → fade to `0.35f` (250ms)

**Capabilities must be re-checked every time the app connects to a TV** — different TVs have different support levels.

#### Handling Command Failures

If a command is sent and the TV responds with an error (e.g., `"errorCode": "403"` or `"type": "error"`):
- Do **not** show an error dialog or toast for capability-related failures
- Log the error: `Log.w("LGRemotePro", "Command not supported: " + uri)`
- If the same command fails 2 times in a row, mark that capability as `false` in `TVCapabilities` and animate the button to dimmed state (`alpha 0.35f`, `setEnabled(false)`)
- For connectivity errors (timeout, socket closed), show a non-intrusive snackbar: *"Mất kết nối. Đang kết nối lại…"*

---

### 8. 🆓 Free & Open — No Monetization

This app is **completely free** with no ads, no paywalls, no premium tiers, and no in-app purchases. Every feature is available to every user from the moment they install the app. Do **not** implement Google Play Billing, subscription gates, feature locks, upgrade prompts, or any form of monetization. There must be zero references to purchasing, upgrading, or unlocking anything anywhere in the codebase or UI.

---

## 📁 FILES TO GENERATE

Generate ALL of the following files in full, with no placeholders or TODOs:

### Java Source Files
1. `MainActivity.java` — Navigation host, bottom nav setup, deep link handling
2. `OnboardingActivity.java` — ViewPager2 onboarding with skip/next/finish
3. `RemoteFragment.java` — Full remote UI logic, button listeners, touchpad mode
4. `RemoteViewModel.java` — LiveData for connection state, TV status, command queue
5. `DeviceDiscoveryFragment.java` — RecyclerView + SSDP scan trigger + manual entry
6. `DeviceDiscoveryViewModel.java` — Scan logic, device list LiveData
7. `DeviceListAdapter.java` — RecyclerView adapter with DiffUtil
8. `SettingsFragment.java` — PreferenceFragmentCompat with all settings
9. `LGTVClient.java` — WebSocket client, connect/disconnect, send command, subscriptions
10. `LGWebOSSocket.java` — OkHttp WebSocket wrapper with reconnect logic
11. `CommandBuilder.java` — Static factory methods for all SSAP JSON commands
12. `NetworkScanner.java` — SSDP multicast scan + parsing + WoL implementation
13. `TVDevice.java` — Room entity (id, name, model, ipAddress, clientKey, lastConnected)
14. `TVCapabilities.java` — In-memory model of what the connected TV actually supports; populated after connect
15. `RemoteCommand.java` — POJO for command history
15. `AppPreferences.java` — SharedPreferences wrapper (Singleton)
16. `DeviceRepository.java` — Room + network data operations
17. `CommandRepository.java` — Command dispatch + history management
18. `AppDatabase.java` — Room database singleton with migration strategy
19. `DeviceDao.java` — Room DAO interface (insert, delete, getAll, getById)
20. `NetworkUtils.java` — IP validation, connectivity checks, WoL packet builder
21. `AnimationUtils.java` — Button press animations, shake, pulse, fade helpers
22. `Constants.java` — All magic strings/numbers (URIs, ports, timeouts, prefs keys)
23. `RemoteWidget.java` — `AppWidgetProvider` for home screen widget
24. `PowerTileService.java` — `TileService` for Quick Settings tile

### XML Layout Files
25. `activity_main.xml` — NavHostFragment + BottomNavigationView
26. `activity_onboarding.xml` — ViewPager2 + dots indicator + CTA button
27. `fragment_onboarding_page.xml` — Single onboarding slide layout
28. `fragment_remote.xml` — Full remote layout (ConstraintLayout, all sections)
29. `fragment_device_discovery.xml` — RecyclerView + scan animation + FAB
30. `item_device.xml` — Device card layout
31. `fragment_settings.xml` — Preference screen container
32. `layout_dpad.xml` — Custom D-Pad layout (include in remote fragment)
33. `layout_media_controls.xml` — Media playback button row
34. `layout_volume_strip.xml` — Volume +/- with label and progress indicator
35. `layout_app_launcher.xml` — Smart app grid (Netflix, YouTube, etc.)
36. `layout_number_pad.xml` — Collapsible number input grid
37. `layout_touchpad.xml` — Full-screen touchpad overlay
38. `widget_remote.xml` — Home screen widget layout
39. `dialog_manual_ip.xml` — Bottom sheet for manual IP entry

### Drawable / Resource Files
40. `drawable/bg_button_circle.xml` — Circle button with states (normal, pressed, disabled)
41. `drawable/bg_button_rect.xml` — Rounded rect button with states
42. `drawable/bg_remote_card.xml` — Glass-morphism card background
43. `drawable/ic_power.xml` — Power icon (vector)
44. `drawable/ic_volume_up.xml`, `ic_volume_down.xml`, `ic_mute.xml`
45. `drawable/ic_arrow_up.xml`, `ic_arrow_down.xml`, `ic_arrow_left.xml`, `ic_arrow_right.xml`
46. `drawable/ic_home.xml`, `ic_back.xml`, `ic_settings_gear.xml`
47. `drawable/ic_netflix.xml`, `ic_youtube.xml`, `ic_prime.xml`, `ic_disney.xml`
48. `drawable/glow_accent.xml` — LayerDrawable for neon glow effect
49. `drawable/ic_remote_tab.xml`, `ic_devices_tab.xml`, `ic_settings_tab.xml`

### Other Resource Files
50. `values/colors.xml` — Full color palette (all colors listed above)
51. `values/styles.xml` — App theme (Material3 DayNight), button styles, text styles
52. `values/themes.xml` — Light and dark theme definitions
53. `values/strings.xml` — **Toàn bộ chuỗi hiển thị bằng tiếng Việt** — đây là ngôn ngữ mặc định của app. Bao gồm tất cả: nhãn nút, tiêu đề màn hình, thông báo, trạng thái, lỗi, onboarding, cài đặt. Không có chuỗi tiếng Anh nào hiển thị cho người dùng ngoại trừ tên thương hiệu và thuật ngữ kỹ thuật (HDMI, IP, model TV).
54. `values/dimens.xml` — All spacing, size, corner radius values
55. `values/attrs.xml` — Custom attribute declarations
56. `font/rajdhani_bold.ttf` — (note: instruct to download from Google Fonts)
57. `font/dm_sans_regular.ttf` — (note: instruct to download from Google Fonts)
58. `xml/network_security_config.xml` — Allow cleartext for local IPs (TV WebSocket is ws://)
59. `xml/app_widget_info.xml` — Widget metadata
60. `AndroidManifest.xml` — Complete manifest with all activities, services, permissions, widget provider

### Build Files
61. `build.gradle` (project-level) — repositories, classpath
62. `build.gradle` (app-level) — dependencies, buildTypes, signing config stubs
63. `proguard-rules.pro` — Keep rules for OkHttp, Room, Gson, WebOS classes
64. `gradle.properties` — Standard properties + `android.useAndroidX=true`

---

## 🧪 CODE QUALITY REQUIREMENTS

- **Zero hardcoded strings** — tất cả chuỗi hiển thị cho người dùng phải nằm trong `strings.xml` bằng tiếng Việt, không hardcode trực tiếp trong code Java hay XML layout
- **Zero hardcoded colors** — all colors from `colors.xml`
- **Null safety** — all null checks, no NPE-prone code
- **Thread safety** — no UI updates on background threads; use `LiveData.postValue()` from workers
- **Error handling** — every network call wrapped in try-catch with user-visible error states
- **Logging** — use `Log.d/e/w` with tag `"LGRemotePro"` throughout; Timber-ready structure
- **Comments** — every class has a Javadoc block; complex methods have inline comments
- **No deprecated APIs** — use modern equivalents (e.g., `registerForActivityResult` not `startActivityForResult`)
- **Accessibility** — all interactive elements have `contentDescription`, min touch target 48dp

---

## 🚀 DELIVERY INSTRUCTIONS FOR CLAUDE OPUS 4.6

Generate files in this order:
1. `AndroidManifest.xml` — set the full stage
2. All `build.gradle` files — dependencies first
3. `Constants.java` — defines all shared values
4. All model/entity classes (`TVDevice.java`, `RemoteCommand.java`)
5. Database layer (`AppDatabase.java`, `DeviceDao.java`)
6. Network layer (`LGWebOSSocket.java`, `LGTVClient.java`, `CommandBuilder.java`, `NetworkScanner.java`)
7. Repository layer
8. All ViewModels
9. All Fragments and Activities
10. All XML layouts (in dependency order — includes before dependents)
11. All drawable XMLs
12. All values XMLs
13. Widget and TileService
14. ProGuard rules
15. Final integration notes and setup instructions

**For each file, output:**
```
=== FILE: path/to/FileName.java ===
[full file contents]
=== END FILE ===
```

Do not summarize, skip, abbreviate, or stub any file. Every file must be 100% complete, compilable, and production-ready.

---

## ✅ DEFINITION OF DONE

The output is complete when:
- [ ] A developer can clone the output into Android Studio and build immediately with 0 errors
- [ ] The app discovers and pairs with an LG WebOS TV on the same Wi-Fi network
- [ ] All remote buttons send real WebOS commands over WebSocket
- [ ] Pairing key persists across app restarts
- [ ] TV capabilities are queried on every connect; unsupported buttons are hidden automatically
- [ ] No broken or non-functional buttons are ever visible to the user
- [ ] The UI renders correctly on phones 5"–7" screens at all densities (mdpi to xxxhdpi)
- [ ] Dark mode is fully supported with no visual artifacts
- [ ] Haptic feedback fires on button press
- [ ] All animations are smooth at 60fps
- [ ] Widget appears on home screen after installation
- [ ] App handles network loss gracefully with auto-reconnect

---

*Built for Antigravity | Powered by Claude Opus 4.6 | © LG Remote Pro*
