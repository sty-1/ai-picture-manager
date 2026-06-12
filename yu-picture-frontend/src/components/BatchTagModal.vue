<template>
  <div class="batch-tag-modal">
    <a-modal
      v-model:visible="visible"
      title="批量插入标签"
      width="640px"
      :footer="false"
      @cancel="closeModal"
    >
      <a-typography-paragraph type="secondary">
        已选中 {{ pictureIdList.length }} 张图片
      </a-typography-paragraph>

      <a-tabs v-model:activeKey="activeTab">
        <!-- 手动批量插入 -->
        <a-tab-pane key="manual" tab="手动批量插入标签">
          <a-form
            name="manualForm"
            layout="vertical"
            :model="manualForm"
            @finish="handleManualSubmit"
          >
            <a-form-item name="category" label="分类">
              <a-auto-complete
                v-model:value="manualForm.category"
                placeholder="请选择或输入分类"
                :options="categoryOptions"
                allow-clear
              />
            </a-form-item>
            <a-form-item name="tags" label="标签">
              <a-select
                v-model:value="manualForm.tags"
                mode="tags"
                placeholder="请选择或输入标签"
                :options="tagOptions"
                allow-clear
              />
            </a-form-item>
            <a-form-item>
              <a-button
                v-btn-animate
                type="primary"
                html-type="submit"
                :loading="manualLoading"
                style="width: 100%"
              >
                批量插入标签
              </a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <!-- AI 智能标签生成 -->
        <a-tab-pane key="ai" tab="AI 智能标签生成">
          <div class="ai-tab-content">
            <a-typography-paragraph type="secondary">
              AI 将分析选中图片的内容，自动生成分类和标签。
            </a-typography-paragraph>
            <a-button
              v-btn-animate
              type="primary"
              size="large"
              :loading="aiLoading"
              @click="handleAiGenerate"
            >
              {{ aiLoading ? 'AI 正在分析中…' : 'AI 批量生成' }}
            </a-button>
            <div v-if="aiResults.length > 0" class="ai-results">
              <a-divider>生成结果</a-divider>
              <a-table
                :columns="aiColumns"
                :data-source="aiResults"
                :pagination="false"
                size="small"
              />
            </div>
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import {
  editPictureByBatchUsingPost,
  generatePictureTagsUsingPost,
  listPictureTagCategoryUsingGet,
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'

interface Props {
  pictureIdList: (string | number)[]
  onSuccess: () => void
}

const props = defineProps<Props>()

const visible = ref(false)
const activeTab = ref('manual')

const openModal = () => {
  activeTab.value = 'manual'
  aiResults.value = []
  visible.value = true
}

const closeModal = () => {
  visible.value = false
}

defineExpose({ openModal })

// ---- 手动模式 ----
const manualLoading = ref(false)
const manualForm = reactive({
  category: '',
  tags: [] as string[],
})

const handleManualSubmit = async () => {
  manualLoading.value = true
  const res = await editPictureByBatchUsingPost({
    pictureIdList: props.pictureIdList,
    category: manualForm.category || undefined,
    tags: manualForm.tags.length > 0 ? manualForm.tags : undefined,
  })
  if (res.data.code === 0 && res.data.data) {
    message.success(`已为 ${props.pictureIdList.length} 张图片批量插入标签`)
    manualForm.category = ''
    manualForm.tags = []
    closeModal()
    props.onSuccess()
  } else {
    message.error('操作失败，' + res.data.message)
  }
  manualLoading.value = false
}

// ---- AI 模式 ----
const aiLoading = ref(false)
const aiResults = ref<
  { id: number; name: string; category: string; tags: string }[]
>([])

const aiColumns = [
  { title: 'ID', dataIndex: 'id', width: 60 },
  { title: '名称', dataIndex: 'name', ellipsis: true },
  { title: '分类', dataIndex: 'category', width: 100 },
  { title: '标签', dataIndex: 'tags', ellipsis: true },
]

const handleAiGenerate = async () => {
  aiLoading.value = true
  aiResults.value = []
  const res = await generatePictureTagsUsingPost({
    pictureIdList: props.pictureIdList,
  })
  if (res.data.code === 0 && res.data.data) {
    aiResults.value = (res.data.data ?? []).map((item: API.AiTagResult) => ({
      id: item.id,
      name: item.name ?? '-',
      category: item.category ?? '-',
      tags: (item.tags ?? []).join(', '),
    }))
    message.success('AI 标签生成完成')
    props.onSuccess()
  } else {
    message.error('AI 生成失败，' + res.data.message)
  }
  aiLoading.value = false
}

// ---- 分类/标签选项 ----
const categoryOptions = ref<{ value: string; label: string }[]>([])
const tagOptions = ref<{ value: string; label: string }[]>([])

onMounted(async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    tagOptions.value = (res.data.data.tagList ?? []).map((data: string) => ({
      value: data,
      label: data,
    }))
    categoryOptions.value = (res.data.data.categoryList ?? []).map(
      (data: string) => ({
        value: data,
        label: data,
      })
    )
  }
})
</script>

<style scoped>
.ai-tab-content {
  text-align: center;
  padding: 24px 0;
}

.ai-results {
  margin-top: 24px;
  text-align: left;
}
</style>
