<template>
  <router-view />
</template>

<script setup>
import { useAppStore } from '@/store/app'
// 应用启动即实例化 app store，应用本地保存的主题（暗黑模式/主题色），登录页也生效
useAppStore()
</script>

<style>
#app {
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
}
</style>
