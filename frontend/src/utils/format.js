export function formatBytes(bytes) {
  if (bytes == null) return '—'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}

export function formatDate(s) {
  if (s == null || s === '') return '—'
  // 兼容 epoch 毫秒数字 和 ISO 字符串两种格式
  if (typeof s === 'number') {
    const d = new Date(s)
    if (Number.isNaN(d.getTime())) return '—'
    const pad = (n) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  }
  return s.replace('T', ' ').slice(0, 16)
}
