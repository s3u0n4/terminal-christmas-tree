# CLAUDE.md

터미널 크리스마스 애니메이션. 외부 라이브러리는 없다.

- `src/main/java/CodeTree.java`: 코드 글자로 쌓는 크리스마스 트리
- `src/main/java/MusicBox.java`: 3D 오르골 (유리 돔 안 트리·눈사람·선물이 회전판에서 돌고, 고요한 밤이 오르골 소리로 나옴)

## 실행

```bash
javac -d out src/main/java/*.java
java -cp out CodeTree            # 현재 터미널 크기에 맞춰 그림 (stty로 읽음)
java -cp out CodeTree 45 160     # 줄 수, 칸 수를 직접 지정
java -cp out MusicBox            # 3D 오르골 (인자 규칙은 같음)
```

- ANSI 커서 이동을 쓰므로 IntelliJ Run 창에서는 깨진다. 실제 터미널에서 실행해야 한다.
- 사용자에게 보여 줄 때는 macOS 터미널 새 창을 화면 크기로 띄워 실행한다:
  ```bash
  osascript -e 'tell application "Finder" to set screenBounds to bounds of window of desktop
  tell application "Terminal"
    activate
    do script "cd ~/IdeaProjects/untitled && sleep 1 && clear && java -cp out CodeTree"
    set bounds of front window to {0, 25, item 3 of screenBounds, item 4 of screenBounds}
  end tell'
  ```
- 끝나지 않는 프로그램이다 (Ctrl+C로 종료). 자동으로 확인할 때는 백그라운드로 돌리고 몇 초 뒤 kill 한다.
- 확인용으로 돌릴 때는 `-Djavax.sound.midi.Sequencer=#none`을 붙여 음악을 끈다. 안 그러면 사용자 스피커로 징글벨이 나온다.

## CodeTree 구조

- 화면 배치(트리 높이·폭, 기둥, 선물 위치)는 모두 `setupLayout()`에서 터미널 크기로 계산한다. 위치를 상수로 박지 않는다.
- 잎 위 장식은 `overlay` / `overlayKind` / `overlayColor` 배열에 칸 단위로 기록하고, `renderCell()`이 그 종류를 보고 그린다 (S: 전구 줄, L: 꼬마전구, O: 오너먼트 볼, 0: 잎 글자).
- 눈은 `isOccupied()`로 별·트리·기둥·선물·바닥 칸을 피한다. 새 요소를 추가하면 여기에도 반영해야 눈이 그 위를 지우지 않는다.
- 색은 256색 ANSI 번호를 쓴다 (`bg(n)`, `\033[38;5;nm`).
- 유니코드 기호(❄, ● 등)는 터미널에 따라 두 칸으로 그려져 줄이 어긋날 수 있어 피한다. 별(`★`)도 픽셀아트(배경색 칸)로 바꿨다.

## MusicBox 구조

- 칸마다 광선을 쏴서(레이마칭) 그린다. 물체 모양은 `map()`에 거리 함수로 추가하고 `add(거리, 재질)`로 등록, 색은 `shade()`에서 재질 번호로 정한다.
- 회전판 위 물건은 `map()` 안의 회전 좌표(`lx`, `lz`)로 배치해야 같이 돈다.
- 유리 돔은 `map()`에 없고 `glass()`에서 따로 계산한다 (투명해서 광선을 막지 않음).
- 색은 0~1 RGB로 계산한 뒤 `toAnsi()`가 디더링해서 가장 가까운 256색 번호로 바꾼다. 어두운 파랑은 회색으로 잘못 매칭되기 쉬우니 파랑 성분을 충분히 준다.

## 코드 스타일

- 주석은 한국어로, 사용자가 읽고 값을 바꿔 볼 수 있게 짧게 쓴다.
- 사용자가 자주 바꾸는 값(간격, 색, 모양, 개수)은 클래스 상단 상수로 둔다.
