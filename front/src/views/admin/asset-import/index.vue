<template>
  <div class="app-container admin-list-page asset-import-page">
    <el-card shadow="never" class="admin-list-card">
      <el-form class="filter-form" label-width="88px" @submit.prevent>
        <div class="filter-row-single">
          <div class="filter-grid filter-grid--inline">
            <el-form-item label="文件名">
              <el-input
                v-model="filters.keyword"
                clearable
                placeholder="请输入文件名关键词"
                @keyup.enter="search"
              />
            </el-form-item>
            <el-form-item label="来源">
              <el-select v-model="filters.sourceType" clearable placeholder="全部">
                <el-option
                  v-for="item in sourceTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="上传时间">
              <el-date-picker
                v-model="uploadDateRange"
                type="daterange"
                value-format="YYYY-MM-DD"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
              />
            </el-form-item>
            <el-form-item label="审核状态">
              <el-select v-model="filters.onlyPending" placeholder="全部">
                <el-option
                  v-for="item in pendingFilterOptions"
                  :key="String(item.value)"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </div>
          <div class="filter-actions">
            <el-upload
              :auto-upload="false"
              accept=".xlsx"
              :show-file-list="false"
              :disabled="uploading"
              @change="onUploadChange"
            >
              <el-button type="primary" :loading="uploading && uploadMode === 'excel'">上传 Excel</el-button>
            </el-upload>
            <el-upload
              :auto-upload="false"
              accept="image/*"
              multiple
              :limit="10"
              :show-file-list="false"
              :disabled="uploading"
              @change="onImageUploadChange"
            >
              <el-button type="success" :loading="uploading && uploadMode === 'image'">上传图片</el-button>
            </el-upload>
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
        table-layout="fixed"
      >
        <el-table-column prop="fileName" label="文件名" min-width="180" show-overflow-tooltip />
        <el-table-column prop="sourceTypeLabel" label="来源" width="108" show-overflow-tooltip />
        <el-table-column prop="operatorName" label="上传人" width="120" show-overflow-tooltip />
        <el-table-column label="上传时间" width="168" show-overflow-tooltip>
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="totalCount" label="总数" width="72" align="center" />
        <el-table-column prop="pendingCount" label="待审核" width="80" align="center" />
        <el-table-column prop="confirmedCount" label="已确认" width="80" align="center" />
        <el-table-column prop="ignoredCount" label="已忽略" width="80" align="center" />
        <el-table-column label="操作" width="96" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="openDetail(row)">详情</el-button>
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
  </div>
</template>

<script setup src="./index.js"></script>
<style scoped lang="scss" src="../common/admin-list.scss"></style>
