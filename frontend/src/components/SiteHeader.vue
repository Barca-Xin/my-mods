<template>
  <header class="site-header">
    <router-link to="/" class="brand">🧱 <span>Barca-Xin的网站</span></router-link>
    <div class="nav-links">
      <router-link v-if="!isHome" to="/" class="home-link">← 返回首页</router-link>
      <router-link v-if="!IS_STATIC" to="/admin/mods">后台管理</router-link>
      <slot name="extra" />
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { IS_STATIC } from '../api'

const route = useRoute()
const isHome = computed(() => route.name === 'home')
</script>

<style scoped lang="scss">
.home-link {
  color: var(--accent) !important;
  font-weight: 600;
}
</style>
