<template>
  <div
    class="app-container admin-resource"
    :class="{ 'admin-resource--users': isUserResource, 'admin-resource--compact': hidePageHead }"
  >
    <header v-if="!isUserResource && !hidePageHead" class="page-head">
      <div>
        <span>ADMIN CONSOLE</span>
        <h2>{{ title }}</h2>
        <p>{{ resourceHint }}</p>
      </div>

      <el-button v-if="creatable && !showTableFooterCreate && !isUserResource" type="primary" @click="openCreate">
        新增记录
      </el-button>
    </header>

    <el-card class="data-card" shadow="never">
      <el-form v-if="isUserResource" class="user-filter-form" label-width="88px" @submit.prevent>
        <div class="user-filter-grid">
          <el-form-item label="姓名">
            <el-input
              v-model="userQuery.keyword"
              clearable
              placeholder="请输入姓名"
              @keyup.enter="searchUsers"
            />
          </el-form-item>

          <el-form-item label="角色">
            <el-select
              v-model="userQuery.roleIds"
              multiple
              collapse-tags
              collapse-tags-tooltip
              clearable
              placeholder="全部角色"
            >
              <el-option
                v-for="role in roleFilterOptions"
                :key="role.value"
                :label="role.label"
                :value="role.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="激活状态">
            <el-select
              v-model="userQuery.activationStatuses"
              multiple
              collapse-tags
              collapse-tags-tooltip
              clearable
              placeholder="全部状态"
            >
              <el-option
                v-for="item in activationFilterOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="账号状态">
            <el-select
              v-model="userQuery.accountStatuses"
              multiple
              collapse-tags
              collapse-tags-tooltip
              clearable
              placeholder="全部状态"
            >
              <el-option
                v-for="item in accountFilterOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </div>

        <div class="user-filter-toolbar">
          <div class="user-filter-toolbar__main">
            <el-button type="primary" @click="searchUsers">查询</el-button>
            <el-button @click="resetUserFilters">重置</el-button>
            <el-button @click="enterExportMode">导出用户</el-button>
          </div>
          <el-button v-if="creatable" type="primary" @click="openCreate">
            新增用户
          </el-button>
        </div>
      </el-form>

      <div v-if="isUserResource && exportMode" class="export-bar">
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
            :disabled="!total"
            @click="exportFiltered"
          >
            一键导出（{{ total }} 条）
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
        v-loading="loading"
        :data="rows"
        :row-key="isUserResource ? 'userId' : undefined"
        stripe
        :class="{ 'export-mode': exportMode && isUserResource }"
        @selection-change="onSelectionChange"
      >
        <el-table-column
          v-if="exportMode && isUserResource"
          type="selection"
          width="48"
          :reserve-selection="true"
        />

        <el-table-column
          v-for="column in columns"
          :key="column.prop"
          :label="column.label"
          :min-width="column.width || 130"
        >
          <template #default="{ row }">
            <el-tag v-if="isTagColumn(column.prop)" :type="tagType(row, column.prop)">
              {{ displayValue(row, column) }}
            </el-tag>

            <span v-else>
              {{ displayValue(row, column) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column
          v-if="canEdit || canRemove"
          label="操作"
          fixed="right"
          width="150"
        >
          <template #default="{ row }">
            <el-button v-if="canEdit" type="primary" link @click="openEdit(row)">
              编辑
            </el-button>

            <el-button v-if="canRemove" type="danger" link @click="remove(row)">
              移除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-if="isPaged && total"
        v-model:page="query.pageNum"
        v-model:limit="query.pageSize"
        :total="total"
        @pagination="load"
      />

      <div v-if="showTableFooterCreate" class="table-footer-actions">
        <el-button type="primary" @click="openCreate">新增记录</el-button>
      </div>
    </el-card>

    <el-dialog v-model="dialog" :title="dialogTitle" width="600px">
      <el-alert
        v-if="!hideDialogHint && !isUserResource"
        class="form-tip"
        type="info"
        :closable="false"
        show-icon
      >
        {{ resourceHint }}
      </el-alert>

      <el-form label-position="top" class="resource-form">
        <el-form-item
          v-for="field in formFields"
          :key="field.key"
          :label="field.label"
          :class="{ 'form-item--half': field.half }"
        >
          <div v-if="field.type === 'searchSelect'" class="search-select-row">
            <el-input
              v-model="searchKeywords[field.optionType]"
              clearable
              :placeholder="searchSelectPlaceholder(field.optionType)"
            />
            <el-select
              v-model="form[field.key]"
              clearable
              :placeholder="`请选择${field.label}`"
            >
              <el-option
                v-for="option in filteredSearchSelectOptions(field.optionType)"
                :key="option.value"
                :label="option.label"
                :value="option.value"
                :disabled="option.disabled"
              />
            </el-select>
          </div>

          <el-select
            v-else-if="field.type === 'select'"
            v-model="form[field.key]"
            filterable
            clearable
            :placeholder="field.placeholder || `请选择${field.label}`"
          >
            <el-option
              v-for="option in field.options"
              :key="option.value"
              :label="option.label"
              :value="option.value"
              :disabled="option.disabled"
            />
          </el-select>

          <el-input-number
            v-else-if="field.type === 'number'"
            v-model="form[field.key]"
            :min="0"
            controls-position="right"
          />

          <el-input
            v-else-if="field.type === 'textarea'"
            v-model="form[field.key]"
            type="textarea"
            :rows="3"
            :placeholder="field.placeholder || `请输入${field.label}`"
          />

          <el-input
            v-else
            v-model="form[field.key]"
            :type="field.type === 'password' ? 'password' : 'text'"
            :show-password="field.type === 'password'"
            :placeholder="field.placeholder || `请输入${field.label}`"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialog = false">
          取消
        </el-button>

        <el-button type="primary" :loading="saving" @click="submit">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup src="./resource.js"></script>
<style scoped lang="scss" src="./resource.scss"></style>
