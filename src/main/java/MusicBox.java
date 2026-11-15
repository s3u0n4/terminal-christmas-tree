import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.util.Random;

// 터미널에 3D 크리스마스 오르골을 그림 (광선 추적: 칸마다 광선을 쏴서 물체를 찾고 빛을 계산)
public class MusicBox {
    private static final String RESET = "\033[0m";

    // 터미널 한 칸은 세로로 길쭉해서 가로 폭을 세로의 절반으로 보고 계산
    private static final double CELL_ASPECT = 0.5;
    private static final int FRAME_MS = 50;              // 한 프레임 최소 시간

    // 카메라: 오르골과의 거리, 눈높이, 좌우로 천천히 오가는 폭(라디안)과 속도, 확대 배율
    private static final double CAM_DIST = 4.3;
    private static final double CAM_HEIGHT = 1.0;
    private static final double CAM_SWING = 0.55;
    private static final double CAM_SWING_SPEED = 0.2;
    private static final double ZOOM = 2.8;

    // 회전판과 태엽 손잡이가 도는 속도 (라디안/초)
    private static final double SPIN_SPEED = 0.6;
    private static final double CRANK_SPEED = 2.2;

    // 빛이 오는 방향 (왼쪽 위 앞), 그늘을 살짝 밝히는 보조광 (오른쪽)
    private static final double[] LIGHT = normalize(-0.6, 0.9, 0.7);
    private static final double[] FILL = normalize(0.9, 0.2, 0.3);
    private static final double FILL_STRENGTH = 0.3;

    // 재질 색 (0~1 RGB)
    private static final double[] LACQUER = {0.42, 0.02, 0.01};  // 받침 상자 빨간 칠
    private static final double[] GOLD = {0.95, 0.66, 0.22};
    private static final double[] PINE = {0.03, 0.28, 0.09};
    private static final double[] TRUNK = {0.30, 0.15, 0.06};
    private static final double[] SNOW = {0.92, 0.94, 1.00};
    private static final double[] HAT = {0.04, 0.04, 0.06};
    private static final double[] CARROT = {1.00, 0.40, 0.05};
    private static final double[] SCARF = {0.85, 0.05, 0.08};
    private static final double[] KNOB = {0.95, 0.90, 0.78};     // 손잡이 끝 상아색
    private static final double[] GROUND = {0.15, 0.25, 0.70};   // 눈 덮인 바닥
    private static final double[] SKY_TOP = {0.00, 0.00, 0.05};
    private static final double[] SKY_BOTTOM = {0.00, 0.03, 0.20};
    // 트리 오너먼트 볼 색 (빨강, 금색, 파랑, 은색, 핑크)
    private static final double[][] ORNAMENT_COLORS = {
            {1.0, 0.05, 0.05}, {1.0, 0.75, 0.10}, {0.10, 0.30, 1.0}, {0.85, 0.85, 0.90}, {1.0, 0.30, 0.60},
    };
    // 선물 상자: {중심 x, y, z, 반폭 x, y, z} (회전판 위 좌표), 상자색, 리본색
    private static final double[][] GIFTS = {
            {-0.50, 0.08, 0.36, 0.09, 0.08, 0.09},
            {-0.12, 0.06, -0.60, 0.11, 0.06, 0.07},
            {0.36, 0.05, -0.50, 0.07, 0.05, 0.07},
    };
    private static final double[][] GIFT_BOX_COLORS = {{0.80, 0.03, 0.05}, {0.08, 0.15, 0.70}, {0.05, 0.50, 0.15}};
    private static final double[][] GIFT_RIBBON_COLORS = {{1.0, 0.75, 0.15}, {0.95, 0.95, 1.0}, {0.90, 0.05, 0.05}};

    // 오르골 모양 (받침 상자 반폭 1.1 기준)
    private static final double BASE_Y = -0.45, BASE_HX = 1.1, BASE_HY = 0.3, BASE_HZ = 1.05;
    private static final double FLOOR_Y = BASE_Y - BASE_HY - 0.075;
    private static final double TRIM = 0.05;                     // 받침 상자 위아래 금테 두께
    private static final double DOME_Y = -0.09, DOME_R = 0.98;    // 유리 돔 중심 높이, 반지름
    private static final double PLATE_R = 0.85;                    // 회전판 반지름
    // 트리 층: {바닥 높이, 높이, 바닥 반지름}
    private static final double[][] TIERS = {{0.08, 0.36, 0.40}, {0.29, 0.31, 0.32}, {0.47, 0.29, 0.23}};
    private static final double STAR_Y = 0.80, STAR_SIZE = 0.085;
    private static final double SNOWMAN_X = 0.56, SNOWMAN_Z = 0.14;
    private static final double CRANK_ARM = 0.22;

    // 유리 돔 안 눈송이 수, 바깥 눈송이는 몇 칸에 하나 (하늘부터 바닥 맨 아래 줄까지 내림)
    private static final int GLOBE_SNOW = 80;
    private static final int SKY_SNOW_CELLS = 70;
    private static final String MESSAGE = "Merry Christmas!";

