<template>
  <div class="app-container admin-list-page suggestion-admin">
    <el-card shadow="never" class="admin-list-card">
      <el-form class="filter-form" label-width="48px" @submit.prevent>
        <div class="filter-row-single">
          <div class="filter-grid filter-grid--inline suggestion-filter-grid">
            <el-form-item label="状态">
              <el-select v-model="query.status" clearable placeholder="全部">
                <el-option
                  v-for="item in suggestionStatusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="分类">
              <el-select v-model="query.category" clearable placeholder="全部">
                <el-option
                  v-for="item in suggestionCategoryOptions"
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
        <el-table-column prop="repairerRealName" label="维修师傅" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.repairerRealName || "-" }}</template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
        <el-table-column label="分类" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.categoryLabel || suggestionCategoryText(row.category) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="96" align="center">
          <template #default="{ row }">
            <el-tag :type="suggestionStatusType(row.status)" size="small">
              {{ row.statusLabel || suggestionStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="168" show-overflow-tooltip />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button size="small" type="primary" plain @click="openDetail(row)">查看</el-button>
              <el-button
                v-if="row.status === 'PENDING'"
                size="small"
                type="success"
                plain
                @click="openHandle(row)"
              >
                处理
              </el-button>
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

    <el-dialog v-model="detailVisible" title="建议详情" width="640px" destroy-on-close class="suggestion-detail-dialog">
      <template v-if="current">
        <table class="suggestion-detail-table">
          <tbody>
            <tr>
              <td rowspan="3" class="avatar-cell">
                <el-avatar :size="88" :src="current.repairerAvatar" class="repairer-avatar">
                  {{ avatarFallback(current.repairerRealName) }}
                </el-avatar>
              </td>
              <th>师傅姓名</th>
              <td>{{ current.repairerRealName || "-" }}</td>
            </tr>
            <tr>
              <th>分类</th>
              <td>{{ current.categoryLabel || suggestionCategoryText(current.category) }}</td>
            </tr>
            <tr>
              <th>内容</th>
              <td>{{ current.content || "-" }}</td>
            </tr>
            <tr>
              <th>管理员回复</th>
              <td>{{ current.adminReply || "暂无" }}</td>
            </tr>
          </tbody>
        </table>
      </template>
    </el-dialog>

    <el-dialog v-model="handleVisible" title="处理建议" width="520px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="处理结果" required>
          <el-radio-group v-model="handleForm.status">
            <el-radio value="ACCEPTED">采纳</el-radio>
            <el-radio value="REJECTED">未采纳</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="回复说明" required>
          <el-input
            v-model="handleForm.adminReply"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 6 }"
            resize="none"
            maxlength="1000"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitHandle">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup src="./index.js"></script>
<style scoped lang="scss" src="../common/admin-list.scss"></style>
<style scoped lang="scss" src="./index.scss"></style>
