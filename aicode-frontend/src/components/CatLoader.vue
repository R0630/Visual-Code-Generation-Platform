<template>
  <div class="cat-loader" role="img" aria-label="AI 正在努力工作中">
    <svg class="cat-svg" viewBox="0 0 320 240" aria-hidden="true">
      <!-- 漂浮的代码符号 -->
      <text class="sym sym-1" x="30" y="72">&lt;/&gt;</text>
      <text class="sym sym-2" x="256" y="88">{ }</text>
      <text class="sym sym-3" x="44" y="176">✦</text>
      <text class="sym sym-4" x="262" y="168">★</text>

      <!-- 尾巴：左右摇摆 -->
      <path
        class="cat-tail"
        d="M212 186 q46 8 50 -28 q2 -20 -14 -24"
        fill="none"
        stroke="#161031"
        stroke-width="14"
        stroke-linecap="round"
      />

      <!-- 身体 -->
      <ellipse cx="158" cy="172" rx="54" ry="44" fill="#161031" />

      <!-- 头部（轻微起伏） -->
      <g class="cat-head">
        <!-- 耳朵 -->
        <polygon points="120,74 110,32 148,58" fill="#161031" />
        <polygon points="196,74 206,32 168,58" fill="#161031" />
        <polygon points="126,66 121,45 141,59" fill="#f0abfc" opacity="0.55" />
        <polygon points="190,66 195,45 175,59" fill="#f0abfc" opacity="0.55" />
        <!-- 脸 -->
        <circle cx="158" cy="100" r="42" fill="#161031" />
        <!-- 眼睛（周期性眨眼） -->
        <g class="cat-eyes">
          <ellipse cx="144" cy="98" rx="6.5" ry="9" fill="#a7f3d0" />
          <ellipse cx="172" cy="98" rx="6.5" ry="9" fill="#a7f3d0" />
        </g>
        <!-- 腮红 -->
        <ellipse cx="132" cy="114" rx="7" ry="4" fill="#7c3aed" opacity="0.5" />
        <ellipse cx="184" cy="114" rx="7" ry="4" fill="#7c3aed" opacity="0.5" />
        <!-- 嘴 -->
        <path d="M152 118 q6 5 12 0" fill="none" stroke="#c4b5fd" stroke-width="2" stroke-linecap="round" />
        <!-- 胡须 -->
        <path
          d="M120 106 h-24 M120 114 l-22 7 M196 106 h24 M196 114 l22 7"
          stroke="#c4b5fd"
          stroke-width="1.6"
          stroke-linecap="round"
          opacity="0.65"
        />
      </g>

      <!-- 笔记本电脑（背面朝向观众） -->
      <g class="laptop">
        <rect x="108" y="152" width="104" height="58" rx="8" fill="#2e1a5e" stroke="#7c3aed" stroke-width="1.5" />
        <circle cx="160" cy="172" r="9" fill="none" stroke="#c4b5fd" stroke-width="2" opacity="0.9" />
        <rect class="laptop-progress" x="126" y="192" width="68" height="6" rx="3" fill="#4c1d95" />
        <rect class="laptop-progress-bar" x="126" y="192" width="68" height="6" rx="3" fill="#a78bfa" />
      </g>

      <!-- 敲键盘的两只前爪（交替起落） -->
      <ellipse class="paw paw-l" cx="138" cy="156" rx="13" ry="9" fill="#161031" />
      <ellipse class="paw paw-r" cx="182" cy="156" rx="13" ry="9" fill="#161031" />
    </svg>
    <p v-if="text" class="cat-text">{{ text }}<span class="cat-dots"><i></i><i></i><i></i></span></p>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    /** 动画下方的提示文字 */
    text?: string
  }>(),
  { text: '' },
)
</script>

<style scoped>
.cat-loader {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.cat-svg {
  width: 200px;
  max-width: 100%;
  height: auto;
}

.cat-text {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--purple-600, #7c3aed);
}

/* 省略号跳动 */
.cat-dots {
  display: inline-flex;
  gap: 3px;
  margin-left: 4px;
}

.cat-dots i {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: currentColor;
  animation: dot-bounce 1.2s ease-in-out infinite;
}

.cat-dots i:nth-child(2) {
  animation-delay: 0.15s;
}

.cat-dots i:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes dot-bounce {
  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.5;
  }
  30% {
    transform: translateY(-4px);
    opacity: 1;
  }
}

/* 头部轻微起伏 */
.cat-head {
  animation: head-bob 2.4s ease-in-out infinite;
  transform-box: fill-box;
  transform-origin: center;
}

@keyframes head-bob {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-3px);
  }
}

/* 眨眼 */
.cat-eyes {
  animation: eye-blink 3.6s ease-in-out infinite;
  transform-box: fill-box;
  transform-origin: center;
}

@keyframes eye-blink {
  0%,
  42%,
  50%,
  100% {
    transform: scaleY(1);
  }
  46% {
    transform: scaleY(0.12);
  }
}

/* 前爪交替敲击 */
.paw {
  transform-box: fill-box;
  transform-origin: center;
  animation: paw-tap 0.4s ease-in-out infinite alternate;
}

.paw-l {
  animation-delay: 0s;
}

.paw-r {
  animation-delay: 0.2s;
}

@keyframes paw-tap {
  from {
    transform: translateY(0);
  }
  to {
    transform: translateY(-6px);
  }
}

/* 尾巴摇摆 */
.cat-tail {
  transform-box: fill-box;
  transform-origin: left center;
  animation: tail-sway 1.8s ease-in-out infinite alternate;
}

@keyframes tail-sway {
  from {
    transform: rotate(-7deg);
  }
  to {
    transform: rotate(9deg);
  }
}

/* 笔记本进度条循环 */
.laptop-progress-bar {
  transform-box: fill-box;
  transform-origin: left center;
  animation: progress-run 1.6s ease-in-out infinite;
}

@keyframes progress-run {
  0% {
    transform: scaleX(0.05);
  }
  70% {
    transform: scaleX(1);
  }
  100% {
    transform: scaleX(0.05);
  }
}

/* 漂浮代码符号 */
.sym {
  font-size: 20px;
  font-weight: 700;
  fill: #a78bfa;
  opacity: 0.85;
  animation: sym-float 3.4s ease-in-out infinite;
  transform-box: fill-box;
  transform-origin: center;
}

.sym-2 {
  animation-delay: 0.6s;
  fill: #c4b5fd;
}

.sym-3 {
  animation-delay: 1.1s;
  fill: #e9d5ff;
}

.sym-4 {
  animation-delay: 1.7s;
  fill: #8b5cf6;
}

@keyframes sym-float {
  0%,
  100% {
    transform: translateY(0);
    opacity: 0.55;
  }
  50% {
    transform: translateY(-9px);
    opacity: 1;
  }
}

@media (prefers-reduced-motion: reduce) {
  .cat-head,
  .cat-eyes,
  .paw,
  .cat-tail,
  .laptop-progress-bar,
  .sym,
  .cat-dots i {
    animation-duration: 4s;
  }
}
</style>
