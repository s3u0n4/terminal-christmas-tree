# 🎄 Terminal Christmas Tree

터미널에서 Java 코드 글자가 한 글자씩 쌓이며 크리스마스 트리가 만들어지는 애니메이션입니다.

![미리보기](docs/preview.png)

## 이런 게 나와요

1. 갈색 기둥과 하얀 트리가 **코드 글자로 아래에서 위로** 쌓여요
2. 꼭대기에 큰 별이 뜨고, 핑크 전구 줄이 지그재그로 감겨요
3. 줄을 따라 알록달록한 오너먼트 볼이 하나씩 달려요
4. 크기와 무늬가 제각각인 선물 6개가 트리 밑에 놓여요
5. 전구·별·리본이 반짝이고 눈이 내리면서 **징글벨**이 흘러나와요 🎵

트리 크기는 실행할 때의 터미널 창 크기에 맞춰 자동으로 정해집니다.

## 실행 방법

JDK 17 이상이 필요합니다 (JDK 21에서 테스트).

```bash
javac -d out src/main/java/CodeTree.java
java -cp out CodeTree
```

- 터미널 창을 **전체화면으로 키운 다음** 실행하면 가장 예뻐요.
- 끝내려면 `Ctrl+C`를 누르세요.
- 크기를 직접 정하려면 줄 수와 칸 수를 넘기면 됩니다: `java -cp out CodeTree 40 120`

> IntelliJ의 Run 창은 커서 이동을 지원하지 않아 그림이 깨져요.
> macOS 터미널이나 IntelliJ 하단의 **Terminal 탭**에서 실행하세요.

## 꾸미기

`src/main/java/CodeTree.java`에서 아래 값만 바꾸면 됩니다.

| 바꾸고 싶은 것 | 값 |
|---|---|
| 트리를 채우는 코드 글자 | `CODE` |
| 꼭대기 별 모양 | `STAR` |
| 오너먼트 간격 / 색 | `ORNAMENT_SPACING`, `ORNAMENT_COLORS` |
| 선물 크기·색·무늬 | `GIFT_STYLES` |
| 음악 템포 / 악기 | `setTempoInBPM(160)`, `GLOCKENSPIEL` |
