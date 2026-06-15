<template>
  <div class="header-search">
    <svg-icon
      class-name="search-icon"
      icon-class="search"
      @click.stop="click"
    />

    <el-dialog
      v-model="show"
      width="600"
      :show-close="false"
      append-to-body
      @close="close"
      @opened="onDialogOpened"
    >
      <el-input
        ref="headerSearchSelectRef"
        v-model="search"
        size="large"
        prefix-icon="Search"
        placeholder="菜单搜索，支持标题、URL 模糊查询"
        clearable
        @input="querySearch"
        @keyup.enter="selectActiveResult"
        @keydown.up.prevent="navigateResult('up')"
        @keydown.down.prevent="navigateResult('down')"
      />

      <div v-if="search && options.length > 0" class="result-count">
        找到 <strong>{{ options.length }}</strong> 个结果
      </div>

      <div class="result-wrap">
        <el-scrollbar>
          <template v-if="options.length > 0">
            <div
              v-for="(item, index) in options"
              :key="item.path"
              class="search-item"
              tabindex="1"
              :class="{ 'is-active': index === activeIndex }"
              :style="activeStyle(index)"
              @mouseenter="activeIndex = index"
              @mouseleave="activeIndex = -1"
            >
              <div class="left">
                <svg-icon class="menu-icon" :icon-class="item.icon" />
              </div>

              <div class="search-info" @click="change(item)">
                <div
                  class="menu-title"
                  v-html="highlightText(item.title.join(' / '))"
                />
                <div class="menu-path" v-html="highlightText(item.path)" />
              </div>

              <svg-icon v-show="index === activeIndex" icon-class="enter" />
            </div>
          </template>

          <div v-else-if="search && options.length === 0" class="empty-state">
            <el-icon class="empty-icon">
              <Search />
            </el-icon>
            <p class="empty-text">
              未找到 "<strong>{{ search }}</strong
              >" 相关菜单
            </p>
            <p class="empty-tip">试试其他关键词或路径</p>
          </div>
        </el-scrollbar>
      </div>

      <div class="search-footer">
        <span class="shortcut-item">
          <kbd>↑</kbd>
          <kbd>↓</kbd>
          切换
        </span>
        <span class="shortcut-item">
          <kbd>↵</kbd>
          选择
        </span>
        <span class="shortcut-item">
          <kbd>Esc</kbd>
          关闭
        </span>
      </div>
    </el-dialog>
  </div>
</template>

<script setup src="./index.js"></script>

<style lang="scss" scoped src="./index.scss"></style>
