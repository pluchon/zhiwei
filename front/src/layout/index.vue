<template>
  <div
    :class="classObj"
    class="app-wrapper"
    :style="{
      '--current-color': theme,
      '--current-color-light': theme + '1a',
      '--current-color-dark-bg': theme + '33',
    }"
  >
    <div
      v-if="device === 'mobile' && sidebar.opened"
      class="drawer-bg"
      @click="handleClickOutside"
    />
    <sidebar v-if="!sidebar.hide" class="sidebar-container" />
    <div
      :class="{ hasTagsView: needTagsView, sidebarHide: sidebar.hide }"
      class="main-container"
    >
      <div :class="{ 'fixed-header': fixedHeader }">
        <navbar @setLayout="setLayout" />
        <tags-view v-if="needTagsView" />
      </div>
      <app-main />
      <settings ref="settingRef" />
    </div>
  </div>
</template>

<script setup src="./index.js"></script>

<style lang="scss" scoped src="./index.scss"></style>
