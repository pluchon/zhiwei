<template>
  <div class="repair-create-page" v-loading="loading">
    <el-form label-width="96px" label-position="left" class="create-form">
      <section class="form-section">
        <div class="form-section__head">
          <h3>报修信息</h3>
        </div>
        <div class="form-grid">
          <el-form-item label="报修方式" class="form-grid__full">
            <el-radio-group
              v-model="form.repairType"
              :disabled="isEdit && editOrderStatus > 2"
              @change="onRepairTypeChange"
            >
              <el-radio
                v-for="item in repairTypeOptions"
                :key="item.value"
                :value="item.value"
              >{{ item.label }}</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="报修标题" required>
            <el-input v-model="form.title" maxlength="100" placeholder="简要概括故障" clearable />
          </el-form-item>
          <el-form-item label="故障类型" required>
            <el-select v-model="form.categoryId" clearable placeholder="请选择">
              <el-option
                v-for="c in categories"
                :key="c.categoryId"
                :label="c.categoryName"
                :value="c.categoryId"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="故障描述" required>
            <el-input
              v-model="form.description"
              maxlength="50"
              show-word-limit
              placeholder="例如：教室前排灯不亮"
              clearable
            />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="form.contactPhone" maxlength="20" placeholder="便于联系确认" clearable />
          </el-form-item>
        </div>

        <div v-if="form.categoryId" v-loading="workforceLoading" class="workforce-tip">
          <template v-if="workforce && !workforce.hasRepairer">
            当前故障类型暂无匹配维修师傅，提交后将由管理员安排。
          </template>
          <template v-else-if="workforce">
            当前维修力量：
            <el-tag :type="busyLevelType(workforce.busyLevel)" size="small">
              {{ busyLevelText(workforce.busyLevel, workforce.busyLevelLabel) }}
            </el-tag>
          </template>
        </div>
      </section>

      <section v-if="isAssetRepair" class="form-section">
        <div class="form-section__head">
          <h3>关联资产</h3>
          <el-button
            v-if="canChangeAsset"
            type="primary"
            plain
            size="small"
            @click="openAssetPicker"
          >选择资产</el-button>
        </div>
        <el-empty v-if="!selectedAsset" description="请选择需要报修的资产" :image-size="72" />
        <div v-else class="asset-summary">
          <div>
            <strong>{{ selectedAsset.assetNo }}</strong>
            <span>{{ selectedAsset.assetName }}</span>
            <p class="sub-text">{{ formatAssetLocation(selectedAsset) }}</p>
          </div>
          <el-button v-if="canChangeAsset" link type="danger" @click="clearAsset">清除</el-button>
        </div>
        <el-alert
          v-if="assetConflict"
          type="error"
          :closable="false"
          show-icon
          title="该资产存在未结束关联工单，暂不可提交。"
        />
      </section>

      <section class="form-section">
        <div class="form-section__head">
          <h3>{{ isAssetRepair ? "位置补充" : "现场位置" }}</h3>
        </div>
        <div class="form-grid form-grid--location">
          <el-form-item label="校区" required>
            <el-select v-model="form.campusId" placeholder="请选择" @change="onCampusChange">
              <el-option
                v-for="campus in locations"
                :key="campus.campusId"
                :label="campus.campusName"
                :value="campus.campusId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="楼栋">
            <el-select v-model="form.buildingId" clearable placeholder="可选" :disabled="!form.campusId">
              <el-option
                v-for="building in buildingOptions"
                :key="building.buildingId"
                :label="building.buildingName"
                :value="building.buildingId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="楼层">
            <el-input v-model="form.floor" maxlength="20" placeholder="如 3F" clearable />
          </el-form-item>
          <el-form-item label="教室/房间">
            <el-input v-model="form.room" maxlength="50" placeholder="如 A301" clearable />
          </el-form-item>
          <el-form-item
            class="form-grid__wide"
            :label="form.buildingId ? '位置补充' : '具体位置'"
            :required="!form.buildingId"
          >
            <el-input
              v-model="form.locationDetail"
              maxlength="50"
              show-word-limit
              :placeholder="form.buildingId ? '如需补充说明可填写' : '未选楼栋时请填写'"
              clearable
            />
          </el-form-item>
        </div>

        <p v-if="selectedCampus?.description || selectedBuilding?.description" class="location-note">
          <span v-if="selectedCampus?.description">校区：{{ selectedCampus.description }}</span>
          <span v-if="selectedBuilding?.description">楼栋：{{ selectedBuilding.description }}</span>
        </p>
      </section>

      <section v-if="!isEdit" class="form-section">
        <div class="form-section__head">
          <h3>现场图片</h3>
          <span class="form-section__hint">1 至 5 张</span>
        </div>
        <el-upload
          v-model:file-list="files"
          :auto-upload="false"
          accept="image/*"
          :limit="5"
          list-type="picture-card"
          class="photo-upload"
        >
          <el-icon><Plus /></el-icon>
        </el-upload>
      </section>

      <div class="form-actions">
        <el-button type="primary" size="large" :loading="loading" @click="submit">
          {{ isEdit ? "保存并提交" : "创建并提交" }}
        </el-button>
        <el-button size="large" :loading="loading" @click="saveDraft">保存草稿</el-button>
      </div>
    </el-form>

    <el-dialog v-model="assetPickerVisible" title="选择资产" width="760px" destroy-on-close>
      <el-input
        v-model="assetPickerKeyword"
        placeholder="输入资产编号或名称搜索"
        clearable
        @keyup.enter="searchAssets"
      >
        <template #append>
          <el-button :icon="Search" @click="searchAssets" />
        </template>
      </el-input>
      <el-table
        v-loading="assetPickerLoading"
        :data="assetOptions"
        class="asset-table"
        highlight-current-row
        @row-click="chooseAsset"
      >
        <el-table-column prop="assetNo" label="编号" width="150" />
        <el-table-column prop="assetName" label="名称" min-width="140" />
        <el-table-column label="位置" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ formatAssetLocation(row) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag
              v-if="row.hasActiveOrder && (!isEdit || row.activeOrderId !== Number(editId))"
              type="warning"
              size="small"
            >占用中</el-tag>
            <el-tag
              v-else-if="row.hasActiveOrder"
              type="info"
              size="small"
            >本单占用</el-tag>
            <el-tag v-else type="success" size="small">可选</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup src="./create.js"></script>
<style scoped lang="scss" src="./create.scss"></style>
