<template>
  <div class="merged-dashboard">
    <!-- 上半部分：经营 / 营收分析（原营业分析页，原样嵌入） -->
    <section class="dash-section">
      <BusinessAnalysis />
    </section>

    <!-- 分隔 -->
    <div class="dash-divider"></div>

    <!-- 下半部分：人力资源分析 -->
    <section class="dash-section">
      <!-- 1. 原仪表盘的人力图文（统计卡 / 人员结构 / 入离职趋势 / 考勤 / 薪资 / 绩效 / 快捷入口），原样嵌入 -->
      <HrOverview />
      <!-- 2. 原智能分析页（部门分布 / 流失预测 / 人才雷达 / 招聘预测 / AI 洞察），原样嵌入 -->
      <div class="dash-inner-divider"></div>
      <Analytics />
    </section>
  </div>
</template>

<script setup>
import BusinessAnalysis from '@/views/business/BusinessAnalysis.vue'
import HrOverview from '@/views/dashboard/HrOverview.vue'
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

.dash-inner-divider {
  height: 10px;
  background: #f5f7fa;
  border-top: 1px dashed #dcdfe6;
  border-bottom: 1px dashed #dcdfe6;
}
</style>
