<template>
  <section class="app-main">
    <router-view v-slot="{ Component, route }">
      <transition name="fade-transform" mode="out-in">
        <keep-alive :include="tagsViewStore.cachedViews">
          <component
            v-if="!route.meta.link"
            :is="Component"
            :key="route.path"
          />
        </keep-alive>
      </transition>
    </router-view>
    <copyright />
  </section>
</template>

<script setup src="./AppMain.js"></script>

<style lang="scss" scoped src="./AppMain.scss"></style>
