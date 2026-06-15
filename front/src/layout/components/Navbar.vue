<template>
  <div class="navbar" :class="'nav' + settingsStore.navType">
    <hamburger
      id="hamburger-container"
      :is-active="appStore.sidebar.opened"
      class="hamburger-container"
      @toggleClick="toggleSideBar"
    />
    <breadcrumb
      v-if="settingsStore.navType == 1"
      id="breadcrumb-container"
      class="breadcrumb-container"
    />
    <top-nav
      v-if="settingsStore.navType == 2"
      id="topmenu-container"
      class="topmenu-container"
    />
    <template v-if="settingsStore.navType == 3">
      <logo v-show="settingsStore.sidebarLogo" :collapse="false"></logo>
      <top-bar id="topbar-container" class="topbar-container" />
    </template>

    <div class="right-menu">
      <div
        v-if="canUseAiAssistant"
        class="right-menu-item hover-effect ai-assistant-entry"
        @click="aiAssistantVisible = true"
      >
        <span class="ai-assistant-entry__label">AI 助手</span>
        <el-icon class="ai-assistant-entry__icon"><MagicStick /></el-icon>
      </div>

      <template v-if="appStore.device !== 'mobile'">
        <screenfull id="screenfull" class="right-menu-item hover-effect" />

        <el-tooltip content="布局大小" effect="dark" placement="bottom">
          <size-select id="size-select" class="right-menu-item hover-effect" />
        </el-tooltip>
      </template>

      <el-dropdown
        @command="handleCommand"
        class="avatar-container right-menu-item hover-effect"
        trigger="hover"
      >
        <div class="avatar-wrapper">
          <img :src="userStore.avatar" class="user-avatar" />
          <span class="user-nickname"> {{ userStore.nickName }} </span>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <router-link to="/user/profile">
              <el-dropdown-item>个人中心</el-dropdown-item>
            </router-link>
            <el-dropdown-item
              command="setLayout"
              v-if="settingsStore.showSettings"
            >
              <span>布局设置</span>
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <span>退出登录</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
    <AiAssistant v-model="aiAssistantVisible" />
  </div>
</template>

<script setup src="./Navbar.js"></script>

<style lang="scss" scoped src="./Navbar.scss"></style>
