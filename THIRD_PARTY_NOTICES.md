# Third-party components

TikTools contains independently implemented module and settings code. It does not distribute the TikTok or GlobalKit APKs or their implementation code.

## Xposed API 82

- Author: rovo89 / Xposed Framework contributors.
- Upstream artifact: <https://api.xposed.info/de/robv/android/xposed/api/82/api-82.jar>.
- Upstream POM: <https://api.xposed.info/de/robv/android/xposed/api/82/api-82.pom>.
- License: Apache License 2.0; a copy is in [third_party/Apache-2.0.txt](third_party/Apache-2.0.txt).
- Used only for compilation; the API JAR is not included in the application APK.
- SHA-256: `f48c635f1c7469fdec0e00ad2ea0b7a6b2f5b55065784a35b7ca3a84615e8e25`.

## Google ML Kit and transitive libraries

Translation uses `com.google.mlkit:translate:17.0.3` and language identification uses `com.google.mlkit:language-id:17.0.6`. Their runtime dependencies are obtained from Google's Maven repository. These components and their models remain subject to their upstream terms and licenses; publication of TikTools source does not relicense them.

- [ML Kit terms and privacy](https://developers.google.com/ml-kit/terms).
- [Translation for Android](https://developers.google.com/ml-kit/language/translation/android).
- [Language identification for Android](https://developers.google.com/ml-kit/language/identification/android).

## OpenStreetMap

The interactive map loads the currently visible OpenStreetMap tiles over HTTPS and displays `© OpenStreetMap contributors`. Tiles are cached according to origin HTTP caching headers. No tile archives or prefetched offline maps are distributed in this repository.

- [Copyright and data license](https://www.openstreetmap.org/copyright).
- [Tile usage policy](https://operations.osmfoundation.org/policies/tiles/).

## Project source

Copyright © 2026 sunisup666. No project-wide open-source license has been selected. Third-party components retain their separate licenses.
