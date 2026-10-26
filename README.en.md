<div align="center">
  <img src="docs/assets/tiktools.svg" width="88" height="88" alt="TikTools" />
  <h1>TikTools</h1>
  <p>Your TikTok toolbox</p>
  <p>Region presets · Map picker · Device parameters · Interface tools</p>
  <p><img src="docs/assets/platforms.svg" width="348" height="28" alt="Android 13+ · Xposed API 82 · MIT" /></p>
  <p><a href="README.md">简体中文</a> · <strong>English</strong></p>
  <p>
    <a href="https://github.com/sunisup666/TikTools/releases/latest"><strong>Download the latest release</strong></a> ·
    <a href="#quick-start">Quick start</a> ·
    <a href="CHANGELOG.md">Changelog</a> ·
    <a href="https://github.com/sunisup666/TikTools/issues">Report an issue</a>
  </p>
</div>

## Features

| Feature | What it does |
| :--- | :--- |
| **Region presets** | Fill in matching carrier, MCC/MNC, language, time zone and default city coordinates for 23 regions. |
| **Location and map** | Choose from 46 cities or select a point on the map. Supports fixed points, drift, circular walks and custom routes. |
| **Device parameters** | Read matching values from this device, use templates for supported hardware, or edit fields manually. |
| **Interface tools** | Control view rules for navigation, side buttons, author, caption and music elements, plus keyword comment filtering. |
| **Translation and saving** | Translate on the device with ML Kit. Confirm before saving a direct video URL that can be matched to the current content. |
| **Configuration** | JSON import/export, saved coordinates, launcher icon controls and local diagnostics. |

## Quick start

**Requirements:** Android 13 or newer, ARM64, and an enabled Vector or compatible LSPosed / Xposed framework. TikTools uses the legacy Xposed API 82.

1. Download and install the APK from [Releases](https://github.com/sunisup666/TikTools/releases/latest).
2. Enable **TikTools** in your framework and select only the TikTok app you use. Disable other modules that modify the same interfaces.
3. Open TikTools, enable the main switch, then turn on the feature groups you need.
4. Force stop and reopen TikTok after changing parameters.

For the saving and translation menu, enable **Interface → Show the in-app tools button**, then tap **TT** in TikTok.

## App language

Open **Overview → App language** and choose:

| Option | Display language |
| :--- | :--- |
| **Follow system** | Follow the system language, falling back to English when a translation is unavailable. |
| **English** | English settings, map, preset names and validation messages. |
| **简体中文** | Simplified Chinese settings, map, preset names and validation messages. |

Android system app-language settings offer the same choice. Switching the display language works offline and preserves module parameters. TikTok locale overrides remain separate controls on the Region page.

## Presets and map

| Goal | Where to go |
| :--- | :--- |
| Fill in region parameters together | **Region → Country / region** |
| Use a common city | **Location → Choose a city**, with city and country search |
| Select a precise point | **Location → Choose on map**; only **Use this location** saves the selection |
| Use this device's fields | **Device → Choose a device preset → This device (recommended)** |

The map uses **WGS84** and does not read the phone's real GPS. Coordinates chosen on the map or from a city preset stay fixed during IP country updates. Use **Follow region coordinates again** to restore that behavior. Manually selecting a region or city disables automatic IP linking.

City presets work offline. Online maps require a connection, and translation models must be downloaded before first use. Cell tower and Wi-Fi fields are separate test parameters, not real network information for the selected city.

## Compatibility

| Item | Recorded scope |
| :--- | :--- |
| Module package | `io.github.sunisup666.tiktools` |
| Recorded device baseline | Redmi K40 · Android 17 · Vector 2.2 |
| TikTok baseline | `com.ss.android.ugc.trill` · 47.4.3 / 470403 |
| Other TikTok package | `com.zhiliaoapp.musically` can be selected in scope; not fully verified |
| Other apps | Douyin, Kuaishou and Xiaohongshu are outside the current scope |

View rules only affect recognizable small views, and some rules depend on the target version. Saving does not support live streams, photo posts, DRM, segmented streams, or addresses that cannot be matched unambiguously to the current content. See the [testing notes](docs/TESTING.md) for results.

Region behavior also depends on the account and network exit. Process-local overrides do not change the hardware or guarantee server behavior. Location overrides modify the `Location` values an app reads; they do not create location events that the system has not supplied.

## Build and development

<details>
<summary><strong>Show local build steps</strong></summary>

Requires JDK 17, Android SDK 36, Build Tools 36.1.0 and Python 3. Set `ANDROID_HOME` or add your SDK path to `local.properties`:

```properties
sdk.dir=C:/Android/Sdk
```

```sh
git clone https://github.com/sunisup666/TikTools.git
cd TikTools
./gradlew :app:assembleDebug :probe:assembleDebug
python tools/build.py
```

On Windows, use `gradlew.bat`. The Python builder accepts `--install`, `--gradle` and `--proxy`. Device scripts use the current ADB device by default, or the device selected through `ANDROID_SERIAL`.

Release signing keys are not included in source control. Maintainers can set `TIKTOOLS_KEYSTORE`, `TIKTOOLS_STORE_PASSWORD`, `TIKTOOLS_KEY_ALIAS` and `TIKTOOLS_KEY_PASSWORD`. A locally generated development key cannot update a package with a different signature. See the [release guide](docs/RELEASING.md).

</details>

The [app plugin analysis guide](docs/APP_PLUGIN_ANALYSIS_GUIDE.md) covers diagnosis, implementation, device validation and Agent handoffs for work on other apps and plugins. The guide is currently in Chinese.

## Support and license

Use [Issues](https://github.com/sunisup666/TikTools/issues) for questions and feedback. Include your TikTools, TikTok, Android and framework versions, plus what happens with the module disabled. Remove private information from logs and screenshots first.

This project is licensed under the [MIT License](LICENSE). See [third-party notices](THIRD_PARTY_NOTICES.md) for component and map-service terms, and the [privacy notes](docs/PRIVACY.md) for data handling. Privacy notes are currently in Chinese.
