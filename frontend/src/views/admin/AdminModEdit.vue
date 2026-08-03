<template>
  <div class="admin-wrap">
    <SiteHeader>
      <template #extra>
        <router-link to="/admin/mods">返回管理列表</router-link>
      </template>
    </SiteHeader>

    <main class="container" v-loading="loading">
      <h2>{{ isNew ? '新建模组' : `编辑：${modForm.name || ''}` }}</h2>

      <!-- 模组基本信息 -->
      <el-form :model="modForm" label-width="110px" class="mod-form">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="名称" required>
              <el-input v-model="modForm.name" placeholder="如 Advanced Enchanting" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="slug" required>
              <el-input v-model="modForm.slug" placeholder="URL 标识，如 advancedenchanting" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类">
              <el-input v-model="modForm.category" placeholder="冒险 / 科技 / 魔法 / 实用" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="加载器">
              <el-select v-model="modForm.modLoader" placeholder="Fabric / Forge / NeoForge" allow-create clearable class="w-full">
                <el-option label="Fabric" value="Fabric" />
                <el-option label="Forge" value="Forge" />
                <el-option label="NeoForge" value="NeoForge" />
                <el-option label="Quilt" value="Quilt" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="简介">
              <el-input v-model="modForm.shortDesc" type="textarea" :rows="2" placeholder="一句话介绍" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="独立实现声明">
              <el-input
                v-model="modForm.declaration"
                type="textarea"
                :rows="6"
                placeholder="卡片/详情页点「声明」弹出的内容；留空则不显示声明按钮。建议写明：独立实现、玩法不受版权保护、代码独立编写、依赖合规。"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="封面图 URL">
              <el-input v-model="modForm.logoUrl" placeholder="https://..." />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="源码 URL">
              <el-input v-model="modForm.sourceCodeUrl" placeholder="https://github.com/..." />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Wiki URL">
              <el-input v-model="modForm.wikiUrl" placeholder="https://..." />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="同步源">
              <el-input v-model="modForm.autoSyncSource" placeholder="预留 GitHub 自动同步口子（可空）" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="saveMod">保存模组</el-button>
          <el-button v-if="!isNew" @click="goBack">取消</el-button>
        </el-form-item>
      </el-form>

      <!-- 版本管理（编辑模式才显示） -->
      <template v-if="!isNew">
        <el-divider />

        <!-- 上传 jar 自动解析（切入点五） -->
        <h3>发布新版本</h3>
        <div class="version-form">
          <el-upload
            drag
            :auto-upload="false"
            :limit="1"
            :on-change="onFileChange"
            :on-remove="() => (versionFile = null)"
            accept=".jar"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽 jar 到此处，或<em>点击选择</em></div>
            <template #tip>
              <div class="el-upload__tip">上传后点「解析元数据」自动回填下方表单（支持 fabric.mod.json / mods.toml）</div>
            </template>
          </el-upload>

          <el-button :disabled="!versionFile" :loading="parsing" @click="parseJar" style="margin-top: 10px">
            解析元数据
          </el-button>

          <el-form :model="versionForm" label-width="110px" class="version-form-fields">
            <el-row :gutter="20">
              <el-col :span="8">
                <el-form-item label="模组版本" required>
                  <el-input v-model="versionForm.modVersion" placeholder="如 1.0.0" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="游戏版本" required>
                  <el-input v-model="versionForm.gameVersion" placeholder="如 1.21.4" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="推荐版">
                  <el-switch v-model="versionForm.recommended" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="发布日期">
                  <el-date-picker
                    v-model="versionForm.releaseDate"
                    type="date"
                    value-format="YYYY-MM-DD"
                    placeholder="选择日期"
                    class="w-full"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="16">
                <el-form-item label="依赖">
                  <el-select
                    v-model="versionForm.dependencies"
                    multiple
                    filterable
                    allow-create
                    default-first-option
                    placeholder="前置模组，如 fabric-api（回车添加）"
                    class="w-full"
                  >
                    <el-option v-for="d in versionForm.dependencies" :key="d" :label="d" :value="d" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="更新日志">
                  <el-input v-model="versionForm.changelog" type="textarea" :rows="3" placeholder="本次更新内容" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item>
              <el-button type="success" :disabled="!versionFile" :loading="uploading" @click="uploadVersion">
                上传并发布版本
              </el-button>
            </el-form-item>
          </el-form>
        </div>

        <el-divider />

        <!-- 已有版本列表 -->
        <h3>已有版本（{{ versions.length }}）</h3>
        <el-table :data="versions" stripe style="width: 100%">
          <el-table-column prop="gameVersion" label="游戏版本" width="100" />
          <el-table-column prop="modVersion" label="版本" width="110" />
          <el-table-column label="推荐" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.recommended" type="success" size="small">推荐</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="大小" width="100">
            <template #default="{ row }">{{ formatBytes(row.fileSize) }}</template>
          </el-table-column>
          <el-table-column prop="downloadCount" label="下载" width="80" />
          <el-table-column label="日期" width="120">
            <template #default="{ row }">{{ formatDate(row.releaseDate) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button v-if="!row.recommended" size="small" @click="markRecommended(row)">设为推荐</el-button>
              <el-popconfirm title="确认删除该版本？" @confirm="removeVersion(row)">
                <template #reference>
                  <el-button size="small" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminApi } from '../../api'
