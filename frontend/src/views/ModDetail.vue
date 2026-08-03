<template>
  <div>
    <SiteHeader />

    <main v-if="detail" class="container">
      <div class="back">
        <router-link to="/">← 返回列表</router-link>
      </div>

      <div class="hero">
        <div class="hero-cover">
          <img
            v-if="detail.logoUrl && !heroImgFailed"
            :src="detail.logoUrl"
            class="hero-img"
            @error="heroImgFailed = true"
          />
          <div v-if="!detail.logoUrl || heroImgFailed" class="hero-placeholder">
            <span class="initial">{{ (detail.name || '?').charAt(0) }}</span>
          </div>
        </div>
        <div class="hero-info">
          <h1>{{ detail.name }}</h1>
          <p class="short-desc">{{ detail.shortDesc }}</p>
          <div class="tags">
            <el-tag size="small">{{ detail.category }}</el-tag>
            <el-tag size="small" type="primary">{{ detail.modLoader }}</el-tag>
            <a v-if="detail.sourceCodeUrl" :href="detail.sourceCodeUrl" target="_blank" rel="noopener">
              <el-tag size="small" type="success" effect="plain">源码</el-tag>
            </a>
            <a v-if="detail.wikiUrl" :href="detail.wikiUrl" target="_blank" rel="noopener">
              <el-tag size="small" type="warning" effect="plain">Wiki</el-tag>
            </a>
            <el-button
              v-if="detail.declaration"
              size="small"
              type="info"
              plain
              class="decl-btn"
              @click="declShow = true"
            >声明</el-button>
          </div>
        </div>
      </div>

      <el-dialog v-model="declShow" title="独立实现声明" width="560px">
        <div class="decl-content">{{ detail.declaration }}</div>
      </el-dialog>

      <div class="content">
        <!-- 下载区（切入点四：切换版本只刷新这一块） -->
        <section class="panel download-panel" v-if="selectedVersion">
          <div class="panel-title">下载 · {{ selectedVersion.gameVersion }} × {{ selectedVersion.modVersion }}</div>

          <div class="switcher">
            <div class="switcher-label">游戏版本</div>
            <el-select v-model="selectedGameVersion" placeholder="游戏版本" clearable class="w160" @change="onGameVersionChange">
              <el-option v-for="g in gameVersions" :key="g" :label="g" :value="g" />
            </el-select>
            <div class="switcher-label" style="margin-left: 16px">模组版本</div>
            <el-select v-model="selectedVersionId" placeholder="选择版本" class="w220" @change="onVersionSelect">
              <el-option
                v-for="v in filteredVersions"
                :key="v.id"
                :value="v.id"
                :label="`${v.modVersion}${v.recommended ? '（推荐）' : ''}`"
              />
            </el-select>
          </div>

          <div class="download-actions">
            <el-button
              type="primary"
              size="large"
              class="download-btn"
              :disabled="!selectedVersion.downloadUrl"
              :loading="downloading"
              @click="onDownload"
            >
              <el-icon style="margin-right: 6px"><Download /></el-icon>
              {{ selectedVersion.downloadUrl ? '立即下载' : '暂无下载文件' }}
            </el-button>
            <div class="meta-stats">
              <span>大小 {{ formatBytes(selectedVersion.fileSize) }}</span>
              <span>⬇ {{ selectedVersion.downloadCount }}</span>
              <span>发布 {{ formatDate(selectedVersion.releaseDate) }}</span>
            </div>
          </div>

          <div class="md5" v-if="selectedVersion.md5">
            <span class="md5-label">MD5</span>
            <code>{{ selectedVersion.md5 }}</code>
          </div>

          <div class="changelog" v-if="selectedVersion.changelog">
            <div class="sub-title">更新日志</div>
            <p>{{ selectedVersion.changelog }}</p>
          </div>

          <div class="disclaimer">
            This mod is developed from scratch independently. Any similarity to existing mods in
            functionality is coincidental, as the mechanics are based on common game design concepts.
          </div>
        </section>

        <!-- 依赖图谱（切入点六） -->
        <section class="panel" v-if="deps && deps.nodes.length">
          <div class="panel-title">依赖关系</div>
          <DependencyGraph :nodes="deps.nodes" :edges="deps.edges" />
          <div class="hint">节点可点击跳转到对应模组；虚线为外部依赖</div>
        </section>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { modApi } from '../api'
import DependencyGraph from '../components/DependencyGraph.vue'
import SiteHeader from '../components/SiteHeader.vue'
import { formatBytes, formatDate } from '../utils/format'

const route = useRoute()
const router = useRouter()

const detail = ref(null)
const allVersions = ref([])
const deps = ref(null)
const selectedVersionId = ref(null)
const selectedGameVersion = ref('')
const downloading = ref(false)
const heroImgFailed = ref(false)
const declShow = ref(false)

