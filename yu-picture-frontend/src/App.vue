<template>
  <div id="app">
    <a-config-provider :locale="zhCN">
      <BasicLayout />
    </a-config-provider>
  </div>
</template>

<script setup lang="ts">
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
dayjs.locale('zh-cn')

import BasicLayout from '@/layouts/BasicLayout.vue'
import '@/styles/global-button.css'
import '@/styles/global-input.css'
import '@/styles/global-glass.css'
</script>

<style scoped></style>
