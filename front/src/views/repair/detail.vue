<template>
  <div class="app-container repair-detail-page" v-loading="loading">
    <template v-if="detail.order">
      <header class="detail-head">
        <div class="detail-head__info">
          <span>{{ detail.order.orderNo }}</span>
          <h2>{{ detail.order.title }}</h2>
          <div
            v-if="!admin"
            class="detail-status-chip"
            :style="{ '--status-color': timelineDotColor(detail.order.status) }"
          >
            <span>{{ statusText(detail.order.status) }}</span>
          </div>
        </div>
        <div class="detail-head__actions">
          <div class="action-toolbar">
            <el-button v-if="canEditLocation" @click="goEdit">编辑位置</el-button>
            <el-button
              v-if="can('accept')"
              type="primary"
              :loading="submitting"
              @click="acceptOrder"
            >接单</el-button>
            <el-button
              v-if="can('start')"
              type="primary"
              :loading="submitting"
              @click="runAction('start')"
            >开始处理</el-button>
            <el-button
              v-if="can('result')"
              type="success"
              :loading="submitting"
              @click="promptAction('result', '维修结果说明')"
            >提交结果</el-button>
            <el-button
              v-if="can('return')"
              type="warning"
              :loading="submitting"
              @click="promptAction('return', '退回原因')"
            >退回</el-button>
            <el-button
              v-if="can('confirm')"
              type="success"
              :loading="submitting"
              @click="runAction('confirm')"
            >确认完成</el-button>
            <el-button
              v-if="can('unresolved')"
              type="danger"
              :loading="submitting"
              @click="runAction('unresolved')"
            >反馈未解决</el-button>
            <el-button
              v-if="can('submit')"
              type="primary"
              :loading="submitting"
              @click="runAction('submit')"
            >提交报修</el-button>
            <el-button
              v-if="can('withdraw')"
              :loading="submitting"
              @click="confirmWithdraw"
            >撤回为草稿</el-button>
            <el-button
              v-if="can('reDraft')"
              :loading="submitting"
              @click="confirmReDraft"
            >重新转草稿</el-button>
            <el-button
              v-if="can('arbitration')"
              type="danger"
              plain
              :loading="submitting"
              @click="requestArbitration"
            >申请仲裁</el-button>

            <template v-if="admin">
              <span class="action-toolbar__divider" />
              <div
                class="status-chip"
                :style="{ '--status-color': timelineDotColor(detail.order.status) }"
              >
                <el-icon><component :is="currentStatusIcon" /></el-icon>
                <span>{{ statusText(detail.order.status) }}</span>
              </div>
              <el-button
                v-if="can('dispatch')"
                type="primary"
                :loading="submitting"
                @click="openDispatch"
              >手动派单</el-button>
              <el-button
                v-if="showAiAnalysis"
                :loading="aiAnalysisLoading"
                @click="runAiAnalysis"
              >AI 分析</el-button>
              <el-button
                class="btn-close-order"
                type="danger"
                plain
                :loading="submitting"
                @click="adminAction('close')"
              >关闭工单</el-button>
            </template>
          </div>
        </div>
      </header>

      <div class="detail-body">
        <div class="detail-body__upper">
          <div class="detail-body__main">
          <el-card v-if="admin && duplicateDetail?.suspectedDuplicate" shadow="never" class="mb16">
            <template #header>疑似重复报修</template>
            <div class="duplicate-reason-block">
              <div class="duplicate-reason-block__label">AI 判定理由</div>
              <p v-if="duplicateDetail.duplicateReason" class="duplicate-reason-block__text">
                {{ duplicateDetail.duplicateReason }}
              </p>
              <el-alert
                v-else
                type="warning"
                :closable="false"
                show-icon
                title="暂无 AI 判定理由，请结合下方关联工单人工判断。"
              />
            </div>
            <el-table
              :data="duplicateDetail.links || []"
              size="small"
              class="duplicate-table"
              table-layout="fixed"
            >
              <el-table-column prop="targetOrderNo" label="关联工单" width="172" show-overflow-tooltip />
              <el-table-column prop="targetOrderTitle" label="标题" min-width="120" show-overflow-tooltip />
              <el-table-column prop="aiReason" label="关联推荐理由" min-width="140" show-overflow-tooltip />
              <el-table-column label="操作" width="148" align="center">
                <template #default="{ row }">
                  <el-button
                    v-if="row.confirmed !== 1"
                    size="small"
                    class="btn-link-confirm"
                    @click="confirmLink(row.linkId)"
                  >关联</el-button>
                  <el-button
                    size="small"
                    class="btn-link-remove"
                    @click="removeLink(row.linkId)"
                  >解除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
          <el-card v-if="aiAnalysisText" shadow="never" class="mb16">
            <template #header>AI 派单分析</template>
            <el-alert
              type="info"
              :closable="false"
              show-icon
              title="分析不推荐或排序具体维修师傅，也不会自动填写派单说明。"
              class="mb12"
            />
            <p class="ai-analysis-text">{{ aiAnalysisText }}</p>
          </el-card>
          <el-card shadow="never">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="报修方式">
                {{ repairTypeText(detail.order.repairType) }}
              </el-descriptions-item>
              <el-descriptions-item label="故障类型">
                {{ detail.category?.categoryName }}
              </el-descriptions-item>
              <template v-if="hasAssetSnapshot">
                <el-descriptions-item label="资产编号">
                  {{ detail.order.assetNoSnapshot || "-" }}
                </el-descriptions-item>
                <el-descriptions-item label="资产名称">
                  {{ detail.order.assetNameSnapshot || "-" }}
                </el-descriptions-item>
                <el-descriptions-item label="资产分类">
                  {{ detail.order.assetCategorySnapshot || "-" }}
                </el-descriptions-item>
                <el-descriptions-item label="资产位置" :span="2">
                  {{ detail.order.assetLocationSnapshot || locationText }}
                  <el-button
                    v-if="canViewAssetHistory"
                    link
                    type="primary"
                    @click="openAssetHistory"
                  >查看维修历史</el-button>
                </el-descriptions-item>
              </template>
              <el-descriptions-item label="联系电话">
                {{ detail.order.contactPhone }}
              </el-descriptions-item>
              <el-descriptions-item label="现场位置" :span="2">
                {{ locationText }}
              </el-descriptions-item>
              <el-descriptions-item
                v-if="detail.order.campusDescriptionSnapshot"
                label="校区说明"
                :span="2"
              >
                {{ detail.order.campusDescriptionSnapshot }}
              </el-descriptions-item>
              <el-descriptions-item
                v-if="detail.order.buildingDescriptionSnapshot"
                label="楼栋说明"
                :span="2"
              >
                {{ detail.order.buildingDescriptionSnapshot }}
              </el-descriptions-item>
              <el-descriptions-item
                v-if="detail.order.repairerRealName && detail.order.status >= 3"
                label="当前维修师傅"
              >
                {{ detail.order.repairerRealName }}
                <span class="sub-text">{{ detail.order.repairerUserNo }}</span>
              </el-descriptions-item>
              <el-descriptions-item
                v-if="detail.order.repairerBusyLevel && detail.order.status >= 3 && reporter"
                label="师傅繁忙程度"
              >
                <el-tag
                  :type="busyLevelType(detail.order.repairerBusyLevel)"
                  size="small"
                >
                  {{
                    busyLevelText(
                      detail.order.repairerBusyLevel,
                      detail.order.repairerBusyLevelLabel,
                    )
                  }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="故障描述" :span="2">
                {{ detail.order.description }}
              </el-descriptions-item>
            </el-descriptions>
          </el-card>

          <el-card shadow="never" class="section photo-section">
            <template #header><b>现场与维修图片</b></template>
            <div v-if="detail.attachments.length" class="photo-gallery">
              <el-image
                v-for="(a, index) in detail.attachments"
                :key="a.attachmentId"
                :src="a.signedUrl || ''"
                class="photo-gallery__item"
                fit="cover"
                :preview-src-list="attachmentPreviewList"
                :initial-index="index"
                preview-teleported
              >
                <template #error>
                  <div class="image-slot">图片加载失败</div>
                </template>
              </el-image>
            </div>
            <el-empty v-else description="暂无图片" :image-size="72" />
          </el-card>
          </div>

          <aside class="detail-body__timeline">
            <div class="timeline-panel">
              <div class="timeline-panel__title">流转时间线</div>
              <el-timeline v-if="detail.logs.length" class="flow-timeline">
                <el-timeline-item
                  v-for="l in detail.logs"
                  :key="l.logId"
                  :color="timelineDotColor(l.toStatus)"
                  hide-timestamp
                >
                  <p class="timeline-line-text" :title="timelineLineText(l)">
                    {{ timelineLineText(l) }}
                  </p>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-else description="暂无流转记录" :image-size="64" />
            </div>

            <el-card v-if="showEvaluationSection" shadow="never" class="section evaluate-card">
              <template #header><b>服务评价</b></template>
              <template v-if="canSubmitEvaluation">
                <el-rate v-model="rating" />
                <el-input
                  v-model="evaluation"
                  type="textarea"
                  :rows="3"
                  maxlength="50"
                  show-word-limit
                  placeholder="说说维修体验（最多50字）"
                />
                <el-button type="primary" :loading="submitting" @click="evaluate">提交评价</el-button>
              </template>
              <div v-else-if="detail.evaluation" class="evaluate-result">
                <el-rate
                  :model-value="detail.evaluation.star || 0"
                  disabled
                  show-score
                  score-template="{value} 星"
                />
                <p v-if="detail.evaluation.content" class="evaluate-result__content">
                  {{ detail.evaluation.content }}
                </p>
                <p v-else class="evaluate-result__empty">未填写文字评价</p>
                <p v-if="detail.evaluation.createTime" class="evaluate-result__time">
                  评价时间：{{ formatTimelineTime(detail.evaluation.createTime) }}
                </p>
              </div>
              <p v-else-if="showEvaluationPending" class="evaluate-result__empty">
                报修人暂未评价
              </p>
            </el-card>
          </aside>
        </div>

        <el-card shadow="never" class="section detail-comments-card">
          <template #header><b>沟通记录</b></template>
          <div v-if="detail.comments.length" class="comment-list">
            <div
              v-for="c in detail.comments"
              :key="c.commentId"
              class="comment"
              :class="{ system: c.commentType === 1 }"
            >
              <div class="comment__head">
                <b>{{ c.commentType === 1 ? "系统记录" : "用户评论" }}</b>
                <span>{{ c.createTime }}</span>
              </div>
              <p>{{ c.content }}</p>
            </div>
          </div>
          <el-empty v-else description="暂无沟通记录" :image-size="72" />
          <template v-if="!admin">
            <div class="comment-compose">
              <el-input
                v-model="comment"
                maxlength="200"
                placeholder="补充说明或沟通内容"
                clearable
                @keyup.enter="sendComment"
              />
              <el-button type="primary" plain @click="sendComment">发送评论</el-button>
            </div>
          </template>
        </el-card>
      </div>

      <el-dialog
        v-model="dispatchVisible"
        title="手动派单"
        width="760px"
        destroy-on-close
      >
        <div class="dispatch-ai-block">
          <div class="dispatch-ai-block__head">
            <span>AI 派单辅助</span>
            <el-button
              size="small"
              :loading="dispatchAiLoading"
              @click="runDispatchAiAnalysis"
            >
              AI 分析
            </el-button>
          </div>
          <el-alert
            type="info"
            :closable="false"
            show-icon
            title="分析仅提供文字建议，不推荐维修师傅，也不会自动填写派单说明或选择维修师傅。"
          />
          <p v-if="dispatchAiText" class="ai-analysis-text">{{ dispatchAiText }}</p>
        </div>

        <el-table
          v-loading="dispatchLoading"
          :data="candidates"
          highlight-current-row
          @current-change="(row) => (dispatchForm.repairerId = row?.userId || null)"
        >
          <el-table-column label="维修师傅" min-width="160">
            <template #default="{ row }">
              {{ row.realName }}
              <div class="sub-text">{{ row.userNo }}</div>
            </template>
          </el-table-column>
          <el-table-column label="繁忙程度" width="110">
            <template #default="{ row }">
              <el-tag :type="busyLevelType(row.busyLevel)" size="small">
                {{ busyLevelText(row.busyLevel, row.busyLevelLabel) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="故障能力" width="110">
            <template #default="{ row }">
              <el-tag :type="row.hasCapability ? 'success' : 'warning'" size="small">
                {{ row.hasCapability ? "具备" : "不匹配" }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

        <el-form label-position="top" class="dispatch-form">
          <el-form-item label="派单说明" required>
            <el-input
              v-model="dispatchForm.dispatchNote"
              type="textarea"
              :rows="3"
              maxlength="1000"
            />
          </el-form-item>
          <el-form-item
            v-if="selectedCandidate && !selectedCandidate.hasCapability"
            label="能力不匹配原因"
            required
          >
            <el-input
              v-model="dispatchForm.capabilityMismatchReason"
              type="textarea"
              :rows="3"
              maxlength="1000"
            />
          </el-form-item>
          <el-alert
            v-if="selectedCandidate && selectedCandidate.busyLevel === 'BUSY'"
            type="warning"
            :closable="false"
            title="该维修师傅当前较繁忙，请确认已与对方沟通。"
          />
        </el-form>

        <template #footer>
          <el-button @click="dispatchVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitDispatch">
            确认派单
          </el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="historyVisible" title="资产维修历史" width="760px" destroy-on-close>
        <el-table v-loading="historyLoading" :data="assetHistory.records" size="small">
          <el-table-column prop="orderNo" label="工单编号" width="170" />
          <el-table-column prop="categoryName" label="故障类型" width="120" />
          <el-table-column prop="repairerRealName" label="维修师傅" width="100" />
          <el-table-column prop="repairResult" label="维修结果" min-width="140" show-overflow-tooltip />
          <el-table-column prop="completionTime" label="完成时间" width="170" />
        </el-table>
        <pagination
          v-show="assetHistory.total"
          :total="assetHistory.total"
          v-model:page="historyQuery.pageNum"
          v-model:limit="historyQuery.pageSize"
          @pagination="loadAssetHistory"
        />
      </el-dialog>
    </template>
  </div>
</template>

<script setup src="./detail.js"></script>
<style scoped lang="scss" src="./detail.scss"></style>
