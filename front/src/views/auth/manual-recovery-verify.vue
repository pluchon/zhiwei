<template>
  <main class="auth-page">
    <div class="auth-bg" aria-hidden="true" />

    <section class="auth-card auth-card--wide">
      <header class="auth-card__head">
        <h1>验证新手机号</h1>
        <p>完成人工恢复申请的手机号换绑验证</p>
      </header>

      <el-skeleton v-if="loading" :rows="5" animated />

      <el-alert v-else-if="blocked" :title="blockMessage" type="error" :closable="false" show-icon />

      <template v-else-if="info">
        <el-descriptions :column="1" border class="info-block">
          <el-descriptions-item label="目标用户">
            {{ info.targetRealName }} ({{ info.targetUserNo }})
          </el-descriptions-item>
          <el-descriptions-item label="新手机号">{{ info.maskedNewPhone }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            {{ info.statusLabel || manualRecoveryStatusText(info.status) }}
          </el-descriptions-item>
          <el-descriptions-item label="验证截止">{{ info.expireTime || "-" }}</el-descriptions-item>
        </el-descriptions>

        <el-form label-position="top" class="auth-form">
          <el-form-item label="新手机号" required>
            <el-input v-model="form.target" size="large" placeholder="请输入申请中的新手机号" />
          </el-form-item>
          <el-form-item label="验证码" required>
            <div class="sms-code-row">
              <el-input v-model="form.verificationCode" size="large" placeholder="6 位验证码" />
              <el-button class="sms-code-btn" @click="send">获取验证码</el-button>
            </div>
          </el-form-item>
          <div class="auth-captcha-row">
            <captcha-verify scene="MANUAL_RECOVERY" :target="form.target" @verified="form.captchaTicket = $event" />
          </div>
          <el-alert v-if="devCode" class="auth-alert" :title="`开发验证码：${devCode}`" type="warning" :closable="false" />
          <div class="auth-actions">
            <el-button type="primary" :loading="submitting" @click="submit">完成验证</el-button>
          </div>
        </el-form>
      </template>

      <router-link class="auth-back" to="/login">返回登录</router-link>
    </section>
  </main>
</template>

<script setup src="./manual-recovery-verify.js"></script>
<style scoped lang="scss" src="./auth-layout.scss"></style>
<style scoped lang="scss">
.auth-card--wide {
  width: min(100%, 560px);
}

.info-block {
  margin-bottom: 18px;
}
</style>
