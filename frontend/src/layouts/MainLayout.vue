<template>
  <div class="main-layout">
    <el-container>
      <el-header class="header">
        <div class="logo" @click="router.push('/')">
          <el-icon size="24"><Headset /></el-icon>
          <span class="logo-text">面经派</span>
        </div>
        <el-menu
          :default-active="route.path"
          mode="horizontal"
          :ellipsis="false"
          router
          class="nav-menu"
        >
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/upload">上传音频</el-menu-item>
          <el-menu-item index="/review">复习模式</el-menu-item>
          <el-menu-item index="/search">搜索</el-menu-item>
        </el-menu>
        <div class="user-info">
          <el-dropdown>
            <span class="user-dropdown">
              <el-avatar :size="32" :src="userStore.user?.avatar">
                {{ userStore.user?.nickname?.charAt(0) || 'U' }}
              </el-avatar>
              <span class="username">{{ userStore.user?.nickname || userStore.user?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

onMounted(() => {
  userStore.fetchCurrentUser()
})

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped lang="scss">
.main-layout {
  height: 100vh;
  
  .el-container {
    height: 100%;
  }
  
  .header {
    display: flex;
    align-items: center;
    background: #fff;
    border-bottom: 1px solid #e6e6e6;
    padding: 0 20px;
    
    .logo {
      display: flex;
      align-items: center;
      cursor: pointer;
      margin-right: 40px;
      
      .logo-text {
        font-size: 20px;
        font-weight: bold;
        color: #409eff;
        margin-left: 8px;
      }
    }
    
    .nav-menu {
      flex: 1;
      border-bottom: none;
    }
    
    .user-info {
      .user-dropdown {
        display: flex;
        align-items: center;
        cursor: pointer;
        
        .username {
          margin: 0 8px;
          color: #333;
        }
      }
    }
  }
  
  .main-content {
    background: #f5f7fa;
    padding: 20px;
    overflow-y: auto;
  }
}
</style>
