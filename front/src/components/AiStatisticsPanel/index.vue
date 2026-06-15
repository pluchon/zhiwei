<template>
  <div v-if="preview" class="ai-statistics-panel">
    <button type="button" class="stats-preview-card" @click="detailOpen = true">
      <div class="stats-preview-card__head">
        <strong>{{ preview.type === "dynamic" ? "统计图表" : "统计结果" }}</strong>
        <span class="stats-preview-card__action">查看详情</span>
      </div>
      <p v-if="summary" class="panel-summary">{{ summary }}</p>
      <p v-if="preview.miniCharts?.[0]?.rangeLabel" class="panel-range-label">
        {{ preview.miniCharts[0].rangeLabel }}
      </p>
      <div v-if="preview.kpi?.length" class="stats-kpi-row">
        <span v-for="item in preview.kpi" :key="item.label">
          {{ item.label }} <em>{{ item.value }}</em>
        </span>
      </div>
      <div :class="['stats-mini-grid', preview.type === 'dynamic' ? 'stats-mini-grid--single' : '']">
        <div v-for="item in preview.miniCharts" :key="item.key" class="stats-mini-item">
          <span class="stats-mini-item__label">{{ item.label }}</span>
          <CampusChart :option="item.option" :height="preview.type === 'dynamic' ? '180px' : '88px'" />
        </div>
      </div>
    </button>

    <el-dialog
      v-model="detailOpen"
      title="统计详情"
      width="960px"
      append-to-body
      destroy-on-close
      class="ai-statistics-detail-dialog"
    >
      <p v-if="summary" class="panel-summary panel-summary--detail">{{ summary }}</p>
      <div v-if="preview.kpi?.length" class="stats-kpi-row stats-kpi-row--detail">
        <article v-for="item in preview.kpi" :key="item.label" class="stats-kpi-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </article>
      </div>
      <div class="chart-grid chart-grid--detail">
        <article
          v-for="(chart, index) in detailCharts"
          :key="index"
          :class="['chart-card', chart.span === 24 ? 'chart-card--wide' : '']"
        >
          <header>{{ chart.title }}</header>
          <CampusChart :option="chart.option" :height="chart.height" />
        </article>
      </div>
    </el-dialog>
  </div>
</template>

<script setup src="./index.js"></script>
<style scoped lang="scss" src="./index.scss"></style>
