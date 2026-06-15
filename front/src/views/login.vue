<template>
  <main class="login-page" @mousemove="onMouseMove">
    <div class="login-bg" aria-hidden="true">
      <div class="login-bg__aurora login-bg__aurora--a" :style="parallaxStyle(0.02)" />
      <div class="login-bg__aurora login-bg__aurora--b" :style="parallaxStyle(0.035)" />
      <div class="login-bg__aurora login-bg__aurora--c" :style="parallaxStyle(-0.015)" />
      <div class="login-bg__vignette" />
      <div class="login-bg__noise" />
      <div class="login-bg__scanline" />
    </div>

    <section class="login-hero" aria-label="系统介绍">
      <div class="login-hero__content">
        <div class="login-hero__brand">
          <h1 class="login-hero__title">
            <span class="login-hero__title-line login-hero__title-line--brand" aria-label="知维">
              <span class="login-hero__brand-char">知</span><span class="login-hero__brand-char">维</span>
            </span>
          </h1>
        </div>

        <div v-if="portalSummary" class="login-hero__stats">
          <article v-for="stat in statItems" :key="stat.key" class="stat-chip">
            <span class="stat-chip__label">{{ stat.label }}</span>
            <strong class="stat-chip__value">{{ displayStats[stat.key] }}</strong>
          </article>
        </div>
      </div>

      <div class="login-hero__visual" aria-hidden="true" :style="parallaxStyle(0.05)">
        <svg class="network-scene" viewBox="0 0 640 420" fill="none" xmlns="http://www.w3.org/2000/svg">
          <defs>
            <radialGradient id="node-glow" cx="50%" cy="50%" r="50%">
              <stop offset="0%" stop-color="rgba(212,168,83,0.9)" />
              <stop offset="100%" stop-color="rgba(212,168,83,0)" />
            </radialGradient>
            <radialGradient id="hub-glow" cx="50%" cy="50%" r="50%">
              <stop offset="0%" stop-color="rgba(56,189,168,0.95)" />
              <stop offset="100%" stop-color="rgba(56,189,168,0)" />
            </radialGradient>
            <linearGradient id="link-gradient" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0%" stop-color="rgba(56,189,168,0.05)" />
              <stop offset="50%" stop-color="rgba(212,168,83,0.55)" />
              <stop offset="100%" stop-color="rgba(56,189,168,0.05)" />
            </linearGradient>
          </defs>

          <g class="network-links">
            <path class="network-link" d="M120 280 L320 200 L520 260" />
            <path class="network-link" d="M120 280 L200 120 L320 200" />
            <path class="network-link" d="M520 260 L480 100 L320 200" />
            <path class="network-link" d="M200 120 L480 100" />
            <path class="network-link" d="M120 280 L80 160 L200 120" />
            <path class="network-link network-link--dim" d="M520 260 L560 180 L480 100" />
          </g>

          <g class="network-nodes">
            <circle class="network-node network-node--hub" cx="320" cy="200" r="10" />
            <circle class="network-node" cx="120" cy="280" r="6" />
            <circle class="network-node" cx="520" cy="260" r="6" />
            <circle class="network-node network-node--gold" cx="200" cy="120" r="5" />
            <circle class="network-node network-node--gold" cx="480" cy="100" r="5" />
            <circle class="network-node network-node--dim" cx="80" cy="160" r="4" />
            <circle class="network-node network-node--dim" cx="560" cy="180" r="4" />
          </g>

          <g class="network-packets">
            <circle r="3.5" fill="#fff" filter="drop-shadow(0 0 4px rgba(255,255,255,0.9))">
              <animateMotion dur="4.5s" repeatCount="indefinite" path="M120 280 L320 200 L520 260" />
            </circle>
            <circle r="3" fill="#d4a853" filter="drop-shadow(0 0 4px rgba(212,168,83,0.8))">
              <animateMotion dur="5.2s" repeatCount="indefinite" begin="-1.8s" path="M200 120 L320 200 L520 260" />
            </circle>
            <circle r="2.5" fill="#fff" opacity="0.85" filter="drop-shadow(0 0 3px rgba(255,255,255,0.7))">
              <animateMotion dur="6s" repeatCount="indefinite" begin="-3.2s" path="M80 160 L200 120 L480 100" />
            </circle>
          </g>
        </svg>

        <div class="visual-ring visual-ring--1" />
        <div class="visual-ring visual-ring--2" />
      </div>
    </section>

    <section class="login-form-shell" aria-label="登录表单">
      <div class="login-card">
        <div class="login-card__shine" aria-hidden="true" />

        <header class="login-card__head">
          <img src="@/assets/logo/logo.png" alt="" class="login-card__logo" width="72" height="72" />
          <h2>账号登录</h2>
          <p>使用账号或手机号登录知维</p>
        </header>

        <div class="login-mode" role="tablist">
          <button
            type="button"
            role="tab"
            :aria-selected="loginType === 'password'"
            :class="['login-mode__btn', { active: loginType === 'password' }]"
            @click="loginType = 'password'"
          >
            账号密码
          </button>
          <button
            type="button"
            role="tab"
            :aria-selected="loginType === 'phone'"
            :class="['login-mode__btn', { active: loginType === 'phone' }]"
            @click="loginType = 'phone'"
          >
            手机验证码
          </button>
          <span class="login-mode__indicator" :class="{ phone: loginType === 'phone' }" />
        </div>

        <div v-show="loginType === 'password'" class="login-pane">
          <el-form ref="passwordRef" :model="password" label-position="top" class="login-form">
            <el-form-item label="学号 / 工号">
              <el-input v-model="password.userNo" size="large" placeholder="请输入学号或工号" />
            </el-form-item>
            <el-form-item label="密码">
              <div class="login-inline-row">
                <el-input
                  v-model="password.password"
                  size="large"
                  type="password"
                  show-password
                  placeholder="请输入密码"
                  @keyup.enter="submitPassword"
                />
                <captcha-verify
                  scene="LOGIN_PASSWORD"
                  :target="password.userNo"
                  compact
                  @verified="password.captchaTicket = $event"
                />
              </div>
            </el-form-item>
            <el-button type="primary" size="large" :loading="loading" class="login-submit" @click="submitPassword">
              <span class="login-submit__text">进入系统</span>
              <span class="login-submit__arrow" aria-hidden="true">→</span>
            </el-button>
          </el-form>
        </div>

        <div v-show="loginType === 'phone'" class="login-pane">
          <el-form label-position="top" class="login-form">
            <el-form-item label="手机号">
              <el-input v-model="phone.target" size="large" placeholder="请输入已绑定手机号" />
            </el-form-item>
            <el-form-item label="验证码">
              <div class="sms-code-row">
                <el-input v-model="phone.verificationCode" size="large" placeholder="6 位短信验证码" />
                <el-button class="sms-code-btn" @click="sendCode">获取验证码</el-button>
              </div>
            </el-form-item>
            <div class="login-captcha-row login-captcha-row--block">
              <captcha-verify
                scene="LOGIN_SMS"
                :target="phone.target"
                block
                @verified="phone.captchaTicket = $event"
              />
            </div>
            <el-alert
              v-if="devCode"
              :title="`开发验证码：${devCode}`"
              type="info"
              :closable="false"
              class="dev-code-alert"
            />
            <el-button type="primary" size="large" :loading="loading" class="login-submit" @click="submitPhone">
              <span class="login-submit__text">进入系统</span>
              <span class="login-submit__arrow" aria-hidden="true">→</span>
            </el-button>
          </el-form>
        </div>

        <footer class="login-card__foot">
          <router-link class="login-link" to="/activation">首次激活</router-link>
          <span class="login-link__sep" aria-hidden="true" />
          <router-link class="login-link" to="/recovery">找回密码</router-link>
        </footer>
      </div>
    </section>
  </main>
</template>

<script setup src="./login.js"></script>
<style scoped lang="scss" src="./login.scss"></style>
