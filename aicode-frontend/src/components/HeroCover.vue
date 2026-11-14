<template>
  <section ref="heroRef" class="cosmic-hero" @mousemove="onMouseMove" @mouseleave="onMouseLeave">
    <!-- 星空底噪：两层错位排布的星点 -->
    <div class="stars stars-a"></div>
    <div class="stars stars-b"></div>
    <!-- 星云 -->
    <div class="nebula nebula-1"></div>
    <div class="nebula nebula-2"></div>
    <!-- 鼠标跟随光斑：DeepSeek 式交互 -->
    <div v-show="spotlightVisible" class="spotlight" :style="spotlightStyle"></div>

    <div class="cosmic-inner">
      <!-- 左侧：交互式大标题 -->
      <div class="cosmic-text">
        <p class="cosmic-eyebrow">✦ 星构 AI · 应用生成平台</p>
        <h1 class="cosmic-title">
          <span
            v-for="(ch, index) in titleChars"
            :key="index"
            class="title-char"
            :style="{ transitionDelay: `${index * 20}ms` }"
            >{{ ch }}</span
          >
        </h1>
        <p class="cosmic-subtitle">用一句话，生成一个完整的网站应用</p>
        <div class="cosmic-tags">
          <span class="cosmic-tag">无需写代码</span>
          <span class="cosmic-tag">实时预览</span>
          <span class="cosmic-tag">一键部署</span>
        </div>
      </div>

      <!-- 右侧：旋转发光的星构星球 -->
      <div class="globe-scene" :style="parallaxStyle">
        <div class="planet-glow"></div>

        <svg class="globe-svg" viewBox="0 0 400 400" aria-hidden="true">
          <defs>
            <radialGradient id="nodeGlow" cx="50%" cy="50%" r="50%">
              <stop offset="0%" stop-color="#e9d5ff" />
              <stop offset="100%" stop-color="#a855f7" />
            </radialGradient>
            <linearGradient id="ringGrad" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0%" stop-color="#c4b5fd" stop-opacity="0.9" />
              <stop offset="100%" stop-color="#7c3aed" stop-opacity="0.2" />
            </linearGradient>
          </defs>

          <!-- 外圈虚线环（反向旋转） -->
          <g class="spin-reverse">
            <circle
              cx="200"
              cy="200"
              r="186"
              fill="none"
              stroke="url(#ringGrad)"
              stroke-width="1"
              stroke-dasharray="4 10"
            />
            <polygon
              points="200,6 209,12 209,22 200,28 191,22 191,12"
              fill="#8b5cf6"
              opacity="0.85"
            />
            <polygon points="388,196 394,202 388,208 382,202" fill="#c4b5fd" opacity="0.7" />
          </g>

          <!-- 粒子网络星球（正向旋转） -->
          <g class="spin-slow">
            <line
              v-for="(edge, index) in EDGES"
              :key="'e' + index"
              :x1="getNode(edge[0]).x"
              :y1="getNode(edge[0]).y"
              :x2="getNode(edge[1]).x"
              :y2="getNode(edge[1]).y"
              stroke="#a78bfa"
              :stroke-opacity="edgeOpacity(index)"
              stroke-width="0.8"
            />
            <circle
              v-for="(node, index) in NODES"
              :key="'n' + index"
              :cx="node.x"
              :cy="node.y"
              :r="node.r"
              :fill="index % 3 === 0 ? 'url(#nodeGlow)' : '#c4b5fd'"
              :opacity="node.o"
            />
          </g>

          <!-- 椭圆轨道 + 环绕卫星 -->
          <path
            id="orbitPath"
            d="M 395 200 A 195 62 0 1 1 5 200 A 195 62 0 1 1 395 200"
            fill="none"
            stroke="#8b5cf6"
            stroke-opacity="0.35"
            stroke-width="1"
          />
          <circle r="5" fill="#e9d5ff" opacity="0.95">
            <animateMotion dur="14s" repeatCount="indefinite">
              <mpath href="#orbitPath" />
            </animateMotion>
          </circle>
          <circle r="2.5" fill="#7c3aed" opacity="0.9">
            <animateMotion dur="14s" begin="-7s" repeatCount="indefinite">
              <mpath href="#orbitPath" />
            </animateMotion>
          </circle>
        </svg>

        <!-- 星球中心文字：发光的「星构」 -->
        <div class="planet-title">
          <span class="planet-char">星</span>
          <span class="planet-char">构</span>
        </div>

        <!-- 悬浮应用图块（对应封面四周的功能图标） -->
        <div class="tile tile-1" :style="{ animationDelay: '0s' }">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M8 6L3 12l5 6M16 6l5 6-5 6" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </div>
        <div class="tile tile-2" :style="{ animationDelay: '1.2s' }">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="4" width="18" height="16" rx="3" />
            <path d="M3 10h18M10 10v10" stroke-linecap="round" />
          </svg>
        </div>
        <div class="tile tile-3" :style="{ animationDelay: '0.6s' }">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path
              d="M21 12a9 9 0 1 1-9-9c2 0 3.5.6 4.8 1.6L21 3l-1.4 4.2c.9 1.3 1.4 3 1.4 4.8z"
              stroke-linejoin="round"
            />
            <circle cx="12" cy="12" r="3" />
          </svg>
        </div>
        <div class="tile tile-4" :style="{ animationDelay: '1.8s' }">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path
              d="M12 3v3M12 18v3M3 12h3M18 12h3M5.6 5.6l2.1 2.1M16.3 16.3l2.1 2.1M5.6 18.4l2.1-2.1M16.3 7.7l2.1-2.1"
              stroke-linecap="round"
            />
            <circle cx="12" cy="12" r="3.5" />
          </svg>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

