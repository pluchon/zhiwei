<template>
  <div class="app-container admin-list-page repairer-availability-page">
    <el-card shadow="never" class="admin-list-card">
      <el-form class="filter-form" label-width="72px" @submit.prevent>
        <div class="filter-row-single">
          <div class="filter-grid filter-grid--inline">
            <el-form-item label="接单状态">
              <el-select v-model="query.acceptingState" clearable placeholder="请选择">
                <el-option
                  v-for="item in acceptingStateOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </div>
          <div class="filter-actions">
            <el-button type="primary" @click="search">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
          </div>
        </div>
      </el-form>

      <el-table
        v-loading="loading"
        :data="data.records"
        stripe
        class="admin-data-table"
        table-layout="auto"
        size="small"
      >
        <el-table-column prop="repairerId" label="师傅 ID" width="88" align="center" />
        <el-table-column prop="repairerRealName" label="师傅姓名" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.repairerRealName || "-" }}</template>
        </el-table-column>
        <el-table-column label="接单状态" width="108" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.acceptingState === 'PAUSED' ? 'warning' : 'success'"
              size="small"
            >
              {{ row.acceptingStateLabel || acceptingStateText(row.acceptingState) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pauseReason" label="暂停原因" min-width="180" show-overflow-tooltip />
        <el-table-column prop="expectedResumeTime" label="预计恢复时间" width="168" show-overflow-tooltip />
      </el-table>
      <pagination
        v-show="data.total"
        :total="data.total"
        v-model:page="query.pageNum"
        v-model:limit="query.pageSize"
        @pagination="load"
      />
    </el-card>
  </div>
</template>

<script setup src="./index.js"></script>
<style scoped lang="scss" src="../common/admin-list.scss"></style>
<style scoped lang="scss" src="./index.scss"></style>
