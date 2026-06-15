<template>

  <div class="app-container admin-list-page manual-recovery-page">

    <el-card shadow="never" class="admin-list-card">

      <el-form class="filter-form" label-width="88px" @submit.prevent>

        <div class="filter-row-single">

          <div class="filter-grid filter-grid--inline">

            <el-form-item label="目标用户">

              <el-select

                v-model="filters.targetUserId"

                clearable

                filterable

                remote

                :remote-method="loadUsers"

                :loading="userLoading"

                placeholder="请选择用户"

              >

                <el-option

                  v-for="user in users"

                  :key="user.userId"

                  :label="`${user.realName || user.userNo} (${user.userNo})`"

                  :value="user.userId"

                />

              </el-select>

            </el-form-item>

            <el-form-item label="状态">

              <el-select v-model="filters.status" clearable placeholder="全部">

                <el-option

                  v-for="item in manualRecoveryStatusOptions"

                  :key="item.value"

                  :label="item.label"

                  :value="item.value"

                />

              </el-select>

            </el-form-item>

            <el-form-item label="创建时间">
              <el-date-picker
                v-model="createDateRange"
                type="daterange"
                value-format="YYYY-MM-DD"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
              />
            </el-form-item>
            <el-form-item label="审批范围">
              <el-select v-model="filters.onlyPending" placeholder="全部">
                <el-option
                  v-for="item in pendingFilterOptions"
                  :key="String(item.value)"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>

          </div>

          <div class="filter-actions">

            <el-button type="success" @click="openCreate">创建申请</el-button>

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

        table-layout="fixed"

      >

        <el-table-column prop="recoveryId" label="申请 ID" width="90" />

        <el-table-column label="目标用户" min-width="160" show-overflow-tooltip>

          <template #default="{ row }">

            {{ row.targetRealName || "-" }}

            <div class="sub-text">{{ row.targetUserNo || "" }}</div>

          </template>

        </el-table-column>

        <el-table-column prop="maskedNewPhone" label="新手机号" width="130" show-overflow-tooltip />

        <el-table-column label="状态" width="100" align="center">

          <template #default="{ row }">

            <el-tag :type="manualRecoveryStatusType(row.status)" size="small">

              {{ row.statusLabel || manualRecoveryStatusText(row.status) }}

            </el-tag>

          </template>

        </el-table-column>

        <el-table-column prop="applicantAdminName" label="发起人" width="110" show-overflow-tooltip />

        <el-table-column label="验证截止" width="168" show-overflow-tooltip>

          <template #default="{ row }">{{ formatTime(row.expireTime) }}</template>

        </el-table-column>

        <el-table-column label="创建时间" width="168" show-overflow-tooltip>

          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>

        </el-table-column>

        <el-table-column label="操作" width="220" fixed="right">

          <template #default="{ row }">

            <div class="table-actions">

              <el-button size="small" type="primary" plain @click="openDetail(row)">详情</el-button>

              <el-button

                v-if="canReview(row)"

                size="small"

                type="success"

                plain

                @click="openReview(row)"

              >

                审批

              </el-button>

              <el-button

                v-if="canCancel(row)"

                size="small"

                type="warning"

                plain

                @click="handleCancel(row)"

              >

                撤销

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



    <el-dialog v-model="createDialog" title="创建人工恢复申请" width="520px" destroy-on-close>

      <el-form label-position="top">

        <el-form-item label="目标用户" required>

          <el-select

            v-model="createForm.targetUserId"

            filterable

            remote

            :remote-method="loadUsers"

            :loading="userLoading"

            style="width: 100%"

            placeholder="请选择目标用户"

          >

            <el-option

              v-for="user in users"

              :key="user.userId"

              :label="`${user.realName || user.userNo} (${user.userNo})`"

              :value="user.userId"

            />

          </el-select>

        </el-form-item>

        <el-form-item label="新手机号" required>

          <el-input v-model="createForm.newPhone" maxlength="20" placeholder="请输入新手机号" />

        </el-form-item>

        <el-form-item label="线下身份核验说明" required>

          <el-input

            v-model="createForm.identityCheckNote"

            type="textarea"

            :autosize="{ minRows: 2, maxRows: 6 }"

            resize="none"

            placeholder="请填写线下核验情况"

          />

        </el-form-item>

      </el-form>

      <template #footer>

        <el-button @click="createDialog = false">取消</el-button>

        <el-button type="primary" :loading="saving" @click="submitCreate">提交</el-button>

      </template>

    </el-dialog>



    <el-dialog v-model="detailVisible" title="申请详情" width="640px" destroy-on-close>

      <template v-if="detail">

        <el-descriptions :column="2" border>

          <el-descriptions-item label="申请 ID">{{ detail.recoveryId }}</el-descriptions-item>

          <el-descriptions-item label="状态">

            <el-tag :type="manualRecoveryStatusType(detail.status)" size="small">

              {{ detail.statusLabel || manualRecoveryStatusText(detail.status) }}

            </el-tag>

          </el-descriptions-item>

          <el-descriptions-item label="目标用户">

            {{ detail.targetRealName }} ({{ detail.targetUserNo }})

          </el-descriptions-item>

          <el-descriptions-item label="新手机号">{{ detail.maskedNewPhone }}</el-descriptions-item>

          <el-descriptions-item label="发起人">{{ detail.applicantAdminName }}</el-descriptions-item>

          <el-descriptions-item label="复核人">{{ detail.reviewerAdminName || "-" }}</el-descriptions-item>

          <el-descriptions-item label="核验说明" :span="2">

            {{ detail.identityCheckNote }}

          </el-descriptions-item>

          <el-descriptions-item label="审批说明" :span="2">

            {{ detail.reviewNote || "-" }}

          </el-descriptions-item>

          <el-descriptions-item label="验证截止">{{ formatTime(detail.expireTime) }}</el-descriptions-item>

          <el-descriptions-item label="完成时间">{{ formatTime(detail.completedTime) }}</el-descriptions-item>

        </el-descriptions>

        <div v-if="detail.status === 'APPROVED'" class="verify-link">

          验证链接：/manual-recovery/{{ detail.recoveryId }}

        </div>

        <div class="detail-actions">

          <el-button v-if="canReview(detail)" type="success" @click="openReview(detail)">

            审批

          </el-button>

          <el-button v-if="canCancel(detail)" type="warning" @click="handleCancel(detail)">

            撤销

          </el-button>

        </div>

      </template>

    </el-dialog>



    <el-dialog v-model="reviewDialog" title="审批申请" width="480px" destroy-on-close>

      <el-form label-position="top">

        <el-form-item label="审批结果" required>

          <el-radio-group v-model="reviewForm.approved">

            <el-radio :value="true">通过</el-radio>

            <el-radio :value="false">驳回</el-radio>

          </el-radio-group>

        </el-form-item>

        <el-form-item label="审批处理说明" required>

          <el-input

            v-model="reviewForm.reviewNote"

            type="textarea"

            :autosize="{ minRows: 2, maxRows: 6 }"

            resize="none"

            placeholder="请填写审批说明"

          />

        </el-form-item>

      </el-form>

      <template #footer>

        <el-button @click="reviewDialog = false">取消</el-button>

        <el-button type="primary" :loading="saving" @click="submitReview">提交</el-button>

      </template>

    </el-dialog>

  </div>

</template>



<script setup src="./index.js"></script>

<style scoped lang="scss" src="../common/admin-list.scss"></style>

<style scoped lang="scss">

.verify-link {

  margin-top: 16px;

  color: #606266;

  font-size: 13px;

}



.detail-actions {

  margin-top: 16px;

}

</style>

