<template>
  <div class="merged-dashboard">
    <!-- 上半部分：经营 / 营收分析（原营业分析页，原样嵌入） -->
    <section class="dash-section">
      <BusinessAnalysis />
    </section>

    <!-- 分隔 -->
    <div class="dash-divider"></div>

    <!-- 下半部分：人力资源分析（原智能分析页，原样嵌入） -->
    <section class="dash-section">
      <Analytics />
    </section>
  </div>
</template>

<script setup>
import BusinessAnalysis from '@/views/business/BusinessAnalysis.vue'
import Analytics from '@/views/analytics/Analytics.vue'
</script>

<style scoped lang="scss">
.merged-dashboard {
  min-height: 100%;
  background: #f0f2f5;
}

.dash-section {
  display: block;
}

.dash-divider {
  height: 12px;
  background: #e4e7ed;
  border-top: 1px solid #dcdfe6;
  border-bottom: 1px solid #dcdfe6;
}
</style>
