<template>
  <el-button
    :type="verified ? 'success' : 'primary'"
    plain
    :size="compact || block ? 'large' : 'default'"
    :class="{
      'captcha-verify--compact': compact,
      'captcha-verify--block': block,
    }"
    :loading="loading"
    @click="verify"
  >
    {{ verified ? "已验证" : "验证" }}
  </el-button>
</template>
<script setup src="./index.js"></script>
<style scoped lang="scss">
.captcha-verify--block {
  width: 100%;
  display: inline-flex;
  justify-content: center;
  align-items: center;
  text-align: center;
}
</style>