import SiteHeader from '../../components/SiteHeader.vue'
import { formatBytes, formatDate } from '../../utils/format'

const route = useRoute()
const router = useRouter()

const modId = computed(() => route.params.id)
const isNew = computed(() => route.name === 'admin-mod-new' || !modId.value)

const loading = ref(false)
const saving = ref(false)
const modForm = reactive({
  name: '', slug: '', category: '', modLoader: '', shortDesc: '',
  logoUrl: '', sourceCodeUrl: '', wikiUrl: '', autoSyncSource: '', declaration: ''
})
const versions = ref([])

const versionFile = ref(null)
const parsing = ref(false)
const uploading = ref(false)
const versionForm = reactive({
  modVersion: '', gameVersion: '', changelog: '',
  releaseDate: '', recommended: false, dependencies: []
})

onMounted(async () => {
  if (isNew.value) return
  loading.value = true
  try {
    const mods = await adminApi.listMods()
    const mod = mods.find((m) => m.modId === Number(modId.value))
    if (!mod) {
      ElMessage.error('模组不存在')
      router.replace('/admin/mods')
      return
    }
    Object.assign(modForm, {
      name: mod.name, slug: mod.slug, category: mod.category, modLoader: mod.modLoader,
      shortDesc: mod.shortDesc, logoUrl: mod.logoUrl, sourceCodeUrl: mod.sourceCodeUrl,
      wikiUrl: mod.wikiUrl, autoSyncSource: mod.autoSyncSource, declaration: mod.declaration
    })
    versions.value = mod.versions || []
  } finally {
    loading.value = false
  }
})

function onFileChange(file) {
  versionFile.value = file.raw
}

async function saveMod() {
  if (!modForm.name || !modForm.slug) {
    ElMessage.warning('名称和 slug 不能为空')
    return
  }
  saving.value = true
  try {
    if (isNew.value) {
      await adminApi.createMod({ ...modForm })
      ElMessage.success('创建成功，现在可以发布版本了')
      const mods = await adminApi.listMods()
      const created = mods.find((m) => m.slug === modForm.slug)
      router.replace(`/admin/mods/${created.modId}`)
    } else {
      await adminApi.updateMod(modId.value, { ...modForm })
      ElMessage.success('已保存')
      loadVersions()
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function loadVersions() {
  const mods = await adminApi.listMods()
  const mod = mods.find((m) => m.modId === Number(modId.value))
  versions.value = mod?.versions || []
}

async function parseJar() {
  parsing.value = true
  try {
    const meta = await adminApi.parseJar(versionFile.value)
    versionForm.modVersion = meta.version || versionForm.modVersion
    versionForm.gameVersion = meta.gameVersion || versionForm.gameVersion
    versionForm.dependencies = meta.dependencies || []
    if (isNew.value && !modForm.name) {
      modForm.name = meta.name
      modForm.slug = meta.modId
    }
    ElMessage.success('解析成功，已自动回填表单')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '解析失败')
  } finally {
    parsing.value = false
  }
}

async function uploadVersion() {
  if (!versionForm.modVersion || !versionForm.gameVersion) {
    ElMessage.warning('请填写模组版本和游戏版本')
    return
  }
  uploading.value = true
  try {
    await adminApi.uploadVersion(modId.value, {
      file: versionFile.value,
      modVersion: versionForm.modVersion,
      gameVersion: versionForm.gameVersion,
      changelog: versionForm.changelog,
      releaseDate: versionForm.releaseDate,
      recommended: versionForm.recommended,
      dependencies: versionForm.dependencies
    })
    ElMessage.success('版本已发布')
    versionForm.modVersion = ''
    versionForm.gameVersion = ''
    versionForm.changelog = ''
    versionForm.releaseDate = ''
    versionForm.recommended = false
    versionForm.dependencies = []
    versionFile.value = null
    loadVersions()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

async function removeVersion(row) {
  await adminApi.deleteVersion(modId.value, row.id)
  ElMessage.success('已删除')
  loadVersions()
}

async function markRecommended(row) {
  await adminApi.setRecommended(modId.value, row.id, true)
  ElMessage.success('已设为推荐版')
  loadVersions()
}

function goBack() {
  router.push('/admin/mods')
}
</script>

<style scoped lang="scss">
.container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 28px 40px 60px;

  h2 {
    margin: 0 0 24px;
  }

  h3 {
    margin: 0 0 16px;
  }
}

.w-full {
  width: 100%;
}
</style>
