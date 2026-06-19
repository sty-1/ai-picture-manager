<template>
  <div id="globalHeader">
    <div class="header-inner">
      <!-- 胶囊导航（含 logo + 菜单项） -->
      <PillNav
        :logo="logoUrl"
        logo-alt="人类图库MANAGER"
        :items="navItems"
        :active-href="activePath"
        base-color="#e0e0e0"
        pill-color="#fafafa"
        hovered-pill-text-color="#333"
        pill-text-color="#333"
        class="header-pill-nav"
      />

      <!-- 用户信息展示栏 -->
      <div class="user-login-status">
        <div v-if="loginUserStore.loginUser.id">
          <a-dropdown>
            <a-space class="user-trigger">
              <a-avatar :src="loginUserStore.loginUser.userAvatar" />
              {{ loginUserStore.loginUser.userName ?? '无名' }}
            </a-space>
            <template #overlay>
              <a-menu>
                <a-menu-item>
                  <router-link to="/user/profile">
                    <EditOutlined />
                    修改信息
                  </router-link>
                </a-menu-item>
                <a-menu-item>
                  <router-link to="/my_space">
                    <UserOutlined />
                    我的空间
                  </router-link>
                </a-menu-item>
                <a-menu-item @click="doLogout">
                  <LogoutOutlined />
                  退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
        <div v-else>
          <a-button v-btn-animate type="primary" href="/user/login">登录</a-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue'
import { EditOutlined, LogoutOutlined, UserOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { userLogoutUsingPost } from '@/api/userController.ts'
import PillNav from '@/components/PillNav.vue'
import type { PillNavItem } from '@/components/PillNav.vue'
import logoImg from '@/assets/logo.png'

const loginUserStore = useLoginUserStore()
const router = useRouter()

const logoUrl = logoImg
const activePath = ref<string>(router.currentRoute.value.path)

// 监听路由变化
router.afterEach((to) => {
  activePath.value = to.path
})

// 所有导航项
const allNavItems: PillNavItem[] = [
  { label: '主页', href: '/' },
  { label: '创建图片', href: '/add_picture' },
  { label: '用户管理', href: '/admin/userManage' },
  { label: '图片管理', href: '/admin/pictureManage' },
  { label: '空间管理', href: '/admin/spaceManage' },
]

// 根据权限过滤
const navItems = computed(() => {
  return allNavItems.filter((item) => {
    if (item.href.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
})

// 用户注销
const doLogout = async () => {
  const res = await userLogoutUsingPost()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({ userName: '未登录' })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
#globalHeader {
  height: 100%;
}

.header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0 20px;
}

.header-pill-nav {
  flex-shrink: 0;
}

.user-login-status {
  flex-shrink: 0;
}

.user-trigger {
  cursor: pointer;
}
</style>
