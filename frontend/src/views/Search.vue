<template>
  <div class="search-container">
    <el-card>
      <template #header>
        <span>搜索面试问题</span>
      </template>
      
      <div class="search-box">
        <el-input
          v-model="keyword"
          placeholder="输入关键词搜索问题或答案"
          size="large"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button :icon="Search" @click="handleSearch" />
          </template>
        </el-input>
      </div>
      
      <div class="hot-tags">
        <span class="label">热门标签：</span>
        <el-tag
          v-for="tag in hotTags"
          :key="tag.id"
          class="tag-item"
          @click="searchByTag(tag.name)"
        >
          {{ tag.name }}
        </el-tag>
      </div>
    </el-card>
    
    <el-card class="mt-20" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>搜索结果 ({{ results.length }}条)</span>
        </div>
      </template>
      
      <div class="search-results" v-if="results.length > 0">
        <el-collapse>
          <el-collapse-item
            v-for="(item, index) in results"
            :key="item.id"
            :name="index"
          >
            <template #title>
              <div class="result-title">
                <el-tag type="primary" size="small">Q</el-tag>
                <span class="question-text">{{ item.question }}</span>
              </div>
            </template>
            <div class="result-content">
              <div class="answer-section">
                <div class="section-label">答案：</div>
                <div class="answer-text">{{ item.answer || '暂无答案' }}</div>
              </div>
              <div class="tags-section" v-if="item.tags">
                <div class="section-label">标签：</div>
                <el-tag
                  v-for="tag in item.tags.split(',')"
                  :key="tag"
                  size="small"
                  class="tag-item"
                >
                  {{ tag }}
                </el-tag>
              </div>
              <div class="result-actions">
                <el-button size="small" type="primary" @click="goDetail(item.audioId)">
                  查看完整面经
                </el-button>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>
      
      <el-empty v-else-if="!loading && keyword" description="未找到相关内容" />
      <el-empty v-else description="输入关键词开始搜索" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { searchByKeyword, searchByTag as searchByTagApi, getAllTags, type QaRecordDTO, type Tag } from '@/api/review'

const router = useRouter()

const keyword = ref('')
const loading = ref(false)
const results = ref<QaRecordDTO[]>([])
const hotTags = ref<Tag[]>([])

onMounted(() => {
  loadTags()
})

async function loadTags() {
  try {
    const res = await getAllTags()
    hotTags.value = res.data.slice(0, 10)
  } catch (error) {
    console.error(error)
  }
}

async function handleSearch() {
  if (!keyword.value.trim()) return
  
  loading.value = true
  try {
    const res = await searchByKeyword(keyword.value)
    results.value = res.data
  } finally {
    loading.value = false
  }
}

async function searchByTag(tag: string) {
  keyword.value = tag
  loading.value = true
  try {
    const res = await searchByTagApi(tag)
    results.value = res.data
  } finally {
    loading.value = false
  }
}

function goDetail(audioId: number) {
  router.push(`/interview/${audioId}`)
}
</script>

<style scoped lang="scss">
.search-container {
  .search-box {
    margin-bottom: 20px;
  }
  
  .hot-tags {
    .label {
      margin-right: 10px;
      color: #666;
    }
    
    .tag-item {
      margin-right: 8px;
      cursor: pointer;
      
      &:hover {
        opacity: 0.8;
      }
    }
  }
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .search-results {
    .result-title {
      display: flex;
      align-items: center;
      gap: 10px;
      
      .question-text {
        font-weight: 500;
      }
    }
    
    .result-content {
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
      
      .result-actions {
        text-align: right;
      }
    }
  }
}
</style>
