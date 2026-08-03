/**
 * 静态模式数据适配器（GitHub Pages 部署用）：
 * 从 data.json 读取数据，在客户端完成筛选、聚合最新版、分页和依赖图谱，
 * 对外暴露与 axios 版 modApi 完全一致的接口，前端页面零改动。
 */
let data = null
let loadPromise = null

function loadData() {
  if (!loadPromise) {
    loadPromise = fetch('data.json')
      .then((r) => {
        if (!r.ok) throw new Error('data.json 加载失败: ' + r.status)
        return r.json()
      })
      .catch((e) => {
        loadPromise = null
        throw e
      })
  }
  return loadPromise
}

function blankToNull(s) {
  return s && String(s).trim() ? String(s).trim() : null
}

/** 把 releaseDate（可能是 epoch 毫秒数字或 ISO 字符串）转成可比较的毫秒数 */
function dateVal(d) {
  if (d == null || d === '') return 0
  const v = typeof d === 'number' ? d : Date.parse(d)
  return Number.isNaN(v) ? 0 : v
}

/** 某模组在筛选条件下的最新版本（对应后端 GROUP BY + MAX(release_date) 聚合） */
function latestVersion(mod, gameVersion, loader) {
  let vs = mod.versions || []
  if (gameVersion) vs = vs.filter((v) => v.gameVersion === gameVersion)
  if (loader) vs = vs.filter((v) => (v.modLoader || mod.modLoader) === loader)
  if (!vs.length) return null
  return [...vs].sort((a, b) => dateVal(b.releaseDate) - dateVal(a.releaseDate) || b.id - a.id)[0]
}

export const modApi = {
  async list(params = {}) {
    const { mods } = await loadData()
    const gameVersion = blankToNull(params.gameVersion)
    const loader = blankToNull(params.loader)
    const category = blankToNull(params.category)
    const keyword = blankToNull(params.keyword)
    const page = Number(params.page) || 0
    const size = Number(params.size) || 12

    const rows = []
    for (const mod of mods) {
      if (category && mod.category !== category) continue
      if (loader && mod.modLoader !== loader) continue
      if (keyword) {
        const kw = keyword.toLowerCase()
        if (!`${mod.name} ${mod.shortDesc || ''}`.toLowerCase().includes(kw)) continue
      }
      const latest = latestVersion(mod, gameVersion, loader)
      if (!latest) continue
      rows.push({ mod, latest })
    }
    rows.sort((a, b) => dateVal(b.latest.releaseDate) - dateVal(a.latest.releaseDate) || b.latest.id - a.latest.id)

    const total = rows.length
    const start = page * size
    const items = rows.slice(start, start + size).map(({ mod, latest }) => ({
      modId: mod.modId,
      slug: mod.slug,
      name: mod.name,
      logoUrl: mod.logoUrl,
      shortDesc: mod.shortDesc,
      category: mod.category,
      modLoader: mod.modLoader,
      downloadCount: (mod.versions || []).reduce((s, v) => s + (v.downloadCount || 0), 0),
      latestVersion: latest
    }))
    return { items, total, page, size, totalPages: Math.ceil(total / size) }
  },

  async detail(slug) {
    const { mods } = await loadData()
    const mod = mods.find((m) => m.slug === slug)
    if (!mod) {
      const e = new Error('模组不存在')
      e.response = { data: { message: '模组不存在' } }
      throw e
    }
    return {
      modId: mod.modId,
      slug: mod.slug,
      name: mod.name,
      logoUrl: mod.logoUrl,
      shortDesc: mod.shortDesc,
      category: mod.category,
      modLoader: mod.modLoader,
      sourceCodeUrl: mod.sourceCodeUrl,
      wikiUrl: mod.wikiUrl,
      versions: mod.versions || []
    }
  },

  async versions(slug, gameVersion) {
    const d = await this.detail(slug)
    return gameVersion ? d.versions.filter((v) => v.gameVersion === gameVersion) : d.versions
  },

  async dependencies(slug) {
    const { mods } = await loadData()
    const mod = mods.find((m) => m.slug === slug)
    if (!mod) {
      const e = new Error('模组不存在')
      e.response = { data: { message: '模组不存在' } }
      throw e
    }
    const selfId = `mod:${mod.slug}`
    const nodes = [{ id: selfId, label: mod.name, type: 'mod', slug: mod.slug }]
    const edges = []
    for (const depName of mod.dependencies || []) {
      const internal = mods.find((m) => m.slug === depName)
      const id = internal ? `mod:${internal.slug}` : `ext:${depName}`
      nodes.push({
        id,
        label: internal ? internal.name : depName,
        type: internal ? 'mod' : 'external',
        slug: internal ? internal.slug : null
      })
      edges.push({ source: selfId, target: id })
    }
    return { nodes, edges }
  },

  async download(versionId) {
    const { mods } = await loadData()
    for (const mod of mods) {
      const v = (mod.versions || []).find((x) => x.id === Number(versionId))
      if (v) return { downloadUrl: v.downloadUrl }
    }
    const e = new Error('版本不存在')
    e.response = { data: { message: '版本不存在' } }
    throw e
  }
}