/** DeepSeek 式大标题：逐字交互 */
const titleChars = '以星为序，构筑无限'.split('')

/** 星球粒子网络的节点（手工排布，形成球面感） */
interface SphereNode {
  x: number
  y: number
  r: number
  o: number
}

const NODES: SphereNode[] = [
  { x: 200, y: 52, r: 3, o: 0.9 },
  { x: 278, y: 74, r: 2.5, o: 0.8 },
  { x: 334, y: 142, r: 3, o: 0.9 },
  { x: 348, y: 226, r: 2.5, o: 0.7 },
  { x: 306, y: 300, r: 3, o: 0.85 },
  { x: 234, y: 348, r: 2.5, o: 0.75 },
  { x: 152, y: 340, r: 3, o: 0.85 },
  { x: 86, y: 288, r: 2.5, o: 0.7 },
  { x: 56, y: 208, r: 3, o: 0.85 },
  { x: 74, y: 126, r: 2.5, o: 0.75 },
  { x: 130, y: 74, r: 3, o: 0.85 },
  // 内层节点（球面纵深）
  { x: 200, y: 126, r: 2, o: 0.55 },
  { x: 258, y: 148, r: 2, o: 0.5 },
  { x: 276, y: 210, r: 2, o: 0.5 },
  { x: 246, y: 268, r: 2, o: 0.55 },
  { x: 184, y: 284, r: 2, o: 0.5 },
  { x: 130, y: 244, r: 2, o: 0.5 },
  { x: 124, y: 172, r: 2, o: 0.55 },
  { x: 178, y: 196, r: 2, o: 0.45 },
  { x: 226, y: 232, r: 1.6, o: 0.4 },
]

/** 节点连线（下标对） */
const EDGES: [number, number][] = [
  [0, 1],
  [1, 2],
  [2, 3],
  [3, 4],
  [4, 5],
  [5, 6],
  [6, 7],
  [7, 8],
  [8, 9],
  [9, 10],
  [10, 0],
  [0, 11],
  [2, 12],
  [4, 14],
  [6, 15],
  [8, 16],
  [10, 17],
  [11, 12],
  [12, 13],
  [13, 14],
  [14, 15],
  [15, 16],
  [16, 17],
  [17, 11],
  [11, 18],
  [13, 18],
  [15, 18],
  [18, 19],
  [13, 19],
]

/** 内层连线更淡，营造球面纵深 */
const edgeOpacity = (index: number) => (index < 11 ? 0.35 : 0.2)

/** 严格索引访问：越界时回退到球心，避免 undefined */
const getNode = (index: number): SphereNode => NODES[index] ?? { x: 200, y: 200, r: 0, o: 0 }

/** 鼠标跟随光斑 */
const heroRef = ref<HTMLElement | null>(null)
const spotlightVisible = ref(false)
const spotlightX = ref(0)
const spotlightY = ref(0)
const parallaxX = ref(0)
const parallaxY = ref(0)

const spotlightStyle = computed(() => ({
  left: `${spotlightX.value}px`,
  top: `${spotlightY.value}px`,
}))

