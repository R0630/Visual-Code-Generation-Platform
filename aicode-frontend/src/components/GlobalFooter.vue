<template>
  <div class="global-footer">
    <p v-for="(line, index) in copyrightLines" :key="index">{{ line }}</p>
  </div>
</template>

<script setup lang="ts">
const copyrightLines = [
  '© 2026 我自己，保留所有权利（反正也没人看）',
  '本代码由咖啡驱动，bug 由熬夜产生',
  '作者很懒，只写了这一行版权',
  '代码能跑就行，别问为什么',
  '如果觉得有用，请我喝杯咖啡；如果觉得没用，也请我喝杯咖啡',
]
</script>

<style scoped>
.global-footer {
  text-align: center;
  color: rgba(0, 0, 0, 0.45);
  font-size: 13px;
  line-height: 1.8;
}

.global-footer p {
  margin: 0;
}
</style>
