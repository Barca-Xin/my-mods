<template>
  <div>
    <SiteHeader />

    <main class="container">
      <div class="filters">
        <el-select
          v-model="filters.gameVersion"
          placeholder="游戏版本"
          clearable
          class="filter-select"
          @change="load(1)"
        >
          <el-option v-for="g in GAME_VERSIONS" :key="g" :label="g" :value="g" />
        </el-select>

        <el-select
          v-model="filters.loader"
          placeholder="加载器"
          clearable
          class="filter-select"
          @change="load(1)"
        >
          <el-option v-for="l in LOADERS" :key="l" :label="l" :value="l" />
        </el-select>

        <el-input
          v-model="filters.keyword"
          placeholder="搜索模组..."
          clearable
          class="filter-input"
          @keyup.enter="load(1)"
        />
        <el-button type="primary" plain @click="load(1)">搜索</el-button>
      </div>

      <div v-loading="loading" class="mod-grid" v-if="items.length">
        <ModCard v-for="mod in items" :key="mod.modId" :mod="mod" />
      </div>
      <el-empty v-else-if="!loading" description="没有符合条件的模组" />

      <div class="pager" v-if="totalPages > 1">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="total"
          :page-size="size"
          :current-page="page + 1"
          @current-change="load($event)"
        />
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { modApi } from '../api'
import ModCard from '../components/ModCard.vue'
import SiteHeader from '../components/SiteHeader.vue'

const GAME_VERSIONS = ['1.21.4', '1.21.1', '1.20.6', '1.20.1', '1.19.4', '1.19.2']
const LOADERS = ['Fabric', 'Forge', 'NeoForge', 'Quilt']

const filters = reactive({ gameVersion: '', loader: '', keyword: '' })
const items = ref([])
const total = ref(0)
const page = ref(0)
const size = ref(12)
const totalPages = computed(() => Math.ceil(total.value / size.value))
const loading = ref(false)

async function load(p = 1) {
  loading.value = true
  try {
    const res = await modApi.list({ ...filters, page: p - 1, size: size.value })
    items.value = res.items || []
    total.value = res.total || 0
    page.value = res.page || 0
  } finally {
    loading.value = false
  }
}

onMounted(() => load(1))
</script>

<style scoped lang="scss">
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 28px 40px 60px;
}

.filters {
  display: flex;
  gap: 12px;
  margin-bottom: 28px;

  .filter-select {
    width: 160px;
  }

  .filter-input {
    width: 240px;
  }
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 36px;
}
</style>
