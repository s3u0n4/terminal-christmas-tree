#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
터미널 3D 크리스마스트리 (글자 아트)

실행 방법
  1) numpy 설치 (처음 한 번만):  pip3 install numpy
  2) 실행:                      python3 tree.py
  3) 끝내기:                    Ctrl+C

터미널 권장
  - iTerm2 또는 VS Code 내장 터미널을 권해요 (24비트 트루컬러 지원).
  - 맥 기본 '터미널(Terminal.app)'은 트루컬러를 제대로 못 보여 줘서 색이 다르게 나올 수 있어요.
  - 실행할 때의 터미널 창 크기에 맞춰 화면을 가득 채워요. 창을 전체화면으로 키운 다음 실행하세요.
"""

import math
import shutil
import signal
import sys
import time

import numpy as np

# ══════════════════════════════════════════════════════════════════════
#  색 팔레트 (R, G, B — 0~255)
#  「하트를 더 진한 분홍으로」 같은 건 여기 숫자만 바꾸면 돼요.
# ══════════════════════════════════════════════════════════════════════
# 트리
DARK_GREEN    = (14, 86, 42)      # 트리 진한 초록
LIGHT_GREEN   = (64, 176, 72)     # 트리 밝은 초록
BRANCH_SNOW   = (228, 240, 255)   # 가지 끝에 살짝 묻은 눈
TRUNK_BROWN   = (118, 70, 34)     # 갈색 기둥
GROUND_SNOW   = (218, 230, 248)   # 눈 쌓인 둥근 땅

# 장식 볼·전구·선물에 같이 쓰는 기본색
RED           = (225, 32, 44)     # 빨강
GOLD          = (255, 192, 52)    # 금색
SILVER        = (200, 206, 222)   # 은색

# 전구
WARM_WHITE    = (255, 226, 172)   # 따뜻한 흰색 전구
BULB_GREEN    = (96, 255, 128)    # 초록 전구

# 꼭대기 별
STAR_GOLD     = (255, 204, 58)    # 금색 별
STAR_RAY      = (255, 236, 150)   # 별 빛살

# 하트
PINK          = (255, 108, 182)   # 분홍 하트

# 선물 상자
GIFT_WHITE    = (238, 238, 245)   # 흰 상자
GIFT_GREEN    = (40, 162, 72)     # 초록 상자
GIFT_BLUE     = (52, 112, 232)    # 파란 상자

# 빛 효과
SWIRL_WHITE   = (232, 242, 255)   # 감아 올라가는 회오리 빛
FLASH_WHITE   = (255, 255, 240)   # 꼭대기에서 번쩍 퍼지는 빛
NEW_GLOW      = (255, 255, 255)   # 방금 생긴 트리 부분이 잠깐 빛나는 색
SNOWFLAKE     = (236, 243, 255)   # 하늘에서 내리는 눈

# 우주 배경
SKY_STARS     = [(255, 255, 255),  # 흰 별
                 (190, 212, 255),  # 푸른 별
                 (255, 236, 192),  # 노란 별
                 (222, 202, 255)]  # 보랏빛 별
MILKY_WAY     = (122, 112, 196)   # 은하수 띠
PLANET_SILVER = (204, 204, 218)   # 왼쪽 위 은빛 행성
PLANET_RING   = (178, 172, 156)   # 은빛 행성의 고리
PLANET_BLUE   = (62, 124, 242)    # 오른쪽 위 푸른 행성
PLANET_PURPLE = (164, 104, 232)   # 왼쪽 중간 보랏빛 행성

# ══════════════════════════════════════════════════════════════════════
#  개수·모양 설정 — 「하트 더 많이」면 N_HEARTS 를 키우면 돼요.
# ══════════════════════════════════════════════════════════════════════
BASE_W, BASE_H = 96, 54       # 기준 화면 크기 — 모양·개수는 이 크기 기준이고, 실행하면 터미널 전체 크기에 맞춰 늘어나요
W, H = BASE_W, BASE_H         # 실제 화면 크기 (실행할 때 터미널 크기로 바뀜)
SC = 1.0                      # 기준 크기 대비 확대 배율 (실행할 때 계산)
AREA = 1.0                    # 기준 크기 대비 화면 넓이 비율 (별·눈 개수를 이만큼 늘림)
FPS = 30                      # 1초에 다시 그리는 횟수
ROT_PERIOD = 17.0             # 트리가 한 바퀴 도는 시간(초)

N_SKY_STARS = 500             # 배경 별 개수 (96x54 기준, 화면이 넓으면 그만큼 늘어남)
N_BALLS = 34                  # 장식 볼 개수
BALL_COLORS = [RED, GOLD, SILVER]                     # 볼 색 (번갈아)
N_LIGHTS = 170                # 전구 개수
LIGHT_TURNS = 5.5             # 전구 줄이 트리를 감는 바퀴 수
LIGHT_COLORS = [WARM_WHITE, RED, GOLD, BULB_GREEN]    # 전구 색 (번갈아)
N_HEARTS = 5                  # 트리에 달리는 하트 개수
N_SNOW = 120                  # 내리는 눈송이 개수 (96x54 기준, 처음부터 계속 내려요)
SNOW_HEART_RATIO = 0.08       # 눈송이 중 분홍 하트 비율

# 선물: (상자색, 리본색, 가로, 세로, 높이, 놓인 방향(도), 가운데에서 거리)
GIFTS = [
    (GIFT_WHITE, RED,       0.41, 0.36, 0.31,  15, 1.20),
    (RED,        GOLD,      0.34, 0.34, 0.40,  75, 1.14),
    (GIFT_GREEN, RED,       0.46, 0.36, 0.24, 135, 1.24),
    (GOLD,       RED,       0.29, 0.29, 0.26, 195, 1.16),
    (GIFT_BLUE,  SILVER,    0.38, 0.41, 0.36, 255, 1.20),
    (SILVER,     GIFT_BLUE, 0.31, 0.26, 0.20, 315, 1.26),
]

# ══════════════════════════════════════════════════════════════════════
#  생기는 순서 (초) — 한 바퀴 전체 13.14초, 끝나면 처음부터 다시
# ══════════════════════════════════════════════════════════════════════
TOTAL    = 13.14
T_GROUND = (0.0, 0.7)    # 1. 눈 쌓인 땅이 가운데부터 퍼짐
T_TREE   = (0.5, 3.5)    # 2. 회오리가 감아 올라가며 트리가 생김
T_FLASH  = (3.5, 4.2)    # 3. 꼭대기에서 8방향으로 번쩍
T_BALLS  = (4.0, 5.6)    # 4. 장식 볼이 아래부터 톡톡
T_LIGHTS = (5.5, 7.4)    # 5. 전구가 아래부터 켜짐
T_STAR   = (7.3, 8.0)    # 6. 꼭대기 별
T_HEARTS = (7.9, 8.7)    # 7. 하트
T_GIFTS  = (8.6, 9.7)    # 8. 선물 상자가 땅에서 쑥
T_CHASE  = 9.9           # 10. 전구 빛이 줄줄이 달려감 (끝까지)

# ══════════════════════════════════════════════════════════════════════
#  내부 설정 (모양·카메라)
# ══════════════════════════════════════════════════════════════════════
RAMP = " .,:;-=+*#%@"                 # 어두움 → 밝음
NR = len(RAMP) - 1
GLYPHS = list(RAMP) + ["♥"]
HEART_CODE = len(RAMP)

# 원뿔 4층: (아래 높이, 꼭대기 높이, 아래 반지름)
TIERS = [(0.50, 1.35, 1.18), (1.15, 1.90, 0.94), (1.70, 2.40, 0.70), (2.22, 2.90, 0.46)]
TREE_TOP = 2.90
HEAD_MAX = TREE_TOP + 0.05            # 회오리가 올라가는 끝 높이
STAR_Y = 3.02
TRUNK_R, TRUNK_TOP = 0.13, 0.62
GROUND_R = 1.5
BALL_R = 0.085
SWIRL_TURNS = 2.0                     # 회오리가 트리를 감는 바퀴 수

CAM_DIST = 8.0
FY = 11.0 * CAM_DIST                  # 세로 배율 (1단위 = 약 11줄, 실행할 때 화면 크기에 맞춰 다시 계산)
FX = FY * 2.0                         # 글자는 세로로 길쭉해서 가로는 2배


def set_screen(cols, rows):
    """터미널 크기에 맞춰 화면 크기·배율을 정해요. 트리는 가로세로 중 빠듯한 쪽에 맞춰 커져요."""
    global W, H, SC, AREA, FY, FX
    W, H = max(cols, 40), max(rows, 24)
    SC = min(W / BASE_W, H / BASE_H)
    AREA = W * H / (BASE_W * BASE_H)
    FY = 11.0 * CAM_DIST * SC
    FX = FY * 2.0
TILT = 0.12                           # 위에서 살짝 내려다보는 각도
COS_T, SIN_T = math.cos(TILT), math.sin(TILT)
YC = 1.55                             # 화면 가운데에 오는 높이


def unit(v):
    v = np.asarray(v, dtype=float)
    return v / np.linalg.norm(v, axis=-1, keepdims=True)


LIGHT_DIR = unit([-0.55, 0.62, 0.56])            # 빛: 왼쪽 위 앞
HALF_DIR = unit(LIGHT_DIR + np.array([0, 0, 1.0]))

rng = np.random.default_rng(1225)


def rgb(c):
    return np.array(c, dtype=float) / 255.0


def clamp01(x):
    return min(1.0, max(0.0, x))


def prog(t, span):
    a, b = span
    return clamp01((t - a) / (b - a))


def ease_out(p):
    return 1 - (1 - p) ** 3


def ease_back(p):
    if p <= 0:
        return 0.0
    c = 1.70158
    q = p - 1
    return 1 + (c + 1) * q ** 3 + c * q ** 2


def ground_y(r):
    return 0.12 * (1 - (r / GROUND_R) ** 2)


def tree_radius(y):
    r = np.zeros_like(y)
    for bot, top, R in TIERS:
        inside = (y >= bot) & (y <= top)
        r = np.where(inside, np.maximum(r, R * (top - y) / (top - bot)), r)
    return r


def swirl_radius(y):
    return 1.55 - 0.43 * y


def sphere_points(n):
    i = np.arange(n) + 0.5
    phi = np.arccos(1 - 2 * i / n)
    th = np.pi * (1 + 5 ** 0.5) * i
    return np.c_[np.cos(th) * np.sin(phi), np.cos(phi), np.sin(th) * np.sin(phi)]


def rot_y(p, a):
    c, s = math.cos(a), math.sin(a)
    return np.c_[c * p[:, 0] + s * p[:, 2], p[:, 1], -s * p[:, 0] + c * p[:, 2]]


# ══════════════════════════════════════════════════════════════════════
#  3D 모양 만들기 (처음 한 번)
# ══════════════════════════════════════════════════════════════════════
def build_ground(n=5000):
    n = int(n * SC * SC)
    r = GROUND_R * np.sqrt(rng.random(n))
    phi = rng.random(n) * 2 * np.pi
    y = ground_y(r) + rng.normal(0, 0.006, n)
    slope = 0.24 * r / GROUND_R ** 2
    nrm = unit(np.c_[slope * np.cos(phi), np.ones(n), slope * np.sin(phi)] + rng.normal(0, 0.22, (n, 3)))
    tint = rng.random(n)[:, None]
    col = rgb(GROUND_SNOW) * (0.85 + 0.15 * tint)
    return dict(P=np.c_[r * np.cos(phi), y, r * np.sin(phi)], N=nrm, C=col, r=r)


def build_tree(n=14000):
    n = int(n * SC * SC)
    P, N, C = [], [], []
    dark, light, snow = rgb(DARK_GREEN), rgb(LIGHT_GREEN), rgb(BRANCH_SNOW)
    areas = np.array([R * math.hypot(R, top - bot) for bot, top, R in TIERS])
    counts = (n * areas / areas.sum()).astype(int)
    for k, ((bot, top, R), m) in enumerate(zip(TIERS, counts)):
        h = top - bot
        v = np.sqrt(rng.random(m))                 # 0=꼭대기, 1=아래 가장자리
        phi = rng.random(m) * 2 * np.pi
        nb = int(round(8 + 9 * R))                  # 가지 수
        layer = 0.55 + 0.45 * np.cos(v * 2 * np.pi * 3 + phi * 2)  # 가지 층
        br = (0.5 + 0.5 * np.cos(phi * nb + v * 9 + k * 1.7)) ** 2 * layer
        fine = 0.5 + 0.5 * np.cos(phi * nb * 3 + v * 23)
        r = R * v * (1 + 0.11 * br + 0.04 * fine)
        y = top - v * h - 0.08 * br * v * v         # 가지 끝이 살짝 처짐
        P.append(np.c_[r * np.cos(phi), y, r * np.sin(phi)])
        nrm = unit(np.c_[1.8 * h * np.cos(phi), np.full(m, R), 1.8 * h * np.sin(phi)])
        N.append(unit(nrm + rng.normal(0, 0.22, (m, 3))))
        mix = np.clip(0.2 + 0.55 * br + 0.4 * (rng.random(m) - 0.5), 0, 1)[:, None]
        col = dark * (1 - mix) + light * mix
        if k < len(TIERS) - 1:                      # 위층 그늘에 가린 윗부분은 어둡게 → 층이 또렷
            col *= (0.45 + 0.55 * np.clip(v / 0.65, 0, 1))[:, None]
        tip = (br > 0.6) & (v > 0.7) & (rng.random(m) < 0.35)
        col[tip] = snow * 0.9 + col[tip] * 0.1
        C.append(col)
    # 갈색 기둥
    m = int(700 * SC * SC)
    phi = rng.random(m) * 2 * np.pi
    y = rng.random(m) * TRUNK_TOP
    P.append(np.c_[TRUNK_R * np.cos(phi), y, TRUNK_R * np.sin(phi)])
    N.append(unit(np.c_[np.cos(phi), np.zeros(m), np.sin(phi)] + rng.normal(0, 0.15, (m, 3))))
    bark = 0.75 + 0.3 * (0.5 + 0.5 * np.cos(phi * 9 + rng.normal(0, 0.4, m)))
    C.append(rgb(TRUNK_BROWN) * bark[:, None])
    return dict(P=np.vstack(P), N=np.vstack(N), C=np.vstack(C))


def build_balls():
    weights = np.array([R for _, _, R in TIERS])
    centers = []
    for i in range(N_BALLS):
        k = rng.choice(len(TIERS), p=weights / weights.sum())
        bot, top, R = TIERS[k]
        v = rng.uniform(0.5, 0.95)
        phi = i * 2.39996 + rng.uniform(-0.3, 0.3)
        r = R * v * 1.06 + BALL_R * 0.7
        y = top - v * (top - bot) - 0.04
        centers.append((r * math.cos(phi), y, r * math.sin(phi)))
    centers = np.array(sorted(centers, key=lambda c: c[1]))   # 아래부터
    sph = sphere_points(int(70 * SC * SC))
    K = len(sph)
    bi = np.repeat(np.arange(N_BALLS), K)
    cols = np.array([rgb(BALL_COLORS[i % len(BALL_COLORS)]) for i in range(N_BALLS)])
    return dict(centers=centers, bi=bi, center=centers[bi], off=np.tile(sph, (N_BALLS, 1)),
                C=cols[bi], w=rng.uniform(1.5, 4.0, N_BALLS), p=rng.uniform(0, 6.3, N_BALLS))


def build_lights():
    u = (np.arange(N_LIGHTS) + 0.5) / N_LIGHTS
    y = 0.6 + u * (2.85 - 0.6)
    phi = 2 * np.pi * LIGHT_TURNS * u
    r = tree_radius(y) * 1.08 + 0.03
    cols = np.array([rgb(LIGHT_COLORS[i % len(LIGHT_COLORS)]) for i in range(N_LIGHTS)])
    return dict(P=np.c_[r * np.cos(phi), y, r * np.sin(phi)], C=cols, u=u,
                w=rng.uniform(2.0, 6.0, N_LIGHTS), p=rng.uniform(0, 6.3, N_LIGHTS))


def build_hearts():
    pos = []
    for j in range(N_HEARTS):
        k = (j * 3 + 1) % len(TIERS)
        bot, top, R = TIERS[k]
        v = 0.55 + 0.35 * ((j * 0.618) % 1)
        phi = j * 2.4 + 0.4
        r = R * v * 1.1 + 0.06
        pos.append((r * math.cos(phi), top - v * (top - bot), r * math.sin(phi)))
    return np.array(pos).reshape(-1, 3)


def box_points(w, d, h, step=0.022):
    P, N, rib = [], [], []
    rw = 0.03

    def grid(a, b):
        u = np.arange(-a / 2 + step / 2, a / 2, step)
        v = np.arange(-b / 2 + step / 2, b / 2, step)
        U, V = np.meshgrid(u, v)
        return U.ravel(), V.ravel()

    X, Z = grid(w, d)                                   # 윗면
    P.append(np.c_[X, np.full_like(X, h), Z]); N.append(np.tile([0, 1, 0], (len(X), 1)))
    rib.append((np.abs(X) < rw) | (np.abs(Z) < rw))
    Z, Y = grid(d, h)                                   # 옆면 ±x
    for s in (-1, 1):
        P.append(np.c_[np.full_like(Z, s * w / 2), Y + h / 2, Z]); N.append(np.tile([s, 0, 0], (len(Z), 1)))
        rib.append(np.abs(Z) < rw)
    X, Y = grid(w, h)                                   # 옆면 ±z
    for s in (-1, 1):
        P.append(np.c_[X, Y + h / 2, np.full_like(X, s * d / 2)]); N.append(np.tile([0, 0, s], (len(X), 1)))
        rib.append(np.abs(X) < rw)
    sph = sphere_points(int(40 * SC * SC))              # 위의 리본 매듭
    for cx, rad in ((-0.045, (0.045, 0.03, 0.025)), (0.045, (0.045, 0.03, 0.025)), (0.0, (0.022, 0.022, 0.022))):
        P.append(sph * rad + [cx, h + 0.025, 0]); N.append(unit(sph / rad))
        rib.append(np.ones(len(sph), bool))
    return np.vstack(P).astype(float), np.vstack(N).astype(float), np.concatenate(rib)


def build_gifts():
    P, N, C, gi = [], [], [], []
    base, height = [], []
    for i, (box, ribbon, w, d, h, ang, rad) in enumerate(GIFTS):
        p, n, isrib = box_points(w, d, h, 0.022 / SC)
        yaw = rng.uniform(0, np.pi / 2)
        p, n = rot_y(p, yaw), rot_y(n, yaw)
        a = math.radians(ang)
        by = ground_y(rad) - 0.01
        P.append(p + [rad * math.cos(a), by, rad * math.sin(a)]); N.append(n)
        col = np.where(isrib[:, None], rgb(ribbon), rgb(box)) * (0.94 + 0.06 * rng.random(len(p)))[:, None]
        C.append(col); gi.append(np.full(len(p), i))
        base.append(by); height.append(h)
    return dict(P=np.vstack(P), N=np.vstack(N), C=np.vstack(C), gi=np.concatenate(gi),
                base=np.array(base), h=np.array(height))


def build_snow():
    n = int(N_SNOW * AREA)
    return dict(x0=rng.uniform(0, W, n), y0=rng.uniform(-15, H, n),
                speed=rng.uniform(4.0, 8.5, n) * SC, sway=rng.uniform(0.6, 1.8, n) * SC,
                f=rng.uniform(0.8, 2.0, n), p=rng.uniform(0, 6.3, n),
                glyph=rng.choice([1, 3, 7, 8], n, p=[0.35, 0.2, 0.2, 0.25]),
                heart=rng.random(n) < SNOW_HEART_RATIO,
                b=rng.uniform(0.6, 1.0, n))


# ══════════════════════════════════════════════════════════════════════
#  우주 배경 (별·은하수·행성)
# ══════════════════════════════════════════════════════════════════════
# 행성: (색, 가운데 가로 위치, 세로 위치(화면 비율 0~1), 반지름(기준 화면 줄 수), 고리?, 떠다니는 빠르기, 위상)
PLANETS = [
    (PLANET_SILVER, 0.17, 0.17, 3.8, True, 0.35, 0.0),     # 왼쪽 위, 고리 달린 은빛
    (PLANET_BLUE, 0.86, 0.19, 5.0, False, 0.28, 1.7),      # 오른쪽 위, 푸른 행성
    (PLANET_PURPLE, 0.08, 0.54, 2.3, False, 0.45, 3.1),    # 왼쪽 중간, 작은 보랏빛
]
PLANET_LIGHT = unit([-0.7, 0.55, 0.45])


class Sky:
    def __init__(self):
        yy, xx = np.mgrid[0:H, 0:W].astype(float)
        X = (xx + 0.5 - W / 2) / 2
        Y = H / 2 - (yy + 0.5)
        a = math.radians(24)
        dist = (-X * math.sin(a) + Y * math.cos(a)) / SC - 3
        noise = rng.random((H, W))
        for _ in range(3):
            noise = (noise + np.roll(noise, 1, 0) + np.roll(noise, -1, 0)
                     + np.roll(noise, 1, 1) + np.roll(noise, -1, 1)) / 5
        noise = (noise - noise.min()) / (noise.max() - noise.min())
        inten = 0.21 * np.exp(-(dist / 4.5) ** 2) * (0.2 + 0.9 * noise)
        self.L0 = inten
        self.C0 = rgb(MILKY_WAY) * np.clip(0.35 + 2.0 * inten, 0, 1)[..., None]
        n = min(int(N_SKY_STARS * AREA), W * H)
        cells = rng.choice(W * H, n, replace=False)
        self.cells = cells
        bright = rng.random(n) < 0.18
        self.b0 = np.where(bright, rng.uniform(0.85, 1.0, n), rng.uniform(0.15, 0.7, n))
        self.w = rng.uniform(0.5, 4.0, n)
        self.p = rng.uniform(0, 6.3, n)
        self.col = np.array([rgb(c) for c in SKY_STARS])[rng.integers(0, len(SKY_STARS), n)]

    def draw(self, t):
        L = self.L0.copy().ravel()
        C = self.C0.copy().reshape(-1, 3)
        tw = 0.5 + 0.5 * np.sin(self.w * t + self.p)
        b = self.b0 * (0.2 + 0.8 * tw ** 2)
        idx = np.where(b > 0.82, 8, np.where(b > 0.62, 7, np.where(b > 0.4, 3, np.where(b > 0.18, 2, 1))))
        L[self.cells] = np.maximum(L[self.cells], idx / NR)
        C[self.cells] = self.col * (0.35 + 0.65 * b)[:, None]
        L = L.reshape(H, W)
        C = C.reshape(H, W, 3)
        for planet in PLANETS:
            self.planet(L, C, t, *planet)
        return L, C

    @staticmethod
    def planet(L, C, t, color, x0, y0, R, ring, sp, ph):
        R *= SC
        cx = x0 * W + 0.8 * SC * math.sin(t * sp * 0.7 + ph)
        cy = y0 * H + 0.7 * SC * math.sin(t * sp + ph)
        ext = R * (1.95 if ring else 1.05)
        xa, xb = max(0, int(cx - 2 * ext) - 1), min(W, int(cx + 2 * ext) + 2)
        ya, yb = max(0, int(cy - ext) - 1), min(H, int(cy + ext) + 2)
        if xa >= xb or ya >= yb:
            return
        yy, xx = np.mgrid[ya:yb, xa:xb].astype(float)
        dx = (xx + 0.5 - cx) / 2
        dy = cy - (yy + 0.5)
        d2 = dx * dx + dy * dy
        inside = d2 < R * R
        nz = np.sqrt(np.clip(1 - d2 / (R * R), 0, 1))
        lam = np.clip((dx / R) * PLANET_LIGHT[0] + (dy / R) * PLANET_LIGHT[1] + nz * PLANET_LIGHT[2], 0, 1)
        band = 1 + 0.12 * np.sin(dy / R * 7 + dx / R * 1.5)
        pL = 0.03 + 0.72 * lam ** 0.8
        pC = rgb(color) * (0.12 + 0.95 * lam * band)[..., None]
        sL, sC = L[ya:yb, xa:xb], C[ya:yb, xa:xb]
        if ring:
            a = 0.35
            u = dx * math.cos(a) + dy * math.sin(a)
            v = -dx * math.sin(a) + dy * math.cos(a)
            q = np.sqrt(u * u + (v / 0.3) ** 2) / R
            rm = (q > 1.35) & (q < 1.9) & ~((q > 1.6) & (q < 1.67))
            rL = 0.30 + 0.10 * np.sin(q * 20)
            rC = rgb(PLANET_RING) * (0.55 + 0.45 * (u < 0))[..., None] * (0.8 + 0.2 * np.sin(q * 20))[..., None]
            back = rm & (v > 0) & ~inside
            sL[back], sC[back] = rL[back] * 0.8, rC[back] * 0.8
        sL[inside], sC[inside] = pL[inside], pC[inside]
        if ring:
            front = rm & (v <= 0)
            sL[front], sC[front] = rL[front], rC[front]


# ══════════════════════════════════════════════════════════════════════
#  투영·빛 계산
# ══════════════════════════════════════════════════════════════════════
def to_view(P, theta, normal=False):
    c, s = math.cos(theta), math.sin(theta)
    x = c * P[:, 0] + s * P[:, 2]
    z = -s * P[:, 0] + c * P[:, 2]
    y = P[:, 1] if normal else P[:, 1] - YC
    return x, COS_T * y - SIN_T * z, SIN_T * y + COS_T * z


def project(P, theta):
    x, y, z = to_view(P, theta)
    depth = CAM_DIST - z
    return W / 2 + FX * x / depth, H / 2 - FY * y / depth, depth


class Glow:
    """빛나는 것들은 더하기로 쌓여요 — 겹칠수록 밝아짐."""

    def __init__(self):
        self.idx, self.I, self.C = [], [], []

    def add(self, ix, iy, I, color):
        ix = np.atleast_1d(np.asarray(ix, dtype=int))
        iy = np.atleast_1d(np.asarray(iy, dtype=int))
        I = np.broadcast_to(np.asarray(I, dtype=float), ix.shape)
        color = np.broadcast_to(np.asarray(color, dtype=float), ix.shape + (3,))
        m = (ix >= 0) & (ix < W) & (iy >= 0) & (iy < H) & (I > 0.003)
        self.idx.append((iy * W + ix)[m])
        self.I.append(I[m])
        self.C.append(color[m] * I[m, None])

    def splat(self, sx, sy, I, color, side=0.35, vert=0.15):
        ix, iy = np.floor(sx).astype(int), np.floor(sy).astype(int)
        self.add(ix, iy, I, color)
        for dx, dy, f in ((-1, 0, side), (1, 0, side), (0, -1, vert), (0, 1, vert)):
            self.add(ix + dx, iy + dy, np.asarray(I) * f, color)

    def ray(self, cx, cy, ang, length, I0, color, start=0.0):
        if length <= start:
            return
        ca, sa = math.cos(ang), math.sin(ang)
        step = 0.9 / max(abs(2 * ca), abs(sa))
        d = np.arange(start, length, step)
        I = I0 * (1 - (d - start) / (length - start)) ** 1.4
        self.add(np.floor(cx + 2 * d * ca), np.floor(cy - d * sa), I, color)

    def bloom(self, cx, cy, radius, I0, color):
        if radius <= 0:                     # 별이 막 생기는 순간 (크기 0)
            return
        xa, xb = int(cx - 2 * radius) - 1, int(cx + 2 * radius) + 2
        ya, yb = int(cy - radius) - 1, int(cy + radius) + 2
        yy, xx = np.mgrid[ya:yb, xa:xb]
        d = np.hypot((xx + 0.5 - cx) / 2, yy + 0.5 - cy)
        self.add(xx.ravel(), yy.ravel(), (I0 * np.clip(1 - d / radius, 0, 1) ** 1.5).ravel(), color)

    def buffers(self):
        if not self.idx:
            return np.zeros((H, W)), np.zeros((H, W, 3))
        idx = np.concatenate(self.idx)
        I = np.concatenate(self.I)
        C = np.concatenate(self.C)
        GI = np.bincount(idx, I, W * H).reshape(H, W)
        GC = np.stack([np.bincount(idx, C[:, k], W * H) for k in range(3)], -1).reshape(H, W, 3)
        return GI, GC


# ══════════════════════════════════════════════════════════════════════
#  한 장면 그리기
# ══════════════════════════════════════════════════════════════════════
class Scene:
    def __init__(self):
        self.sky = Sky()
        self.ground = build_ground()
        self.tree = build_tree()
        self.balls = build_balls()
        self.lights = build_lights()
        self.hearts = build_hearts()
        self.gifts = build_gifts()
        self.snow = build_snow()

    # ── 단단한 것들 (땅·트리·볼·선물) ──────────────────────────
    def solids(self, tc):
        Ps, Ns, Cs, SP, SH, EM = [], [], [], [], [], []

        def push(p, n, c, spec, shin, em=None):
            Ps.append(p); Ns.append(n); Cs.append(c)
            SP.append(np.full(len(p), spec)); SH.append(np.full(len(p), shin))
            EM.append(np.zeros(len(p)) if em is None else em)

        g = self.ground
        pg = ease_out(prog(tc, T_GROUND))
        if pg > 0:
            m = g["r"] <= GROUND_R * pg
            push(g["P"][m], g["N"][m], g["C"][m], 0.15, 10)

        a, b = T_TREE
        if tc >= a:
            tr = self.tree
            yh = HEAD_MAX * min(1.0, (tc - a) / (b - a))
            m = tr["P"][:, 1] <= yh
            born = a + tr["P"][m, 1] / HEAD_MAX * (b - a)
            em = 0.6 * np.exp(-np.maximum(tc - born, 0) / 0.25)
            push(tr["P"][m], tr["N"][m], tr["C"][m], 0.08, 8, em)

        s = self.ball_scales(tc)
        if s.any():
            bl = self.balls
            sp = s[bl["bi"]]
            m = sp > 0
            P = bl["center"][m] + (sp[m] * BALL_R)[:, None] * bl["off"][m]
            push(P, bl["off"][m], bl["C"][m], 0.9, 30)

        a, b = T_GIFTS
        if tc >= a:
            gf = self.gifts
            n = len(GIFTS)
            starts = a + np.arange(n) * (b - a - 0.35) / max(n - 1, 1)
            rise = np.array([ease_back(clamp01((tc - st) / 0.35)) for st in starts])
            off = -(gf["h"] + 0.12) * (1 - rise)
            P = gf["P"].copy()
            P[:, 1] += off[gf["gi"]]
            m = (rise[gf["gi"]] > 0) & (P[:, 1] >= gf["base"][gf["gi"]] - 0.003)
            push(P[m], gf["N"][m], gf["C"][m], 0.25, 12)

        if not Ps:
            return None
        return (np.vstack(Ps), np.vstack(Ns), np.vstack(Cs),
                np.concatenate(SP), np.concatenate(SH), np.concatenate(EM))

    @staticmethod
    def ball_scales(tc):
        a, b = T_BALLS
        st = a + np.arange(N_BALLS) * (b - a - 0.25) / max(N_BALLS - 1, 1)
        return np.array([ease_back(clamp01((tc - s) / 0.25)) for s in st])

    @staticmethod
    def draw_solids(data, theta, FL, FC, ZB):
        P, N, C, spec, shin, em = data
        sx, sy, depth = project(P, theta)
        ix, iy = np.floor(sx).astype(int), np.floor(sy).astype(int)
        sel = np.nonzero((ix >= 0) & (ix < W) & (iy >= 0) & (iy < H) & (depth > 0.5))[0]
        if not len(sel):
            return
        cell = iy[sel] * W + ix[sel]
        order = np.argsort(depth[sel], kind="stable")
        cells, first = np.unique(cell[order], return_index=True)   # 가장 가까운 점만
        win = sel[order[first]]
        nx, ny, nz = to_view(N[win], theta, normal=True)
        n = np.c_[nx, ny, nz]
        lam = np.clip(n @ LIGHT_DIR, 0, 1)
        hl = np.clip(n @ HALF_DIR, 0, 1) ** shin[win] * spec[win]
        e = em[win][:, None]
        col = C[win] * (0.28 + 0.9 * lam)[:, None] + hl[:, None]
        col = col * (1 - e) + rgb(NEW_GLOW) * e
        FL[cells] = 0.18 + 0.75 * lam + hl + 0.9 * e[:, 0]
        FC[cells] = col
        ZB[cells] = depth[win]

    # ── 빛나는 것들 ───────────────────────────────────────────
    def glow3d(self, G, P, I, color, theta, ZB, tol, side=0.35, vert=0.15):
        sx, sy, depth = project(P, theta)
        ix, iy = np.floor(sx).astype(int), np.floor(sy).astype(int)
        inb = (ix >= 0) & (ix < W) & (iy >= 0) & (iy < H)
        vis = inb.copy()
        vis[inb] = depth[inb] <= ZB[iy[inb] * W + ix[inb]] + tol
        color = np.broadcast_to(color, (len(P), 3))
        G.splat(sx[vis], sy[vis], np.asarray(I)[vis], color[vis], side, vert)
        return sx, sy, depth, vis

    def swirl(self, G, tc, theta, ZB):
        a, b = T_TREE
        if tc < a or tc > b + 0.35:
            return
        fade = clamp01((tc - b) / 0.35)
        s = min(1.0, (tc - a) / (b - a))
        nk = int(150 * SC)
        k = np.arange(nk) / nk
        Ps, Is = [], []
        for j in range(2):                                  # 두 줄기
            sk = s - 0.24 * k * (1 - 0.7 * fade)
            ok = sk >= 0
            y = sk[ok] * HEAD_MAX
            phi = j * np.pi + 2 * np.pi * SWIRL_TURNS * sk[ok]
            r = swirl_radius(y)
            Ps.append(np.c_[r * np.cos(phi), y, r * np.sin(phi)])
            Is.append(1.4 * (1 - k[ok]) ** 2 * (1 - fade))
            yh = s * HEAD_MAX                               # 머리 둘레 반짝이
            ph = j * np.pi + 2 * np.pi * SWIRL_TURNS * s
            rh = swirl_radius(yh)
            jit = rng.normal(0, 0.09, (10, 3))
            Ps.append(np.array([rh * math.cos(ph), yh, rh * math.sin(ph)]) + jit)
            Is.append(rng.uniform(0.2, 0.9, 10) * (1 - fade))
        self.glow3d(G, np.vstack(Ps), np.concatenate(Is), rgb(SWIRL_WHITE), theta, ZB, 0.2, 0.3, 0.12)

    @staticmethod
    def flash(G, tc, theta):
        p = prog(tc, T_FLASH)
        if not (T_FLASH[0] <= tc <= T_FLASH[1]):
            return
        sx, sy, _ = project(np.array([[0, HEAD_MAX, 0.0]]), theta)
        fade = (1 - p) ** 1.5
        for i in range(8):
            G.ray(sx[0], sy[0], i * math.pi / 4, (2 + 24 * ease_out(p)) * SC, 2.2 * fade, rgb(FLASH_WHITE))
        G.bloom(sx[0], sy[0], (1.5 + 3 * ease_out(p)) * SC, 2.0 * fade, rgb(FLASH_WHITE))

    def lights_glow(self, G, tc, t, theta, ZB):
        a, b = T_LIGHTS
        lt = self.lights
        on_t = a + lt["u"] * (b - a)
        age = tc - on_t
        on = age >= 0
        if not on.any():
            return
        tw = 0.65 + 0.35 * np.sin(lt["w"] * t + lt["p"])
        I = 0.8 * tw * np.clip(age / 0.12, 0, 1) + 1.0 * np.exp(-np.maximum(age, 0) / 0.15) * on
        if tc >= T_CHASE:                                   # 줄줄이 달려가는 빛
            k = clamp01((tc - T_CHASE) / 0.5)
            d = ((tc - T_CHASE) * 55 - np.arange(N_LIGHTS)) % 18
            chase = 0.25 + 1.4 * np.exp(-d / 2.2)
            I = I * (1 - k) + chase * k
        I = I * on
        self.glow3d(G, lt["P"][on], I[on], lt["C"][on], theta, ZB, 0.15)

    def ball_glints(self, G, tc, t, theta, ZB):
        s = self.ball_scales(tc)
        m = s > 0.9
        if not m.any():
            return
        bl = self.balls
        sx, sy, depth = project(bl["centers"][m], theta)
        ix, iy = np.floor(sx).astype(int), np.floor(sy).astype(int)
        inb = (ix >= 0) & (ix < W) & (iy >= 0) & (iy < H)
        vis = inb.copy()
        vis[inb] = depth[inb] - BALL_R <= ZB[iy[inb] * W + ix[inb]] + 0.05
        I = 1.3 * np.maximum(0, np.sin(bl["w"][m] * t + bl["p"][m])) ** 16
        G.splat(sx[vis] - 1.0 * SC, sy[vis] - 0.6 * SC, I[vis], np.ones(3), 0.25, 0.1)

    def heart_state(self, tc, t, theta, ZB):
        a, b = T_HEARTS
        n = len(self.hearts)
        if n == 0 or tc < a:
            return None
        starts = a + np.arange(n) * (b - a - 0.3) / max(n - 1, 1)
        ap = np.clip((tc - starts) / 0.3, 0, 1)
        sx, sy, depth = project(self.hearts, theta)
        ix, iy = np.floor(sx).astype(int), np.floor(sy).astype(int)
        inb = (ix >= 0) & (ix < W) & (iy >= 0) & (iy < H) & (ap > 0)
        vis = inb.copy()
        vis[inb] = depth[inb] <= ZB[iy[inb] * W + ix[inb]] + 0.2
        ph = (t * 1.15 + np.arange(n) * 0.13) % 1          # 두근두근
        beat = np.exp(-((ph - 0.05) / 0.07) ** 2) + 0.65 * np.exp(-((ph - 0.25) / 0.07) ** 2)
        return ix[vis], iy[vis], beat[vis], ap[vis]

    def star(self, G, tc, t, theta, FL, FC, ZB):
        p = prog(tc, T_STAR)
        if tc < T_STAR[0]:
            return
        sc = ease_back(p)
        sx, sy, depth = project(np.array([[0, STAR_Y, 0.0]]), theta)
        cx, cy = sx[0], sy[0]
        Ro, Ri = 3.0 * sc * SC, 1.25 * sc * SC
        if Ro > 0.3:
            xa, xb = max(0, int(cx - 2 * Ro) - 1), min(W, int(cx + 2 * Ro) + 2)
            ya, yb = max(0, int(cy - Ro) - 1), min(H, int(cy + Ro) + 2)
            yy, xx = np.mgrid[ya:yb, xa:xb]
            dx = (xx + 0.5 - cx) / 2
            dy = cy - (yy + 0.5)
            rr = np.hypot(dx, dy)
            ang = np.arctan2(dy, dx)
            seg = 2 * np.pi / 5
            rel = (ang - np.pi / 2) % seg
            delta = np.minimum(rel, seg - rel)
            s5 = math.sin(seg / 2)
            edge = Ro * Ri * s5 / (Ro * np.sin(delta) + Ri * np.sin(seg / 2 - delta) + 1e-9)
            inside = rr <= edge
            tip = ang - rel + np.where(rel < seg / 2, 0, seg)   # 가까운 뾰족 끝 방향
            facet = tip + np.where(rel < seg / 2, 0.6, -0.6)
            lit = 0.5 + 0.5 * np.cos(facet - 2.3)
            L = 0.55 + 0.35 * lit + 0.25 * (1 - rr / max(Ro, 1e-6))
            col = rgb(STAR_GOLD)[None, None] * (0.6 + 0.5 * lit)[..., None]
            cells = (yy * W + xx)[inside]
            FL[cells] = L[inside]
            FC[cells] = col[inside]
            ZB[cells] = depth[0] - 0.5
        tw = 0.75 + 0.25 * math.sin(t * 5.3)
        ray = rgb(STAR_RAY)
        for i in range(4):
            G.ray(cx, cy, i * math.pi / 2 + math.pi / 2,
                  (Ro + (3.5 + 2 * math.sin(t * 2.6 + i)) * SC) * sc, 0.9 * tw * sc, ray, Ro * 0.85)
            G.ray(cx, cy, i * math.pi / 2 + math.pi / 4,
                  (Ro * 0.6 + (2 + 1.2 * math.sin(t * 3.7 + i)) * SC) * sc, 0.6 * tw * sc, ray, Ro * 0.6)
        G.bloom(cx, cy, 1.2 * sc * SC, 0.5 * tw, ray)

    def snowfall(self, t, code, col):
        sn = self.snow
        y = sn["y0"] + t * sn["speed"]
        y = (y + 15) % (H + 15) - 15
        x = (sn["x0"] + sn["sway"] * np.sin(t * sn["f"] + sn["p"])) % W
        ix, iy = x.astype(int), np.floor(y).astype(int)
        m = (iy >= 0) & (iy < H)
        cells = iy[m] * W + ix[m]
        heart = sn["heart"][m]
        code[cells] = np.where(heart, HEART_CODE, sn["glyph"][m])
        col[cells] = np.where(heart[:, None], rgb(PINK), rgb(SNOWFLAKE)) * sn["b"][m][:, None]

    # ── 전체 합치기 ───────────────────────────────────────────
    def render(self, t):
        tc = t % TOTAL
        theta = 2 * math.pi * t / ROT_PERIOD
        bgL, bgC = self.sky.draw(t)
        L, col = bgL.ravel(), bgC.reshape(-1, 3)
        FL = np.full(W * H, -1.0)
        FC = np.zeros((W * H, 3))
        ZB = np.full(W * H, np.inf)
        G = Glow()

        data = self.solids(tc)
        if data is not None:
            self.draw_solids(data, theta, FL, FC, ZB)
        self.star(G, tc, t, theta, FL, FC, ZB)
        self.swirl(G, tc, theta, ZB)
        self.flash(G, tc, theta)
        self.lights_glow(G, tc, t, theta, ZB)
        self.ball_glints(G, tc, t, theta, ZB)
        hs = self.heart_state(tc, t, theta, ZB)
        if hs is not None:
            hx, hy, beat, ap = hs
            G.add(hx - 1, hy, 0.3 * beat * ap, rgb(PINK))
            G.add(hx + 1, hy, 0.3 * beat * ap, rgb(PINK))

        fg = FL >= 0
        L[fg], col[fg] = FL[fg], FC[fg]
        GI, GC = G.buffers()
        GI, GC = GI.ravel(), GC.reshape(-1, 3)
        a = np.clip(GI, 0, 1)
        col = col * (1 - 0.75 * a)[:, None] + GC
        L = L * (1 - 0.4 * a) + GI
        white = np.clip((L - 1.15) / 1.0, 0, 1)[:, None]    # 아주 밝으면 하얗게
        col = np.clip(col + (1 - col) * white, 0, 1)
        code = np.clip(np.rint(L * NR), 0, NR).astype(int)

        if hs is not None:
            cells = hy * W + hx
            code[cells] = HEART_CODE
            hc = rgb(PINK) * (0.45 + 0.6 * beat)[:, None] * ap[:, None]
            hc = hc + (1 - hc) * (np.clip(beat - 0.8, 0, 1) * 0.5 + (1 - ap) * 0.8)[:, None]
            col[cells] = np.clip(hc, 0, 1)
        self.snowfall(t, code, col)

        return to_ansi(code.reshape(H, W), (col * 255).astype(np.uint8).reshape(H, W, 3))


def to_ansi(code, col):
    codes, cols = code.tolist(), col.tolist()
    out = ["\x1b[?2026h"]
    for y in range(H):
        parts = ["\x1b[%d;1H\x1b[48;2;0;0;0m" % (y + 1)]
        last = None
        crow, colrow = codes[y], cols[y]
        for x in range(W):
            g = crow[x]
            if g == 0:
                parts.append(" ")
                continue
            c = colrow[x]
            if c != last:
                parts.append("\x1b[38;2;%d;%d;%dm" % (c[0], c[1], c[2]))
                last = c
            parts.append(GLYPHS[g])
        out.append("".join(parts))
    out.append("\x1b[?2026l")
    return "".join(out)


# ══════════════════════════════════════════════════════════════════════
#  실행
# ══════════════════════════════════════════════════════════════════════
def main():
    cols, rows = shutil.get_terminal_size((BASE_W, BASE_H))
    set_screen(cols, rows)                           # 터미널 전체 크기에 맞춤
    scene = Scene()
    out = sys.stdout.buffer
    signal.signal(signal.SIGTERM, lambda *_: (_ for _ in ()).throw(KeyboardInterrupt))
    # 다른 화면으로 전환 · 커서 숨김 · 처음 한 번만 까맣게 칠하기
    out.write(b"\x1b[?1049h\x1b[?25l\x1b[48;2;0;0;0m\x1b[2J")
    out.flush()
    try:
        t0 = time.perf_counter()
        frame = 0
        while True:
            now = time.perf_counter()
            out.write(scene.render(now - t0).encode("utf-8"))
            out.flush()
            frame += 1
            nxt = t0 + frame / FPS
            now = time.perf_counter()
            if nxt > now:
                time.sleep(nxt - now)
            else:
                frame = int((now - t0) * FPS)            # 늦어지면 따라잡기
    except KeyboardInterrupt:
        pass
    finally:
        # 글자색·커서·원래 화면 되돌리기
        out.write(b"\x1b[0m\x1b[?25h\x1b[?1049l")
        out.flush()


if __name__ == "__main__":
    main()
