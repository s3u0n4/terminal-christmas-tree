import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CodeTree {
    // ANSI 색상 코드
    private static final String RESET = "\033[0m";
    private static final String BOLD = "\033[1m";
    private static final String LIGHT_GRAY = "\033[37m";
    private static final String PINK = "\033[38;5;205m";
    private static final String LIGHT_PINK = "\033[38;5;218m";
    private static final String BROWN = "\033[38;5;130m";
    private static final String YELLOW = "\033[93m";
    private static final String WHITE = "\033[97m";
    private static final String RED = "\033[91m";

    // 트리를 채울 코드 (공백은 빼고 한 글자씩 순서대로 쌓음)
    private static final String CODE = ("public class ChristmasTree { public static void main(String[] args) {"
            + " for (int i = 0; i < height; i++) { System.out.println(\"*\".repeat(i * 2 + 1)); }"
            + " if (santa.isComing()) { gifts.add(new Gift(\"red\")); gifts.add(new Gift(\"green\")); }"
            + " while (true) { tree.twinkle(); Thread.sleep(150); } } }").replaceAll("\\s+", "");

    // 화면 크기에 맞춰 setupLayout()에서 계산되는 배치 값
    private static int screenRows;
    private static int screenCols;
    private static final int TOP = 2;                  // 별이 그려지기 시작하는 줄
    // 꼭대기 별 픽셀아트 (Y: 노랑, W: 가운데 반짝이는 흰 점)
    private static final String[] STAR = {
            "....Y....",
            "...YYY...",
            "YYYYWYYYY",
            "..YYYYY..",
            ".YY...YY.",
    };
    private static final int LEAF_TOP = TOP + STAR.length; // 잎 첫 줄
    private static int center;                         // 트리 중심 열
    private static int leafRows;
    private static int[] leafHalfWidth;                // 잎 각 줄의 중심 기준 반폭
    private static int trunkTop;
    private static int trunkHeight;
    private static int trunkHalfWidth;
    private static int ground;
    private static String trunkCode;
    private static int charsPerStep;                   // 쌓기 애니메이션에서 한 번에 찍는 글자 수

    // 선물 6개 스타일: {폭 %, 높이 %, 상자색, 리본색, 무늬색, 무늬(0 없음, 1 물방울, 2 줄무늬)} — 색은 256색 번호
    // 기둥 왼쪽으로 0,2,4번, 오른쪽으로 1,3,5번이 안쪽부터 놓임
    private static final int[][] GIFT_STYLES = {
            {100, 100, 160, 34, 0, 0},     // 빨강 상자 + 초록 리본
            {80, 70, 220, 160, 0, 0},      // 금색 상자 + 빨강 리본
            {70, 55, 28, 196, 231, 1},     // 초록 상자 + 흰 물방울 + 빨강 리본
            {115, 85, 205, 231, 231, 1},   // 핑크 상자 + 흰 물방울 + 흰 리본
            {85, 80, 231, 34, 196, 2},     // 흰·빨강 줄무늬 + 초록 리본
            {60, 50, 196, 220, 231, 1},    // 빨강 상자 + 흰 물방울 + 금색 리본
    };
    private static int[][] gifts;                      // {왼쪽 열, 폭, 높이}

    private static final Random random = new Random();
    private static char[][] leaves;

    // 오너먼트: 핑크 줄을 따라 몇 칸마다 하나씩 매달지, 볼 색은 이 순서대로 돌아가며
    // (빨강, 금색, 파랑, 보라, 은색, 초록, 주황)
    private static final int ORNAMENT_SPACING = 6;
    private static final int[] ORNAMENT_COLORS = {196, 220, 33, 129, 250, 34, 208};
    private static final List<int[]> ornaments = new ArrayList<>(); // {잎 줄, 줄 안 위치, 칸 수}
    private static char[][] overlay;                   // 잎 위에 덮어 그릴 글자
    private static char[][] overlayKind;               // S: 전구 줄, L: 꼬마전구, O: 오너먼트 볼, 0이면 잎
    private static int[][] overlayColor;               // 오너먼트 볼 색
    private static final List<int[]> strandCells = new ArrayList<>(); // {잎 줄, 줄 안 위치}

    // 눈
    private static final char[] SNOW_CHARS = {'*', '.', '+'};
    private static int[][] snowflakes;

    // 음악: 징글벨 후렴 (음 높이, 길이 — 1 = 8분음표)
    private static final int C = 60, D = 62, E = 64, F = 65, G = 67;
    private static final int[][] MELODY = {
            {E, 2}, {E, 2}, {E, 4}, {E, 2}, {E, 2}, {E, 4}, {E, 2}, {G, 2}, {C, 2}, {D, 2}, {E, 8},
            {F, 2}, {F, 2}, {F, 2}, {F, 2}, {F, 2}, {E, 2}, {E, 2}, {E, 1}, {E, 1},
            {E, 2}, {D, 2}, {D, 2}, {E, 2}, {D, 4}, {G, 4},
            {E, 2}, {E, 2}, {E, 4}, {E, 2}, {E, 2}, {E, 4}, {E, 2}, {G, 2}, {C, 2}, {D, 2}, {E, 8},
            {F, 2}, {F, 2}, {F, 2}, {F, 2}, {F, 2}, {E, 2}, {E, 2}, {E, 1}, {E, 1},
            {G, 2}, {G, 2}, {F, 2}, {D, 2}, {C, 8},
    };
    // 마디마다 반주 화음 (C, F, D7, G)
    private static final int[] CHORD_C = {48, 52, 55}, CHORD_F = {48, 53, 57}, CHORD_D7 = {50, 54, 60}, CHORD_G = {47, 50, 55};
    private static final int[][] CHORDS = {
            CHORD_C, CHORD_C, CHORD_C, CHORD_C, CHORD_F, CHORD_C, CHORD_D7, CHORD_G,
            CHORD_C, CHORD_C, CHORD_C, CHORD_C, CHORD_F, CHORD_C, CHORD_G, CHORD_C,
    };
    private static final int GLOCKENSPIEL = 9, PIANO = 0, TAMBOURINE = 54, DRUM_CHANNEL = 9;
    private static Sequencer sequencer;

    public static void main(String[] args) throws InterruptedException {
        // 인자로 "줄 칸"을 주면 그 크기로, 아니면 현재 터미널 크기로
        int[] size = args.length == 2
                ? new int[]{Integer.parseInt(args[0]), Integer.parseInt(args[1])}
                : terminalSize();
        setupLayout(size[0], size[1]);

        // Ctrl+C로 종료해도 커서와 색이 원래대로 돌아오게 함
        Runtime.getRuntime().addShutdownHook(new Thread(() ->
                System.out.print(RESET + "\033[?25h" + moveTo(screenRows, 1) + "\n")));

        startMusic();
        print("\033[2J\033[?25l"); // 화면 지우기 + 커서 숨기기

        // 1단계: 갈색 기둥이 코드로 쌓임
        for (int i = trunkHeight - 1; i >= 0; i--) {
            typeAt(trunkTop + i, center - trunkHalfWidth, trunkCode, BROWN);
        }

        // 2단계: 하얀 잎이 아래 줄부터 위로 코드 한 글자씩 쌓임
        for (int row = leafRows - 1; row >= 0; row--) {
            typeAt(LEAF_TOP + row, center - leafHalfWidth[row], new String(leaves[row]), WHITE);
        }
        drawStar(true);
        sleep(400);

        // 3단계: 꼬마전구 달린 핑크 줄이 감김
        for (int[] cell : strandCells) {
            String look = overlayKind[cell[0]][cell[1]] == 'L' ? renderCell(cell[0], cell[1]) : BOLD + PINK + "~";
            print(moveTo(LEAF_TOP + cell[0], center - leafHalfWidth[cell[0]] + cell[1]) + look + RESET);
            sleep(8);
        }
        // 오너먼트 볼이 줄을 따라 하나씩 달림
        for (int[] o : ornaments) {
            StringBuilder sb = new StringBuilder(moveTo(LEAF_TOP + o[0], center - leafHalfWidth[o[0]] + o[1]));
            for (int k = 0; k < o[2]; k++) {
                sb.append(renderCell(o[0], o[1] + k));
            }
            print(sb.append(RESET).toString());
            sleep(40);
        }

        // 4단계: 크기와 색이 제각각인 선물이 하나씩 등장
        int leftmost = Math.min(gifts[gifts.length - 2][0], center - leafHalfWidth[leafRows - 1]);
        int groundLeft = Math.max(1, leftmost - 3);
        print(moveTo(ground, groundLeft) + WHITE + "~".repeat(2 * (center - groundLeft) + 1) + RESET);
        for (int i = 0; i < gifts.length; i++) {
            drawGift(i, true);
            sleep(350);
        }
        String message = "Merry Christmas!";
        print(moveTo(ground + 1, center - message.length() / 2 + 1) + BOLD + RED + message + RESET);

        // 5단계: Ctrl+C 누를 때까지 반짝반짝 + 눈 내리기
        initSnow();
        for (int frame = 0; ; frame++) {
            if (frame % 2 == 0) {
                twinkle(frame / 2);
            }
            updateSnow(frame);
            sleep(75);
        }
    }

    // 선물 크기를 스타일 비율대로 정하고, 기둥 양옆으로 3개씩 바깥으로 늘어놓음 (화면에 안 들어가면 기준 폭을 줄임)
    private static void setupGifts(int bottomHalf) {
        int maxHeight = Math.max(2, trunkHeight - 1); // 맨 위에 매듭 한 줄 남김
        int space = center - trunkHalfWidth - 3;      // 한쪽에 쓸 수 있는 칸 수
        for (int baseWidth = Math.max(9, bottomHalf / 3); ; baseWidth--) {
            gifts = new int[GIFT_STYLES.length][];
            int[] used = new int[2];
            for (int g = 0; g < GIFT_STYLES.length; g++) {
                int width = Math.max(5, baseWidth * GIFT_STYLES[g][0] / 100) | 1; // 홀수 폭 (가운데 세로 리본)
                int height = Math.max(2, (int) Math.round(maxHeight * GIFT_STYLES[g][1] / 100.0));
                int side = g % 2;
                int left = side == 0
                        ? center - trunkHalfWidth - 2 - used[0] - width
                        : center + trunkHalfWidth + 2 + used[1];
                used[side] += width + 2;
                gifts[g] = new int[]{left, width, height};
            }
            if (Math.max(used[0], used[1]) <= space || baseWidth <= 5) {
                return;
            }
        }
    }

    // stty로 현재 터미널의 줄·칸 수를 읽음 (실패하면 24x80)
    private static int[] terminalSize() {
        try {
            Process process = new ProcessBuilder("sh", "-c", "stty size < /dev/tty").start();
            String[] parts = new String(process.getInputStream().readAllBytes()).trim().split("\\s+");
            process.waitFor();
            return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
        } catch (Exception e) {
            return new int[]{24, 80};
        }
    }

    private static void setupLayout(int rows, int cols) {
        screenRows = rows;
        screenCols = cols;
        center = cols / 2;

        // 위에서부터: 여백, 별, 잎, 기둥(선물), 바닥, 문구, 여백
        trunkHeight = Math.max(4, rows / 7);
        leafRows = Math.max(6, rows - trunkHeight - 4 - STAR.length);
        trunkTop = LEAF_TOP + leafRows;
        ground = trunkTop + trunkHeight;

        // 글자는 세로로 길쭉해서 한 줄 내려갈 때 1.6칸씩 넓혀야 트리 모양이 예쁨 (화면이 좁으면 줄임)
        double slope = Math.min(1.6, (center - 2.0) / leafRows);
        // 여러 층으로 나눠서, 각 층은 윗층 끝보다 안쪽(60%)에서 시작해 바깥으로 퍼짐 → 들어가고 나온 가지 모양
        int tiers = Math.max(3, leafRows / 8);
        leafHalfWidth = new int[leafRows];
        double prevEnd = 0;
        for (int tier = 0; tier < tiers; tier++) {
            int from = leafRows * tier / tiers;
            int to = leafRows * (tier + 1) / tiers - 1;
            double start = prevEnd * 0.6;
            double end = to * slope;
            for (int row = from; row <= to; row++) {
                double p = to == from ? 1 : (double) (row - from) / (to - from);
                leafHalfWidth[row] = (int) Math.round(start + (end - start) * p);
            }
            prevEnd = end;
        }
        int bottomHalf = leafHalfWidth[leafRows - 1];

        trunkHalfWidth = Math.max(2, bottomHalf / 8);
        trunkCode = "{" + "|".repeat(trunkHalfWidth * 2 - 1) + "}";

        setupGifts(bottomHalf);

        fillLeavesWithCode();
        setupGarland();
        int totalChars = trunkCode.length() * trunkHeight;
        for (char[] row : leaves) {
            totalChars += row.length;
        }
        charsPerStep = Math.max(1, totalChars / 500); // 크기와 상관없이 약 5초 안에 다 쌓이도록
    }

    // 자바 내장 MIDI 신시사이저로 징글벨을 무한 반복 재생
    private static void startMusic() {
        try {
            Sequence sequence = new Sequence(Sequence.PPQ, 2); // 1틱 = 8분음표
            Track track = sequence.createTrack();
            track.add(new MidiEvent(new ShortMessage(ShortMessage.PROGRAM_CHANGE, 0, GLOCKENSPIEL, 0), 0));
            track.add(new MidiEvent(new ShortMessage(ShortMessage.PROGRAM_CHANGE, 1, PIANO, 0), 0));

            long tick = 0;
            for (int[] note : MELODY) {
                addNote(track, 0, note[0], 90, tick, note[1]);
                tick += note[1];
            }
            for (int bar = 0; bar < CHORDS.length; bar++) {
                for (int note : CHORDS[bar]) {
                    addNote(track, 1, note, 45, bar * 8L, 8);
                }
            }
            // 썰매 방울 느낌의 탬버린을 4분음표마다
            for (long t = 0; t < tick; t += 2) {
                addNote(track, DRUM_CHANNEL, TAMBOURINE, 35, t, 1);
            }

            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequencer.setSequence(sequence);
            sequencer.setTempoInBPM(160);
            sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            sequencer.start();
        } catch (Exception e) {
            // 소리 장치를 쓸 수 없으면 음악 없이 애니메이션만 진행
        }
    }

    private static void addNote(Track track, int channel, int note, int velocity, long start, long length)
            throws Exception {
        track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, channel, note, velocity), start));
        track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, channel, note, 0), start + length));
    }

    private static void initSnow() {
        snowflakes = new int[screenRows * screenCols / 60][];
        for (int i = 0; i < snowflakes.length; i++) {
            snowflakes[i] = newSnowflake(1 + random.nextInt(ground - 1));
        }
    }

    // {행, 열, 속도(1=빠름, 2=느림), 모양 인덱스}
    private static int[] newSnowflake(int row) {
        return new int[]{row, 1 + random.nextInt(screenCols), 1 + random.nextInt(2), random.nextInt(SNOW_CHARS.length)};
    }

    private static void updateSnow(int frame) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < snowflakes.length; i++) {
            int[] s = snowflakes[i];
            if (frame % s[2] != 0) {
                continue;
            }
            if (!isOccupied(s[0], s[1])) {
                sb.append(moveTo(s[0], s[1])).append(' '); // 이전 위치 지우기
            }

            int nextRow = s[0] + 1;
            int nextCol = Math.max(1, Math.min(screenCols, s[1] + random.nextInt(3) - 1)); // 살짝 흔들림
            if (isOccupied(nextRow, nextCol)) {
                // 트리·선물·바닥에 닿으면 맨 위에서 다시 시작
                s = snowflakes[i] = newSnowflake(1);
            } else {
                s[0] = nextRow;
                s[1] = nextCol;
            }
            if (!isOccupied(s[0], s[1])) {
                sb.append(moveTo(s[0], s[1])).append(WHITE).append(SNOW_CHARS[s[3]]).append(RESET);
            }
        }
        print(sb.toString());
    }

    // 별·트리·기둥·선물·바닥이 있는 칸이면 눈을 그리지 않음
    private static boolean isOccupied(int row, int col) {
        if (row >= ground) {
            return true;
        }
        if (row >= TOP && row < LEAF_TOP) {
            int c = col - (center - STAR[0].length() / 2);
            return c >= 0 && c < STAR[0].length() && STAR[row - TOP].charAt(c) != '.';
        }
        if (row >= LEAF_TOP && row < trunkTop) {
            return Math.abs(col - center) <= leafHalfWidth[row - LEAF_TOP];
        }
        if (row >= trunkTop) {
            if (Math.abs(col - center) <= trunkHalfWidth) {
                return true;
            }
            for (int[] gift : gifts) {
                int top = ground - gift[2];
                if (row >= top && col >= gift[0] && col < gift[0] + gift[1]) {
                    return true;
                }
                if (row == top - 1 && Math.abs(col - (gift[0] + gift[1] / 2)) <= 1) { // 매듭
                    return true;
                }
            }
        }
        return false;
    }

    private static void fillLeavesWithCode() {
        leaves = new char[leafRows][];
        int index = 0;
        for (int row = leafRows - 1; row >= 0; row--) {
            leaves[row] = new char[leafHalfWidth[row] * 2 + 1];
            for (int i = 0; i < leaves[row].length; i++) {
                leaves[row][i] = CODE.charAt(index++ % CODE.length());
            }
        }
    }

    // 트리를 대각선으로 감는 전구 줄 여러 가닥과, 줄에 매달린 꼬마전구·오너먼트 배치
    private static void setupGarland() {
        overlay = new char[leafRows][];
        overlayKind = new char[leafRows][];
        overlayColor = new int[leafRows][];
        for (int row = 0; row < leafRows; row++) {
            overlay[row] = new char[leaves[row].length];
            overlayKind[row] = new char[leaves[row].length];
            overlayColor[row] = new int[leaves[row].length];
        }

        int strands = Math.max(3, leafRows / 5);
        int drop = Math.max(2, leafRows / 8);
        int[] leftRows = new int[strands];
        int[] rightRows = new int[strands];
        for (int s = 0; s < strands; s++) {
            int mid = (int) (leafRows * (0.2 + 0.65 * s / (strands - 1)));
            // 가닥마다 기울기 방향을 바꿔서 트리를 지그재그로 감은 느낌
            leftRows[s] = s % 2 == 0 ? mid - drop / 2 : mid + drop / 2;
            rightRows[s] = s % 2 == 0 ? mid + drop / 2 : mid - drop / 2;

            // t(0=왼쪽 끝, 1=오른쪽 끝)를 따라가며 그 줄의 폭에 맞춰 칸을 찍으므로 항상 트리 끝에서 끝까지 이어짐
            int steps = Math.max(1, leafHalfWidth[mid] * 4);
            for (int k = 0; k <= steps; k++) {
                int[] cell = strandCell(leftRows[s], rightRows[s], drop, (double) k / steps);
                if (overlayKind[cell[0]][cell[1]] != 'S') {
                    overlay[cell[0]][cell[1]] = '~';
                    overlayKind[cell[0]][cell[1]] = 'S';
                    strandCells.add(cell);
                }
            }
        }
        // 줄을 다 감은 다음에 장식을 달아야 뒤 가닥이 앞 장식을 덮지 않음
        // 핑크 줄을 왼쪽부터 따라가며 ORNAMENT_SPACING 칸마다 매달 자리(줄 바로 아래)를 정함
        List<int[]> slots = new ArrayList<>(); // {매달 줄, 열}
        for (int s = 0; s < strands; s++) {
            int steps = Math.max(1, leafHalfWidth[leafRows - 1] * 4);
            int lastCol = Integer.MIN_VALUE / 2;
            for (int k = 0; k <= steps; k++) {
                int[] cell = strandCell(leftRows[s], rightRows[s], drop, (double) k / steps);
                int col = center - leafHalfWidth[cell[0]] + cell[1];
                if (col - lastCol >= ORNAMENT_SPACING) {
                    slots.add(new int[]{cell[0] + 1, col});
                    lastCol = col;
                }
            }
        }

        // 자리마다 오너먼트 볼을, 색은 정해진 순서대로 돌아가며
        int colorIndex = 0;
        for (int[] slot : slots) {
            int row = slot[0];
            if (row >= leafRows) {
                continue;
            }
            int i = slot[1] - (center - leafHalfWidth[row]);
            if (ornamentFits(row, i, 2)) {
                int color = ORNAMENT_COLORS[colorIndex++ % ORNAMENT_COLORS.length];
                overlay[row][i] = '\'';               // 왼쪽 위 반짝이는 하이라이트
                overlay[row][i + 1] = ' ';
                for (int c = i; c < i + 2; c++) {
                    overlayKind[row][c] = 'O';
                    overlayColor[row][c] = color;
                }
                ornaments.add(new int[]{row, i, 2});
            }
        }

        // 전구 줄 4칸마다 작은 꼬마전구 o (오너먼트가 덮은 칸은 건너뜀)
        for (int k = 0; k < strandCells.size(); k += 4) {
            int[] cell = strandCells.get(k);
            if (overlayKind[cell[0]][cell[1]] == 'S') {
                overlay[cell[0]][cell[1]] = 'o';
                overlayKind[cell[0]][cell[1]] = 'L';
            }
        }
    }

    // 놓을 칸이 비어 있고(전구 줄 위는 괜찮음), 바로 옆·위·아래에 다른 오너먼트가 없을 때만
    private static boolean ornamentFits(int row, int i, int size) {
        int col = center - leafHalfWidth[row] + i;
        for (int r = row - 1; r <= row + 1; r++) {
            if (r < 0 || r >= leafRows) {
                continue;
            }
            for (int c = col - 1; c <= col + size; c++) {
                int j = c - (center - leafHalfWidth[r]);
                boolean target = r == row && c >= col && c < col + size;
                if (j < 0 || j >= leaves[r].length) {
                    if (target) {
                        return false;
                    }
                    continue;
                }
                if (overlayKind[r][j] == 'O') {
                    return false;
                }
            }
        }
        return true;
    }

    // 전구 줄 위의 t 지점이 몇 번째 잎 줄, 그 줄의 몇 번째 칸인지 {줄, 칸}
    private static int[] strandCell(int leftRow, int rightRow, int drop, double t) {
        double y = leftRow + (rightRow - leftRow) * t + drop / 2.0 * Math.sin(Math.PI * t); // 가운데가 아래로 처짐
        int row = Math.max(0, Math.min(leafRows - 1, (int) Math.round(y)));
        return new int[]{row, (int) Math.round(leafHalfWidth[row] * 2 * t)};
    }

    // 잎 한 칸을 색과 함께 문자열로 (전구 줄·꼬마전구·오너먼트가 있으면 그걸 그림)
    private static String renderCell(int row, int i) {
        char kind = overlayKind[row][i];
        if (kind == 0) {
            int r = random.nextInt(100);
            String color = r < 5 ? BOLD + WHITE : r < 9 ? LIGHT_PINK : r % 2 == 0 ? WHITE : LIGHT_GRAY;
            return color + leaves[row][i];
        }
        if (kind == 'S') {
            return BOLD + PINK + overlay[row][i];
        }
        if (kind == 'L') {
            int r = random.nextInt(10);
            return (r < 3 ? BOLD + PINK : r < 5 ? BOLD + WHITE : r < 7 ? BOLD + LIGHT_PINK : "\033[38;5;132m") + 'o';
        }
        return bg(overlayColor[row][i]) + BOLD + WHITE + overlay[row][i]; // 오너먼트 볼
    }

    // 큰 별: 켜지면 밝은 노랑 + 가운데 흰 반짝임, 꺼지면 한 톤 어두운 금색
    private static void drawStar(boolean bright) {
        int left = center - STAR[0].length() / 2;
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < STAR.length; r++) {
            for (int c = 0; c < STAR[r].length(); c++) {
                char kind = STAR[r].charAt(c);
                if (kind == '.') {
                    continue;
                }
                int color = kind == 'W' ? (bright ? 231 : 228) : (bright ? 226 : 178);
                sb.append(moveTo(TOP + r, left + c)).append(bg(color)).append(' ').append(RESET);
            }
        }
        print(sb.toString());
    }

    private static String bg(int color256) {
        return "\033[48;5;" + color256 + "m";
    }

    // 타자 치듯 글자를 조금씩 출력
    private static void typeAt(int row, int col, String text, String color) throws InterruptedException {
        for (int i = 0; i < text.length(); i += charsPerStep) {
            String chunk = text.substring(i, Math.min(text.length(), i + charsPerStep));
            print(moveTo(row, col + i) + color + chunk + RESET);
            sleep(10);
        }
    }

    // 매듭 1줄 + 상자 (가운데 줄은 가로 리본, 가운데 칸은 세로 리본, 나머지는 무늬)
    private static void drawGift(int index, boolean bowOn) {
        int left = gifts[index][0];
        int width = gifts[index][1];
        int height = gifts[index][2];
        int[] style = GIFT_STYLES[index];
        int top = ground - height;

        String bow = (bowOn ? BOLD : "") + "\033[38;5;" + style[3] + "m";
        StringBuilder sb = new StringBuilder(moveTo(top - 1, left + width / 2 - 1) + bow + "\\o/" + RESET);
        for (int r = 0; r < height; r++) {
            sb.append(moveTo(top + r, left));
            for (int c = 0; c < width; c++) {
                if (c == width / 2 || r == height / 2) {
                    sb.append(bg(style[3])).append(' ');
                } else if (style[5] == 2) {
                    sb.append(bg((c / 2) % 2 == 0 ? style[2] : style[4])).append(' ');
                } else if (style[5] == 1 && r % 2 == 0 && c % 3 == 1) {
                    sb.append(bg(style[2])).append("\033[38;5;").append(style[4]).append("m*");
                } else {
                    sb.append(bg(style[2])).append(' ');
                }
            }
            sb.append(RESET);
        }
        print(sb.toString());
    }

    private static void twinkle(int frame) {
        // 꼬마전구는 저마다 켜졌다 꺼졌다, 하얀 잎은 은은하게 반짝
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < leafRows; row++) {
            sb.append(moveTo(LEAF_TOP + row, center - leafHalfWidth[row]));
            for (int i = 0; i < leaves[row].length; i++) {
                sb.append(renderCell(row, i)).append(RESET);
            }
        }
        print(sb.toString());

        drawStar(frame % 2 == 0);

        for (int i = 0; i < gifts.length; i++) {
            drawGift(i, (frame + i) % 4 < 2);
        }
    }

    private static String moveTo(int row, int col) {
        return "\033[" + row + ";" + col + "H";
    }

    private static void print(String s) {
        System.out.print(s);
        System.out.flush();
    }

    private static void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }
}
