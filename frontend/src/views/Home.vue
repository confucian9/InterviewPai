<template>
  <div class="home-container">
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="welcome-card">
          <div class="welcome-content">
            <h2>欢迎回来，{{ userStore.user?.nickname || userStore.user?.username }}！</h2>
            <p>上传面试录音，让AI帮你整理面试经验</p>
          </div>
          <el-button type="primary" size="large" @click="router.push('/upload')">
            <el-icon><Upload /></el-icon>
            上传音频
          </el-button>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" class="mt-20">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>最近面试记录</span>
              <el-button text type="primary" @click="loadData">刷新</el-button>
            </div>
          </template>
          
          <el-table :data="interviewList" v-loading="loading" stripe>
            <el-table-column prop="fileName" label="文件名" min-width="200">
              <template #default="{ row }">
                <el-link type="primary" @click="goDetail(row.id)">
                  {{ row.fileName }}
                </el-link>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="上传时间" width="180" />
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 'UPLOADED'"
                  type="primary"
                  size="small"
                  @click="handleProcess(row.id)"
                >
                  开始处理
                </el-button>
                <el-button
                  v-if="row.status === 'FINISHED'"
                  type="success"
                  size="small"
                  @click="goDetail(row.id)"
                >
                  查看详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          
          <div class="pagination-container">
            <el-pagination
              v-model:current-page="pagination.page"
              v-model:page-size="pagination.size"
              :total="pagination.total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              @size-change="loadData"
              @current-change="loadData"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getAudioList, processAudio, type AudioRecordDTO } from '@/api/audio'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const interviewList = ref<AudioRecordDTO[]>([])

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const res = await getAudioList(pagination.page, pagination.size)
    interviewList.value = res.data.records
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

async function handleProcess(audioId: number) {
  try {
    await processAudio(audioId)
    ElMessage.success('已开始处理，请稍后刷新查看结果')
    loadData()
  } catch (error) {
    console.error(error)
  }
}

function goDetail(id: number) {
  router.push(`/interview/${id}`)
}

function getStatusType(status: string) {
  const map: Record<string, string> = {
    UPLOADED: 'info',
    PROCESSING: 'warning',
    FINISHED: 'success',
    FAILED: 'danger'
  }
  return map[status] || 'info'
}

function getStatusText(status: string) {
  const map: Record<string, string> = {
    UPLOADED: '待处理',
    PROCESSING: '处理中',
    FINISHED: '已完成',
    FAILED: '处理失败'
  }
  return map[status] || status
}
</script>

<style scoped lang="scss">
.home-container {
  .welcome-card {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    
    :deep(.el-card__body) {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    
    .welcome-content {
      h2 {
        margin: 0 0 8px;
        font-size: 24px;
      }
      
      p {
        margin: 0;
        opacity: 0.8;
      }
    }
  }
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
