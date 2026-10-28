<template>
  <div class="merged-dashboard">
    <!-- 深度驾驶舱入口：仅在原有仪表盘内容上方增加入口，不改动下方任何模块 -->
    <div class="cockpit-entry">
      <div class="ce-left">
        <div class="ce-icon">
          <el-icon :size="22"><Odometer /></el-icon>
        </div>
        <div class="ce-text">
          <div class="ce-title">进入深度数据驾驶舱</div>
          <div class="ce-sub">全屏沉浸式实时数据流 · 经营与人力一屏监控</div>
        </div>
      </div>
      <el-button class="ce-btn" :icon="Right" @click="enterCockpit">立即进入</el-button>
    </div>

    <!-- 上半部分：经营 / 营收分析（原营业分析页，原样嵌入） -->
    <section class="dash-section">
      <BusinessAnalysis />
    </section>

    <!-- 分隔 -->
    <div class="dash-divider"></div>

    <!-- 下半部分：人力资源分析 -->
    <section class="dash-section">
      <!-- 1. 原智能分析页（核心指标 / 部门分布 / 员工结构 / 流失预测 / 薪资 / 绩效 / 考勤 / 雷达 / 入离职趋势 / AI 洞察），原样嵌入 -->
      <Analytics />
      <!-- 2. 快捷入口 + 最新动态（页面收尾） -->
      <div class="dash-inner-divider"></div>
      <HrOverview />
    </section>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { Odometer, Right } from '@element-plus/icons-vue'
import BusinessAnalysis from '@/views/business/BusinessAnalysis.vue'
import HrOverview from '@/views/dashboard/HrOverview.vue'
import Analytics from '@/views/analytics/Analytics.vue'

const router = useRouter()
const enterCockpit = () => router.push('/cockpit')
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

/* 深度驾驶舱入口条 */
.cockpit-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: 16px 20px 0;
  padding: 14px 20px;
  border-radius: 12px;
  background: linear-gradient(100deg, #071021 0%, #0d2340 55%, #0b3155 100%);
  border: 1px solid rgba(56, 189, 248, 0.28);
  box-shadow: 0 8px 24px rgba(8, 18, 34, 0.18);
  cursor: pointer;
  transition: transform 0.25s, box-shadow 0.25s, border-color 0.25s;

  &:hover {
    transform: translateY(-2px);
    border-color: rgba(34, 211, 238, 0.6);
    box-shadow: 0 12px 30px rgba(34, 211, 238, 0.22);
  }
}

.ce-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.ce-icon {
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  border-radius: 10px;
  color: #22d3ee;
  background: rgba(34, 211, 238, 0.12);
  border: 1px solid rgba(34, 211, 238, 0.4);
  box-shadow: 0 0 18px rgba(34, 211, 238, 0.3);
}

.ce-title {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 1.5px;
  color: #e6f6ff;
}

.ce-sub {
  margin-top: 3px;
  font-size: 12px;
  color: #7b8ca6;
}

.ce-btn {
  flex-shrink: 0;
  height: 34px;
  padding: 0 20px;
  font-weight: 600;
  color: #050b18;
  background: linear-gradient(90deg, #22d3ee, #38bdf8);
  border: none;
  border-radius: 8px;

  &:hover {
    background: linear-gradient(90deg, #38bdf8, #60d6f5);
    color: #050b18;
  }
}

@media (max-width: 768px) {
  .cockpit-entry { margin: 12px 12px 0; padding: 12px 14px; }
  .ce-sub { display: none; }
  .ce-title { font-size: 14px; }
}
</style>
