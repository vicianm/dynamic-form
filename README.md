# StickyLinearLayout

[![Maven Central](https://img.shields.io/maven-central/v/com.github.vicianm/sticky-linear-layout.svg?style=flat)](https://github.com/vicianm/sticky-linear-layout/releases/latest)

`StickyLinearLayout` is `LinearLayout` based UI component with a capability to pin headers (footers) of large forms.

There are some solutions similar to this one, but:
1. They are mostly base around `ListView`, therefore they are not applicable for simple forms.
1. They usually don't support multiple floating headers (just one header sticks to the top at time).

`StickyLinearLayout` is suitable for the situation when you need to pin section headers of large form based on `LinearLayout`.

## Demo

https://github.com/vicianm/sticky-linear-layout-demo

![](https://raw.githubusercontent.com/vicianm/sticky-linear-layout/master/docs/images/demo-tablet-01.gif)
![](https://raw.githubusercontent.com/vicianm/sticky-linear-layout/master/docs/images/demo-tablet-02.gif)

## Download

### Manual download

https://github.com/vicianm/sticky-linear-layout/releases/latest

### Maven dependency

    <dependency>
        <groupId>com.github.vicianm</groupId>
        <artifactId>sticky-linear-layout</artifactId>
        <version>0.4</version>
    </dependency>

### Gradle dependency

    compile 'com.github.vicianm:sticky-linear-layout:0.4'

## License

```license
Copyright 2017 Michal Vician

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
