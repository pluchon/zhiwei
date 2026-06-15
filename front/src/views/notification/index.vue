<template>
  <div class="app-container">
    <header class="page-head">
      <span>INBOX</span>
      <h2>站内信</h2>
    </header>

    <el-card shadow="never">
      <div class="toolbar">
        <el-form inline>
          <el-form-item label="阅读状态">
            <el-select v-model="query.isRead" clearable style="width: 140px" @change="search">
              <el-option
                v-for="item in readFilterOptions"
                :key="String(item.value)"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="通知类型">
            <el-select
              v-model="query.notificationType"
              clearable
              style="width: 160px"
              @change="search"
            >
              <el-option
                v-for="item in notificationTypes"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-form>
        <div class="actions">
          <el-button
            :disabled="!selectedIds.length"
            :loading="submitting"
            @click="batchRead"
          >批量已读</el-button>
          <el-button :loading="submitting" @click="readAll">全部已读</el-button>
        </div>
      </div>

      <div
        v-for="n in data.records"
        :key="n.notificationId"
        class="notice"
        :class="{ unread: !n.isRead }"
        @click="readOne(n)"
      >
        <el-checkbox
          :model-value="selectedIds.includes(n.notificationId)"
          @click.stop
          @change="(checked) => onCheckChange(n.notificationId, checked)"
        />
        <div class="notice-body">
          <div class="notice-title">
            <b>{{ n.title }}</b>
            <el-tag size="small" type="info">{{ notificationTypeText(n.notificationType) }}</el-tag>
          </div>
          <p>{{ n.content }}</p>
        </div>
        <time>{{ n.createTime }}</time>
      </div>

      <el-empty v-if="!loading && !data.records.length" description="暂无通知" />
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
<style scoped lang="scss" src="./index.scss"></style>
