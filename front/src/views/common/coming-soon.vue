<template>
  <el-card shadow="never">
    <el-result
      icon="info"
      :title="route.meta.title || '页面建设中'"
      sub-title="当前页面已纳入第一阶段开发计划"
    >
      <template #extra>
        <el-button type="primary" @click="router.push('/index')"
          >返回首页</el-button
        >
      </template>
    </el-result>
  </el-card>
</template>

<script setup src="./coming-soon.js"></script>