    // 재질 번호
    private static final int M_LACQUER = 1, M_GOLD = 2, M_PINE = 3, M_TRUNK = 4, M_STAR = 5, M_SNOW = 6,
            M_HAT = 7, M_CARROT = 8, M_SCARF = 9, M_KNOB = 10, M_GIFT = 20, M_RIBBON = 30;

    // 음악: 고요한 밤 (음 높이, 길이 — 1 = 8분음표, 3/4박자)
    private static final int C4 = 60, D4 = 62, E4 = 64, F4 = 65, G4 = 67, A4 = 69, B4 = 71, C5 = 72, D5 = 74, E5 = 76, F5 = 77;
    private static final int[][] MELODY = {
            {G4, 3}, {A4, 1}, {G4, 2}, {E4, 6}, {G4, 3}, {A4, 1}, {G4, 2}, {E4, 6},
            {D5, 4}, {D5, 2}, {B4, 6}, {C5, 4}, {C5, 2}, {G4, 6},
            {A4, 4}, {A4, 2}, {C5, 3}, {B4, 1}, {A4, 2}, {G4, 3}, {A4, 1}, {G4, 2}, {E4, 6},
            {A4, 4}, {A4, 2}, {C5, 3}, {B4, 1}, {A4, 2}, {G4, 3}, {A4, 1}, {G4, 2}, {E4, 6},
            {D5, 4}, {D5, 2}, {F5, 3}, {D5, 1}, {B4, 2}, {C5, 6}, {E5, 6},
            {C5, 3}, {G4, 1}, {E4, 2}, {G4, 3}, {F4, 1}, {D4, 2}, {C4, 12},
    };
    // 마디마다 반주 화음 {베이스, 위 두 음}
    private static final int[] CHORD_C = {48, 55, 64}, CHORD_F = {41, 57, 60}, CHORD_G = {43, 55, 62};
    private static final int[][] CHORDS = {
            CHORD_C, CHORD_C, CHORD_C, CHORD_C, CHORD_G, CHORD_G, CHORD_C, CHORD_C,
            CHORD_F, CHORD_F, CHORD_C, CHORD_C, CHORD_F, CHORD_F, CHORD_C, CHORD_C,
            CHORD_G, CHORD_G, CHORD_C, CHORD_C, CHORD_C, CHORD_G, CHORD_C, CHORD_C,
    };
    private static final int MUSIC_BOX = 10;             // General MIDI 오르골 소리
    private static final int OCTAVE_UP = 12;             // 오르골답게 한 옥타브 높게
    private static Sequencer sequencer;

    // 4x4 디더링 표: 256색으로 부드러운 명암을 흉내 냄
    private static final int[][] BAYER = {{0, 8, 2, 10}, {12, 4, 14, 6}, {3, 11, 1, 9}, {15, 7, 13, 5}};
    private static final double DITHER = 0.03;
    private static final int[] PALETTE_LUT = new int[64 * 64 * 64];

    private static final Random random = new Random();
    private static int rows, cols;
    private static double unit;                          // 화면 좌표 1이 몇 줄인지
    private static double time, spinCos, spinSin, crankCos, crankSin;
    private static double camX, camY, camZ, fwdX, fwdY, fwdZ, rightX, rightY, rightZ, upX, upY, upZ;
    private static double dist;                          // map() 결과: 가장 가까운 물체까지 거리
    private static int mat;                              // map() 결과: 그 물체의 재질
    private static final double[] rgb = new double[3];   // shade() 결과

    private static int[] cellColor;
    private static char[] cellChar;
    private static int[] cellFg;
    private static double[] cellDepth;
    private static boolean[] cellOpen;                   // 오르골이 없는 칸 (하늘, 바닥): 바깥 눈이 내리는 곳
    private static double[][] globeSnow;                 // {x, y, z, 떨어지는 속도, 흔들림 위상}
    private static int[][] skySnow;                      // {줄, 칸, 글자}

    public static void main(String[] args) throws InterruptedException {
        // 인자로 "줄 칸"을 주면 그 크기로, 아니면 현재 터미널 크기로
        int[] size = args.length == 2
                ? new int[]{Integer.parseInt(args[0]), Integer.parseInt(args[1])}
                : terminalSize();
        rows = size[0];
        cols = size[1];
        unit = Math.min(rows / 2.0, cols * CELL_ASPECT / 2.0 / 1.3);
        cellColor = new int[rows * cols];
        cellChar = new char[rows * cols];
        cellFg = new int[rows * cols];
        cellDepth = new double[rows * cols];
        cellOpen = new boolean[rows * cols];
        buildPalette();
        initSnow();

        Runtime.getRuntime().addShutdownHook(new Thread(() ->
                System.out.print(RESET + "\033[?25h" + moveTo(rows, 1) + "\n")));

        startMusic();
        print("\033[2J\033[?25l"); // 화면 지우기 + 커서 숨기기

        long start = System.nanoTime();
        double last = 0;
        while (true) {
            long frameStart = System.nanoTime();
            time = (frameStart - start) / 1e9;
            double spin = time * SPIN_SPEED, crank = time * CRANK_SPEED;
            spinCos = Math.cos(spin);
            spinSin = Math.sin(spin);
            crankCos = Math.cos(crank);
            crankSin = Math.sin(crank);
            setupCamera();
            updateSnow(time - last);
            last = time;

            render();
            print(frameToString());

            long spent = (System.nanoTime() - frameStart) / 1_000_000;
            Thread.sleep(Math.max(1, FRAME_MS - spent));
        }
    }

