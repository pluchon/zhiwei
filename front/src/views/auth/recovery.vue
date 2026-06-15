<template>
  <main class="auth-page">
    <div class="auth-bg" aria-hidden="true" />

    <section class="auth-card">
      <header class="auth-card__head">
        <h1>找回密码</h1>
        <p>通过已验证手机号或邮箱重置密码</p>
      </header>

      <el-form label-position="top" class="auth-form">
        <el-form-item label="手机号或邮箱">
          <el-input v-model="form.target" size="large" placeholder="请输入绑定的手机号或邮箱" />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="sms-code-row">
            <el-input v-model="form.verificationCode" size="large" placeholder="6 位验证码" />
            <el-button class="sms-code-btn" @click="send">获取验证码</el-button>
          </div>
        </el-form-item>
        <el-alert v-if="devCode" class="auth-alert" :title="`开发验证码：${devCode}`" type="warning" :closable="false" />
        <el-form-item label="新密码">
          <div class="auth-inline-row">
            <el-input v-model="form.newPassword" size="large" type="password" show-password placeholder="请设置新密码" />
            <captcha-verify scene="RECOVERY" :target="form.target" compact @verified="form.captchaTicket = $event" />
          </div>
        </el-form-item>
        <div class="auth-actions">
          <el-button type="primary" @click="complete">重置密码</el-button>
        </div>
      </el-form>

      <router-link class="auth-back" to="/login">返回登录</router-link>
    </section>
  </main>
</template>

<script setup src="./recovery.js"></script>
<style scoped lang="scss" src="./auth-layout.scss"></style>
