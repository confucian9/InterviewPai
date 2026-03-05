<template>
  <div class="interview-detail" v-loading="loading">
    <el-page-header @back="router.back()">
      <template #content>
        <span class="page-title">{{ detail?.fileName || '面试详情' }}</span>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getInterviewDetail, updateQa, deleteQa, type InterviewDetailDTO, type QaRecordDTO } from '@/api/interview'
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

onMounted(() => {
  loadDetail()
  loadTags()
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
</style>
