<template>
  <div class="app-container asset-category-page">
    <el-card shadow="never" class="category-card" v-loading="loading">
      <template #header>
        <div class="card-head card-head--actions-only">
          <el-button type="primary" @click="openCreate">新增分类</el-button>
        </div>
      </template>

      <el-form class="filter-form" label-width="88px" @submit.prevent>
        <div class="filter-row">
          <div class="filter-grid">
            <el-form-item label="分类名称">
              <el-input
                v-model="filters.keyword"
                clearable
                placeholder="请输入分类名称"
                @keyup.enter="search"
              />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="filters.status" clearable placeholder="全部状态">
                <el-option
                  v-for="item in assetCategoryStatusOptions"
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
            <el-button @click="enterExportMode">导出分类</el-button>
          </div>
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

      <el-table
        ref="tableRef"
        :data="data.records"
        row-key="assetCategoryId"
        stripe
        :class="{ 'export-mode': exportMode }"
        @selection-change="onSelectionChange"
      >
        <el-table-column
          v-if="exportMode"
          type="selection"
          width="48"
          :reserve-selection="true"
        />
        <el-table-column prop="categoryName" label="分类名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
              {{ assetCategoryStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column v-if="!exportMode" label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button
              v-if="row.status === 1"
              link
              type="success"
              @click="handleEnable(row)"
            >启用</el-button>
            <el-button
              v-if="row.status === 0"
              link
              type="warning"
              @click="handleDisable(row)"
            >停用</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="data.total"
        :total="data.total"
        v-model:page="query.pageNum"
        v-model:limit="query.pageSize"
        @pagination="load"
      />
    </el-card>

    <el-dialog v-model="dialog" :title="dialogTitle" width="480px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="分类名称">
          <el-input
            v-model="form.categoryName"
            maxlength="100"
            placeholder="请输入分类名称，如多媒体设备"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup src="./index.js"></script>
<style scoped lang="scss" src="./index.scss"></style>
