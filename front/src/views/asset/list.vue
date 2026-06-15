<template>
  <div class="app-container">
    <header class="page-head">
      <div>
        <span>ASSET BROWSE</span>
        <h2>资产浏览</h2>
        <p>查看资产信息与维修历史。</p>
      </div>
    </header>

    <el-card shadow="never">
      <el-form inline class="filter-form">
        <el-form-item label="资产编号">
          <el-input
            v-model="query.assetNoSuffix"
            clearable
            class="asset-no-input"
            style="width: 200px"
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
            style="width: 160px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.assetCategoryId" clearable style="width: 150px" @change="search">
            <el-option
              v-for="c in categories"
              :key="c.assetCategoryId"
              :label="c.categoryName"
              :value="c.assetCategoryId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="校区">
          <el-select v-model="query.campusId" clearable style="width: 150px" @change="onCampusChange">
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
            style="width: 150px"
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
        <el-form-item v-if="isAdminOrRepairer" label="状态">
          <el-select v-model="query.status" clearable style="width: 130px" @change="search">
            <el-option
              v-for="item in assetStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table
        v-loading="loading"
        :data="data.records"
        stripe
        class="asset-browse-table"
        table-layout="fixed"
        @row-click="openDetail"
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
      </el-table>
      <pagination
        v-show="data.total"
        :total="data.total"
        v-model:page="query.pageNum"
        @pagination="load"
      />
    </el-card>

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

<script setup src="./list.js"></script>

<style lang="scss" scoped src="./list.scss"></style>