    // 카메라가 오르골 앞에서 좌우로 천천히 오가며 내려다봄
    private static void setupCamera() {
        double angle = CAM_SWING * Math.sin(time * CAM_SWING_SPEED);
        camX = CAM_DIST * Math.sin(angle);
        camY = CAM_HEIGHT;
        camZ = CAM_DIST * Math.cos(angle);
        double[] f = normalize(0.1 - camX, -0.08 - camY, -camZ);
        fwdX = f[0];
        fwdY = f[1];
        fwdZ = f[2];
        double[] r = normalize(-fwdZ, 0, fwdX);
        rightX = r[0];
        rightY = r[1];
        rightZ = r[2];
        upX = rightY * fwdZ - rightZ * fwdY;
        upY = rightZ * fwdX - rightX * fwdZ;
        upZ = rightX * fwdY - rightY * fwdX;
    }

    // ───── 장면 모양: 점에서 가장 가까운 물체까지 거리 ─────

    private static double map(double x, double y, double z) {
        dist = 1e9;
        // 받침 상자 + 아래 받침대 + 유리 돔 테두리 + 회전판
        add(roundBox(x, y - BASE_Y, z, BASE_HX, BASE_HY, BASE_HZ, 0.05), M_LACQUER);
        add(roundBox(x, y - (BASE_Y - BASE_HY - 0.03), z, BASE_HX + 0.07, 0.045, BASE_HZ + 0.07, 0.02), M_GOLD);
        add(torus(x, y - DOME_Y, z, DOME_R, 0.035), M_GOLD);
        add(cylinderY(x, y + 0.04, z, PLATE_R, 0.04), M_GOLD);

        // 회전판 위 물건들 (회전판과 함께 돌아가는 좌표)
        if (length(x, y - 0.4, z) - 0.95 < dist) {
            double lx = spinCos * x + spinSin * z;
            double lz = -spinSin * x + spinCos * z;
            // 트리: 기둥 + 원뿔 3층 + 별
            add(cylinderY(lx, y - 0.05, lz, 0.06, 0.05), M_TRUNK);
            for (double[] tier : TIERS) {
                add(cone(lx, y - tier[0], lz, tier[2], tier[1]), M_PINE);
            }
            add(octahedron(lx, y - STAR_Y, lz, STAR_SIZE), M_STAR);

            // 눈사람: 몸, 머리, 목도리, 모자, 당근 코
            double sx = lx - SNOWMAN_X, sz = lz - SNOWMAN_Z;
            add(sphere(sx, y - 0.12, sz, 0.13), M_SNOW);
            add(sphere(sx, y - 0.30, sz, 0.09), M_SNOW);
            add(torus(sx, y - 0.225, sz, 0.075, 0.025), M_SCARF);
            add(cylinderY(sx, y - 0.39, sz, 0.085, 0.008), M_HAT);
            add(cylinderY(sx, y - 0.44, sz, 0.055, 0.05), M_HAT);
            double[] out = noseDir();
            add(capsule(sx, y - 0.30, sz, out[0] * 0.07, 0, out[2] * 0.07, out[0] * 0.15, -0.01, out[2] * 0.15, 0.018), M_CARROT);

            // 선물 상자 + 위에 리본 매듭
            for (int i = 0; i < GIFTS.length; i++) {
                double[] g = GIFTS[i];
                add(roundBox(lx - g[0], y - g[1], lz - g[2], g[3], g[4], g[5], 0.01), M_GIFT + i);
                add(sphere(lx - g[0], y - g[1] - g[4] - 0.015, lz - g[2], 0.03), M_RIBBON + i);
            }
        }

        // 오른쪽 옆면 태엽 손잡이: 축 → 팔 → 상아색 손잡이
        if (length(x - BASE_HX - 0.25, y - BASE_Y, z) - 0.35 < dist) {
            double ex = BASE_HX + 0.2;
            double ay = BASE_Y + CRANK_ARM * crankCos, az = CRANK_ARM * crankSin;
            add(capsule(x, y, z, BASE_HX, BASE_Y, 0, ex, BASE_Y, 0, 0.03), M_GOLD);
            add(capsule(x, y, z, ex, BASE_Y, 0, ex, ay, az, 0.025), M_GOLD);
            add(capsule(x, y, z, ex, ay, az, ex + 0.14, ay, az, 0.04), M_KNOB);
        }
        return dist;
    }

