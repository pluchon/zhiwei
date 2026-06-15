<template>
  <div class="dashboard-page statistics-page">
    <div class="statistics-toolbar">
      <p v-if="stats" class="statistics-toolbar__meta">{{ rangeMeta }}</p>
      <p v-else class="statistics-toolbar__meta">管理统计</p>
      <div class="statistics-toolbar__actions">
        <el-radio-group v-model="rangeType" @change="onRangeChange">
          <el-radio-button
            v-for="item in statisticsRangeOptions"
            :key="item.value"
            :value="item.value"
          >{{ item.label }}</el-radio-button>
        </el-radio-group>
        <el-button type="success" :loading="exporting" @click="handleExport">导出统计</el-button>
      </div>
    </div>

    <el-skeleton v-if="loading && !stats" :rows="8" animated />

    <template v-else-if="stats">
      <el-row :gutter="12" v-loading="loading" class="kpi-row">
        <el-col
          v-for="item in kpiCards"
          :key="item.title"
          :xs="24"
          :sm="8"
        >
          <article class="kpi-card kpi-card--static" :class="item.tone ? `kpi-card--${item.tone}` : ''">
            <div class="kpi-card__label">{{ item.title }}</div>
            <div class="kpi-card__value">{{ item.value }}</div>
          </article>
        </el-col>
      </el-row>

      <el-row :gutter="12" v-loading="loading" class="chart-row">
        <el-col :xs="24" :lg="12">
          <el-card shadow="never" class="chart-card">
            <template #header>
              <div class="chart-card__head">
                <div class="chart-card__title">
                  <b>未完成工单趋势图</b>
                  <el-tooltip content="统计范围内每日结束时仍未完成的工单数量变化" placement="top">
                    <span class="chart-tag">{{ rangeLabel }}</span>
                  </el-tooltip>
                </div>
              </div>
            </template>
            <CampusChart :option="unfinishedTrendOption" height="340px" />
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="12">
          <el-card shadow="never" class="chart-card">
            <template #header>
              <div class="chart-card__head">
                <div class="chart-card__title">
                  <b>资产分类报修占比</b>
                  <el-tooltip content="统计范围内各资产分类的报修数量占比" placement="top">
                    <span class="chart-tag">{{ rangeLabel }}</span>
                  </el-tooltip>
                </div>
              </div>
            </template>
            <CampusChart :option="categoryChartOption" height="340px" />
          </el-card>
        </el-col>
        <el-col :xs="24">
          <el-card shadow="never" class="chart-card">
            <template #header>
              <div class="chart-card__head">
                <div class="chart-card__title">
                  <b>维修次数较多资产 TOP</b>
                  <el-tooltip content="统计范围内报修次数排名靠前的资产" placement="top">
                    <span class="chart-tag">TOP 8</span>
                  </el-tooltip>
                </div>
              </div>
            </template>
            <CampusChart :option="topAssetChartOption" height="340px" />
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="never" class="chart-card data-card">
        <template #header>
          <div class="chart-card__head">
            <b>维修次数较多资产明细</b>
          </div>
        </template>
        <el-table
          :data="stats.topRepairedAssets || []"
          stripe
          class="admin-data-table"
          table-layout="auto"
          size="small"
        >
          <el-table-column prop="assetNo" label="资产编号" min-width="148" show-overflow-tooltip />
          <el-table-column prop="enabledDate" label="启用日期" width="112">
            <template #default="{ row }">{{ row.enabledDate || "-" }}</template>
          </el-table-column>
          <el-table-column prop="assetName" label="资产名称" min-width="120" show-overflow-tooltip />
          <el-table-column prop="assetCategoryName" label="资产分类" min-width="128" show-overflow-tooltip>
            <template #default="{ row }">{{ row.assetCategoryName || "-" }}</template>
          </el-table-column>
          <el-table-column prop="repairCount" label="维修次数" width="88" align="center" />
          <el-table-column label="状态" width="96" align="center">
            <template #default="{ row }">
              <el-tag
                v-if="row.status"
                :type="assetStatusType(row.status)"
                size="small"
              >
                {{ row.statusLabel || assetStatusText(row.status) }}
              </el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" plain @click="openRepairHistory(row)">
                查看维修历史
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <el-dialog
      v-model="historyVisible"
      width="680px"
      destroy-on-close
      class="history-dialog"
      :show-header="false"
    >
      <div v-loading="historyLoading" class="history-list">
        <p v-if="!historyLoading && !history.records.length" class="history-empty">暂无维修记录</p>
        <p
          v-for="row in history.records"
          :key="row.orderId || row.orderNo"
          class="history-line"
        >
          {{ formatHistoryLine(row) }}
        </p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup src="./index.js"></script>
<style scoped lang="scss" src="./index.scss"></style>
