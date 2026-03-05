<template>
  <div class="interview-detail" v-loading="loading">
    <el-page-header @back="router.back()">
      <template #content>
        <span class="page-title">{{ detail?.fileName || '面试详情' }}</span>
      </template>
      <template #extra>
        <el-button 
          v-if="detail?.status === 'PROCESSING' || detail?.status === 'FAILED'"
          type="warning" 
          @click="startStreamProcess"
          :loading="streaming"
        >
          {{ detail?.status === 'PROCESSING' ? '查看处理进度' : '重新处理' }}
        </el-button>
        <el-button 
          type="primary" 
          @click="showGenerateDialog"
          :disabled="detail?.status !== 'FINISHED'"
        >
          生成面经
        </el-button>
      </template>
    </el-page-header>
    
    <el-row :gutter="20" class="mt-20">
      <el-col :span="16">
        <el-card>
          <el-tabs v-model="activeTab">
            <el-tab-pane label="问答列表" name="qa">
              <div class="qa-list">
                <el-collapse v-if="detail?.qaList?.length">
                  <el-collapse-item
                    v-for="(qa, index) in detail.qaList"
                    :key="qa.id"
                    :name="index"
                  >
                    <template #title>
                      <div class="qa-title">
                        <el-tag type="primary" size="small">Q{{ index + 1 }}</el-tag>
                        <span class="question-text">{{ qa.question }}</span>
                      </div>
                    </template>
                    <div class="qa-content">
                      <div class="answer-section">
                        <div class="section-label">答案：</div>
                        <div class="answer-text">{{ qa.answer || '暂无答案' }}</div>
                      </div>
                      <div class="tags-section" v-if="qa.tags">
                        <div class="section-label">标签：</div>
                        <el-tag
                          v-for="tag in qa.tags.split(',')"
                          :key="tag"
                          size="small"
                          class="tag-item"
                        >
                          {{ tag }}
                        </el-tag>
                      </div>
                      <div class="qa-actions">
                        <el-button size="small" @click="editQa(qa)">编辑</el-button>
                        <el-button size="small" type="danger" @click="deleteQaRecord(qa.id)">删除</el-button>
                      </div>
                    </div>
                  </el-collapse-item>
                </el-collapse>
                <el-empty v-else description="暂无问答记录" />
              </div>
            </el-tab-pane>
            
            <el-tab-pane label="转写文本" name="transcript">
              <div class="transcript-content">
                <pre>{{ detail?.transcript || '暂无转写文本' }}</pre>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>AI总结</span>
          </template>
          <div v-if="detail?.summary" class="summary-content">
            <p>{{ detail.summary.summary }}</p>
            <div class="keywords" v-if="detail.summary.keywords">
              <span class="label">关键词：</span>
              <el-tag
                v-for="keyword in detail.summary.keywords.split(',')"
                :key="keyword"
                size="small"
                class="keyword-tag"
              >
                {{ keyword }}
              </el-tag>
            </div>
          </div>
          <el-empty v-else description="暂无总结" :image-size="80" />
        </el-card>
        
        <el-card class="mt-20">
          <template #header>
            <span>基本信息</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="文件名">{{ detail?.fileName }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="getStatusType(detail?.status)">
                {{ getStatusText(detail?.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="上传时间">{{ detail?.createTime }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
    
    <el-dialog v-model="editDialogVisible" title="编辑问答" width="600px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="问题">
          <el-input v-model="editForm.question" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="答案">
          <el-input v-model="editForm.answer" type="textarea" :rows="5" />
        </el-form-item>
        <el-form-item label="标签">
          <el-select
            v-model="editForm.tags"
            multiple
            filterable
            allow-create
            placeholder="选择或输入标签"
            style="width: 100%"
          >
            <el-option
              v-for="tag in allTags"
              :key="tag.id"
              :label="tag.name"
              :value="tag.name"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
    
    <el-dialog v-model="generateDialogVisible" title="生成面经" width="500px">
      <el-form :model="generateForm" label-width="80px">
        <el-form-item label="面经标题">
          <el-input v-model="generateForm.title" placeholder="请输入面经标题（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleGenerate" :loading="generating">生成</el-button>
      </template>
    </el-dialog>
    
    <el-dialog v-model="resultDialogVisible" title="面经预览" width="800px" top="5vh">
      <div class="experience-result">
        <div class="result-header">
          <h3>{{ experienceResult?.title }}</h3>
          <div class="result-actions">
            <el-button type="primary" @click="downloadExperience">下载文件</el-button>
            <el-button @click="copyContent">复制内容</el-button>
          </div>
        </div>
        <div class="result-content">
          <pre>{{ experienceResult?.content }}</pre>
        </div>
      </div>
    </el-dialog>
    
    <el-dialog v-model="processDialogVisible" title="实时处理进度" width="700px" :close-on-click-modal="false" :close-on-press-escape="false">
      <div class="process-status">
        <div class="status-item">
          <el-icon v-if="processStatus.status !== 'done'" class="is-loading"><Loading /></el-icon>
          <el-icon v-else color="#67C23A"><SuccessFilled /></el-icon>
          <span>{{ processStatus.status }}</span>
        </div>
        
        <div class="stream-output">
          <div class="output-label">LLM 输出：</div>
          <pre class="output-content">{{ processStatus.streamOutput || '等待输出...' }}</pre>
        </div>
        
        <el-progress 
          v-if="processStatus.chunkTotal > 0"
          :percentage="Math.round((processStatus.chunkIndex / processStatus.chunkTotal) * 100)"
          :format="() => `${processStatus.chunkIndex}/${processStatus.chunkTotal}`"
          class="mt-20"
        />
      </div>
      <template #footer>
        <el-button @click="cancelStream" :disabled="processStatus.status === 'done'">取消</el-button>
        <el-button type="primary" @click="closeProcessDialog" v-if="processStatus.status === 'done'">完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, SuccessFilled } from '@element-plus/icons-vue'
import { getInterviewDetail, updateQa, deleteQa, generateExperience, streamProcessAudio, type InterviewDetailDTO, type QaRecordDTO, type GenerateExperienceResponse } from '@/api/interview'
import { getAllTags, type Tag } from '@/api/review'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const activeTab = ref('qa')
const detail = ref<InterviewDetailDTO | null>(null)
const allTags = ref<Tag[]>([])

const editDialogVisible = ref(false)
const editForm = reactive({
  id: 0,
  question: '',
  answer: '',
  tags: [] as string[]
})

const generateDialogVisible = ref(false)
const generating = ref(false)
const generateForm = reactive({
  title: ''
})

const resultDialogVisible = ref(false)
const experienceResult = ref<GenerateExperienceResponse | null>(null)

const streaming = ref(false)
const processDialogVisible = ref(false)
const processStatus = reactive({
  status: '',
  streamOutput: '',
  chunkIndex: 0,
  chunkTotal: 0
})

let cancelStreamFn: (() => void) | null = null

onMounted(() => {
  loadDetail()
  loadTags()
})

onUnmounted(() => {
  if (cancelStreamFn) {
    cancelStreamFn()
  }
})

async function loadDetail() {
  const id = Number(route.params.id)
  if (!id) return
  
  loading.value = true
  try {
    const res = await getInterviewDetail(id)
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

async function loadTags() {
  try {
    const res = await getAllTags()
    allTags.value = res.data
  } catch (error) {
    console.error(error)
  }
}

function editQa(qa: QaRecordDTO) {
  editForm.id = qa.id
  editForm.question = qa.question
  editForm.answer = qa.answer || ''
  editForm.tags = qa.tags ? qa.tags.split(',') : []
  editDialogVisible.value = true
}

async function saveEdit() {
  try {
    await updateQa(editForm.id, {
      question: editForm.question,
      answer: editForm.answer,
      tags: editForm.tags
    })
    ElMessage.success('保存成功')
    editDialogVisible.value = false
    loadDetail()
  } catch (error) {
    console.error(error)
  }
}

async function deleteQaRecord(qaId: number) {
  try {
    await ElMessageBox.confirm('确定要删除这条问答记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteQa(qaId)
    ElMessage.success('删除成功')
    loadDetail()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

function showGenerateDialog() {
  generateForm.title = ''
  generateDialogVisible.value = true
}

async function handleGenerate() {
  const id = Number(route.params.id)
  if (!id) return
  
  generating.value = true
  try {
    const res = await generateExperience(id, { title: generateForm.title || undefined })
    experienceResult.value = res.data
    generateDialogVisible.value = false
    resultDialogVisible.value = true
    ElMessage.success('面经生成成功')
  } catch (error) {
    console.error(error)
  } finally {
    generating.value = false
  }
}

function downloadExperience() {
  if (!experienceResult.value?.content) return
  
  const blob = new Blob([experienceResult.value.content], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  
  const link = document.createElement('a')
  link.href = url
  link.download = `${experienceResult.value.title}.md`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  
  URL.revokeObjectURL(url)
  ElMessage.success('文件下载成功')
}

async function copyContent() {
  if (!experienceResult.value?.content) return
  
  try {
    await navigator.clipboard.writeText(experienceResult.value.content)
    ElMessage.success('内容已复制到剪贴板')
  } catch (error) {
    console.error(error)
    ElMessage.error('复制失败')
  }
}

function startStreamProcess() {
  const id = Number(route.params.id)
  if (!id) return
  
  processStatus.status = '初始化...'
  processStatus.streamOutput = ''
  processStatus.chunkIndex = 0
  processStatus.chunkTotal = 0
  processDialogVisible.value = true
  streaming.value = true
  
  cancelStreamFn = streamProcessAudio(id, (event) => {
    switch (event.type) {
      case 'status':
        processStatus.status = event.data
        break
      case 'qa_chunk_start':
        try {
          const data = JSON.parse(event.data)
          processStatus.chunkIndex = data.chunk
          processStatus.chunkTotal = data.total
        } catch (e) {}
        break
      case 'qa_stream':
      case 'summary_stream':
        processStatus.streamOutput += event.data
        break
      case 'qa_chunk_end':
        processStatus.streamOutput += '\n\n--- 片段处理完成 ---\n\n'
        break
      case 'summary_start':
        processStatus.status = '正在生成总结...'
        processStatus.streamOutput += '\n\n=== 开始生成总结 ===\n\n'
        break
      case 'summary_end':
        processStatus.streamOutput += '\n\n=== 总结生成完成 ===\n\n'
        break
      case 'done':
        processStatus.status = '处理完成'
        streaming.value = false
        loadDetail()
        break
      case 'error':
        processStatus.status = '处理失败: ' + event.data
        streaming.value = false
        ElMessage.error(event.data)
        break
      case 'warning':
        ElMessage.warning(event.data)
        break
    }
  })
}

function cancelStream() {
  if (cancelStreamFn) {
    cancelStreamFn()
    cancelStreamFn = null
  }
  streaming.value = false
  processDialogVisible.value = false
}

function closeProcessDialog() {
  processDialogVisible.value = false
}

function getStatusType(status?: string) {
  const map: Record<string, string> = {
    UPLOADED: 'info',
    PROCESSING: 'warning',
    FINISHED: 'success',
    FAILED: 'danger'
  }
  return map[status || ''] || 'info'
}

function getStatusText(status?: string) {
  const map: Record<string, string> = {
    UPLOADED: '待处理',
    PROCESSING: '处理中',
    FINISHED: '已完成',
    FAILED: '处理失败'
  }
  return map[status || ''] || status
}
</script>

<style scoped lang="scss">
.interview-detail {
  .page-title {
    font-size: 18px;
    font-weight: bold;
  }
  
  .qa-list {
    .qa-title {
      display: flex;
      align-items: center;
      gap: 10px;
      
      .question-text {
        font-weight: 500;
      }
    }
    
    .qa-content {
      padding: 10px;
      background: #f9f9f9;
      border-radius: 4px;
      
      .section-label {
        font-weight: 500;
        margin-bottom: 8px;
        color: #666;
      }
      
      .answer-section {
        margin-bottom: 16px;
        
        .answer-text {
          line-height: 1.8;
          white-space: pre-wrap;
        }
      }
      
      .tags-section {
        margin-bottom: 16px;
        
        .tag-item {
          margin-right: 8px;
        }
      }
      
      .qa-actions {
        text-align: right;
      }
    }
  }
  
  .transcript-content {
    pre {
      white-space: pre-wrap;
      word-wrap: break-word;
      line-height: 1.8;
      font-family: inherit;
      margin: 0;
      padding: 10px;
      background: #f9f9f9;
      border-radius: 4px;
    }
  }
  
  .summary-content {
    p {
      line-height: 1.8;
      margin-bottom: 16px;
    }
    
    .keywords {
      .label {
        font-weight: 500;
        margin-right: 8px;
      }
      
      .keyword-tag {
        margin-right: 8px;
        margin-bottom: 8px;
      }
    }
  }
}

.experience-result {
  .result-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    padding-bottom: 16px;
    border-bottom: 1px solid #eee;
    
    h3 {
      margin: 0;
      font-size: 18px;
    }
  }
  
  .result-content {
    max-height: 60vh;
    overflow-y: auto;
    
    pre {
      white-space: pre-wrap;
      word-wrap: break-word;
      line-height: 1.8;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      margin: 0;
      padding: 16px;
      background: #f9f9f9;
      border-radius: 8px;
      font-size: 14px;
    }
  }
}

.process-status {
  .status-item {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 16px;
    margin-bottom: 20px;
    
    .el-icon {
      font-size: 20px;
    }
  }
  
  .stream-output {
    .output-label {
      font-weight: 500;
      margin-bottom: 8px;
      color: #666;
    }
    
    .output-content {
      background: #1e1e1e;
      color: #d4d4d4;
      padding: 16px;
      border-radius: 8px;
      max-height: 400px;
      overflow-y: auto;
      white-space: pre-wrap;
      word-wrap: break-word;
      font-family: 'Consolas', 'Monaco', monospace;
      font-size: 13px;
      line-height: 1.6;
    }
  }
}
</style>
