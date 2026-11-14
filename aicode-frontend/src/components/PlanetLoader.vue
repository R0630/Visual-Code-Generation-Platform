<template>
  <div
    class="planet-loader"
    :style="{ width: `${size}px`, height: `${size}px` }"
    role="img"
    aria-label="加载中"
  >
    <!-- 外圈反向旋转的虚线轨道 -->
    <div class="planet-orbit"></div>
    <!-- 底部光晕脉冲 -->
    <div class="planet-halo"></div>
    <!-- 星球本体：持续自转 -->
    <img :src="planetImg" class="planet-img" alt="星球加载动画" draggable="false" />
  </div>
</template>

<script setup lang="ts">
import planetImg from '@/assets/decor/planet.png'

withDefaults(
  defineProps<{
    /** 直径（像素） */
    size?: number
  }>(),
  { size: 96 },
)
</script>

<style scoped>
.planet-loader {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.planet-img {
  position: relative;
  z-index: 2;
  width: 72%;
  height: 72%;
  object-fit: contain;
  animation: planet-spin 9s linear infinite;
  filter: drop-shadow(0 0 10px rgba(168, 85, 247, 0.65))
    drop-shadow(0 0 28px rgba(139, 92, 246, 0.4));
  user-select: none;
}

.planet-orbit {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 1.5px dashed rgba(167, 139, 250, 0.55);
  animation: planet-spin 14s linear infinite reverse;
}

.planet-orbit::after {
  content: '';
  position: absolute;
  top: -4px;
  left: 50%;
  width: 7px;
  height: 7px;
  margin-left: -3.5px;
  border-radius: 50%;
  background: #e9d5ff;
  box-shadow: 0 0 8px rgba(233, 213, 255, 0.95);
}

.planet-halo {
  position: absolute;
  inset: 12%;
  border-radius: 50%;
  background: radial-gradient(
    circle,
    rgba(167, 139, 250, 0.45) 0%,
    rgba(124, 58, 237, 0.22) 50%,
    transparent 72%
  );
  filter: blur(4px);
  animation: planet-pulse 3.2s ease-in-out infinite;
}

@keyframes planet-spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes planet-pulse {
  0%,
  100% {
    opacity: 0.65;
    transform: scale(0.96);
  }
  50% {
    opacity: 1;
    transform: scale(1.06);
  }
}

@media (prefers-reduced-motion: reduce) {
  .planet-img,
  .planet-orbit,
  .planet-halo {
    animation-duration: 30s;
  }
}
</style>
