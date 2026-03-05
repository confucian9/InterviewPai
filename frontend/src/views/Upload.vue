<template>
  <div class="upload-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>上传面试录音</span>
        </div>
      </template>
      
      <el-upload
        ref="uploadRef"
        class="upload-area"
        drag
        :auto-upload="false"
        :limit="1"
        :on-change="handleFileChange"
        :on-exceed="handleExceed"
        accept=".mp3,.wav,.m4a"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">
          将音频文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持 mp3、wav、m4a 格式，文件大小不超过 100MB
          </div>
        </template>
      </el-upload>
      
      <div v-if="selectedFile" class="file-info">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="文件名">{{ selectedFile.name }}</el-descriptions-item>
          <el-descriptions-item label="大小">{{ formatSize(selectedFile.size) }}</el-descriptions-item>
        </el-descriptions>
        
        <div class="upload-actions">
          <el-button type="primary" size="large" :loading="uploading" @click="handleUpload">
            <el-icon><Upload /></el-icon>
            开始上传
          </el-button>
          <el-button size="large" @click="clearFile">清除</el-button>
        </div>
      </div>
    </el-card>
    
    <el-card class="mt-20">
      <template #header>
        <div class="card-header">
          <span>上传记录</span>
          <el-button text type="primary" @click="loadUploadHistory">刷新</el-button>
        </div>
      </template>
      
      <el-table :data="uploadHistory" v-loading="historyLoading" stripe>
        <el-table-column prop="fileName" label="文件名" min-width="200" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="上传时间" width="180" />
        <el-table-column label="操作" width="200">
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
            <el-button
              type="danger"
              size="small"
              @click="handleDelete(row.id)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { uploadAudio, getAudioList, processAudio, deleteAudio, type AudioRecordDTO } from '@/api/audio'

const router = useRouter()

const uploadRef = ref()
const selectedFile = ref<File | null>(null)
const uploading = ref(false)
const historyLoading = ref(false)
const uploadHistory = ref<AudioRecordDTO[]>([])

onMounted(() => {
  loadUploadHistory()
})

function handleFileChange(file: any) {
  if (file.size > 100 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过100MB')
    return false
  }
  selectedFile.value = file.raw
  return true
}

function handleExceed() {
  ElMessage.warning('一次只能上传一个文件')
}

function clearFile() {
  selectedFile.value = null
  uploadRef.value?.clearFiles()
}

async function handleUpload() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  
  uploading.value = true
  try {
    const res = await uploadAudio(selectedFile.value)
    ElMessage.success('上传成功')
    clearFile()
    loadUploadHistory()
    
    ElMessageBox.confirm('音频上传成功，是否立即开始处理？', '提示', {
      confirmButtonText: '开始处理',
      cancelButtonText: '稍后处理',
      type: 'info'
    }).then(() => {
      handleProcess(res.data.audioId)
    }).catch(() => {})
  } finally {
    uploading.value = false
  }
}

async function loadUploadHistory() {
  historyLoading.value = true
  try {
    const res = await getAudioList(1, 20)
    uploadHistory.value = res.data.records
  } finally {
    historyLoading.value = false
  }
}

async function handleProcess(audioId: number) {
  try {
    await processAudio(audioId)
    ElMessage.success('已开始处理，请稍后刷新查看结果')
    loadUploadHistory()
  } catch (error) {
    console.error(error)
  }
}

async function handleDelete(audioId: number) {
  try {
    await ElMessageBox.confirm('确定要删除这条记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteAudio(audioId)
    ElMessage.success('删除成功')
    loadUploadHistory()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

function goDetail(id: number) {
  router.push(`/interview/${id}`)
}

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
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
.upload-container {
  .upload-area {
    width: 100%;
    
    :deep(.el-upload-dragger) {
      width: 100%;
      height: 200px;
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
    }
  }
  
  .file-info {
    margin-top: 20px;
    
    .upload-actions {
      margin-top: 20px;
      text-align: center;
    }
  }
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
