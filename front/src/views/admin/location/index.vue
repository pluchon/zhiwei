<template>
  <div class="app-container location-page">
    <el-card shadow="never" class="location-card" v-loading="loading">
      <template #header>
        <div class="card-head">
          <b>校区列表</b>
          <el-button type="primary" @click="openCampusCreate">新增校区</el-button>
        </div>
      </template>

      <el-form class="filter-form" label-width="88px" @submit.prevent>
        <div class="filter-grid">
          <el-form-item label="状态">
            <el-select v-model="campusFilters.status" clearable placeholder="全部状态">
              <el-option
                v-for="item in locationStatusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="删除状态">
            <el-select v-model="campusFilters.deleteState" clearable placeholder="全部状态">
              <el-option
                v-for="item in locationDeleteStateOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </div>
        <div class="filter-actions">
          <el-button type="primary" @click="applyCampusFilters">查询</el-button>
          <el-button @click="resetCampusFilters">重置</el-button>
        </div>
      </el-form>

      <el-table
        ref="campusTableRef"
        :data="displayCampuses"
        row-key="campusId"
        highlight-current-row
        @current-change="selectCampus"
        :row-class-name="({ row }) => row.campusId === selectedCampusId ? 'is-active' : ''"
      >
        <el-table-column prop="campusName" label="校区名称" min-width="120" />
        <el-table-column prop="description" label="说明" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
              {{ locationStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="删除状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.deleteState === 0 ? 'success' : 'info'" size="small">
              {{ locationDeleteStateText(row.deleteState) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="openCampusEdit(row)">编辑</el-button>
            <el-button
              v-if="row.deleteState === 0 && row.status === 1"
              link
              type="success"
              @click.stop="handleEnableCampus(row)"
            >启用</el-button>
            <el-button
              v-if="row.deleteState === 0 && row.status === 0"
              link
              type="warning"
              @click.stop="handleDisableCampus(row)"
            >停用</el-button>
            <el-button
              v-if="row.deleteState === 0"
              link
              type="danger"
              @click.stop="handleDeleteCampus(row)"
            >删除</el-button>
            <el-button
              v-if="row.deleteState === 1"
              link
              type="primary"
              @click.stop="handleRestoreCampus(row)"
            >恢复</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="location-card" v-loading="buildingLoading">
      <template #header>
        <div class="card-head">
          <b>楼栋列表</b>
          <el-button type="primary" plain :disabled="!selectedCampusId" @click="openBuildingCreate">
            新增楼栋
          </el-button>
        </div>
      </template>

      <el-form
        v-if="selectedCampusId"
        class="filter-form"
        label-width="88px"
        @submit.prevent
      >
        <div class="filter-grid">
          <el-form-item label="状态">
            <el-select v-model="buildingFilters.status" clearable placeholder="全部状态">
              <el-option
                v-for="item in locationStatusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="删除状态">
            <el-select v-model="buildingFilters.deleteState" clearable placeholder="全部状态">
              <el-option
                v-for="item in locationDeleteStateOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </div>
        <div class="filter-actions">
          <el-button type="primary" @click="applyBuildingFilters">查询</el-button>
          <el-button @click="resetBuildingFilters">重置</el-button>
        </div>
      </el-form>

      <el-empty v-if="!selectedCampusId" description="请选择校区" />
      <el-table v-else :data="displayBuildings" row-key="buildingId">
        <el-table-column prop="buildingName" label="楼栋名称" min-width="120" />
        <el-table-column prop="description" label="说明" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
              {{ locationStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="删除状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.deleteState === 0 ? 'success' : 'info'" size="small">
              {{ locationDeleteStateText(row.deleteState) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openBuildingEdit(row)">编辑</el-button>
            <el-button
              v-if="row.deleteState === 0 && row.status === 1"
              link
              type="success"
              @click="handleEnableBuilding(row)"
            >启用</el-button>
            <el-button
              v-if="row.deleteState === 0 && row.status === 0"
              link
              type="warning"
              @click="handleDisableBuilding(row)"
            >停用</el-button>
            <el-button
              v-if="row.deleteState === 0"
              link
              type="danger"
              @click="handleDeleteBuilding(row)"
            >删除</el-button>
            <el-button
              v-if="row.deleteState === 1"
              link
              type="primary"
              @click="handleRestoreBuilding(row)"
            >恢复</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog" :title="dialogTitle" width="520px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item v-if="dialogTarget === 'campus'" label="校区名称">
          <el-input v-model="form.campusName" maxlength="100" placeholder="请输入校区名称" />
        </el-form-item>
        <el-form-item v-else label="楼栋名称">
          <el-input v-model="form.buildingName" maxlength="100" placeholder="请输入楼栋名称" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input
            v-model="form.description"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 6 }"
            resize="none"
            maxlength="500"
            placeholder="选填，简要说明用途"
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
