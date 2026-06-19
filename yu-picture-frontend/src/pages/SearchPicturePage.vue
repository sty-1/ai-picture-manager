<template>
  <div id="searchPicturePage">
    <h2 style="margin-bottom: 16px">以图搜图</h2>
    <h3 style="margin-bottom: 16px">原图</h3>
    <a-card hoverable style="width: 240px">
      <template #cover>
        <img
          :alt="picture.name"
          :src="picture.thumbnailUrl ?? picture.url"
          style="height: 180px; object-fit: cover"
        />
      </template>
    </a-card>
    <div style="margin: 24px 0">
      <a-button type="primary" size="large" :loading="searching" @click="doSearch">
        在百度中搜索相似图片
      </a-button>
    </div>
    <!-- 搜索结果列表 -->
    <h3 v-if="dataList.length > 1" style="margin: 16px 0">识图结果</h3>
    <a-list
      v-if="dataList.length > 1"
      :grid="{ gutter: 16, xs: 1, sm: 2, md: 3, lg: 4, xl: 5, xxl: 6 }"
      :data-source="dataList"
    >
      <template #renderItem="{ item: picture }">
        <a-list-item style="padding: 0">
          <a :href="picture.fromUrl" target="_blank">
            <a-card hoverable>
              <template #cover>
                <img
                  :alt="picture.name"
                  :src="picture.thumbUrl"
                  style="height: 180px; object-fit: cover"
                />
              </template>
            </a-card>
          </a>
        </a-list-item>
      </template>
    </a-list>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  getPictureVoByIdUsingGet,
  searchPictureByPictureUsingPost,
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const pictureId = computed(() => {
  return route.query?.pictureId
})
const picture = ref<API.PictureVO>({})
const searching = ref(false)

// 获取图片详情
const fetchPictureDetail = async () => {
  try {
    const res = await getPictureVoByIdUsingGet({
      id: pictureId.value,
    })
    if (res.data.code === 0 && res.data.data) {
      picture.value = res.data.data
    } else {
      message.error('获取图片详情失败，' + res.data.message)
    }
  } catch (e: any) {
    message.error('获取图片详情失败：' + e.message)
  }
}

onMounted(() => {
  fetchPictureDetail()
})

// 搜图结果
const dataList = ref<API.ImageSearchResult[]>([])

// 点击在百度搜索
const doSearch = async () => {
  if (!pictureId.value) {
    message.warn('缺少图片ID')
    return
  }
  searching.value = true
  // 在异步请求前同步打开窗口，避免被浏览器弹窗拦截器阻止
  const searchWindow = window.open('about:blank', '_blank')
  try {
    const res = await searchPictureByPictureUsingPost({
      pictureId: pictureId.value,
    })
    if (res.data.code === 0 && res.data.data) {
      dataList.value = res.data.data ?? []
      if (dataList.value.length > 0 && dataList.value[0].fromUrl && searchWindow) {
        searchWindow.location.href = dataList.value[0].fromUrl
      } else if (searchWindow) {
        searchWindow.close()
      }
    } else {
      if (searchWindow) searchWindow.close()
      message.error('获取搜图结果失败，' + res.data.message)
    }
  } catch (e: any) {
    if (searchWindow) searchWindow.close()
    message.error('获取搜图结果失败：' + e.message)
  }
  searching.value = false
}
</script>

<style scoped>
#searchPicturePage {
  margin-bottom: 16px;
}
</style>
