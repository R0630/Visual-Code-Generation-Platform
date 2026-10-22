<template>
  <div class="global-footer">
    <!-- 版权与署名：方括号内的内容请替换为你自己的信息 -->
    <p class="footer-line">【© 2026 我自己，保留所有权利（反正也没人看）】</p>
    <p class="footer-line">【本代码由咖啡驱动，bug 由熬夜产生,代码能跑就行，别问为什么】</p>
    <p class="footer-line footer-muted">
      【如果觉得有用，请我喝杯咖啡；如果觉得没用，也请我喝杯咖啡】
    </p>
  </div>
</template>

<script setup lang="ts">
// 无需额外的响应式数据
</script>

<style scoped>
.global-footer {
  text-align: center;
  color: var(--ink-500);
  font-size: 13px;
  line-height: 1.9;
}

.global-footer p {
  margin: 0;
}

.footer-muted {
  color: var(--ink-300);
  font-size: 12px;
}
</style>
