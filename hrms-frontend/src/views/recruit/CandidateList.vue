<template>
  <div class="page-container">
    <!-- 阶段漏斗：点一下即按阶段筛选 -->
    <el-row :gutter="12" class="stat-row">
      <el-col :xs="12" :sm="8" :md="4" v-for="item in funnel" :key="item.stage">
        <div
          class="stat-card funnel-card"
          :class="{ active: searchForm.stage === item.stage }"
          @click="filterByStage(item.stage)"
        >
          <div class="stat-label">{{ item.name }}</div>
          <div class="stat-value" :style="{ color: stageColor(item.stage) }">{{ item.count }}</div>
        </div>
      </el-col>
    </el-row>

    <el-card shadow="hover">
      <div class="search-bar">
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="关键字">
            <el-input
              v-model="searchForm.keyword"
              placeholder="姓名 / 手机号 / 编号 / 公司"
              clearable
              style="width: 210px"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="应聘岗位">
            <el-select
              v-model="searchForm.requisitionId"
              placeholder="全部岗位"
              clearable
              filterable
              style="width: 190px"
            >
              <el-option
                v-for="r in requisitionOptions"
                :key="r.id"
                :label="`${r.position}（${r.reqNo}）`"
                :value="r.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="来源">
            <el-select v-model="searchForm.source" placeholder="全部" clearable style="width: 130px">
              <el-option v-for="s in sourceOptions" :key="s" :label="s" :value="s" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
            <el-button :icon="Filter" @click="openAdvFilter">
              高级筛选<span v-if="advCount" class="adv-count">{{ advCount }}</span>
            </el-button>
          </el-form-item>
        </el-form>
        <el-button type="primary" :icon="Plus" @click="handleAdd">登记候选人</el-button>
      </div>

      <!-- 已生效的高级筛选条件，可单个移除 -->
      <div v-if="advTags.length" class="adv-tags">
        <span class="adv-tags-label">已筛选</span>
        <el-tag
          v-for="t in advTags"
          :key="t.key"
          closable
          size="small"
          type="info"
          @close="removeAdvTag(t.key)"
        >{{ t.text }}</el-tag>
        <el-button link type="primary" size="small" @click="resetAdvFilter">清空全部</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="candidateNo" label="编号" width="130" />
        <el-table-column prop="name" label="姓名" width="95">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">{{ row.name }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="requisitionPosition" label="应聘岗位" min-width="130" show-overflow-tooltip />
        <el-table-column label="学历 / 院校" min-width="165" show-overflow-tooltip>
          <template #default="{ row }">{{ row.education || '-' }} / {{ row.school || '-' }}</template>
        </el-table-column>
        <el-table-column prop="workYears" label="经验" width="75" align="center">
          <template #default="{ row }">{{ row.workYears || 0 }} 年</template>
        </el-table-column>
        <el-table-column prop="expectSalary" label="期望薪资" width="95" align="center">
          <template #default="{ row }">{{ row.expectSalary ? row.expectSalary + 'K' : '-' }}</template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="100" />
        <el-table-column label="面试" width="72" align="center">
          <template #default="{ row }">{{ row.interviewCount || 0 }} 轮</template>
        </el-table-column>
        <el-table-column prop="stage" label="阶段" width="95" align="center">
          <template #default="{ row }">
            <el-tag :type="stageMap[row.stage]?.type || 'info'" size="small">
              {{ stageMap[row.stage]?.text || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">详情</el-button>
            <el-button
              link
              :type="row.resumeUrl ? 'success' : 'info'"
              @click="openResume(row)"
            >{{ row.resumeUrl ? '简历' : '无简历' }}</el-button>
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-dropdown trigger="click" @command="(cmd) => handleRowCommand(cmd, row)">
              <el-button type="primary" link class="more-btn">
                更多<el-icon class="more-icon"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="stage">调整阶段</el-dropdown-item>
                  <el-dropdown-item
                    v-if="row.stage !== 5 && row.stage !== 6"
                    command="hire"
                  >录用入职</el-dropdown-item>
                  <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="handleSizeChange"
        @current-change="loadData"
      />
    </el-card>

    <!-- 新增 / 编辑候选人 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑候选人' : '登记候选人'"
      width="760px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="11 位手机号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="应聘岗位" prop="requisitionId">
              <el-select v-model="form.requisitionId" placeholder="请选择在招岗位" filterable style="width: 100%">
                <el-option
                  v-for="r in requisitionOptions"
                  :key="r.id"
                  :label="`${r.position}（${r.reqNo}）`"
                  :value="r.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="来源渠道" prop="source">
              <el-select v-model="form.source" placeholder="请选择" style="width: 100%">
                <el-option v-for="s in sourceOptions" :key="s" :label="s" :value="s" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="性别">
              <el-radio-group v-model="form.gender">
                <el-radio :label="1">男</el-radio>
                <el-radio :label="0">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="学历" prop="education">
              <el-select v-model="form.education" style="width: 100%">
                <el-option v-for="e in educationOptions" :key="e" :label="e" :value="e" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="工作年限" prop="workYears">
              <el-input-number v-model="form.workYears" :min="0" :max="50" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="毕业院校">
              <el-input v-model="form.school" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="专业">
              <el-input v-model="form.major" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="当前公司">
              <el-input v-model="form.currentCompany" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="当前职位">
              <el-input v-model="form.currentPosition" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="期望薪资">
              <el-input-number v-model="form.expectSalary" :min="0" :max="999" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="出生日期">
              <el-date-picker
                v-model="form.birthday"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="简历附件">
          <div class="resume-field">
            <div class="resume-field-row">
              <el-upload
                :show-file-list="false"
                :before-upload="beforeResumeUpload"
                :http-request="doResumeUpload"
                accept=".jpg,.jpeg,.png,.gif,.webp,.bmp,.pdf"
              >
                <el-button :icon="Upload" :loading="resumeUploading">
                  {{ form.resumeUrl ? '重新上传' : '上传简历' }}
                </el-button>
              </el-upload>
              <template v-if="form.resumeUrl">
                <el-icon class="resume-field-icon"><Document /></el-icon>
                <span class="resume-field-name">{{ form.resumeName }}</span>
                <span class="resume-field-size">{{ formatFileSize(form.resumeSize) }}</span>
                <el-button link type="primary" @click="previewResumeFile(form)">预览</el-button>
                <el-button link type="danger" @click="clearResume">移除</el-button>
              </template>
            </div>
            <div v-if="!form.resumeUrl" class="resume-field-tip">
              支持 jpg / png / gif / webp / bmp 图片或 pdf，单个文件不超过 10MB
            </div>
          </div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 调整阶段 -->
    <el-dialog v-model="stageVisible" title="调整候选人阶段" width="440px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="候选人">
          <span>{{ currentRow?.name }}（当前：{{ stageMap[currentRow?.stage]?.text }}）</span>
        </el-form-item>
        <el-form-item label="新阶段">
          <el-select v-model="stageForm.stage" style="width: 100%">
            <el-option v-for="s in stageOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="stageForm.stage === 6" label="淘汰原因">
          <el-input v-model="stageForm.reason" type="textarea" :rows="2" placeholder="必填，便于复盘" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stageVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitStage">确定</el-button>
      </template>
    </el-dialog>

    <!-- 录用入职 -->
    <el-dialog v-model="hireVisible" title="录用入职" width="660px" destroy-on-close>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="确认录用后将自动生成员工档案（状态为试用期）并建立试用期跟踪记录，可在「转正管理」里办理转正。"
        style="margin-bottom: 14px"
      />
      <el-form ref="hireFormRef" :model="hireForm" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="候选人">
              <span>{{ currentRow?.name }} · {{ currentRow?.requisitionPosition }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入职日期" prop="entryDate">
              <el-date-picker
                v-model="hireForm.entryDate"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="入职部门" prop="deptId">
              <el-select v-model="hireForm.deptId" style="width: 100%">
                <el-option v-for="d in deptOptions" :key="d.id" :label="d.deptName" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入职岗位" prop="position">
              <el-input v-model="hireForm.position" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="职级">
              <el-input v-model="hireForm.jobLevel" placeholder="如 P5" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="月薪(K)">
              <el-input-number v-model="hireForm.salary" :min="0" :max="999" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="试用期(月)">
              <el-input-number v-model="hireForm.probationMonths" :min="1" :max="12" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="带教导师">
              <el-input v-model="hireForm.mentor" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工作地点">
              <el-input v-model="hireForm.workLocation" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="hireVisible = false">取消</el-button>
        <el-button type="success" :loading="submitting" @click="submitHire">确认录用</el-button>
      </template>
    </el-dialog>

    <!-- 候选人详情 + 面试记录 -->
    <el-drawer v-model="detailVisible" :title="`候选人详情 · ${currentRow?.name || ''}`" size="620px">
      <template v-if="currentRow">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="候选人编号">{{ currentRow.candidateNo }}</el-descriptions-item>
          <el-descriptions-item label="当前阶段">
            <el-tag :type="stageMap[currentRow.stage]?.type" size="small">
              {{ stageMap[currentRow.stage]?.text }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="应聘岗位">{{ currentRow.requisitionPosition || '-' }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ currentRow.deptName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ currentRow.phone }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ currentRow.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="学历">{{ currentRow.education || '-' }}</el-descriptions-item>
          <el-descriptions-item label="院校 / 专业">
            {{ currentRow.school || '-' }} / {{ currentRow.major || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="工作年限">{{ currentRow.workYears || 0 }} 年</el-descriptions-item>
          <el-descriptions-item label="期望薪资">
            {{ currentRow.expectSalary ? currentRow.expectSalary + 'K' : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="当前公司">{{ currentRow.currentCompany || '-' }}</el-descriptions-item>
          <el-descriptions-item label="当前职位">{{ currentRow.currentPosition || '-' }}</el-descriptions-item>
          <el-descriptions-item label="来源">{{ currentRow.source || '-' }}</el-descriptions-item>
          <el-descriptions-item label="淘汰原因">{{ currentRow.rejectReason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="简历">
            <el-button
              v-if="currentRow.resumeUrl"
              link
              type="primary"
              @click="openResume(currentRow)"
            >查看简历（{{ currentRow.resumeType === 'pdf' ? 'PDF' : '图片' }}）</el-button>
            <span v-else class="text-muted">未上传</span>
          </el-descriptions-item>
          <el-descriptions-item label="备注">{{ currentRow.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="drawer-section">
          <div class="drawer-title">
            <span>面试记录</span>
            <el-button
              v-if="currentRow.stage !== 5 && currentRow.stage !== 6"
              type="primary"
              size="small"
              :icon="Plus"
              @click="openInterview"
            >安排面试</el-button>
          </div>
          <el-empty v-if="interviews.length === 0" description="还没有面试记录" :image-size="60" />
          <el-timeline v-else>
            <el-timeline-item
              v-for="item in interviews"
              :key="item.id"
              :timestamp="formatTime(item.interviewTime)"
              placement="top"
              :color="resultColor(item.result)"
            >
              <div class="interview-item">
                <div class="interview-head">
                  <strong>第 {{ item.round }} 轮 · {{ roundText(item.round) }}</strong>
                  <el-tag :type="resultType(item.result)" size="small">{{ resultText(item.result) }}</el-tag>
                </div>
                <div class="interview-sub">
                  面试官：{{ item.interviewer || '-' }}　形式：{{ typeText(item.interviewType) }}
                </div>
                <div class="interview-sub">地点：{{ item.location || '-' }}</div>
                <div class="interview-sub" v-if="item.score !== null && item.score !== undefined">
                  评分：{{ item.score }} 分
                </div>
                <div class="interview-sub" v-if="item.comment">评价：{{ item.comment }}</div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </template>
    </el-drawer>

    <!-- 简历详情：独立抽屉，图片可缩放查看、PDF 内嵌预览、支持下载 -->
    <el-drawer
      v-model="resumeVisible"
      :title="`简历详情 · ${resumeRow?.name || ''}`"
      size="760px"
      destroy-on-close
    >
      <div v-if="resumeRow" class="resume-detail">
        <div class="resume-detail-bar">
          <div class="resume-detail-meta">
            <el-icon class="resume-detail-icon"><Document /></el-icon>
            <div>
              <div class="resume-detail-name">{{ resumeRow.resumeName || '简历' }}</div>
              <div class="resume-detail-sub">
                {{ resumeRow.requisitionPosition || '-' }}
                · {{ formatFileSize(resumeRow.resumeSize) }}
                · {{ resumeRow.resumeType === 'pdf' ? 'PDF' : '图片' }}
              </div>
            </div>
          </div>
          <div class="resume-detail-actions">
            <el-button :icon="Download" :loading="resumeDownloading" @click="downloadResume(resumeRow)">
              下载
            </el-button>
            <el-button :icon="Refresh" @click="loadResumePreview(resumeRow)">重新加载</el-button>
          </div>
        </div>

        <div v-loading="resumeLoading" class="resume-detail-body">
          <template v-if="resumeBlobUrl">
            <el-image
              v-if="resumeRow.resumeType !== 'pdf'"
              :src="resumeBlobUrl"
              :preview-src-list="[resumeBlobUrl]"
              :initial-index="0"
              fit="contain"
              class="resume-image"
            />
            <iframe v-else :src="resumeBlobUrl" class="resume-pdf"></iframe>
          </template>
          <el-empty
            v-else-if="!resumeLoading"
            description="简历文件加载失败或已被移除"
            :image-size="80"
          />
        </div>

        <div class="resume-detail-foot">
          <el-button link type="primary" @click="openDetail(resumeRow)">查看候选人档案</el-button>
          <el-button link type="primary" @click="handleEdit(resumeRow)">编辑并重新上传</el-button>
        </div>
      </div>
    </el-drawer>

    <!-- 高级筛选 -->
    <el-drawer v-model="advVisible" title="高级筛选" size="440px">
      <el-form label-width="96px">
        <el-form-item label="学历">
          <el-select v-model="advForm.education" placeholder="不限" clearable style="width: 100%">
            <el-option v-for="e in educationOptions" :key="e" :label="e" :value="e" />
          </el-select>
        </el-form-item>
        <el-form-item label="工作年限">
          <div class="range-row">
            <el-input-number v-model="advForm.workYearsMin" :min="0" :max="50" :controls="false" placeholder="下限" />
            <span class="range-sep">—</span>
            <el-input-number v-model="advForm.workYearsMax" :min="0" :max="50" :controls="false" placeholder="上限" />
          </div>
        </el-form-item>
        <el-form-item label="期望薪资">
          <div class="range-row">
            <el-input-number v-model="advForm.salaryMin" :min="0" :max="999" :controls="false" placeholder="下限" />
            <span class="range-sep">—</span>
            <el-input-number v-model="advForm.salaryMax" :min="0" :max="999" :controls="false" placeholder="上限" />
          </div>
          <div class="adv-hint">单位：K / 月</div>
        </el-form-item>
        <el-form-item label="录入时间">
          <el-date-picker
            v-model="advForm.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="面试情况">
          <el-radio-group v-model="advForm.hasInterview">
            <el-radio :value="null">不限</el-radio>
            <el-radio :value="1">已安排面试</el-radio>
            <el-radio :value="0">从未安排</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetAdvForm">重置条件</el-button>
        <el-button type="primary" @click="applyAdvFilter">应用筛选</el-button>
      </template>
    </el-drawer>

    <!-- 抽屉里快速安排面试 -->
    <el-dialog v-model="interviewVisible" title="安排面试" width="520px" destroy-on-close append-to-body>
      <el-form ref="interviewFormRef" :model="interviewForm" label-width="90px">
        <el-form-item label="轮次" prop="round">
          <el-select v-model="interviewForm.round" style="width: 100%">
            <el-option label="初试" :value="1" />
            <el-option label="复试" :value="2" />
            <el-option label="终试" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="面试形式" prop="interviewType">
          <el-radio-group v-model="interviewForm.interviewType">
            <el-radio :label="1">现场</el-radio>
            <el-radio :label="2">电话</el-radio>
            <el-radio :label="3">视频</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="面试时间" prop="interviewTime">
          <el-date-picker
            v-model="interviewForm.interviewTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="时长(分钟)">
          <el-input-number v-model="interviewForm.duration" :min="15" :max="240" :step="15" style="width: 100%" />
        </el-form-item>
        <el-form-item label="面试官" prop="interviewer">
          <el-input v-model="interviewForm.interviewer" placeholder="多人用逗号分隔" />
        </el-form-item>
        <el-form-item label="地点" prop="location">
          <el-input v-model="interviewForm.location" placeholder="会议室 / 线上会议链接" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="interviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitInterview">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, Refresh, Plus, Edit, Delete,
  Upload, Document, Download, Filter, ArrowDown
} from '@element-plus/icons-vue'
import { uploadFileApi, createFileBlobUrlApi, downloadFileApi } from '@/api/file'
import {
  getCandidatePageApi,
  getCandidateFunnelApi,
  addCandidateApi,
  updateCandidateApi,
  deleteCandidateApi,
  changeCandidateStageApi,
  hireCandidateApi,
  getRequisitionListApi,
  getInterviewsByCandidateApi,
  addInterviewApi
} from '@/api/recruit'
import { getDepartmentListApi } from '@/api/department'

const route = useRoute()

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const funnel = ref([])
const requisitionOptions = ref([])
const deptOptions = ref([])
const interviews = ref([])

const dialogVisible = ref(false)
const stageVisible = ref(false)
const hireVisible = ref(false)
const detailVisible = ref(false)
const interviewVisible = ref(false)
// 简历详情 + 高级筛选
const resumeVisible = ref(false)
const resumeRow = ref(null)
const resumeBlobUrl = ref('')
const resumeLoading = ref(false)
const resumeDownloading = ref(false)
const resumeUploading = ref(false)
const advVisible = ref(false)
const isEdit = ref(false)
const currentRow = ref(null)
const formRef = ref(null)
const hireFormRef = ref(null)
const interviewFormRef = ref(null)

const searchForm = reactive({
  keyword: '',
  requisitionId: null,
  source: null,
  stage: null,
  // 高级筛选条件，和后端 RecruitQueryDTO 一一对应
  education: null,
  workYearsMin: null,
  workYearsMax: null,
  salaryMin: null,
  salaryMax: null,
  createTimeStart: null,
  createTimeEnd: null,
  hasInterview: null
})

const defaultAdvForm = {
  education: null,
  workYearsMin: null,
  workYearsMax: null,
  salaryMin: null,
  salaryMax: null,
  dateRange: [],
  hasInterview: null
}
const advForm = reactive({ ...defaultAdvForm })

const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const stageMap = {
  1: { text: '待筛选', type: 'info' },
  2: { text: '初试', type: 'warning' },
  3: { text: '复试', type: 'primary' },
  4: { text: '待录用', type: 'success' },
  5: { text: '已录用', type: 'success' },
  6: { text: '已淘汰', type: 'danger' }
}

const stageOptions = [
  { value: 1, label: '待筛选' },
  { value: 2, label: '初试' },
  { value: 3, label: '复试' },
  { value: 4, label: '待录用' },
  { value: 5, label: '已录用' },
  { value: 6, label: '已淘汰' }
]

const sourceOptions = ['简历投递', '内部推荐', '猎头', '招聘网站', '校园招聘']
const educationOptions = ['大专', '本科', '硕士', '博士']

const defaultForm = {
  id: null,
  name: '',
  phone: '',
  requisitionId: null,
  source: '简历投递',
  gender: 1,
  education: '本科',
  workYears: 0,
  school: '',
  major: '',
  currentCompany: '',
  currentPosition: '',
  expectSalary: null,
  birthday: '',
  email: '',
  remark: '',
  // 简历附件（由上传接口返回，随表单一起提交）
  resumeUrl: null,
  resumeName: null,
  resumeSize: null,
  resumeType: null
}
const form = reactive({ ...defaultForm })

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^[0-9+\-\s]{6,20}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  requisitionId: [{ required: true, message: '请选择应聘岗位', trigger: 'change' }],
  source: [{ required: true, message: '请选择来源渠道', trigger: 'change' }],
  education: [{ required: true, message: '请选择学历', trigger: 'change' }],
  workYears: [{ required: true, message: '请输入工作年限', trigger: 'blur' }]
}

const stageForm = reactive({ stage: null, reason: '' })
const hireForm = reactive({
  entryDate: '',
  deptId: null,
  position: '',
  jobLevel: '',
  salary: null,
  probationMonths: 3,
  mentor: '',
  workLocation: ''
})
const interviewForm = reactive({
  round: 1,
  interviewType: 1,
  interviewTime: '',
  duration: 60,
  interviewer: '',
  location: ''
})

const stageColor = (stage) => {
  const colors = { 1: '#909399', 2: '#E6A23C', 3: '#409EFF', 4: '#67C23A', 5: '#67C23A', 6: '#F56C6C' }
  return colors[stage] || '#606266'
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getCandidatePageApi({
      ...searchForm,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    tableData.value = res.data.list
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

const loadFunnel = async () => {
  const res = await getCandidateFunnelApi()
  funnel.value = res.data || []
}

const loadOptions = async () => {
  const [reqRes, deptRes] = await Promise.all([getRequisitionListApi(), getDepartmentListApi()])
  requisitionOptions.value = reqRes.data || []
  deptOptions.value = deptRes.data || []
}

const handleSearch = () => { pagination.pageNum = 1; loadData() }
const handleReset = () => {
  searchForm.keyword = ''
  searchForm.requisitionId = null
  searchForm.source = null
  searchForm.stage = null
  clearAdvConditions()
  Object.assign(advForm, { ...defaultAdvForm })
  handleSearch()
}
const handleSizeChange = () => { pagination.pageNum = 1; loadData() }

const filterByStage = (stage) => {
  searchForm.stage = searchForm.stage === stage ? null : stage
  handleSearch()
}

const handleAdd = () => {
  Object.assign(form, defaultForm)
  isEdit.value = false
  dialogVisible.value = true
}

const handleEdit = (row) => {
  Object.assign(form, defaultForm, row)
  isEdit.value = true
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateCandidateApi(form)
      ElMessage.success('更新成功')
    } else {
      await addCandidateApi(form)
      ElMessage.success('登记成功')
    }
    dialogVisible.value = false
    loadData()
    loadFunnel()
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除候选人「${row.name}」吗？其面试记录会一并删除。`, '提示', { type: 'warning' })
  await deleteCandidateApi(row.id)
  ElMessage.success('删除成功')
  loadData()
  loadFunnel()
}

/** 操作列「更多」下拉 */
const handleRowCommand = (cmd, row) => {
  if (cmd === 'stage') openStage(row)
  else if (cmd === 'hire') openHire(row)
  else if (cmd === 'delete') handleDelete(row)
}

// ========== 简历附件 ==========
const formatFileSize = (size) => {
  const n = Number(size || 0)
  if (!n) return '-'
  if (n < 1024) return n + ' B'
  if (n < 1024 * 1024) return (n / 1024).toFixed(1) + ' KB'
  return (n / 1024 / 1024).toFixed(1) + ' MB'
}

/** 上传前置校验：类型与大小，与后端保持一致 */
const beforeResumeUpload = (file) => {
  if (!/\.(jpe?g|png|gif|webp|bmp|pdf)$/i.test(file.name)) {
    ElMessage.warning('只支持 jpg / png / gif / webp / bmp 图片或 pdf')
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('文件不能超过 10MB')
    return false
  }
  return true
}

const doResumeUpload = async ({ file }) => {
  resumeUploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    const res = await uploadFileApi(fd)
    form.resumeUrl = res.data.path
    form.resumeName = res.data.name
    form.resumeSize = res.data.size
    form.resumeType = res.data.type
    ElMessage.success('简历已上传，点「确定」后保存')
  } catch (e) {
    ElMessage.error('简历上传失败')
  } finally {
    resumeUploading.value = false
  }
}

const clearResume = () => {
  form.resumeUrl = null
  form.resumeName = null
  form.resumeSize = null
  form.resumeType = null
}

const releaseResumeUrl = () => {
  if (resumeBlobUrl.value) {
    URL.revokeObjectURL(resumeBlobUrl.value)
    resumeBlobUrl.value = ''
  }
}

/** 打开简历详情抽屉 */
const openResume = (row) => {
  if (!row || !row.resumeUrl) {
    ElMessage.warning('该候选人还没有上传简历')
    if (row) handleEdit(row)
    return
  }
  resumeRow.value = row
  resumeVisible.value = true
  loadResumePreview(row)
}

/** 简历以 blob 方式读取：接口需要 token，不能直接当 img src 用 */
const loadResumePreview = async (row) => {
  const target = row || resumeRow.value
  if (!target || !target.resumeUrl) return
  resumeLoading.value = true
  releaseResumeUrl()
  try {
    resumeBlobUrl.value = await createFileBlobUrlApi(target.resumeUrl)
  } catch (e) {
    ElMessage.error('简历加载失败，请确认文件是否还在')
  } finally {
    resumeLoading.value = false
  }
}

const downloadResume = async (row) => {
  const target = row || resumeRow.value
  if (!target || !target.resumeUrl) return
  resumeDownloading.value = true
  try {
    await downloadFileApi(target.resumeUrl, target.resumeName)
  } catch (e) {
    ElMessage.error('下载失败')
  } finally {
    resumeDownloading.value = false
  }
}

/** 表单里刚上传、还没保存时也能先预览 */
const previewResumeFile = (target) => {
  if (!target || !target.resumeUrl) return
  resumeRow.value = target
  resumeVisible.value = true
  loadResumePreview(target)
}

// ========== 高级筛选 ==========
// 标签只反映「已生效」的条件（searchForm），抽屉里的草稿不算
const advTags = computed(() => {
  const tags = []
  if (searchForm.education) {
    tags.push({ key: 'education', text: '学历：' + searchForm.education })
  }
  if (searchForm.workYearsMin != null || searchForm.workYearsMax != null) {
    const min = searchForm.workYearsMin
    const max = searchForm.workYearsMax
    let text
    if (max != null) {
      text = '年限：' + (min != null ? min : 0) + '~' + max + ' 年'
    } else {
      text = '年限：' + (min != null ? min : 0) + ' 年以上'
    }
    tags.push({ key: 'workYears', text })
  }
  if (searchForm.salaryMin != null || searchForm.salaryMax != null) {
    const min = searchForm.salaryMin
    const max = searchForm.salaryMax
    tags.push({
      key: 'salary',
      text: '期望薪资：' + (min != null ? min : 0) + '~' + (max != null ? max : '不限') + ' K'
    })
  }
  if (searchForm.createTimeStart && searchForm.createTimeEnd) {
    tags.push({
      key: 'createTime',
      text: '录入：' + searchForm.createTimeStart + ' 至 ' + searchForm.createTimeEnd
    })
  }
  if (searchForm.hasInterview === 1) tags.push({ key: 'hasInterview', text: '已安排面试' })
  if (searchForm.hasInterview === 0) tags.push({ key: 'hasInterview', text: '从未安排面试' })
  return tags
})
const advCount = computed(() => advTags.value.length)

const openAdvFilter = () => {
  // 回填已生效条件，便于微调而不是从零填
  advForm.education = searchForm.education
  advForm.workYearsMin = searchForm.workYearsMin
  advForm.workYearsMax = searchForm.workYearsMax
  advForm.salaryMin = searchForm.salaryMin
  advForm.salaryMax = searchForm.salaryMax
  advForm.hasInterview = searchForm.hasInterview
  advForm.dateRange = (searchForm.createTimeStart && searchForm.createTimeEnd)
    ? [searchForm.createTimeStart, searchForm.createTimeEnd]
    : []
  advVisible.value = true
}

const applyAdvFilter = () => {
  if (advForm.workYearsMin != null && advForm.workYearsMax != null
      && advForm.workYearsMin > advForm.workYearsMax) {
    ElMessage.warning('工作年限的下限不能大于上限')
    return
  }
  if (advForm.salaryMin != null && advForm.salaryMax != null
      && advForm.salaryMin > advForm.salaryMax) {
    ElMessage.warning('期望薪资的下限不能大于上限')
    return
  }
  searchForm.education = advForm.education || null
  searchForm.workYearsMin = advForm.workYearsMin
  searchForm.workYearsMax = advForm.workYearsMax
  searchForm.salaryMin = advForm.salaryMin
  searchForm.salaryMax = advForm.salaryMax
  searchForm.hasInterview = advForm.hasInterview
  const range = advForm.dateRange || []
  searchForm.createTimeStart = range[0] || null
  searchForm.createTimeEnd = range[1] || null
  advVisible.value = false
  handleSearch()
}

const resetAdvForm = () => {
  Object.assign(advForm, { ...defaultAdvForm, dateRange: [] })
}

const clearAdvConditions = () => {
  searchForm.education = null
  searchForm.workYearsMin = null
  searchForm.workYearsMax = null
  searchForm.salaryMin = null
  searchForm.salaryMax = null
  searchForm.createTimeStart = null
  searchForm.createTimeEnd = null
  searchForm.hasInterview = null
}

const resetAdvFilter = () => {
  clearAdvConditions()
  Object.assign(advForm, { ...defaultAdvForm, dateRange: [] })
  handleSearch()
}

const removeAdvTag = (key) => {
  if (key === 'education') searchForm.education = null
  if (key === 'workYears') {
    searchForm.workYearsMin = null
    searchForm.workYearsMax = null
  }
  if (key === 'salary') {
    searchForm.salaryMin = null
    searchForm.salaryMax = null
  }
  if (key === 'createTime') {
    searchForm.createTimeStart = null
    searchForm.createTimeEnd = null
  }
  if (key === 'hasInterview') searchForm.hasInterview = null
  handleSearch()
}

onUnmounted(releaseResumeUrl)

const openStage = (row) => {
  currentRow.value = row
  stageForm.stage = row.stage
  stageForm.reason = ''
  stageVisible.value = true
}

const submitStage = async () => {
  if (!stageForm.stage) {
    ElMessage.warning('请选择新阶段')
    return
  }
  if (stageForm.stage === 6 && !stageForm.reason.trim()) {
    ElMessage.warning('淘汰候选人必须填写原因')
    return
  }
  submitting.value = true
  try {
    await changeCandidateStageApi(currentRow.value.id, { stage: stageForm.stage, reason: stageForm.reason })
    ElMessage.success('阶段已更新')
    stageVisible.value = false
    loadData()
    loadFunnel()
  } finally {
    submitting.value = false
  }
}

const openHire = (row) => {
  currentRow.value = row
  Object.assign(hireForm, {
    entryDate: new Date().toISOString().slice(0, 10),
    deptId: null,
    position: row.requisitionPosition || '',
    jobLevel: '',
    salary: row.expectSalary || null,
    probationMonths: 3,
    mentor: '',
    workLocation: ''
  })
  // 部门默认取该岗位所属部门
  const req = requisitionOptions.value.find((r) => r.id === row.requisitionId)
  if (req) hireForm.deptId = req.deptId
  hireVisible.value = true
}

const submitHire = async () => {
  if (!hireForm.entryDate) {
    ElMessage.warning('请选择入职日期')
    return
  }
  if (!hireForm.deptId) {
    ElMessage.warning('请选择入职部门')
    return
  }
  if (!hireForm.position) {
    ElMessage.warning('请填写入职岗位')
    return
  }
  submitting.value = true
  try {
    const res = await hireCandidateApi(currentRow.value.id, { ...hireForm })
    ElMessage.success(`已录用，员工ID：${res.data}，请到「转正管理」跟踪试用期`)
    hireVisible.value = false
    loadData()
    loadFunnel()
  } finally {
    submitting.value = false
  }
}

const openDetail = async (row) => {
  currentRow.value = row
  detailVisible.value = true
  const res = await getInterviewsByCandidateApi(row.id)
  interviews.value = res.data || []
}

const openInterview = () => {
  Object.assign(interviewForm, {
    round: (currentRow.value?.interviewCount || 0) + 1 > 3 ? 3 : (currentRow.value?.interviewCount || 0) + 1,
    interviewType: 1,
    interviewTime: '',
    duration: 60,
    interviewer: '',
    location: ''
  })
  interviewVisible.value = true
}

const submitInterview = async () => {
  if (!interviewForm.interviewTime) {
    ElMessage.warning('请选择面试时间')
    return
  }
  if (!interviewForm.interviewer.trim()) {
    ElMessage.warning('请填写面试官')
    return
  }
  if (!interviewForm.location.trim()) {
    ElMessage.warning('请填写面试地点')
    return
  }
  submitting.value = true
  try {
    await addInterviewApi({
      candidateId: currentRow.value.id,
      requisitionId: currentRow.value.requisitionId,
      ...interviewForm
    })
    ElMessage.success('面试已安排')
    interviewVisible.value = false
    const res = await getInterviewsByCandidateApi(currentRow.value.id)
    interviews.value = res.data || []
    loadData()
    loadFunnel()
  } finally {
    submitting.value = false
  }
}

const roundText = (round) => ({ 1: '初试', 2: '复试', 3: '终试' }[round] || '面试')
const typeText = (type) => ({ 1: '现场', 2: '电话', 3: '视频' }[type] || '现场')
const resultText = (result) => ({ 0: '待面试', 1: '通过', 2: '未通过', 3: '爽约' }[result] || '待面试')
const resultType = (result) => ({ 0: 'info', 1: 'success', 2: 'danger', 3: 'warning' }[result] || 'info')
const resultColor = (result) => ({ 0: '#909399', 1: '#67C23A', 2: '#F56C6C', 3: '#E6A23C' }[result] || '#909399')
const formatTime = (time) => (time ? String(time).replace('T', ' ').slice(0, 16) : '-')

onMounted(async () => {
  // 从招聘需求页点「候选人」跳过来时，默认只看该岗位
  if (route.query.requisitionId) {
    searchForm.requisitionId = Number(route.query.requisitionId)
  }
  await loadOptions()
  await Promise.all([loadData(), loadFunnel()])
})
</script>

<style scoped>
.page-container { width: 100%; }
.stat-row { margin-bottom: 12px; }
.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 14px 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  margin-bottom: 12px;
}
.funnel-card { cursor: pointer; transition: all 0.2s; border: 1px solid transparent; }
.funnel-card:hover { transform: translateY(-2px); }
.funnel-card.active { border-color: #409eff; box-shadow: 0 2px 10px rgba(64, 158, 255, 0.25); }
.stat-label { font-size: 13px; color: #909399; }
.stat-value { font-size: 24px; font-weight: 700; line-height: 1.4; }
.search-bar { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; }
.pagination { margin-top: 16px; justify-content: flex-end; }
.drawer-section { margin-top: 18px; }
.drawer-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  margin-bottom: 12px;
}
.interview-item { font-size: 13px; }
.interview-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.interview-sub { color: #606266; line-height: 1.7; }

/* ===== 简历上传 ===== */
.resume-field-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.resume-field-icon { color: #909399; }
.resume-field-name {
  font-size: 13px; color: #303133; max-width: 220px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.resume-field-size { font-size: 12px; color: #909399; }
.resume-field-tip { font-size: 12px; color: #909399; line-height: 1.6; margin-top: 4px; }

/* ===== 高级筛选 ===== */
.adv-count {
  display: inline-block; min-width: 16px; height: 16px; padding: 0 4px;
  margin-left: 4px; border-radius: 8px; background: #f56c6c; color: #fff;
  font-size: 11px; line-height: 16px; text-align: center;
}
.adv-tags { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 12px; }
.adv-tags-label { font-size: 12px; color: #909399; }
.range-row { display: flex; align-items: center; gap: 8px; width: 100%; }
.range-row :deep(.el-input-number) { flex: 1; width: auto; }
.range-sep { color: #c0c4cc; }
.adv-hint { font-size: 12px; color: #909399; line-height: 1.6; }

/* ===== 简历详情 ===== */
.resume-detail { display: flex; flex-direction: column; height: 100%; }
.resume-detail-bar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 14px; background: #f5f7fa; border-radius: 6px; margin-bottom: 12px;
}
.resume-detail-meta { display: flex; align-items: center; gap: 10px; min-width: 0; }
.resume-detail-icon { font-size: 26px; color: #409eff; }
.resume-detail-name {
  font-size: 14px; font-weight: 600; color: #303133; max-width: 300px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.resume-detail-sub { font-size: 12px; color: #909399; margin-top: 2px; }
.resume-detail-actions { display: flex; gap: 8px; flex: none; }
.resume-detail-body {
  flex: 1; min-height: 420px; display: flex; align-items: center; justify-content: center;
  border: 1px solid #ebeef5; border-radius: 6px; background: #fafafa; overflow: auto;
}
.resume-image { max-width: 100%; max-height: 620px; }
.resume-pdf { width: 100%; height: 640px; border: none; }
.resume-detail-foot { display: flex; gap: 16px; margin-top: 12px; justify-content: flex-end; }
.text-muted { color: #c0c4cc; }
.more-icon { margin-left: 2px; }
</style>
