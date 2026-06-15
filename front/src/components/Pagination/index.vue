<template>
  <div :class="{ hidden: hidden }" class="pagination-container">
    <el-pagination
      :background="background"
      v-model:current-page="currentPage"
      :page-size="limit"
      :layout="resolvedLayout"
      :page-sizes="showSizeSelect ? pageSizes : undefined"
      :pager-count="pagerCount"
      :total="total"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
</template>

<script setup src="./index.js"></script>

<style scoped lang="scss" src="./index.scss"></style>
