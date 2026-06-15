<template>
  <div class="app-container admin-list-page asset-import-detail">
    <el-card v-loading="batchLoading" shadow="never" class="admin-list-card summary-card">
      <div v-if="batch" class="batch-summary">
        <div class="batch-summary-main">
          <span class="batch-file-name">{{ batch.fileName }}</span>
          <span class="batch-meta">上传人 {{ batch.operatorName || "-" }}</span>
        </div>
        <div class="batch-stats">
          <div class="stat-item">
            <span class="stat-label">总数</span>
            <span class="stat-value">{{ batch.totalCount }}</span>
          </div>
          <div class="stat-item stat-item--pending">
            <span class="stat-label">待审核</span>
            <span class="stat-value">{{ batch.pendingCount }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">已确认</span>
            <span class="stat-value">{{ batch.confirmedCount }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">已忽略</span>
            <span class="stat-value">{{ batch.ignoredCount }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">来源</span>
            <span class="stat-value stat-value--text">{{ batch.sourceTypeLabel || "-" }}</span>
          </div>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="admin-list-card">
      <el-form class="filter-form" label-width="72px" @submit.prevent>
        <div class="filter-row-single detail-filter-row">
          <div class="filter-grid filter-grid--inline detail-filter-grid">
            <el-form-item label="卡片状态">
              <el-select v-model="query.status" clearable placeholder="全部">
                <el-option
                  v-for="item in importItemStatusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </div>
          <div class="filter-actions">
            <el-button type="primary" @click="search">查询</el-button>
            <el-button @click="goBack">返回列表</el-button>
            <el-button
              type="danger"
              plain
              :disabled="!batch || batch.confirmedCount > 0"
              @click="handleDeleteBatch"
            >
              删除批次
            </el-button>
            <el-button
              type="success"
              :loading="confirming"
              :disabled="!batch || !batch.pendingCount"
              @click="handleBatchConfirm"
            >
              批量确认
            </el-button>
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
        <el-table-column prop="rowNumber" label="行号" width="70" align="center" />
        <el-table-column prop="assetName" label="资产名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="categoryText" label="分类文本" width="120" show-overflow-tooltip />
        <el-table-column prop="purchaseDate" label="购入日期" width="120" />
        <el-table-column prop="enabledDate" label="启用日期" width="120" />
        <el-table-column prop="aiRecognizeStatusLabel" label="AI 识别" width="100" />
        <el-table-column prop="locationText" label="位置文本" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="importItemStatusType(row.status)" size="small">
              {{ row.statusLabel || importItemStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="duplicateHint" label="疑似重复" min-width="140" show-overflow-tooltip />
        <el-table-column prop="failureReason" label="失败原因" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <div v-if="row.status === 'PENDING'" class="table-actions">
              <el-button size="small" type="primary" plain @click="openEdit(row)">编辑</el-button>
              <el-button
                size="small"
                type="success"
                plain
                :loading="confirming"
                @click="handleConfirm(row)"
              >
                确认
              </el-button>
              <el-button size="small" type="warning" plain @click="handleIgnore(row)">忽略</el-button>
            </div>
            <span v-else class="readonly-hint">只读</span>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="data.total"
        :total="data.total"
        v-model:page="query.pageNum"
        v-model:limit="query.pageSize"
        @pagination="loadItems"
      />
    </el-card>

    <el-dialog v-model="editDialog" title="编辑待审核卡片" width="640px" destroy-on-close class="edit-item-dialog">
      <el-form label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="资产名称" required>
              <el-input v-model="editForm.assetName" maxlength="100" placeholder="请输入资产名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资产分类" required>
              <el-select v-model="editForm.assetCategoryId" style="width: 100%" placeholder="请选择分类">
                <el-option
                  v-for="c in categories"
                  :key="c.assetCategoryId"
                  :label="c.categoryName"
                  :value="c.assetCategoryId"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="购入日期">
              <el-date-picker
                v-model="editForm.purchaseDate"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="启用日期">
              <el-date-picker
                v-model="editForm.enabledDate"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="识别位置文本">
          <el-input v-model="editForm.locationText" maxlength="200" placeholder="AI 识别或原始位置描述" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="校区" required>
              <el-select v-model="editForm.campusId" style="width: 100%" @change="onFormCampusChange">
                <el-option
                  v-for="campus in locations"
                  :key="campus.campusId"
                  :label="campus.campusName"
                  :value="campus.campusId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="楼栋">
              <el-select
                v-model="editForm.buildingId"
                clearable
                style="width: 100%"
                placeholder="选填"
                :disabled="!editForm.campusId"
              >
                <el-option
                  v-for="building in formBuildingOptions"
                  :key="building.buildingId"
                  :label="building.buildingName"
                  :value="building.buildingId"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="楼层">
              <el-input v-model="editForm.floor" maxlength="50" placeholder="如 2楼" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="教室或房间">
              <el-input v-model="editForm.room" maxlength="100" placeholder="如 205" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="位置补充说明">
          <el-input v-model="editForm.locationDetail" maxlength="200" placeholder="选填" />
        </el-form-item>
        <el-form-item label="资产说明">
          <el-input
            v-model="editForm.assetDescription"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 4 }"
            resize="none"
            maxlength="1000"
            placeholder="选填，简要说明用途"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="confirmResultVisible" title="确认结果" width="520px">
      <template v-if="confirmResult">
        <p>成功 {{ confirmResult.successCount }} 条，失败 {{ confirmResult.failureCount }} 条</p>
        <el-table
          v-if="confirmResult.failures?.length"
          :data="confirmResult.failures"
          size="small"
          class="failure-table"
        >
          <el-table-column prop="itemId" label="卡片 ID" width="100" />
          <el-table-column prop="reason" label="失败原因" min-width="200" />
        </el-table>
      </template>
      <template #footer>
        <el-button type="primary" @click="confirmResultVisible = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup src="./detail.js"></script>
<style scoped lang="scss" src="../common/admin-list.scss"></style>
<style scoped lang="scss" src="./detail.scss"></style>