    private static void add(double d, int material) {
        if (d < dist) {
            dist = d;
            mat = material;
        }
    }

    // 눈사람이 바라보는 방향 (트리 바깥쪽)
    private static double[] noseDir() {
        return normalize(SNOWMAN_X, 0, SNOWMAN_Z);
    }

    private static double roundBox(double x, double y, double z, double bx, double by, double bz, double r) {
        double qx = Math.abs(x) - bx + r, qy = Math.abs(y) - by + r, qz = Math.abs(z) - bz + r;
        return length(Math.max(qx, 0), Math.max(qy, 0), Math.max(qz, 0))
                + Math.min(Math.max(qx, Math.max(qy, qz)), 0) - r;
    }

    private static double cylinderY(double x, double y, double z, double r, double halfHeight) {
        double dx = Math.sqrt(x * x + z * z) - r, dy = Math.abs(y) - halfHeight;
        return Math.min(Math.max(dx, dy), 0) + Math.sqrt(Math.max(dx, 0) * Math.max(dx, 0) + Math.max(dy, 0) * Math.max(dy, 0));
    }

    // 바닥 반지름 r, 높이 h인 원뿔 (바닥이 y=0)
    private static double cone(double x, double y, double z, double r, double h) {
        double q = Math.sqrt(x * x + z * z);
        return Math.max((q * h + r * y - r * h) / Math.sqrt(h * h + r * r), -y);
    }

    private static double torus(double x, double y, double z, double ring, double tube) {
        double q = Math.sqrt(x * x + z * z) - ring;
        return Math.sqrt(q * q + y * y) - tube;
    }

    private static double sphere(double x, double y, double z, double r) {
        return length(x, y, z) - r;
    }

    private static double octahedron(double x, double y, double z, double s) {
        return (Math.abs(x) + Math.abs(y) * 0.7 + Math.abs(z) - s) * 0.57735;
    }

    private static double capsule(double x, double y, double z, double ax, double ay, double az,
                                  double bx, double by, double bz, double r) {
        double px = x - ax, py = y - ay, pz = z - az, dx = bx - ax, dy = by - ay, dz = bz - az;
        double h = clamp((px * dx + py * dy + pz * dz) / (dx * dx + dy * dy + dz * dz), 0, 1);
        return length(px - dx * h, py - dy * h, pz - dz * h) - r;
    }

    // ───── 광선 추적 ─────

    // 광선이 처음 닿는 거리 (안 닿으면 -1)
    private static double march(double ox, double oy, double oz, double dx, double dy, double dz, double t, double tMax) {
        for (int i = 0; i < 110 && t < tMax; i++) {
            double d = map(ox + dx * t, oy + dy * t, oz + dz * t);
            if (d < 0.0012 * t) {
                return t;
            }
            t += d * 0.9;
        }
        return -1;
    }

    // 빛 쪽으로 가다가 막히면 그림자 (가장자리는 부드럽게)
    private static double shadow(double px, double py, double pz) {
        double result = 1, t = 0.02;
        for (int i = 0; i < 40 && t < 3; i++) {
            double h = map(px + LIGHT[0] * t, py + LIGHT[1] * t, pz + LIGHT[2] * t);
            if (h < 0.001) {
                return 0;
            }
            result = Math.min(result, 10 * h / t);
            t += clamp(h, 0.02, 0.2);
        }
        return clamp(result, 0, 1);
    }

