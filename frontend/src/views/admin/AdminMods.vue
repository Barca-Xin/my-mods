<template>
  <div class="admin-wrap">
    <SiteHeader>
      <template #extra>
        <a href="#" @click.prevent="logout">退出登录</a>
      </template>
    </SiteHeader>

    <main class="container">
      <div class="toolbar">
        <h2>模组管理</h2>
        <router-link to="/admin/mods/new">
          <el-button type="primary">＋ 新建模组</el-button>
        </router-link>
      </div>

      <el-table :data="mods" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="slug" label="slug" min-width="160" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="modLoader" label="加载器" width="100" />
        <el-table-column label="版本数" width="90">
          <template #default="{ row }">{{ row.versions.length }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" @click="goEdit(row)">编辑</el-button>
            <el-popconfirm title="确认删除该模组及其全部版本？" @confirm="remove(row)">
              <template #reference>
                <el-button size="small" type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminApi } from '../../api'
import { useAuthStore } from '../../stores/auth'
import SiteHeader from '../../components/SiteHeader.vue'

const router = useRouter()
const auth = useAuthStore()
const mods = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    mods.value = await adminApi.listMods()
  } finally {
    loading.value = false
  }
}

function goEdit(row) {
  router.push(`/admin/mods/${row.modId}`)
}

async function remove(row) {
  await adminApi.deleteMod(row.modId)
  ElMessage.success('已删除')
  load()
}

function logout() {
  auth.logout()
  router.replace('/admin/login')
}

onMounted(load)
</script>

<style scoped lang="scss">
.container {
  max-width: 1100px;
  margin: 0 auto;
  padding: 28px 40px 60px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;

  h2 {
    margin: 0;
  }
}
</style>