const selectedVersion = computed(() => allVersions.value.find((v) => v.id === selectedVersionId.value) || null)
const gameVersions = computed(() => [...new Set(allVersions.value.map((v) => v.gameVersion))])
const filteredVersions = computed(() =>
  selectedGameVersion.value
    ? allVersions.value.filter((v) => v.gameVersion === selectedGameVersion.value)
    : allVersions.value
)

onMounted(async () => {
  const slug = route.params.slug
  const [d, vs, dep] = await Promise.all([
    modApi.detail(slug),
    modApi.versions(slug),
    modApi.dependencies(slug)
  ])
  detail.value = d
  allVersions.value = vs
  deps.value = dep

  // 初始版本：优先 URL 上的 ?v=，否则推荐版，再退到最新版（切入点四）
  const urlVersion = route.query.v
  let target = urlVersion ? vs.find((v) => v.modVersion === urlVersion) : null
  if (!target) target = vs.find((v) => v.recommended) || vs[0] || null
  if (target) {
    selectedVersionId.value = target.id
    selectedGameVersion.value = target.gameVersion
  }
})

function onGameVersionChange() {
  const filtered = filteredVersions.value
  const target = filtered.find((v) => v.recommended) || filtered[0]
  if (target) selectedVersionId.value = target.id
}

function onVersionSelect(id) {
  const v = allVersions.value.find((x) => x.id === id)
  if (!v) return
  // 只更新 URL 参数，不整页刷新（history.pushState）
  router.replace({ query: { v: v.modVersion } })
}

async function onDownload() {
  const v = selectedVersion.value
  if (!v || !v.downloadUrl) return
  downloading.value = true
  try {
    // 先 POST 计数，再跳转真实下载地址（切入点六）
    const res = await modApi.download(v.id)
    window.location.href = res.downloadUrl
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '下载失败')
  } finally {
    downloading.value = false
  }
}
</script>

<style scoped lang="scss">
.container {
  max-width: 1080px;
  margin: 0 auto;
  padding: 24px 40px 60px;
}

.back {
  margin-bottom: 18px;
  color: var(--text-dim);
  font-size: 14px;

  a:hover {
    color: var(--accent);
  }
}

.hero {
  display: flex;
  gap: 28px;
  align-items: center;
  padding: 28px;
  background: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 18px;
  margin-bottom: 24px;

  .hero-cover {
    width: 120px;
    height: 120px;
    border-radius: 14px;
    flex-shrink: 0;
    overflow: hidden;

    .hero-img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }

    .hero-placeholder {
      width: 100%;
      height: 100%;
      background: linear-gradient(135deg, #232a3d, #171b29);
      display: flex;
      align-items: center;
      justify-content: center;

      .initial {
        font-size: 52px;
        font-weight: 800;
        opacity: 0.25;
      }
    }
  }

  .hero-info {
    h1 {
      margin: 0 0 8px;
      font-size: 26px;
    }

    .short-desc {
      margin: 0 0 12px;
      color: var(--text-dim);
      line-height: 1.6;
    }

    .tags {
      display: flex;
      gap: 8px;
      align-items: center;

      .decl-btn {
        margin-left: 4px;
      }
    }
  }
}

.decl-content {
  white-space: pre-wrap;
  line-height: 1.8;
  font-size: 14px;
  color: var(--text);
}

.content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  align-items: start;
}

.panel {
  background: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 24px;

  .panel-title {
    font-size: 16px;
    font-weight: 700;
    margin-bottom: 18px;
  }

  .sub-title {
    font-weight: 600;
    margin-bottom: 6px;
    color: var(--text);
  }
}

.switcher {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;

  .switcher-label {
    font-size: 13px;
    color: var(--text-dim);
  }

  .w160 {
    width: 140px;
  }

  .w220 {
    width: 200px;
  }
}

.download-actions {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 16px;

  .download-btn {
    padding: 14px 34px;
    font-size: 16px;
    border-radius: 12px;
  }

  .meta-stats {
    display: flex;
    flex-direction: column;
    gap: 4px;
    font-size: 13px;
    color: var(--text-dim);
  }
}

.md5 {
  font-size: 13px;
  color: var(--text-dim);
  margin-bottom: 16px;

  .md5-label {
    margin-right: 8px;
    color: var(--text);
  }

  code {
    background: #141824;
    padding: 3px 8px;
    border-radius: 6px;
    color: var(--green);
  }
}

.changelog {
  p {
    color: var(--text-dim);
    line-height: 1.7;
    margin: 0;
    white-space: pre-wrap;
  }
}

.disclaimer {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed var(--border);
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-dim);
}

.hint {
  margin-top: 10px;
  font-size: 12px;
  color: var(--text-dim);
}

@media (max-width: 900px) {
  .content {
    grid-template-columns: 1fr;
  }
}
</style>