    private static void render() {
        // 장면 전체를 감싸는 구: 이 밖으로 지나가는 광선은 바로 바닥이나 하늘
        double bx = 0.15, by = -0.2, br = 1.75;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                double su = (col + 0.5 - cols / 2.0) * CELL_ASPECT / unit;
                double sv = (rows / 2.0 - row - 0.5) / unit;
                double[] d = normalize(fwdX * ZOOM + rightX * su + upX * sv,
                        fwdY * ZOOM + rightY * su + upY * sv,
                        fwdZ * ZOOM + rightZ * su + upZ * sv);
                double dx = d[0], dy = d[1], dz = d[2];

                double hit = -1;
                double ox = camX - bx, oy = camY - by, oz = camZ;
                double b = ox * dx + oy * dy + oz * dz, c = ox * ox + oy * oy + oz * oz - br * br;
                if (b * b - c > 0) {
                    double s = Math.sqrt(b * b - c);
                    hit = march(camX, camY, camZ, dx, dy, dz, Math.max(0, -b - s), -b + s);
                }

                int idx = row * cols + col;
                cellChar[idx] = ' ';
                cellOpen[idx] = false;
                double depth = Double.MAX_VALUE;
                if (hit > 0) {
                    double px = camX + dx * hit, py = camY + dy * hit, pz = camZ + dz * hit;
                    map(px, py, pz);
                    int m = mat;
                    double[] n = normal(px, py, pz);
                    shade(px, py, pz, n[0], n[1], n[2], dx, dy, dz, m);
                    depth = hit;
                } else if (dy < 0 && (FLOOR_Y - camY) / dy > 0) {
                    depth = (FLOOR_Y - camY) / dy;
                    shadeGround(camX + dx * depth, camZ + dz * depth, depth, sv);
                    cellOpen[idx] = true;
                } else {
                    sky(sv);
                    cellOpen[idx] = true;
                }
                cellDepth[idx] = depth;
                glass(dx, dy, dz, depth);
                starGlow(dx, dy, dz, depth);

                double th = (BAYER[row & 3][col & 3] + 0.5) / 16 - 0.5;
                cellColor[idx] = toAnsi(Math.sqrt(rgb[0]) + th * DITHER, Math.sqrt(rgb[1]) + th * DITHER,
                        Math.sqrt(rgb[2]) + th * DITHER);
            }
        }
        drawSnow();
        int msgRow = rows - 2, msgCol = (cols - MESSAGE.length()) / 2;
        for (int i = 0; i < MESSAGE.length() && msgRow >= 0; i++) {
            int idx = msgRow * cols + msgCol + i;
            cellChar[idx] = MESSAGE.charAt(i);
            cellFg[idx] = 220;
        }
    }

    private static double[] normal(double x, double y, double z) {
        double e = 0.0008;
        double a = map(x + e, y - e, z - e), b = map(x - e, y - e, z + e);
        double c = map(x - e, y + e, z - e), d = map(x + e, y + e, z + e);
        return normalize(a - b - c + d, -a - b + c + d, -a + b - c + d);
    }

    // 재질별 색 + 빛 계산 → rgb
    private static void shade(double px, double py, double pz, double nx, double ny, double nz,
                              double dx, double dy, double dz, int m) {
        double[] albedo = SNOW;
        double spec = 0.15, shininess = 16, er = 0, eg = 0, eb = 0;
        boolean metal = false;
        double lx = spinCos * px + spinSin * pz, lz = -spinSin * px + spinCos * pz;

        if (m == M_LACQUER) {
            albedo = LACQUER;
            spec = 0.35;
            shininess = 90;
            if (lacquerPattern(px, py, pz)) {
                albedo = GOLD;
                metal = true;
            }
        } else if (m == M_GOLD) {
            albedo = GOLD;
            metal = true;
        } else if (m == M_KNOB) {
            albedo = KNOB;
            spec = 0.5;
            shininess = 30;
        } else if (m == M_TRUNK) {
            albedo = TRUNK;
        } else if (m == M_STAR) {
            albedo = GOLD;
            double pulse = 0.8 + 0.2 * Math.sin(time * 3);
            er = 1.0 * pulse;
            eg = 0.8 * pulse;
            eb = 0.25 * pulse;
        } else if (m == M_SNOW) {
            if (isEye(lx - SNOWMAN_X, py - 0.30, lz - SNOWMAN_Z)) {
                albedo = HAT;
            }
        } else if (m == M_HAT) {
            albedo = HAT;
            spec = 0.3;
        } else if (m == M_CARROT) {
            albedo = CARROT;
        } else if (m == M_SCARF) {
            albedo = SCARF;
        } else if (m >= M_RIBBON) {
            albedo = GIFT_RIBBON_COLORS[m - M_RIBBON];
            spec = 0.6;
        } else if (m >= M_GIFT) {
            double[] g = GIFTS[m - M_GIFT];
            boolean ribbon = Math.abs(lx - g[0]) < 0.018 || Math.abs(lz - g[2]) < 0.018;
            albedo = ribbon ? GIFT_RIBBON_COLORS[m - M_GIFT] : GIFT_BOX_COLORS[m - M_GIFT];
            spec = ribbon ? 0.6 : 0.25;
        } else if (m == M_PINE) {
            // 나선으로 감긴 금색 줄, 그 사이에 오너먼트 볼과 깜빡이는 꼬마전구
            double around = Math.atan2(lz, lx) / (2 * Math.PI) + 0.5;
            int row = (int) Math.floor(py / 0.07);
            double u = around * 12 + row * 0.5;
            double fu = u - Math.floor(u) - 0.5, fv = py / 0.07 - row - 0.5;
            int hash = Math.abs(row * 73856093 ^ (int) Math.floor(u) * 19349663);
            if (frac(around + py * 3.0) < 0.05) {
                albedo = GOLD;
                metal = true;
            } else if (fu * fu * 0.6 + fv * fv < 0.06) {
                double[] color = ORNAMENT_COLORS[hash % ORNAMENT_COLORS.length];
                albedo = color;
                spec = 1.0;
                shininess = 60;
                if (hash % 3 == 0 && Math.sin(time * 4 + hash % 100) > 0) {
                    er = color[0];
                    eg = color[1];
                    eb = color[2];
                }
            } else {
                double needle = 0.75 + 0.25 * Math.sin(around * 230) * Math.sin(py * 90);
                rgb[0] = PINE[0] * needle;
                rgb[1] = PINE[1] * needle;
                rgb[2] = PINE[2] * needle;
                albedo = rgb.clone();
            }
        }

        double diffuse = Math.max(0, nx * LIGHT[0] + ny * LIGHT[1] + nz * LIGHT[2]);
        double sh = diffuse > 0 ? shadow(px + nx * 0.01, py + ny * 0.01, pz + nz * 0.01) : 0;
        double ambient = 0.22 + 0.10 * ny + FILL_STRENGTH * Math.max(0, nx * FILL[0] + ny * FILL[1] + nz * FILL[2]);
        // 별에서 나오는 따뜻한 빛
        double wx = -px, wy = STAR_Y - py, wz = -pz;
        double wl = length(wx, wy, wz);
        double warm = Math.max(0, (nx * wx + ny * wy + nz * wz) / wl) * 0.25 / (1 + wl * wl * 6);
        // 반사광
        double[] h = normalize(LIGHT[0] - dx, LIGHT[1] - dy, LIGHT[2] - dz);
        double highlight = Math.pow(Math.max(0, nx * h[0] + ny * h[1] + nz * h[2]), metal ? 30 : shininess)
                * (metal ? 1.0 : spec) * sh;
        // 금속은 주변(위쪽 하늘)을 비춤
        double env = 0;
        if (metal) {
            double rdotn = dx * nx + dy * ny + dz * nz;
            double ry = dy - 2 * rdotn * ny;
            env = 0.15 + 0.35 * Math.max(0, ry);
        }

        double[] light = {1.25, 1.15, 1.0}, sky = {0.40, 0.45, 0.75}, starLight = {1.0, 0.8, 0.45};
        for (int k = 0; k < 3; k++) {
            double base = albedo[k] * (ambient * sky[k] + diffuse * sh * light[k] + warm * starLight[k] + env);
            double specColor = metal ? albedo[k] : 1.0;
            rgb[k] = base + highlight * specColor + (k == 0 ? er : k == 1 ? eg : eb);
        }
    }

    // 받침 상자 옆면 위아래 금테 + 앞뒷면 가운데 눈꽃 메달
    private static boolean lacquerPattern(double x, double y, double z) {
        double v = y - BASE_Y;
        if (Math.abs(v) > BASE_HY - TRIM && y < BASE_Y + BASE_HY - 0.01) {
            return true;
        }
        if (Math.abs(z) > BASE_HZ - 0.02) {
            double r = Math.sqrt(x * x + v * v);
            if (r > 0.16 && r < 0.2) {
                return true;
            }
            return r < 0.14 && Math.abs(Math.sin(3 * Math.atan2(v, x))) * r < 0.02; // 눈꽃 가지 6개
        }
        return false;
    }

    // 눈사람 머리 위 두 눈 (코 방향 기준 양옆으로 조금 위)
    private static boolean isEye(double x, double y, double z) {
        double[] p = normalize(x, y, z);
        double[] o = noseDir();
        for (int side = -1; side <= 1; side += 2) {
            double[] eye = normalize(o[0] * 0.85 - o[2] * 0.4 * side, 0.35, o[2] * 0.85 + o[0] * 0.4 * side);
            if (p[0] * eye[0] + p[1] * eye[1] + p[2] * eye[2] > 0.975) {
                return true;
            }
        }
        return false;
    }

    // 눈 덮인 바닥 (그림자 + 오르골 주변은 별빛으로 따뜻하게 + 멀어질수록 어둠에 묻힘)
    private static void shadeGround(double x, double z, double depth, double sv) {
        double sh = 0.4 + 0.6 * shadow(x, FLOOR_Y + 0.001, z);
        double glow = 0.15 / (1 + (x * x + z * z) * 1.5);
        double fade = Math.exp(-Math.max(0, depth - 3.5) * 0.9);
        double[] warm = {1.0, 0.8, 0.5};
        sky(sv);
        for (int k = 0; k < 3; k++) {
            double c = GROUND[k] * (0.15 + 0.6 * sh) + warm[k] * glow;
            rgb[k] = c * fade + rgb[k] * (1 - fade);
        }
    }

    private static void sky(double sv) {
        double t = clamp(0.5 - sv * 0.4, 0, 1);
        for (int k = 0; k < 3; k++) {
            rgb[k] = SKY_TOP[k] * (1 - t) + SKY_BOTTOM[k] * t;
        }
    }

    // 유리 돔: 가장자리는 하얗게 비치고, 빛이 비친 자리에 반짝이는 하이라이트
    private static void glass(double dx, double dy, double dz, double depth) {
        double ox = camX, oy = camY - DOME_Y, oz = camZ;
        double b = ox * dx + oy * dy + oz * dz, c = ox * ox + oy * oy + oz * oz - DOME_R * DOME_R;
        if (b * b - c <= 0) {
            return;
        }
        double s = Math.sqrt(b * b - c);
        double[] hits = {-b - s, -b + s};
        for (int i = 0; i < 2; i++) {
            double t = hits[i];
            double hy = oy + dy * t;
            if (t <= 0 || t >= depth || hy < 0) {
                continue;
            }
            double nx = (ox + dx * t) / DOME_R, ny = hy / DOME_R, nz = (oz + dz * t) / DOME_R;
            double cos = Math.abs(nx * dx + ny * dy + nz * dz);
            double fresnel = Math.pow(1 - cos, 6);
            double strength = i == 0 ? 1.0 : 0.15;
            double rdotn = dx * nx + dy * ny + dz * nz;
            double rx = dx - 2 * rdotn * nx, ry = dy - 2 * rdotn * ny, rz = dz - 2 * rdotn * nz;
            double sparkle = Math.pow(Math.max(0, rx * LIGHT[0] + ry * LIGHT[1] + rz * LIGHT[2]), 80) * 2.0;
            double[] tint = {0.2, 0.35, 1.0};
            for (int k = 0; k < 3; k++) {
                rgb[k] = rgb[k] * (1 - fresnel * 0.3 * strength) + (tint[k] * fresnel * 0.6 + sparkle) * strength;
            }
        }
    }

    // 꼭대기 별 주위 은은한 빛 번짐
    private static void starGlow(double dx, double dy, double dz, double depth) {
        double wx = -camX, wy = STAR_Y - camY, wz = -camZ;
        double t = wx * dx + wy * dy + wz * dz;
        if (t <= 0 || t > depth + 0.05) {
            return;
        }
        double d2 = wx * wx + wy * wy + wz * wz - t * t;
        double glow = 0.35 * (0.8 + 0.2 * Math.sin(time * 3)) / (1 + d2 * 3000);
        rgb[0] += glow;
        rgb[1] += glow * 0.8;
        rgb[2] += glow * 0.4;
    }

    // ───── 눈 ─────

    private static void initSnow() {
        globeSnow = new double[GLOBE_SNOW][];
        for (int i = 0; i < GLOBE_SNOW; i++) {
            globeSnow[i] = newGlobeFlake(0.02 + random.nextDouble() * 0.75);
        }
        skySnow = new int[rows * cols / SKY_SNOW_CELLS][];
        for (int i = 0; i < skySnow.length; i++) {
            skySnow[i] = new int[]{random.nextInt(rows), random.nextInt(cols), random.nextInt(3)};
        }
    }

    // 돔 안 높이 y(바깥 좌표)에서 새 눈송이 (돔 밖으로 나가지 않게)
    private static double[] newGlobeFlake(double y) {
        double maxR = Math.sqrt(Math.max(0, 0.9 * 0.9 - (y - DOME_Y) * (y - DOME_Y))) * 0.95;
        double r = maxR * Math.sqrt(random.nextDouble()), a = random.nextDouble() * 2 * Math.PI;
        return new double[]{r * Math.cos(a), y, r * Math.sin(a), 0.04 + random.nextDouble() * 0.06, random.nextDouble() * 6.3};
    }

    private static int skyTick;

    private static void updateSnow(double dt) {
        for (int i = 0; i < GLOBE_SNOW; i++) {
            double[] f = globeSnow[i];
            f[1] -= f[3] * dt;
            f[0] += Math.sin(time * 1.3 + f[4]) * 0.04 * dt;
            f[2] += Math.cos(time * 1.1 + f[4]) * 0.04 * dt;
            if (f[1] < 0.01) {
                globeSnow[i] = newGlobeFlake(0.65 + random.nextDouble() * 0.15);
            }
        }
        // 바깥 눈은 2프레임에 한 줄씩 내려옴
        if (++skyTick % 2 == 0) {
            for (int[] f : skySnow) {
                f[0]++;
                f[1] += random.nextInt(3) - 1;
                if (f[0] >= rows) {
                    f[0] = 0;
                    f[1] = random.nextInt(cols);
                }
            }
        }
    }

    private static void drawSnow() {
        char[] skyChars = {'.', '*', '+'};
        for (int[] f : skySnow) {
            if (f[1] >= 0 && f[1] < cols && cellOpen[f[0] * cols + f[1]]) {
                cellChar[f[0] * cols + f[1]] = skyChars[f[2]];
                cellFg[f[0] * cols + f[1]] = f[2] == 0 ? 250 : 255;
            }
        }
        // 돔 안 눈송이를 화면에 투영 (물체 뒤에 있으면 안 그림)
        for (double[] f : globeSnow) {
            double wx = f[0] - camX, wy = f[1] - camY, wz = f[2] - camZ;
            double z = wx * fwdX + wy * fwdY + wz * fwdZ;
            double su = (wx * rightX + wy * rightY + wz * rightZ) / z * ZOOM;
            double sv = (wx * upX + wy * upY + wz * upZ) / z * ZOOM;
            int col = (int) Math.floor(su * unit / CELL_ASPECT + cols / 2.0);
            int row = (int) Math.floor(rows / 2.0 - sv * unit);
            if (row < 0 || row >= rows || col < 0 || col >= cols) {
                continue;
            }
            int idx = row * cols + col;
            if (length(wx, wy, wz) < cellDepth[idx]) {
                cellChar[idx] = z < CAM_DIST ? '*' : '.';
                cellFg[idx] = 231;
            }
        }
    }

    // ───── 출력 ─────

    // 256색 표에서 가장 가까운 색을 미리 찾아 둠 (RGB 각 64단계)
    private static void buildPalette() {
        int[][] palette = new int[240][];
        int[] levels = {0, 95, 135, 175, 215, 255};
        for (int i = 0; i < 216; i++) {
            palette[i] = new int[]{levels[i / 36], levels[i / 6 % 6], levels[i % 6]};
        }
        for (int i = 0; i < 24; i++) {
            palette[216 + i] = new int[]{8 + 10 * i, 8 + 10 * i, 8 + 10 * i};
        }
        for (int r = 0; r < 64; r++) {
            for (int g = 0; g < 64; g++) {
                for (int b = 0; b < 64; b++) {
                    int best = 0, bestDist = Integer.MAX_VALUE;
                    for (int i = 0; i < 240; i++) {
                        int dr = palette[i][0] - r * 255 / 63, dg = palette[i][1] - g * 255 / 63, db = palette[i][2] - b * 255 / 63;
                        int d = 2 * dr * dr + 4 * dg * dg + 3 * db * db;
                        if (d < bestDist) {
                            bestDist = d;
                            best = i;
                        }
                    }
                    PALETTE_LUT[(r * 64 + g) * 64 + b] = 16 + best;
                }
            }
        }
    }

    private static int toAnsi(double r, double g, double b) {
        int ri = (int) clamp(r * 63 + 0.5, 0, 63), gi = (int) clamp(g * 63 + 0.5, 0, 63), bi = (int) clamp(b * 63 + 0.5, 0, 63);
        return PALETTE_LUT[(ri * 64 + gi) * 64 + bi];
    }

    private static String frameToString() {
        StringBuilder sb = new StringBuilder(rows * cols * 14);
        for (int row = 0; row < rows; row++) {
            sb.append(moveTo(row + 1, 1));
            int lastBg = -1, lastFg = -1;
            for (int col = 0; col < cols; col++) {
                int idx = row * cols + col;
                if (cellColor[idx] != lastBg) {
                    lastBg = cellColor[idx];
                    sb.append("\033[48;5;").append(lastBg).append('m');
                }
                if (cellChar[idx] != ' ' && cellFg[idx] != lastFg) {
                    lastFg = cellFg[idx];
                    sb.append("\033[1;38;5;").append(lastFg).append('m');
                }
                sb.append(cellChar[idx]);
            }
            sb.append(RESET);
        }
        return sb.toString();
    }

    // ───── 음악 ─────

    private static void startMusic() {
        try {
            Sequence sequence = new Sequence(Sequence.PPQ, 2); // 1틱 = 8분음표
            Track track = sequence.createTrack();
            track.add(new MidiEvent(new ShortMessage(ShortMessage.PROGRAM_CHANGE, 0, MUSIC_BOX, 0), 0));
            track.add(new MidiEvent(new ShortMessage(ShortMessage.PROGRAM_CHANGE, 1, MUSIC_BOX, 0), 0));

            long tick = 0;
            for (int[] note : MELODY) {
                addNote(track, 0, note[0] + OCTAVE_UP, 100, tick, note[1]);
                tick += note[1];
            }
            // 한 마디(8분음표 6개)마다 베이스 한 번, 위 두 음을 2·3박에
            for (int bar = 0; bar < CHORDS.length; bar++) {
                long barStart = bar * 6L;
                addNote(track, 1, CHORDS[bar][0] + OCTAVE_UP, 55, barStart, 2);
                for (int beat = 1; beat < 3; beat++) {
                    addNote(track, 1, CHORDS[bar][1] + OCTAVE_UP, 40, barStart + beat * 2, 2);
                    addNote(track, 1, CHORDS[bar][2] + OCTAVE_UP, 40, barStart + beat * 2, 2);
                }
            }

            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequencer.setSequence(sequence);
            sequencer.setTempoInBPM(84);
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

    // ───── 도구 ─────

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

    private static double[] normalize(double x, double y, double z) {
        double l = length(x, y, z);
        return new double[]{x / l, y / l, z / l};
    }

    private static double length(double x, double y, double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static double frac(double v) {
        return v - Math.floor(v);
    }

    private static String moveTo(int row, int col) {
        return "\033[" + row + ";" + col + "H";
    }

    private static void print(String s) {
        System.out.print(s);
        System.out.flush();
    }
}
