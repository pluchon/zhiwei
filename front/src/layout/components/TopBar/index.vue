<template>
  <el-menu
    class="topbar-menu"
    :ellipsis="false"
    :default-active="activeMenu"
    :active-text-color="theme"
    mode="horizontal"
  >
    <sidebar-item
      :key="route.path + index"
      v-for="(route, index) in topMenus"
      :item="route"
      :base-path="route.path"
    />

    <el-sub-menu
      index="more"
      class="el-sub-menu__hide-arrow"
      v-if="moreRoutes.length > 0"
    >
      <template #title>
        <span>更多菜单</span>
      </template>
      <sidebar-item
        :key="route.path + index"
        v-for="(route, index) in moreRoutes"
        :item="route"
        :base-path="route.path"
      />
    </el-sub-menu>
  </el-menu>
</template>

<script setup src="./index.js"></script>

<style lang="scss" src="./index.scss"></style>
