<template>
  <div class="app-container" :class="{ 'repair-list--reporter': reporter }">
    <el-card shadow="never">
      <el-form v-if="!(available && pausedBlocked)" class="filter-form" label-width="88px">
        <div class="filter-grid">
          <el-form-item label="工单编号">
            <el-input
              v-model="query.orderNoSuffix"
              clearable
              class="order-no-input"
              placeholder="输入编号后缀"
              @input="onOrderNoInput"
              @paste="onOrderNoPaste"
              @keyup.enter="search"
            >
              <template #prepend>{{ orderNoPrefix }}</template>
            </el-input>
          </el-form-item>
          <el-form-item label="标题关键词">
            <el-input
              v-model="query.titleKeyword"
              clearable
              placeholder="支持模糊匹配"
              @keyup.enter="search"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.status" clearable @change="search">
              <el-option
                v-for="(s, i) in repairStatuses"
                :key="s"
                :label="s"
                :value="i"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="故障类型">
            <el-select v-model="query.categoryId" clearable @change="search">
              <el-option
                v-for="c in categories"
                :key="c.categoryId"
                :label="c.categoryName"
                :value="c.categoryId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="校区">
            <el-select v-model="query.campusId" clearable @change="onCampusChange">
              <el-option
                v-for="campus in locations"
                :key="campus.campusId"
                :label="campus.campusName"
                :value="campus.campusId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="楼栋">
            <el-select
              v-model="query.buildingId"
              clearable
              :disabled="!query.campusId"
              @change="search"
            >
              <el-option
                v-for="building in buildingOptions"
                :key="building.buildingId"
                :label="building.buildingName"
                :value="building.buildingId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="创建时间" class="filter-span-2">
            <el-date-picker
              v-model="createDateRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              @change="onCreateDateChange"
            />
          </el-form-item>
        </div>

        <div v-if="isAdmin && advancedVisible" class="filter-grid filter-grid--advanced">
          <el-form-item label="完成时间" class="filter-span-2">
            <el-date-picker
              v-model="completionDateRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              @change="onCompletionDateChange"
            />
          </el-form-item>
          <el-form-item label="报修人">
            <el-input
              v-model="query.reporterKeyword"
              clearable
              placeholder="账号或姓名"
              @keyup.enter="search"
            />
          </el-form-item>
          <el-form-item label="维修师傅">
            <el-input
              v-model="query.repairerKeyword"
              clearable
              placeholder="账号或姓名"
              @keyup.enter="search"
            />
          </el-form-item>
          <el-form-item label="资产编号">
            <el-input v-model="query.assetNo" clearable @keyup.enter="search" />
          </el-form-item>
          <el-form-item label="资产名称">
            <el-input
              v-model="query.assetNameKeyword"
              clearable
              @keyup.enter="search"
            />
          </el-form-item>
          <el-form-item label="疑似重复">
            <el-select v-model="query.suspectedDuplicate" clearable @change="search">
              <el-option label="是" :value="1" />
              <el-option label="否" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item label="已导出">
            <el-select v-model="query.exportedFlag" clearable @change="search">
              <el-option
                v-for="item in exportedFlagOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="长时间未进展" class="filter-long-stagnant">
            <el-select v-model="query.longStagnant" clearable @change="search">
              <el-option label="是" :value="true" />
            </el-select>
          </el-form-item>
        </div>

        <div v-if="isAdmin" class="filter-actions">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
          <el-button v-if="!available" @click="enterExportMode">导出工单</el-button>
          <el-button link type="primary" class="filter-more-btn" @click="toggleAdvanced">
            {{ advancedVisible ? "收起筛选" : "更多筛选" }}
            <el-icon class="filter-more-btn__icon" :class="{ 'is-open': advancedVisible }">
              <ArrowDown />
            </el-icon>
          </el-button>
        </div>
      </el-form>

      <div v-if="exportMode" class="export-bar">
        <div class="export-bar__main">
          <span class="export-bar__count">已选 {{ selectedCount }} 条</span>
          <el-button size="small" class="export-btn-select-page" @click="selectCurrentPage">
            全选本页
          </el-button>
          <el-button size="small" class="export-btn-clear" @click="clearSelection">
            清空已选
          </el-button>
          <el-button
            type="primary"
            size="small"
            :loading="exporting"
            :disabled="!data.total"
            @click="exportFiltered"
          >
            一键导出（{{ data.total }} 条）
          </el-button>
          <el-button
            type="success"
            size="small"
            :loading="exporting"
            :disabled="!selectedCount"
            @click="exportSelected"
          >
            导出已选
          </el-button>
        </div>
        <el-button size="small" class="export-btn-cancel" @click="exitExportMode">取消</el-button>
      </div>

      <div
        v-if="quickFilters.length && !(available && pausedBlocked)"
        class="list-toolbar"
      >
        <div class="list-toolbar__filters">
          <span>快捷筛选：</span>
          <el-check-tag
            v-for="item in quickFilters"
            :key="item.value"
            :checked="query.quickFilter === item.value"
            @change="applyQuickFilter(item.value)"
          >
            {{ item.label }}
          </el-check-tag>
        </div>
        <div class="list-toolbar__actions">
          <el-button v-if="reporter" type="success" @click="createRepair">新增报修</el-button>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </div>
      </div>

      <el-alert
        v-if="available && pausedBlocked"
        type="warning"
        show-icon
        class="list-error"
        :closable="false"
      >
        <template #title>
          当前已暂停接单（{{ acceptingStateText(availability?.acceptingState) }}）
        </template>
        <p v-if="availability?.pauseReason">暂停原因：{{ availability.pauseReason }}</p>
        <p v-if="availability?.expectedResumeTime">预计恢复：{{ availability.expectedResumeTime }}</p>
        <p>暂停期间无法查看待接工单，请前往个人中心恢复接单。</p>
      </el-alert>

      <el-alert v-else-if="error" type="error" :title="error" show-icon class="list-error">
        <el-button link type="primary" @click="load">重新加载</el-button>
      </el-alert>

      <el-table
        v-if="!(available && pausedBlocked)"
        ref="tableRef"
        v-loading="loading || availabilityLoading"
        :data="data.records"
        row-key="orderId"
        stripe
        table-layout="fixed"
        class="repair-list-table"
        :class="{ 'export-mode': exportMode }"
        @row-click="onRowClick"
        @selection-change="onSelectionChange"
      >
        <el-table-column
          v-if="exportMode && isAdmin"
          type="selection"
          width="48"
          :reserve-selection="true"
        />
        <el-table-column
          prop="orderNo"
          label="工单编号"
          :min-width="reporter ? 200 : 176"
          show-overflow-tooltip
        />
        <el-table-column
          label="报修事项"
          :min-width="reporter ? 280 : 120"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <div class="title-cell">
              <span class="title-cell__text">{{ row.title }}</span>
              <el-tag v-if="row.longStagnant" type="danger" size="small" class="stagnant-tag">
                {{ longStagnantLabel }}
              </el-tag>
              <el-tag v-if="isAdmin && row.suspectedDuplicate === 1" type="warning" size="small" class="stagnant-tag">
                疑似重复
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          prop="campus"
          label="校区"
          :min-width="reporter ? 120 : 88"
          show-overflow-tooltip
        />
        <el-table-column
          v-if="isAdmin"
          prop="assetNoSnapshot"
          label="资产编号"
          width="176"
          class-name="asset-no-column"
        />
        <el-table-column
          v-if="isAdmin"
          prop="assetNameSnapshot"
          label="资产名称"
          min-width="100"
          show-overflow-tooltip
        />
        <el-table-column v-if="isAdmin" label="已导出" width="76" align="center">
          <template #default="{ row }">
            <el-tag :type="row.exportedFlag === 1 ? 'success' : 'info'" size="small">
              {{ exportedFlagText(row.exportedFlag) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="isAdmin" label="报修人" min-width="108" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="user-cell">
              <span>{{ row.reporterRealName || "-" }}</span>
              <span class="sub-text">{{ row.reporterUserNo || "" }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column v-if="isAdmin" label="维修师傅" min-width="108" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="user-cell">
              <span>{{ row.repairerRealName || "-" }}</span>
              <span class="sub-text">{{ row.repairerUserNo || "" }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          label="状态"
          :min-width="reporter ? 100 : 88"
          align="center"
        >
          <template #default="{ row }">
            <el-tag :type="repairStatusType[row.status]">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          v-if="isAdmin"
          label="AI 辅助"
          width="108"
          align="center"
          class-name="ai-action-column"
        >
          <template #default="{ row }">
            <el-button
              v-if="canShowListAiAnalysis(row)"
              link
              type="primary"
              @click.stop="runListAiAnalysis(row, $event)"
            >
              AI 分析
            </el-button>
            <span v-else class="sub-text">-</span>
          </template>
        </el-table-column>
        <el-table-column
          label="创建时间"
          :min-width="reporter ? 180 : 152"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            {{ formatCreateTime(row.createTime) }}
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-if="!(available && pausedBlocked)"
        v-show="data.total"
        :total="data.total"
        v-model:page="query.pageNum"
        v-model:limit="query.pageSize"
        @pagination="load"
      />
    </el-card>

    <el-dialog
      v-model="aiAnalysisVisible"
      :title="`AI 派单分析 · ${aiAnalysisOrderNo}`"
      width="640px"
      destroy-on-close
    >
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="分析仅提供文字建议，不推荐或排序具体维修师傅，也不会自动派单。"
        class="mb16"
      />
      <div v-loading="aiAnalysisLoading">
        <p v-if="aiAnalysisText" class="ai-analysis-text">{{ aiAnalysisText }}</p>
        <p v-else-if="!aiAnalysisLoading" class="sub-text">暂无分析结果</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup src="./list.js"></script>
<style scoped lang="scss" src="./list.scss"></style>
