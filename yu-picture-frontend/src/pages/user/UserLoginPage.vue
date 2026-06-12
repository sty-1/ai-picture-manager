<template>
  <div class="login-wrapper">
    <!-- 内层：3D 图片球 -->
    <InfiniteMenu :items="menuItems" class="menu-bg">
      <template #overlay="{ activeItem, isMoving }">
        <Transition name="fade">
          <div v-if="activeItem && !isMoving" class="overlay-left">
            {{ activeItem.title }}
          </div>
        </Transition>
        <Transition name="fade">
          <div v-if="activeItem && !isMoving" class="overlay-right">
            {{ activeItem.description }}
          </div>
        </Transition>
      </template>
    </InfiniteMenu>

    <!-- 外层：原有的登录框 -->
    <div class="login-card-wrapper">
      <div id="userLoginPage">
        <h2 class="title">智能云图库 - 用户登录</h2>
        <div class="desc">企业级智能协同云图库</div>
        <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
          <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
            <a-input v-model:value="formState.userAccount" placeholder="请输入账号" />
          </a-form-item>
          <a-form-item
            name="userPassword"
            :rules="[
              { required: true, message: '请输入密码' },
              { min: 8, message: '密码长度不能小于 8 位' },
            ]"
          >
            <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" />
          </a-form-item>
          <div class="tips">
            没有账号？
            <RouterLink to="/user/register">去注册</RouterLink>
          </div>
          <a-form-item>
            <a-button v-btn-animate type="primary" html-type="submit" style="width: 100%">登录</a-button>
          </a-form-item>
        </a-form>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { reactive } from 'vue'
import { userLoginUsingPost } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { message } from 'ant-design-vue'
import router from '@/router'
import InfiniteMenu from '@/components/InfiniteMenu.vue'
import type { InfiniteMenuItem } from '@/components/InfiniteMenu.vue'

const menuItems: InfiniteMenuItem[] = [
  {
    image: '/images/landscape.png',
    title: 'picture',
    description: '风景',
  },
  {
    image: '/images/emoji.png',
    title: 'picture',
    description: '表情包',
  },
  {
    image: '/images/campus.jpg',
    title: 'picture',
    description: '校园',
  },
  {
    image: '/images/game.png',
    title: 'picture',
    description: '游戏',
  },
  {
    image: '/images/beauty.png',
    title: 'picture',
    description: '美女',
  },
]

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

const loginUserStore = useLoginUserStore()

const handleSubmit = async (values: any) => {
  const res = await userLoginUsingPost(values)
  if (res.data.code === 0 && res.data.data) {
    await loginUserStore.fetchLoginUser()
    message.success('登录成功')
    router.push({
      path: '/',
      replace: true,
    })
  } else {
    message.error('登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
.login-wrapper {
  position: fixed;
  inset: 0;
  overflow: hidden;
  background: #fff;
}

.menu-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.login-card-wrapper {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 10;
  max-width: 360px;
  width: 100%;
  background: transparent;
  backdrop-filter: blur(16px);
  border-radius: 12px;
  padding: 32px 28px;
  box-shadow: 0 2px 20px rgba(0, 0, 0, 0.08);
}

/* ── 覆盖动画层文字 ── */
.overlay-left {
  position: absolute;
  top: 50%;
  left: 8%;
  transform: translateY(-50%);
  font-size: 2rem;
  font-weight: 700;
  color: #333;
  user-select: none;
  pointer-events: none;
  z-index: 5;
}

.overlay-right {
  position: absolute;
  top: 50%;
  right: 8%;
  transform: translateY(-50%);
  font-size: 2rem;
  font-weight: 700;
  color: #333;
  user-select: none;
  pointer-events: none;
  z-index: 5;
}

/* ── 淡入淡出 ── */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.4s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ── 原有样式（保持不变） ── */
#userLoginPage {
  width: 100%;
}

.title {
  text-align: center;
  margin-bottom: 16px;
}

.desc {
  text-align: center;
  color: #bbb;
  margin-bottom: 16px;
}

.tips {
  color: #bbb;
  text-align: right;
  font-size: 13px;
  margin-bottom: 16px;
}
</style>