/** 星球随鼠标轻微视差移动 */
const parallaxStyle = computed(() => ({
  transform: `translate(${parallaxX.value}px, ${parallaxY.value}px)`,
}))

const onMouseMove = (event: MouseEvent) => {
  const rect = heroRef.value?.getBoundingClientRect()
  if (!rect) return
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top
  spotlightX.value = x
  spotlightY.value = y
  spotlightVisible.value = true
  // 视差偏移限制在 ±12px
  parallaxX.value = ((x - rect.width / 2) / rect.width) * 24
  parallaxY.value = ((y - rect.height / 2) / rect.height) * 16
}

const onMouseLeave = () => {
  spotlightVisible.value = false
  parallaxX.value = 0
  parallaxY.value = 0
}
</script>

<style scoped>
/* ========== 整体封面：深紫宇宙 ========== */
.cosmic-hero {
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(1000px 460px at 78% 30%, rgba(124, 58, 237, 0.35), transparent 65%),
    radial-gradient(700px 380px at 12% 85%, rgba(88, 28, 135, 0.4), transparent 60%),
    linear-gradient(160deg, #0b0620 0%, #170b38 55%, #241152 100%);
  color: #ede9fe;
}

.cosmic-inner {
  position: relative;
  z-index: 2;
  max-width: 1200px;
  margin: 0 auto;
  padding: 72px 32px 64px;
  display: flex;
  align-items: center;
  gap: 32px;
}

/* ========== 星空与星云 ========== */
.stars {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.stars-a {
  background-image:
    radial-gradient(circle, rgba(255, 255, 255, 0.9) 1px, transparent 1.4px),
    radial-gradient(circle, rgba(196, 181, 253, 0.7) 1px, transparent 1.4px);
  background-size:
    130px 110px,
    90px 80px;
  background-position:
    12px 18px,
    60px 40px;
  animation: twinkle 5s ease-in-out infinite;
}

.stars-b {
  background-image:
    radial-gradient(circle, rgba(255, 255, 255, 0.55) 1.2px, transparent 1.6px),
    radial-gradient(circle, rgba(167, 139, 250, 0.5) 1px, transparent 1.4px);
  background-size:
    210px 190px,
    150px 170px;
  background-position:
    40px 90px,
    110px 30px;
  animation: twinkle 7s ease-in-out infinite reverse;
}

@keyframes twinkle {
  0%,
  100% {
    opacity: 0.7;
  }
  50% {
    opacity: 1;
  }
}

.nebula {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  pointer-events: none;
  z-index: 0;
}

.nebula-1 {
  width: 420px;
  height: 260px;
  left: -80px;
  top: 12%;
  background: rgba(139, 92, 246, 0.22);
  animation: drift 16s ease-in-out infinite alternate;
}

.nebula-2 {
  width: 360px;
  height: 240px;
  right: 4%;
  bottom: -60px;
  background: rgba(192, 132, 252, 0.18);
  animation: drift 20s ease-in-out infinite alternate-reverse;
}

@keyframes drift {
  from {
    transform: translate(0, 0);
  }
  to {
    transform: translate(36px, -20px);
  }
}

/* ========== 鼠标跟随光斑（DeepSeek 式） ========== */
.spotlight {
  position: absolute;
  z-index: 1;
  width: 340px;
  height: 340px;
  border-radius: 50%;
  pointer-events: none;
  transform: translate(-50%, -50%);
  background: radial-gradient(
    circle,
    rgba(196, 181, 253, 0.28) 0%,
    rgba(124, 58, 237, 0.14) 40%,
    transparent 70%
  );
  mix-blend-mode: screen;
  transition:
    left 0.12s ease-out,
    top 0.12s ease-out,
    opacity 0.3s;
}

/* ========== 左侧文字 ========== */
.cosmic-text {
  flex: 1;
  min-width: 0;
}

.cosmic-eyebrow {
  margin: 0 0 18px;
  font-size: 15px;
  letter-spacing: 2px;
  color: #c4b5fd;
}

.cosmic-title {
  margin: 0 0 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 0.18em;
  font-size: clamp(40px, 5.2vw, 62px);
  font-weight: 800;
  line-height: 1.15;
  color: rgba(255, 255, 255, 0.94);
}

/* DeepSeek 式逐字交互：鼠标掠过字符放大发光 */
.title-char {
  display: inline-block;
  cursor: default;
  transition:
    transform 0.35s cubic-bezier(0.22, 1, 0.36, 1),
    color 0.3s,
    text-shadow 0.3s;
}

.title-char:hover {
  transform: translateY(-10px) scale(1.18) rotate(-2deg);
  color: #fff;
  text-shadow:
    0 0 18px rgba(196, 181, 253, 0.95),
    0 0 46px rgba(139, 92, 246, 0.75);
}

.cosmic-subtitle {
  margin: 0 0 20px;
  font-size: 17px;
  color: rgba(237, 233, 254, 0.78);
}

.cosmic-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.cosmic-tag {
  padding: 8px 22px;
  border-radius: 999px;
  border: 1px solid rgba(196, 181, 253, 0.4);
  background: rgba(124, 58, 237, 0.18);
  color: #ddd6fe;
  font-size: 17px;
  font-weight: 500;
  backdrop-filter: blur(4px);
}

/* ========== 右侧星球场景 ========== */
.globe-scene {
  position: relative;
  flex-shrink: 0;
  width: 400px;
  height: 400px;
  transition: transform 0.25s ease-out;
}

.planet-glow {
  position: absolute;
  inset: 12%;
  border-radius: 50%;
  background: radial-gradient(
    circle,
    rgba(167, 139, 250, 0.5) 0%,
    rgba(124, 58, 237, 0.28) 45%,
    transparent 72%
  );
  filter: blur(6px);
  animation: glowPulse 4.5s ease-in-out infinite;
}

@keyframes glowPulse {
  0%,
  100% {
    opacity: 0.75;
    transform: scale(0.98);
  }
  50% {
    opacity: 1;
    transform: scale(1.05);
  }
}

.globe-svg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.spin-slow {
  transform-origin: 200px 200px;
  animation: spin 48s linear infinite;
}

.spin-reverse {
  transform-origin: 200px 200px;
  animation: spin 72s linear infinite reverse;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 星球中心的发光文字 */
.planet-title {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  pointer-events: none;
}

.planet-char {
  font-size: 76px;
  font-weight: 900;
  background: linear-gradient(180deg, #f5f3ff 10%, #d8b4fe 55%, #a855f7 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 0 14px rgba(168, 85, 247, 0.85))
    drop-shadow(0 0 40px rgba(139, 92, 246, 0.55));
  animation: titlePulse 3.6s ease-in-out infinite;
}

.planet-char:last-child {
  animation-delay: 0.4s;
}

@keyframes titlePulse {
  0%,
  100% {
    filter: drop-shadow(0 0 10px rgba(168, 85, 247, 0.7))
      drop-shadow(0 0 28px rgba(139, 92, 246, 0.45));
    transform: translateY(0);
  }
  50% {
    filter: drop-shadow(0 0 20px rgba(196, 181, 253, 1))
      drop-shadow(0 0 54px rgba(139, 92, 246, 0.8));
    transform: translateY(-4px);
  }
}

/* 悬浮功能图块 */
.tile {
  position: absolute;
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #e9d5ff;
  background: linear-gradient(145deg, rgba(139, 92, 246, 0.75), rgba(91, 33, 182, 0.65));
  border: 1px solid rgba(196, 181, 253, 0.45);
  box-shadow:
    0 8px 28px rgba(76, 29, 149, 0.55),
    inset 0 1px 0 rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(4px);
  animation: float 6s ease-in-out infinite;
}

.tile svg {
  width: 26px;
  height: 26px;
}

.tile-1 {
  left: -6px;
  top: 34%;
}

.tile-2 {
  right: -10px;
  top: 18%;
}

.tile-3 {
  right: 2%;
  bottom: 10%;
}

.tile-4 {
  left: 10%;
  bottom: 2%;
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0) rotate(-2deg);
  }
  50% {
    transform: translateY(-14px) rotate(2deg);
  }
}

/* ========== 响应式 ========== */
@media (max-width: 900px) {
  .cosmic-inner {
    flex-direction: column;
    text-align: center;
    padding: 48px 20px 40px;
  }

  .cosmic-title {
    justify-content: center;
  }

  .cosmic-tags {
    justify-content: center;
  }

  .globe-scene {
    width: 300px;
    height: 300px;
  }

  .planet-char {
    font-size: 56px;
  }

  .tile {
    width: 42px;
    height: 42px;
  }
}
</style>
