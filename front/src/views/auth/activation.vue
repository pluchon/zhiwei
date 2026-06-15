<template>
  <main class="auth-page">
    <div class="auth-bg" aria-hidden="true" />

    <section class="auth-card">
      <header class="auth-card__head">
        <h1>激活账号</h1>
        <p>验证预留手机号并设置登录密码</p>
      </header>

      <el-steps :active="step" finish-status="success" align-center class="auth-steps">
        <el-step title="确认账号" />
        <el-step title="验证手机" />
        <el-step title="设置密码" />
      </el-steps>

      <el-form label-position="top" class="auth-form">
        <template v-if="step === 0">
          <el-form-item label="学号 / 工号">
            <el-input v-model="form.userNo" size="large" placeholder="请输入学号或工号" />
          </el-form-item>
          <el-form-item label="初始密码">
            <el-input v-model="form.initialPassword" size="large" type="password" show-password placeholder="请输入初始密码" />
          </el-form-item>
          <div class="auth-actions">
            <el-button type="primary" @click="start">下一步</el-button>
          </div>
        </template>

        <template v-else-if="step === 1">
          <el-alert class="auth-alert" :title="`请验证预留手机：${maskedPhone}`" type="info" :closable="false" />
          <el-form-item label="预留手机号">
            <el-input v-model="form.target" size="large" placeholder="请输入完整手机号" />
          </el-form-item>
          <el-form-item label="验证码">
            <div class="sms-code-row">
              <el-input v-model="form.verificationCode" size="large" placeholder="6 位短信验证码" />
              <el-button class="sms-code-btn" @click="send">获取验证码</el-button>
            </div>
          </el-form-item>
          <div class="auth-captcha-row">
            <captcha-verify scene="ACTIVATION" :target="form.target" @verified="form.captchaTicket = $event" />
          </div>
          <el-alert v-if="devCode" class="auth-alert" :title="`开发验证码：${devCode}`" type="warning" :closable="false" />
          <div class="auth-actions">
            <el-button @click="step = 0">上一步</el-button>
            <el-button type="primary" @click="step = 2">下一步</el-button>
          </div>
        </template>

        <template v-else>
          <el-form-item label="新密码">
            <el-input v-model="form.newPassword" size="large" type="password" show-password placeholder="请设置新密码" />
          </el-form-item>
          <div class="auth-actions">
            <el-button @click="step = 1">上一步</el-button>
            <el-button type="primary" @click="complete">完成激活</el-button>
          </div>
        </template>
      </el-form>

      <router-link class="auth-back" to="/login">返回登录</router-link>
    </section>
  </main>
</template>

<script setup src="./activation.js"></script>
<style scoped lang="scss" src="./auth-layout.scss"></style>
