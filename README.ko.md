# WebViewer 라이브러리
시작하기전에 구글플레이에서 WebViewer 예제 애플리케이션을 다운받을 수 있습니다.

[![구글플레이로 연결](http://www.android.com/images/brand/get_it_on_play_logo_small.png)](https://play.google.com/store/apps/details?id=com.tfc.webviewer.sample)

# 받아보기
[최신버전의 JAR](https://repo1.maven.org/maven2/com/github/fobid/webviewer/0.70.4/webviewer-0.70.4.aar) or grab via Maven:
```
<dependency>
  <groupId>com.github.fobid</groupId>
  <artifactId>webviewer</artifactId>
  <version>0.70.4</version>
</dependency>
```
or Gradle:
```
compile 'com.github.fobid:webviewer:0.70.4'
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
