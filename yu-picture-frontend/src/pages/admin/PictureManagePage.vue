<template>
  <div id="pictureManagePage">
    <a-flex justify="space-between">
      <h2>图片管理</h2>
      <a-space>
        <a-button v-btn-animate type="primary" href="/add_picture" target="_blank">+ 创建图片</a-button>
        <a-button v-btn-animate type="primary" href="/add_picture/batch" target="_blank" ghost>+ 批量创建图片</a-button>
        <a-button
          v-btn-animate
          type="primary"
          ghost
          :disabled="selectedRowKeys.length === 0"
          @click="doBatchPass"
        >
          批量通过
        </a-button>
        <a-button
          v-btn-animate
          danger
          :disabled="selectedRowKeys.length === 0"
          @click="doBatchDelete"
        >
          批量删除
        </a-button>
        <a-button
          v-btn-animate
          type="primary"
          :disabled="selectedRowKeys.length === 0"
          @click="doBatchTag"
        >
          批量插入标签
        </a-button>
      </a-space>
    </a-flex>
    <div style="margin-bottom: 16px" />
    <!-- 搜索表单 -->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="关键词">
        <a-input
          v-model:value="searchParams.searchText"
          placeholder="从名称和简介搜索"
          allow-clear
        />
      </a-form-item>
      <a-form-item label="类型">
        <a-input v-model:value="searchParams.category" placeholder="请输入类型" allow-clear />
      </a-form-item>
      <a-form-item label="标签">
        <a-select
          v-model:value="searchParams.tags"
          mode="tags"
          placeholder="请输入标签"
          style="min-width: 180px"
          allow-clear
        />
      </a-form-item>
      <a-form-item name="reviewStatus" label="审核状态">
        <a-select
          v-model:value="searchParams.reviewStatus"
          style="min-width: 180px"
          placeholder="请选择审核状态"
          :options="PIC_REVIEW_STATUS_OPTIONS"
          allow-clear
        />
      </a-form-item>
      <a-form-item>
        <a-button v-btn-animate type="primary" html-type="submit">搜索</a-button>
      </a-form-item>
    </a-form>
    <div style="margin-bottom: 16px" />
    <!-- 表格 -->
    <a-table
      :row-selection="rowSelection"
      :row-key="(record: API.Picture) => record.id"
      :columns="columns"
      :data-source="dataList"
      :pagination="pagination"
      @change="doTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'url'">
          <a-image :src="record.url" :width="120" />
        </template>
        <template v-if="column.dataIndex === 'tags'">
          <a-space wrap>
            <a-tag v-for="tag in JSON.parse(record.tags || '[]')" :key="tag">
              {{ tag }}
            </a-tag>
          </a-space>
        </template>
        <template v-if="column.dataIndex === 'picInfo'">
          <div>格式：{{ record.picFormat }}</div>
          <div>宽度：{{ record.picWidth }}</div>
          <div>高度：{{ record.picHeight }}</div>
          <div>宽高比：{{ record.picScale }}</div>
          <div>大小：{{ (record.picSize / 1024).toFixed(2) }}KB</div>
        </template>
        <template v-if="column.dataIndex === 'reviewMessage'">
          <div>审核状态：{{ PIC_REVIEW_STATUS_MAP[record.reviewStatus] }}</div>
          <div>审核信息：{{ record.reviewMessage }}</div>
          <div>
            审核人：<span v-if="record.reviewerId">管理员（{{ record.reviewerId }}）</span>
            <a-tag v-else color="purple">AI 审核</a-tag>
          </div>
          <div v-if="record.reviewTime">
            审核时间：{{ dayjs(record.reviewTime).format('YYYY-MM-DD HH:mm:ss') }}
          </div>
        </template>
        <template v-if="column.dataIndex === 'createTime'">
          {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>
        <template v-if="column.dataIndex === 'editTime'">
          {{ dayjs(record.editTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space wrap>
            <a-button
              v-if="record.reviewStatus !== PIC_REVIEW_STATUS_ENUM.PASS"
              type="link"
              @click="handleReview(record, PIC_REVIEW_STATUS_ENUM.PASS)"
            >
              通过
            </a-button>
            <a-button
              v-if="record.reviewStatus !== PIC_REVIEW_STATUS_ENUM.REJECT"
              type="link"
              danger
              @click="handleReview(record, PIC_REVIEW_STATUS_ENUM.REJECT)"
            >
              拒绝
            </a-button>
            <a-button type="link" :href="`/add_picture?id=${record.id}`" target="_blank">
              编辑
            </a-button>
            <a-button danger @click="doDelete(record.id)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
    <BatchTagModal
      ref="batchTagModalRef"
      :picture-id-list="selectedRowKeys"
      :on-success="onBatchTagSuccess"
    />
  </div>
</template>
<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  deletePictureUsingPost,
  doPictureReviewUsingPost,
  listPictureByPageUsingPost,
} from '@/api/pictureController.ts'
import { message, Modal } from 'ant-design-vue'
import {
  PIC_REVIEW_STATUS_ENUM,
  PIC_REVIEW_STATUS_MAP,
  PIC_REVIEW_STATUS_OPTIONS,
} from '../../constants/picture.ts'
import BatchTagModal from '@/components/BatchTagModal.vue'
import dayjs from 'dayjs'

const columns = [
  {
    title: 'id',
    dataIndex: 'id',
    width: 80,
  },
  {
    title: '图片',
    dataIndex: 'url',
  },
  {
    title: '名称',
    dataIndex: 'name',
  },
  {
    title: '简介',
    dataIndex: 'introduction',
    ellipsis: true,
  },
  {
    title: '类型',
    dataIndex: 'category',
  },
  {
    title: '标签',
    dataIndex: 'tags',
  },
  {
    title: '图片信息',
    dataIndex: 'picInfo',
  },
  {
    title: '用户 id',
    dataIndex: 'userId',
    width: 80,
  },
  {
    title: '空间 id',
    dataIndex: 'spaceId',
    width: 80,
  },
  {
    title: '审核信息',
    dataIndex: 'reviewMessage',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '编辑时间',
    dataIndex: 'editTime',
  },
  {
    title: '操作',
    key: 'action',
  },
]

// 定义数据
const dataList = ref<API.Picture[]>([])
const total = ref(0)

// 搜索条件
const searchParams = reactive<API.PictureQueryRequest>({
  current: 1,
  pageSize: 10,
  sortField: 'createTime',
  sortOrder: 'descend',
})

// 获取数据
const fetchData = async () => {
  const res = await listPictureByPageUsingPost({
    ...searchParams,
    nullSpaceId: true,
  })
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
}

// 页面加载时获取数据，请求一次
onMounted(() => {
  fetchData()
})

// 分页参数
const pagination = computed(() => {
  return {
    current: searchParams.current,
    pageSize: searchParams.pageSize,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total) => `共 ${total} 条`,
  }
})

// 表格变化之后，重新获取数据
const doTableChange = (page: { current: number; pageSize: number }) => {
  searchParams.current = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

// 搜索数据
const doSearch = () => {
  // 重置页码
  searchParams.current = 1
  fetchData()
}

// --- 批量选择 ---
const selectedRowKeys = ref<(string | number)[]>([])

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: (string | number)[]) => {
    selectedRowKeys.value = keys
  },
}))

