<template>
  <el-scrollbar
    ref="scrollContainer"
    :vertical="false"
    class="scroll-container"
    @wheel.prevent="handleScroll"
  >
    <slot />
  </el-scrollbar>
</template>

<script setup src="./ScrollPane.js"></script>

<style lang="scss" scoped src="./ScrollPane.scss"></style>
