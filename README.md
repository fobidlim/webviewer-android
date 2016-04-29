# WebViewer Library
You can download WebViewer Sample application on Google Play.

[![Get it on Google Play](http://www.android.com/images/brand/get_it_on_play_logo_small.png)](https://play.google.com/store/apps/details?id=com.tfc.webviewer.sample)

# Download
Download [the latest JAR](https://repo1.maven.org/maven2/com/github/fobid/webviewer/0.70.0/webviewer-0.70.0.aar) or grab via Maven:
```
<dependency>
  <groupId>com.github.fobid</groupId>
  <artifactId>webviewer</artifactId>
  <version>0.70.0</version>
</dependency>
```
or Gradle:
```
compile 'com.github.fobid:webviewer:0.70.0'
```

# Usage
*1. Include WebViewer into your `AndroidManifest.xml`.*
```
<activity
    android:name="com.tfc.webviewer.ui.WebViewerActivity"
    android:configChanges="orientation|screenSize" />
```

*2. Start `webViewerActivity` with URL.*
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