// 批量通过
const doBatchPass = () => {
  const count = selectedRowKeys.value.length
  Modal.confirm({
    title: '批量审核通过',
    content: `确定要将选中的 ${count} 张图片全部审核通过吗？`,
    okText: '确认通过',
    cancelText: '取消',
    okButtonProps: { type: 'primary' },
    onOk: async () => {
      const results = await Promise.allSettled(
        selectedRowKeys.value.map((id) =>
          doPictureReviewUsingPost({
            id: id as string,
            reviewStatus: PIC_REVIEW_STATUS_ENUM.PASS,
            reviewMessage: '管理员批量操作通过',
          })
        )
      )
      const succeeded = results.filter((r) => r.status === 'fulfilled' && r.value.data?.code === 0).length
      const failed = count - succeeded
      if (failed > 0) {
        message.warning(`通过 ${succeeded} 张，失败 ${failed} 张`)
      } else {
        message.success(`已全部通过（${count} 张）`)
      }
      selectedRowKeys.value = []
      fetchData()
    },
  })
}

// --- 批量插入标签 ---
const batchTagModalRef = ref<InstanceType<typeof BatchTagModal>>()

const doBatchTag = () => {
  batchTagModalRef.value?.openModal()
}

const onBatchTagSuccess = () => {
  selectedRowKeys.value = []
  fetchData()
}

// 批量删除
const doBatchDelete = () => {
  const count = selectedRowKeys.value.length
  Modal.confirm({
    title: '批量删除图片',
    content: `确定要删除选中的 ${count} 张图片吗？此操作不可撤销。`,
    okText: '确认删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
    onOk: async () => {
      const results = await Promise.allSettled(
        selectedRowKeys.value.map((id) =>
          deletePictureUsingPost({ id: id as string })
        )
      )
      const succeeded = results.filter((r) => r.status === 'fulfilled' && r.value.data?.code === 0).length
      const failed = count - succeeded
      if (failed > 0) {
        message.warning(`删除 ${succeeded} 张，失败 ${failed} 张`)
      } else {
        message.success(`已全部删除（${count} 张）`)
      }
      selectedRowKeys.value = []
      fetchData()
    },
  })
}

// 删除数据
const doDelete = async (id: string) => {
  if (!id) {
    return
  }
  const res = await deletePictureUsingPost({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    // 刷新数据
    fetchData()
  } else {
    message.error('删除失败')
  }
}

// 审核图片
const handleReview = async (record: API.Picture, reviewStatus: number) => {
  const reviewMessage =
    reviewStatus === PIC_REVIEW_STATUS_ENUM.PASS ? '管理员操作通过' : '管理员操作拒绝'
  const res = await doPictureReviewUsingPost({
    id: record.id,
    reviewStatus,
    reviewMessage,
  })
  if (res.data.code === 0) {
    message.success('审核操作成功')
    // 重新获取列表数据
    fetchData()
  } else {
    message.error('审核操作失败，' + res.data.message)
  }
}
</script>
