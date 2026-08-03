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
      <div class="declare-row" v-if="mod.hasDeclaration">
        <span class="declare-btn" :class="{ loading: declLoading }" @click.stop="onDeclare">
          {{ declLoading ? '加载中…' : '声明' }}
        </span>
      </div>
    </div>

    <el-dialog v-model="declShow" title="独立实现声明" width="520px">
      <div class="decl-content">{{ declContent }}</div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { modApi } from '../api'

const props = defineProps({ mod: { type: Object, required: true } })
const router = useRouter()

const imgFailed = ref(false)
const declShow = ref(false)
const declContent = ref('')
const declLoading = ref(false)
const initial = props.mod.name?.charAt(0)?.toUpperCase() || '?'

function goDetail() {
  router.push(`/mod/${props.mod.slug}`)
}

/** 声明全文不进列表接口；点按钮时才拉取（避免未点开卡片就泄露内容） */
async function onDeclare() {
  if (declLoading.value) return
  declLoading.value = true
  try {
    declContent.value = await modApi.declaration(props.mod.slug)
    declShow.value = true
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '声明加载失败')
  } finally {
    declLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.declare-row {
  margin-top: 10px;
  text-align: right;
}

.declare-btn {
  font-size: 12px;
  color: var(--text-dim);
  cursor: pointer;
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 2px 10px;
  transition: color 0.15s, border-color 0.15s;

  &:hover {
    color: var(--accent);
    border-color: var(--accent);
  }

  &.loading {
    cursor: wait;
    opacity: 0.6;
  }
}

.decl-content {
  white-space: pre-wrap;
  line-height: 1.8;
  font-size: 14px;
  color: var(--text);
}

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
