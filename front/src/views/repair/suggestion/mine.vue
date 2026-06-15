<template>
  <div class="suggestion-mine-page">
    <el-card shadow="never" class="list-card" v-loading="loading">
      <template #header>
        <div class="list-card__head">
          <b>我的建议</b>
          <el-button type="primary" plain @click="openCreate">提交新建议</el-button>
        </div>
      </template>

      <el-table :data="data.records" stripe class="suggestion-table">
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="分类" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.categoryLabel || suggestionCategoryText(row.category) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="suggestionStatusType(row.status)" size="small">
              {{ row.statusLabel || suggestionStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="172" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row)">查看</el-button>
            <el-button
              v-if="row.status === 'PENDING' && row.withdrawnFlag === 0"
              link
              type="warning"
              @click="withdraw(row)"
            >撤回</el-button>
            <el-button
              v-if="canEdit(row)"
              link
              type="primary"
              @click="startEdit(row)"
            >编辑</el-button>
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

    <el-dialog
      v-model="formVisible"
      :title="editingId ? '编辑建议' : '提交新建议'"
      width="520px"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form label-width="88px" label-position="left" class="submit-form" @submit.prevent>
        <el-form-item label="建议分类" required>
          <el-select v-model="form.category" clearable placeholder="请选择分类" style="width: 100%">
            <el-option
              v-for="item in suggestionCategoryOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="建议标题" required>
          <el-input
            v-model="form.title"
            maxlength="100"
            placeholder="一句话概括建议"
            clearable
          />
        </el-form-item>
        <el-form-item label="建议内容" required>
          <el-input
            v-model="form.content"
            maxlength="100"
            show-word-limit
            placeholder="例如：希望增加某类故障的快速模板"
            clearable
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">
          {{ editingId ? "重新提交" : "提交" }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="建议详情" width="560px" destroy-on-close>
      <template v-if="current">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="标题">{{ current.title }}</el-descriptions-item>
          <el-descriptions-item label="分类">
            {{ current.categoryLabel || suggestionCategoryText(current.category) }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            {{ current.statusLabel || suggestionStatusText(current.status) }}
          </el-descriptions-item>
          <el-descriptions-item label="内容">{{ current.content }}</el-descriptions-item>
          <el-descriptions-item v-if="current.adminReply" label="管理员回复">
            {{ current.adminReply }}
          </el-descriptions-item>
          <el-descriptions-item v-if="current.handledTime" label="处理时间">
            {{ formatTime(current.handledTime) }}
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>
  </div>
</template>

<script setup src="./mine.js"></script>
<style scoped lang="scss" src="./mine.scss"></style>
