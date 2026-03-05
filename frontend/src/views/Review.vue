<template>
  <div class="review-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>面试复习模式</span>
          <div class="header-actions">
            <el-select v-model="selectedTag" placeholder="按标签筛选" clearable style="width: 200px" @change="loadQuestions">
              <el-option
                v-for="tag in allTags"
                :key="tag.id"
                :label="tag.name"
                :value="tag.name"
              />
            </el-select>
            <el-button type="primary" @click="loadQuestions">
              <el-icon><Refresh /></el-icon>
              换一批
            </el-button>
          </div>
        </div>
      </template>
      
      <div class="review-content" v-if="questions.length > 0">
        <div class="progress-bar">
          <el-progress :percentage="progress" :format="progressFormat" />
        </div>
        
        <div class="question-card">
          <div class="question-number">
            问题 {{ currentIndex + 1 }} / {{ questions.length }}
          </div>
          <div class="question-text">
            {{ currentQuestion?.question }}
          </div>
          
          <div class="answer-section" v-if="showAnswer">
            <el-divider />
            <div class="answer-label">答案：</div>
            <div class="answer-text">
              {{ currentQuestion?.answer || '暂无答案' }}
            </div>
            <div class="tags" v-if="currentQuestion?.tags">
              <el-tag
                v-for="tag in currentQuestion.tags.split(',')"
                :key="tag"
                size="small"
                class="tag-item"
              >
                {{ tag }}
              </el-tag>
            </div>
          </div>
          
          <div class="action-buttons">
            <el-button v-if="!showAnswer" type="primary" size="large" @click="showAnswer = true">
              显示答案
            </el-button>
            <template v-else>
              <el-button size="large" @click="prevQuestion" :disabled="currentIndex === 0">
                上一题
              </el-button>
              <el-button type="primary" size="large" @click="nextQuestion" :disabled="currentIndex === questions.length - 1">
                下一题
              </el-button>
            </template>
          </div>
        </div>
      </div>
      
      <el-empty v-else description="暂无面试题目，请先上传面试录音" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getRandomQuestions, getQuestionsByTag, getAllTags, type QaRecordDTO, type Tag } from '@/api/review'

const questions = ref<QaRecordDTO[]>([])
const currentIndex = ref(0)
const showAnswer = ref(false)
const selectedTag = ref('')
const allTags = ref<Tag[]>([])

const currentQuestion = computed(() => questions.value[currentIndex.value])

const progress = computed(() => {
  if (questions.value.length === 0) return 0
  return Math.round(((currentIndex.value + 1) / questions.value.length) * 100)
})

onMounted(() => {
  loadTags()
  loadQuestions()
})

async function loadTags() {
  try {
    const res = await getAllTags()
    allTags.value = res.data
  } catch (error) {
    console.error(error)
  }
}

async function loadQuestions() {
  try {
    let res
    if (selectedTag.value) {
      res = await getQuestionsByTag(selectedTag.value, 20)
    } else {
      res = await getRandomQuestions(20)
    }
    questions.value = res.data
    currentIndex.value = 0
    showAnswer.value = false
  } catch (error) {
    console.error(error)
  }
}

function progressFormat(percentage: number) {
  return `${currentIndex.value + 1}/${questions.value.length}`
}

function prevQuestion() {
  if (currentIndex.value > 0) {
    currentIndex.value--
    showAnswer.value = false
  }
}

function nextQuestion() {
  if (currentIndex.value < questions.value.length - 1) {
    currentIndex.value++
    showAnswer.value = false
  }
}
</script>

<style scoped lang="scss">
.review-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .header-actions {
      display: flex;
      gap: 10px;
    }
  }
  
  .review-content {
    .progress-bar {
      margin-bottom: 30px;
    }
    
    .question-card {
      background: #f9f9f9;
      border-radius: 8px;
      padding: 30px;
      
      .question-number {
        font-size: 14px;
        color: #999;
        margin-bottom: 16px;
      }
      
      .question-text {
        font-size: 20px;
        font-weight: 500;
        line-height: 1.6;
        color: #333;
      }
      
      .answer-section {
        margin-top: 20px;
        
        .answer-label {
          font-weight: 500;
          margin-bottom: 10px;
          color: #666;
        }
        
        .answer-text {
          line-height: 1.8;
          white-space: pre-wrap;
          color: #333;
        }
        
        .tags {
          margin-top: 16px;
          
          .tag-item {
            margin-right: 8px;
          }
        }
      }
      
      .action-buttons {
        margin-top: 30px;
        text-align: center;
        
        .el-button {
          min-width: 120px;
        }
      }
    }
  }
}
</style>
