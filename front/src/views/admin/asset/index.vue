<template>
  <div class="app-container asset-page">
    <el-card shadow="never" class="asset-card">
      <el-form class="filter-form" label-width="88px" @submit.prevent>
        <div class="filter-grid filter-grid--primary">
          <el-form-item label="资产编号">
            <el-input
              v-model="query.assetNoSuffix"
              clearable
              class="asset-no-input"
              placeholder="输入编号后缀"
              @input="onAssetNoInput"
              @paste="onAssetNoPaste"
              @keyup.enter="search"
            >
              <template #prepend>AST-</template>
            </el-input>
          </el-form-item>
          <el-form-item label="资产名称">
            <el-input
              v-model="query.assetNameKeyword"
              clearable
              placeholder="请输入资产名称"
              @keyup.enter="search"
            />
          </el-form-item>
          <el-form-item label="分类">
            <el-select v-model="query.assetCategoryId" clearable placeholder="全部">
              <el-option
                v-for="c in categories"
                :key="c.assetCategoryId"
                :label="c.categoryName"
                :value="c.assetCategoryId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="校区">
            <el-select v-model="query.campusId" clearable placeholder="全部" @change="onCampusChange">
              <el-option
                v-for="campus in locations"
                :key="campus.campusId"
                :label="campus.campusName"
                :value="campus.campusId"
              />
            </el-select>
          </el-form-item>
        </div>

        <div class="filter-row-secondary">
          <div class="filter-grid filter-grid--secondary">
            <el-form-item label="楼栋">
              <el-select
                v-model="query.buildingId"
                clearable
                placeholder="全部"
                :disabled="!query.campusId"
              >
                <el-option
                  v-for="building in buildingOptions"
                  :key="building.buildingId"
                  :label="building.buildingName"
                  :value="building.buildingId"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="query.status" clearable placeholder="全部">
                <el-option
                  v-for="item in assetStatusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item class="filter-checkbox-item">
              <el-checkbox v-model="query.includeDeleted">含已删除</el-checkbox>
            </el-form-item>
          </div>
          <div class="filter-actions">
            <el-button type="success" @click="openCreate">新增资产</el-button>
            <el-button type="primary" @click="search">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
          </div>
        </div>
      </el-form>

      <el-table
        v-loading="loading"
        :data="data.records"
        stripe
        class="asset-ledger-table"
        table-layout="fixed"
      >
        <el-table-column prop="assetNo" label="资产编号" width="172" show-overflow-tooltip />
        <el-table-column prop="assetName" label="资产名称" min-width="110" show-overflow-tooltip />
        <el-table-column prop="assetCategoryName" label="分类" width="132" show-overflow-tooltip />
        <el-table-column label="位置" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ formatAssetLocation(row) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="88" align="center">
          <template #default="{ row }">
            <el-tag :type="assetStatusType(row.status)" size="small">
              {{ row.statusLabel || assetStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="删除状态" width="92" align="center">
          <template #default="{ row }">
            <el-tag :type="row.deleteState === 0 ? 'success' : 'info'" size="small">
              {{ row.deleteState === 0 ? "正常" : "已删除" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button size="small" type="primary" plain @click="openDetail(row)">详情</el-button>
              <el-button
                v-if="row.deleteState === 0 && !row.hasActiveOrder"
                size="small"
                type="primary"
                plain
                @click="openEdit(row)"
              >编辑</el-button>
              <el-button
                v-if="row.deleteState === 0 && row.status === 'OUT_OF_SERVICE' && !row.hasActiveOrder"
                size="small"
                type="success"
                plain
                @click="handleEnable(row)"
              >启用</el-button>
              <el-button
                v-if="row.deleteState === 0 && row.status === 'IN_USE' && !row.hasActiveOrder"
                size="small"
                type="warning"
                plain
                @click="handleDisable(row)"
              >停用</el-button>
              <el-button
                v-if="row.deleteState === 0 && !row.hasActiveOrder"
                size="small"
                type="danger"
                plain
                @click="handleDelete(row)"
              >删除</el-button>
              <el-button
                v-if="row.deleteState === 1"
                size="small"
                type="primary"
                plain
                @click="handleRestore(row)"
              >恢复</el-button>
            </div>
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

    <el-dialog v-model="dialog" :title="dialogTitle" width="640px" destroy-on-close>
      <el-form label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="资产名称" required>
              <el-input v-model="form.assetName" maxlength="100" placeholder="请输入资产名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资产分类" required>
              <el-select v-model="form.assetCategoryId" style="width: 100%" placeholder="请选择分类">
                <el-option
                  v-for="c in enabledCategories"
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
            <el-form-item label="校区" required>
              <el-select v-model="form.campusId" style="width: 100%" placeholder="请选择校区" @change="onFormCampusChange">
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
                v-model="form.buildingId"
                clearable
                style="width: 100%"
                placeholder="选填"
                :disabled="!form.campusId"
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
              <el-input v-model="form.floor" maxlength="50" placeholder="如 2楼" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="教室或房间">
              <el-input v-model="form.room" maxlength="100" placeholder="如 205" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="启用日期">
              <el-date-picker
                v-model="form.enabledDate"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="购入日期">
              <el-date-picker
                v-model="form.purchaseDate"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="位置补充说明">
          <el-input v-model="form.locationDetail" maxlength="200" placeholder="选填" />
        </el-form-item>
        <el-form-item label="资产说明">
          <el-input
            v-model="form.description"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 6 }"
            resize="none"
            maxlength="500"
            placeholder="选填，简要说明用途"
          />
        </el-form-item>
        <el-form-item label="资产图片（最多一张）">
          <el-upload
            :auto-upload="false"
            accept="image/*"
            :limit="1"
            :file-list="imageFiles"
            list-type="picture-card"
            @change="onImageChange"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <el-image
            v-if="form.imageSignedUrl && !imageFiles.length"
            :src="form.imageSignedUrl"
            class="preview-image"
            fit="cover"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="detailVisible"
      width="720px"
      destroy-on-close
      class="asset-detail-dialog"
    >
      <template v-if="detail">
        <el-descriptions :column="2" border class="detail-base">
          <el-descriptions-item label="资产编号">{{ detail.assetNo }}</el-descriptions-item>
          <el-descriptions-item label="资产名称">{{ detail.assetName }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ detail.assetCategoryName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="assetStatusType(detail.status)" size="small">
              {{ detail.statusLabel || assetStatusText(detail.status) }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <div class="detail-extra" :class="{ 'has-image': !!detail.imageSignedUrl }">
          <div class="detail-extra-row">
            <span class="detail-extra-label">位置</span>
            <span class="detail-extra-value">{{ formatAssetLocation(detail) }}</span>
          </div>
          <div class="detail-extra-row">
            <span class="detail-extra-label">说明</span>
            <span class="detail-extra-value">{{ detail.description || "暂无" }}</span>
          </div>
          <el-image
            v-if="detail.imageSignedUrl"
            :src="detail.imageSignedUrl"
            class="detail-thumb"
            fit="contain"
            :preview-src-list="[detail.imageSignedUrl]"
            preview-teleported
          />
        </div>

        <el-divider content-position="center">维修历史</el-divider>
        <el-table
          v-loading="historyLoading"
          :data="history.records"
          size="small"
          class="detail-history-table"
          table-layout="fixed"
        >
          <el-table-column prop="orderNo" label="工单编号" show-overflow-tooltip />
          <el-table-column prop="categoryName" label="故障类型" show-overflow-tooltip />
          <el-table-column prop="repairerRealName" label="维修师傅" show-overflow-tooltip />
          <el-table-column prop="repairResult" label="维修结果" show-overflow-tooltip />
          <el-table-column prop="completionTime" label="完成时间" show-overflow-tooltip />
        </el-table>
        <pagination
          v-show="history.total"
          :total="history.total"
          v-model:page="historyQuery.pageNum"
          v-model:limit="historyQuery.pageSize"
          @pagination="loadHistory"
        />
      </template>
    </el-dialog>
  </div>
</template>

<script setup src="./index.js"></script>
<style scoped lang="scss" src="./index.scss"></style>
