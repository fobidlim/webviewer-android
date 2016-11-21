# WebViewer 라이브러리

[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-webviewer-green.svg?style=true)](https://android-arsenal.com/details/1/4626)

[![Join the chat at https://gitter.im/webviewer-android/Lobby](https://badges.gitter.im/webviewer-android/Lobby.svg)](https://gitter.im/fobid/webviewer?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![Github Release][release-image]][release-url]

시작하기전에 구글플레이에서 WebViewer 예제 애플리케이션을 다운받을 수 있습니다.

[![구글플레이로 연결](http://www.android.com/images/brand/get_it_on_play_logo_small.png)](https://play.google.com/store/apps/details?id=com.tfc.webviewer.sample)

# 받아보기
[최신버전의 JAR](https://repo1.maven.org/maven2/com/github/fobid/webviewer/0.70.5/webviewer-0.70.5.aar) or grab via Maven:
```
<dependency>
  <groupId>com.github.fobid</groupId>
  <artifactId>webviewer</artifactId>
  <version>0.70.5</version>
</dependency>
```
or Gradle:
```
compile 'com.github.fobid:webviewer:0.70.5'
```

# Usage
*1. `AndroidManifest.xml` 파일에 아래와 같이 코드를 추가하세요.*
```
<activity
    android:name="com.tfc.webviewer.ui.WebViewerActivity"
    android:configChanges="orientation|screenSize" />
```

*2. URL을 `webViewerActivity`에 값을 넘겨 실행하세요.*
```
String url = "https://www.github.com/fobid";
// String url = "www.github.com/fobid";
// String url = "github.com/fobid";

Intent intent = new Intent(Context, WebViewerActivity.class);
intent.putExtra(WebViewerActivity.EXTRA_URL, url);
startActivity(intent);
```
# License
```
Copyright 2016 Fobid

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```


[release-image]: https://img.shields.io/badge/release-v0.70.5-lightgrey.svg
[release-url]: https://github.com/fobid/webviewer/releases/tag/0.70.5
