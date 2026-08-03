<template>
  <div class="mod-card" @click="goDetail">
    <div class="cover">
      <img v-if="mod.logoUrl && !imgFailed" :src="mod.logoUrl" class="cover-img" @error="imgFailed = true" />
      <div v-if="!mod.logoUrl || imgFailed" class="cover-placeholder">
        <span class="initial">{{ initial }}</span>
      </div>
    </div>
    <div class="overlay"></div>
    <div class="badges">
      <span class="badge">{{ mod.latestVersion?.gameVersion }}</span>
      <span class="badge loader">{{ mod.modLoader }}</span>
    </div>
    <div class="body">
      <div class="title">{{ mod.name }}</div>
      <div class="desc">{{ mod.shortDesc }}</div>
      <div class="meta">
        <span>{{ mod.category }}</span>
        <span style="margin-left: auto">
          <el-icon style="vertical-align: -1px"><Download /></el-icon>
          {{ mod.downloadCount }}
        </span>
        <span>v{{ mod.latestVersion?.modVersion }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({ mod: { type: Object, required: true } })
const router = useRouter()

const imgFailed = ref(false)
const initial = props.mod.name?.charAt(0)?.toUpperCase() || '?'

function goDetail() {
  router.push(`/mod/${props.mod.slug}`)
}
</script>

<style scoped lang="scss">
.cover {
  position: absolute;
  inset: 0;

  .cover-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }

  .cover-placeholder {
    width: 100%;
    height: 100%;
    background: linear-gradient(135deg, #232a3d, #171b29);
    display: flex;
    align-items: center;
    justify-content: center;

    .initial {
      font-size: 56px;
      font-weight: 800;
      opacity: 0.25;
    }
  }
}
</style>
