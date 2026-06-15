<template>
  <div class="app-container profile-page">
    <header class="page-head">
      <span>ACCOUNT</span>
      <h2>个人中心</h2>
    </header>

    <el-card shadow="never" class="info-card" v-loading="availabilityLoading && isRepairer">
      <template #header><b>基本信息</b></template>
      <table class="profile-info-table">
        <tbody>
          <tr>
            <td :rowspan="isRepairer ? 5 : 4" class="avatar-cell">
              <el-upload
                class="avatar-uploader"
                :show-file-list="false"
                accept="image/jpeg,image/png,image/webp,image/gif"
                :disabled="avatarUploading"
                :http-request="handleAvatarUpload"
              >
                <div class="avatar-wrap">
                  <el-avatar :size="96" :src="displayAvatar">
                    {{ avatarFallback }}
                  </el-avatar>
                  <div v-if="!avatarUploading" class="avatar-hover">更换头像</div>
                  <span v-else class="avatar-mask">审核中</span>
                </div>
              </el-upload>
            </td>
            <th>真实姓名</th>
            <td>{{ profile.user?.realName || "—" }}</td>
            <th>展示昵称</th>
            <td>{{ profile.user?.nickName || "—" }}</td>
          </tr>
          <tr>
            <th>登录账号</th>
            <td>{{ profile.user?.userNo || "—" }}</td>
            <th>角色身份</th>
            <td>{{ profile.roleLabel || profile.roleCode || "—" }}</td>
          </tr>
          <tr>
            <th>手机号</th>
            <td>{{ profile.user?.phoneNumber || "—" }}</td>
            <th>邮箱</th>
            <td>{{ profile.user?.email || "—" }}</td>
          </tr>
          <tr>
            <th>激活状态</th>
            <td>
              <el-tag :type="profile.user?.activationStatus === 1 ? 'success' : 'info'" size="small">
                {{ profile.user?.activationStatus === 1 ? "已激活" : "未激活" }}
              </el-tag>
            </td>
            <th>注册时间</th>
            <td>{{ profile.user?.createTime || "—" }}</td>
          </tr>
          <tr v-if="isRepairer && availability">
            <th>接单状态</th>
            <td colspan="3" class="accept-cell">
              <div
                class="accept-switch"
                :class="{
                  'is-paused': availability.acceptingState === 'PAUSED',
                  'is-loading': availabilitySaving,
                }"
              >
                <button
                  type="button"
                  class="accept-switch__option"
                  :class="{ 'is-active': availability.acceptingState === 'AVAILABLE' }"
                  :disabled="availabilitySaving"
                  @click="switchToAvailable"
                >可接单</button>
                <button
                  type="button"
                  class="accept-switch__option"
                  :class="{ 'is-active': availability.acceptingState === 'PAUSED' }"
                  :disabled="availabilitySaving"
                  @click="switchToPaused"
                >暂停接单</button>
                <span class="accept-switch__thumb" aria-hidden="true" />
              </div>
              <p v-if="availability.pauseReason" class="accept-meta">
                暂停原因：{{ availability.pauseReason }}
              </p>
              <p v-if="availability.expectedResumeTime" class="accept-meta">
                预计恢复：{{ availability.expectedResumeTime }}
              </p>
            </td>
          </tr>
        </tbody>
      </table>
    </el-card>

    <el-card shadow="never" class="security-card">
      <template #header><b>安全设置</b></template>
      <el-form inline class="password-row" @submit.prevent>
        <el-form-item label="当前密码">
          <el-input
            v-model="form.oldPassword"
            type="password"
            show-password
            placeholder="请输入当前密码"
          />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input
            v-model="form.newPassword"
            type="password"
            show-password
            placeholder="请输入新密码"
          />
        </el-form-item>
        <el-button type="primary" @click="submit">修改密码并退出登录</el-button>
      </el-form>
    </el-card>

    <el-dialog v-model="pauseDialog" title="暂停接单" width="480px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="暂停原因" required>
          <el-input v-model="pauseForm.pauseReason" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="预计恢复时间">
          <el-date-picker
            v-model="pauseForm.expectedResumeTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pauseDialog = false">取消</el-button>
        <el-button type="primary" :loading="availabilitySaving" @click="submitPause">确认暂停</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup src="./index.js"></script>
<style scoped lang="scss" src="./index.scss"></style>
