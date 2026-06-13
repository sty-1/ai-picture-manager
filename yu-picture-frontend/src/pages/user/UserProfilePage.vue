<template>
  <div id="userProfilePage">
    <h2 style="margin-bottom: 16px">修改个人信息</h2>
    <a-form name="profileForm" layout="vertical" :model="profileForm" @finish="handleSubmit">
      <a-form-item label="头像">
        <a-upload
          list-type="picture-card"
          :show-upload-list="false"
          :custom-request="handleAvatarUpload"
          :before-upload="beforeAvatarUpload"
        >
          <div class="avatar-upload-area">
            <img v-if="profileForm.userAvatar" :src="profileForm.userAvatar" alt="avatar" />
            <div v-else>
              <loading-outlined v-if="avatarLoading"></loading-outlined>
              <plus-outlined v-else></plus-outlined>
              <div class="ant-upload-text">点击或拖拽上传头像</div>
            </div>
          </div>
        </a-upload>
      </a-form-item>
      <a-form-item name="id" label="用户 ID">
        <a-input v-model:value="userId" disabled />
      </a-form-item>
      <a-form-item name="userName" label="昵称">
        <a-input v-model:value="profileForm.userName" placeholder="请输入昵称" allow-clear />
      </a-form-item>
      <a-form-item name="userProfile" label="个人简介">
        <a-textarea
          v-model:value="profileForm.userProfile"
          placeholder="请输入个人简介"
          :auto-size="{ minRows: 3, maxRows: 6 }"
          allow-clear
        />
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button v-btn-animate type="primary" html-type="submit" :loading="loading">保存</a-button>
          <a-button v-btn-animate @click="doCancel">取消</a-button>
        </a-space>
      </a-form-item>
    </a-form>

    <a-divider />

    <h2 style="margin-bottom: 16px">修改密码</h2>
    <a-form name="passwordForm" layout="vertical" :model="passwordForm" @finish="handleChangePassword">
      <a-form-item name="oldPassword" label="旧密码" :rules="[{ required: true, message: '请输入旧密码' }]">
        <a-input-password v-model:value="passwordForm.oldPassword" placeholder="请输入旧密码" allow-clear />
      </a-form-item>
      <a-form-item name="newPassword" label="新密码" :rules="[{ required: true, min: 8, message: '新密码不能少于 8 位' }]">
        <a-input-password v-model:value="passwordForm.newPassword" placeholder="请输入新密码（至少 8 位）" allow-clear />
      </a-form-item>
      <a-form-item name="checkPassword" label="确认新密码" :rules="[{ required: true, validator: validateCheckPassword }]">
        <a-input-password v-model:value="passwordForm.checkPassword" placeholder="请再次输入新密码" allow-clear />
      </a-form-item>
      <a-form-item>
        <a-button v-btn-animate type="primary" html-type="submit" :loading="pwdLoading">修改密码</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { UploadProps } from 'ant-design-vue'
import { LoadingOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { editProfileUsingPost, getLoginUserUsingGet } from '@/api/userController.ts'
import { uploadFileUsingPost } from '@/api/fileController.ts'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import myAxios from '@/request.ts'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const loading = ref(false)
const avatarLoading = ref(false)
const userId = ref<string>('')

const profileForm = reactive<API.UserEditRequest>({
  userName: '',
  userAvatar: '',
  userProfile: '',
})

// 加载当前用户信息
const fetchUserInfo = async () => {
  const res = await getLoginUserUsingGet()
  if (res.data.code === 0 && res.data.data) {
    const user = res.data.data
    userId.value = String(user.id ?? '')
    profileForm.userName = user.userName ?? ''
    profileForm.userAvatar = user.userAvatar ?? ''
    profileForm.userProfile = user.userProfile ?? ''
  } else {
    message.error('获取用户信息失败，' + res.data.message)
  }
}

onMounted(() => {
  fetchUserInfo()
})

// 上传前校验
const beforeAvatarUpload = (file: UploadProps['fileList'][number]) => {
  const isJpgOrPng =
    file.type === 'image/jpeg' || file.type === 'image/png'
  if (!isJpgOrPng) {
    message.error('只支持上传 JPG/PNG 格式的图片')
    return false
  }
  const isLt2M = (file.size as number) / 1024 / 1024 < 2
  if (!isLt2M) {
    message.error('头像不能超过 2M')
    return false
  }
  return true
}

// 自定义上传头像
const handleAvatarUpload = async ({ file }: any) => {
  avatarLoading.value = true
  try {
    const res = await uploadFileUsingPost(file)
    if (res.data.code === 0 && res.data.data) {
      profileForm.userAvatar = res.data.data
      message.success('头像上传成功')
    } else {
      message.error('头像上传失败，' + res.data.message)
    }
  } catch (e: any) {
    message.error('头像上传失败，' + e.message)
  }
  avatarLoading.value = false
}

// 提交修改
const handleSubmit = async () => {
  loading.value = true
  try {
    const res = await editProfileUsingPost({ ...profileForm })
    if (res.data.code === 0) {
      message.success('修改成功')
      const currentUser = loginUserStore.loginUser
      loginUserStore.setLoginUser({
        ...currentUser,
        userName: profileForm.userName,
        userAvatar: profileForm.userAvatar,
        userProfile: profileForm.userProfile,
      })
      router.back()
    } else {
      message.error('修改失败，' + res.data.message)
    }
  } catch (e: any) {
    message.error('修改失败，' + e.message)
  }
  loading.value = false
}

const doCancel = () => {
  router.back()
}

// ---- 修改密码 ----
const pwdLoading = ref(false)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  checkPassword: '',
})

const validateCheckPassword = async (_rule: any, value: string) => {
  if (!value) {
    return Promise.reject('请确认新密码')
  }
  if (value !== passwordForm.newPassword) {
    return Promise.reject('两次输入的新密码不一致')
  }
  return Promise.resolve()
}

const handleChangePassword = async () => {
  pwdLoading.value = true
  try {
    const res = await myAxios.post('/api/user/change-password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    if (res.data.code === 0) {
      message.success('密码修改成功，请重新登录')
      // 清空表单
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.checkPassword = ''
      // 清除登录态，跳转登录页
      setTimeout(() => {
        window.location.href = '/user/login'
      }, 1500)
    } else {
      message.error('修改失败，' + res.data.message)
    }
  } catch (e: any) {
    message.error('修改失败，' + (e.response?.data?.message || e.message))
  }
  pwdLoading.value = false
}
</script>

<style scoped>
#userProfilePage {
  max-width: 480px;
  margin: 0 auto;
}

/* 用户 ID 输入框置灰，表明不可修改 */
:deep(.ant-input-disabled) {
  background-color: #f5f5f5;
  color: #bfbfbf;
  cursor: not-allowed;
}

.avatar-upload-area {
  text-align: center;
  width: 102px;
  height: 102px;
}

.avatar-upload-area img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
}

.avatar-upload-area .ant-upload-text {
  margin-top: 8px;
  color: #666;
}
</style>
