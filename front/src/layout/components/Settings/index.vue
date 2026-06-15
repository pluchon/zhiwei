<template>
  <el-drawer
    v-model="showSettings"
    :withHeader="false"
    :lock-scroll="false"
    direction="rtl"
    size="300px"
  >
    <div class="setting-drawer-title">
      <h3 class="drawer-title">菜单导航设置</h3>
    </div>
    <div class="nav-wrap">
      <el-tooltip content="左侧菜单" placement="bottom">
        <div
          class="item left"
          @click="handleNavType(1)"
          :class="{ activeItem: navType == 1 }"
        >
          <b></b><b></b>
        </div>
      </el-tooltip>

      <el-tooltip content="混合菜单" placement="bottom">
        <div
          class="item mix"
          @click="handleNavType(2)"
          :class="{ activeItem: navType == 2 }"
        >
          <b></b><b></b>
        </div>
      </el-tooltip>
      <el-tooltip content="顶部菜单" placement="bottom">
        <div
          class="item top"
          @click="handleNavType(3)"
          :class="{ activeItem: navType == 3 }"
        >
          <b></b><b></b>
        </div>
      </el-tooltip>
    </div>
    <div class="setting-drawer-title">
      <h3 class="drawer-title">主题风格设置</h3>
    </div>
    <div class="setting-drawer-block-checbox">
      <div
        class="setting-drawer-block-checbox-item"
        @click="handleTheme('theme-dark')"
      >
        <img src="@/assets/images/dark.svg" alt="dark" />
        <div
          v-if="sideTheme === 'theme-dark'"
          class="setting-drawer-block-checbox-selectIcon"
          style="display: block"
        >
          <i aria-label="图标: check" class="anticon anticon-check">
            <svg
              viewBox="64 64 896 896"
              data-icon="check"
              width="1em"
              height="1em"
              :fill="theme"
              aria-hidden="true"
              focusable="false"
              class
            >
              <path
                d="M912 190h-69.9c-9.8 0-19.1 4.5-25.1 12.2L404.7 724.5 207 474a32 32 0 0 0-25.1-12.2H112c-6.7 0-10.4 7.7-6.3 12.9l273.9 347c12.8 16.2 37.4 16.2 50.3 0l488.4-618.9c4.1-5.1.4-12.8-6.3-12.8z"
              />
            </svg>
          </i>
        </div>
      </div>
      <div
        class="setting-drawer-block-checbox-item"
        @click="handleTheme('theme-light')"
      >
        <img src="@/assets/images/light.svg" alt="light" />
        <div
          v-if="sideTheme === 'theme-light'"
          class="setting-drawer-block-checbox-selectIcon"
          style="display: block"
        >
          <i aria-label="图标: check" class="anticon anticon-check">
            <svg
              viewBox="64 64 896 896"
              data-icon="check"
              width="1em"
              height="1em"
              :fill="theme"
              aria-hidden="true"
              focusable="false"
              class
            >
              <path
                d="M912 190h-69.9c-9.8 0-19.1 4.5-25.1 12.2L404.7 724.5 207 474a32 32 0 0 0-25.1-12.2H112c-6.7 0-10.4 7.7-6.3 12.9l273.9 347c12.8 16.2 37.4 16.2 50.3 0l488.4-618.9c4.1-5.1.4-12.8-6.3-12.8z"
              />
            </svg>
          </i>
        </div>
      </div>
    </div>
    <div class="drawer-item">
      <span>主题颜色</span>
      <span class="comp-style">
        <el-color-picker
          v-model="theme"
          :predefine="predefineColors"
          @change="themeChange"
        />
      </span>
    </div>
    <el-divider />

    <h3 class="drawer-title">系统布局配置</h3>

    <div class="drawer-item">
      <span>开启页签</span>
      <span class="comp-style">
        <el-switch v-model="settingsStore.tagsView" class="drawer-switch" />
      </span>
    </div>

    <div class="drawer-item">
      <span>持久化标签页</span>
      <span class="comp-style">
        <el-switch
          v-model="settingsStore.tagsViewPersist"
          :disabled="!settingsStore.tagsView"
          @change="tagsViewPersistChange"
          class="drawer-switch"
        />
      </span>
    </div>

    <div class="drawer-item">
      <span>显示页签图标</span>
      <span class="comp-style">
        <el-switch
          v-model="settingsStore.tagsIcon"
          :disabled="!settingsStore.tagsView"
          class="drawer-switch"
        />
      </span>
    </div>

    <div class="drawer-item">
      <span>标签页样式</span>
      <span class="comp-style">
        <el-radio-group
          v-model="settingsStore.tagsViewStyle"
          :disabled="!settingsStore.tagsView"
          size="small"
        >
          <el-radio-button label="card">卡片</el-radio-button>
          <el-radio-button label="chrome">谷歌</el-radio-button>
        </el-radio-group>
      </span>
    </div>

    <div class="drawer-item">
      <span>固定 Header</span>
      <span class="comp-style">
        <el-switch v-model="settingsStore.fixedHeader" class="drawer-switch" />
      </span>
    </div>

    <div class="drawer-item">
      <span>显示 Logo</span>
      <span class="comp-style">
        <el-switch v-model="settingsStore.sidebarLogo" class="drawer-switch" />
      </span>
    </div>

    <div class="drawer-item">
      <span>动态标题</span>
      <span class="comp-style">
        <el-switch
          v-model="settingsStore.dynamicTitle"
          @change="dynamicTitleChange"
          class="drawer-switch"
        />
      </span>
    </div>

    <div class="drawer-item">
      <span>底部版权</span>
      <span class="comp-style">
        <el-switch
          v-model="settingsStore.footerVisible"
          class="drawer-switch"
        />
      </span>
    </div>

    <el-divider />

    <el-button type="primary" plain icon="DocumentAdd" @click="saveSetting"
      >保存配置</el-button
    >
    <el-button plain icon="Refresh" @click="resetSetting">重置配置</el-button>
  </el-drawer>
</template>

<script setup src="./index.js"></script>

<style lang="scss" scoped src="./index.scss"></style>
