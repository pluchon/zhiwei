<template>
  <el-dialog
    v-model="visible"
    width="1180px"
    class="ai-assistant-dialog"
    :show-close="false"
    destroy-on-close
    align-center
    @open="handleOpen"
  >
    <template #header>
      <div class="dialog-header">
        <div class="header-brand">
          <span class="brand-icon">AI</span>
          <div>
            <h3>AI 助手</h3>
            <p>自然语言查统计、导出预览与语义搜索</p>
          </div>
        </div>
        <el-button text circle @click="visible = false">
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
    </template>

    <div class="ai-assistant">
      <aside class="session-panel">
        <div class="session-toolbar">
          <span>会话</span>
          <el-tooltip content="新建对话" placement="top">
            <el-button text circle :loading="creatingSession" @click="createNewSession">
              <el-icon><Plus /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
        <div v-loading="loadingSessions" class="session-list">
          <div
            v-for="item in sessionList"
            :key="item.sessionId"
            :class="['session-item', { active: item.sessionId === activeSessionId }]"
            @click="switchSession(item.sessionId)"
          >
            <span class="session-title">{{ item.title || "新对话" }}</span>
            <span class="session-preview">{{ item.preview || "暂无消息" }}</span>
            <div class="session-footer">
              <span class="session-time">{{ formatSessionTime(item.updateTime) }}</span>
              <span class="session-actions" @click.stop>
                <el-tooltip content="重命名" placement="top">
                  <button type="button" class="session-action-btn" @click="renameSession(item)">
                    <el-icon><Edit /></el-icon>
                  </button>
                </el-tooltip>
                <el-tooltip content="删除" placement="top">
                  <button type="button" class="session-action-btn session-action-btn--danger" @click="confirmDeleteSession(item)">
                    <el-icon><Delete /></el-icon>
                  </button>
                </el-tooltip>
              </span>
            </div>
          </div>
          <div v-if="!loadingSessions && !sessionList.length" class="session-empty">暂无会话，点击 + 开始</div>
        </div>
      </aside>

      <section class="chat-panel">
        <div ref="chatScrollRef" v-loading="loadingMessages" class="chat-thread">
          <div v-if="!messages.length && !loadingMessages" class="chat-welcome">
            <p>你好，我是知维 AI 助手。</p>
            <p>你可以直接提问，例如：</p>
            <ul>
              <li>查看本月工单统计</li>
              <li v-if="isAdmin">绘制最近30天各校区报修柱状图</li>
              <li v-if="isAdmin">用饼图看故障类型分布</li>
              <li v-if="isAdmin">导出近 7 天待匹配工单 Excel</li>
              <li v-if="isAdmin">搜索历史工单：图书馆空调故障</li>
              <li>搜索资产：投影仪</li>
              <li v-if="isRepairer">绘制我的完成趋势折线图</li>
              <li>换成上周的数据</li>
            </ul>
          </div>
          <article
            v-for="(item, index) in messages"
            :key="item.messageId || index"
            :class="['chat-row', isUserMessage(item.role) ? 'user' : 'assistant']"
          >
            <div class="avatar">{{ isUserMessage(item.role) ? "我" : "AI" }}</div>
            <div class="message-body">
              <div class="bubble">{{ item.text }}</div>
              <div v-if="item.statisticsResult" class="result-panel result-panel--stats">
                <AiStatisticsPanel :result="item.statisticsResult" />
              </div>
              <div v-if="item.exportPreview" class="result-panel result-panel--export">
                <p class="export-type">导出类型：{{ item.exportPreview.exportType === "ORDER" ? "工单" : "统计" }}</p>
                <p v-if="item.exportPreview.filterSummary" class="export-summary">
                  筛选条件：{{ item.exportPreview.filterSummary }}
                </p>
                <p class="export-summary">{{ item.exportPreview.previewSummary }}</p>
                <p v-if="item.exportPreview.estimatedCount != null" class="export-count">
                  预计数量：{{ item.exportPreview.estimatedCount }} 条
                </p>
                <p v-if="item.exportPreview.confirmDisabledReason" class="export-warning">
                  {{ item.exportPreview.confirmDisabledReason }}
                </p>
                <el-button
                  v-if="isAdmin && !item.exportPreview.confirmDisabled"
                  type="primary"
                  size="small"
                  :loading="exportingToken === item.exportPreview.previewToken"
                  @click="confirmExport(item.exportPreview)"
                >
                  确认导出
                </el-button>
              </div>
              <div v-if="item.orderSearchResult?.items?.length" class="result-panel">
                <p class="panel-title">工单搜索结果</p>
                <div
                  v-for="row in item.orderSearchResult.items"
                  :key="row.orderId"
                  class="search-card"
                >
                  <div class="search-card__title">{{ row.orderNo }} · {{ row.title }}</div>
                  <div class="search-card__meta">{{ row.statusLabel }} · {{ row.locationSummary || "—" }}</div>
                  <el-button link type="primary" @click="openOrderDetail(row.orderId)">查看详情</el-button>
                </div>
              </div>
              <div v-if="item.assetSearchResult?.items?.length" class="result-panel">
                <p class="panel-title">资产搜索结果</p>
                <div
                  v-for="row in item.assetSearchResult.items"
                  :key="row.assetId"
                  class="search-card"
                >
                  <div class="search-card__title">{{ row.assetNo }} · {{ row.assetName }}</div>
                  <div class="search-card__meta">{{ row.categoryName || "—" }} · {{ row.locationSummary || "—" }}</div>
                  <el-button link type="primary" @click="openAssetDetail(row.assetId)">查看详情</el-button>
                </div>
              </div>
              <div v-if="item.suggestionSearchResult?.items?.length" class="result-panel">
                <p class="panel-title">建议搜索结果</p>
                <div
                  v-for="row in item.suggestionSearchResult.items"
                  :key="row.suggestionId"
                  class="search-card"
                >
                  <div class="search-card__title">{{ row.title }}</div>
                  <div class="search-card__meta">{{ row.statusLabel }} · {{ row.category || "—" }}</div>
                  <el-button link type="primary" @click="openSuggestionDetail(row.suggestionId)">查看详情</el-button>
                </div>
              </div>
              <time v-if="item.createTime" class="message-time">{{ formatMessageTime(item.createTime) }}</time>
            </div>
          </article>
        </div>

        <div class="chat-composer">
          <el-input
            v-model="input"
            type="textarea"
            :rows="3"
            resize="none"
            maxlength="2000"
            show-word-limit
            placeholder="请输入自然语言，例如：查看本月工单统计"
          />
          <div class="composer-actions">
            <el-button type="primary" :loading="sending" :disabled="!input.trim()" @click="send">发送</el-button>
          </div>
        </div>
      </section>
    </div>
  </el-dialog>
</template>

<script setup src="./index.js"></script>
<style scoped lang="scss" src="./index.scss"></style>
