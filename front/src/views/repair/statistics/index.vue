<template>
  <div class="app-container repairer-statistics-page">
    <div class="admin-toolbar">
      <p class="admin-toolbar__meta">{{ statisticsRangeText(rangeType) }}</p>
      <el-select v-model="rangeType" style="width: 150px" @change="onRangeChange">
        <el-option
          v-for="item in statisticsRangeOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
    </div>

    <el-skeleton v-if="loading && !stats" :rows="6" animated />

    <template v-else-if="stats">
      <el-row :gutter="16" class="summary-row">
        <el-col :xs="12" :sm="6">
          <article class="summary-card">
            <span>接单数</span>
            <strong>{{ stats.acceptCount ?? 0 }}</strong>
          </article>
        </el-col>
        <el-col :xs="12" :sm="6">
          <article class="summary-card">
            <span>完成数</span>
            <strong>{{ stats.completedCount ?? 0 }}</strong>
          </article>
        </el-col>
        <el-col :xs="12" :sm="6">
          <article class="summary-card">
            <span>处理中</span>
            <strong>{{ stats.processingCount ?? 0 }}</strong>
          </article>
        </el-col>
        <el-col :xs="12" :sm="6">
          <article class="summary-card">
            <span>接单状态</span>
            <strong class="summary-card__state">
              {{ stats.acceptingStateLabel || acceptingStateText(stats.acceptingState) }}
            </strong>
          </article>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :lg="14" :xs="24">
          <el-card shadow="never" class="section-card">
            <template #header><b>工作维度雷达图</b></template>
            <CampusChart :option="radarChartOption" height="340px" />
          </el-card>
        </el-col>
        <el-col :lg="10" :xs="24">
          <el-card shadow="never" class="section-card">
            <template #header><b>完成率</b></template>
            <CampusChart :option="gaugeChartOption" height="340px" />
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="never" class="section-card">
        <template #header><b>效率明细</b></template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="统计范围">
            {{ statisticsRangeText(rangeType) }}
          </el-descriptions-item>
          <el-descriptions-item label="平均首次处理">
            {{ formatMinutes(stats.avgFirstProcessMinutes) }}
          </el-descriptions-item>
          <el-descriptions-item label="平均完成时长">
            {{ formatMinutes(stats.avgCompletionMinutes) }}
          </el-descriptions-item>
          <el-descriptions-item v-if="stats.pauseReason" label="暂停原因">
            {{ stats.pauseReason }}
          </el-descriptions-item>
          <el-descriptions-item v-if="stats.expectedResumeTime" label="预计恢复">
            {{ stats.expectedResumeTime }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>
    </template>
  </div>
</template>

<script setup src="./index.js"></script>
<style scoped lang="scss" src="../../admin/asset/index.scss"></style>
<style scoped lang="scss" src="./index.scss"></style>
