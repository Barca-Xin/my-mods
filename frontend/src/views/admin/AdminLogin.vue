<template>
  <div class="admin-login-page">
    <SiteHeader />
    <div class="login-wrap">
      <el-card class="login-card">
      <div class="title">后台登录</div>
      <div class="sub">Mods 发布管理</div>
      <el-form @submit.prevent="onLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            size="large"
            show-password
            @keyup.enter="onLogin"
          />
        </el-form-item>
        <el-button type="primary" size="large" class="full" :loading="loading" @click="onLogin">
          登录
        </el-button>
      </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminApi } from '../../api'
import { useAuthStore } from '../../stores/auth'
import SiteHeader from '../../components/SiteHeader.vue'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function onLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await adminApi.login(form)
    auth.setToken(res.token)
    router.replace(route.query.redirect || '/admin/mods')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.admin-login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: radial-gradient(800px 500px at 30% 20%, #1b2240 0%, var(--bg) 60%);
}

.login-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-card {
  width: 380px;
  padding: 12px;

  .title {
    font-size: 22px;
    font-weight: 700;
    text-align: center;
    margin-bottom: 4px;
  }

  .sub {
    text-align: center;
    color: var(--text-dim);
    font-size: 13px;
    margin-bottom: 24px;
  }

  .full {
    width: 100%;
  }
}
</style>
