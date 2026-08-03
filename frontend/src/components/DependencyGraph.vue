<template>
  <svg :viewBox="`0 0 ${width} ${height}`" class="dep-graph">
    <defs>
      <marker id="arrow" viewBox="0 0 10 10" refX="10" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
        <path d="M 0 0 L 10 5 L 0 10 z" fill="#3a4160" />
      </marker>
    </defs>
    <line
      v-for="(e, i) in edges"
      :key="'e' + i"
      class="edge"
      :x1="pos(e.source).x"
      :y1="pos(e.source).y"
      :x2="pos(e.target).x"
      :y2="pos(e.target).y"
      marker-end="url(#arrow)"
    />
    <g
      v-for="n in nodes"
      :key="n.id"
      :class="['node', n.type]"
      @click="n.slug && $router.push(`/mod/${n.slug}`)"
    >
      <circle :cx="pos(n.id).x" :cy="pos(n.id).y" :r="n.type === 'mod' ? 14 : 10" />
      <text v-if="n.type === 'mod'" :x="pos(n.id).x" :y="pos(n.id).y + 4" class="node-icon" text-anchor="middle">
        🧩
      </text>
      <text :x="pos(n.id).x" :y="pos(n.id).y + (n.type === 'mod' ? 34 : 28)" class="label" text-anchor="middle">
        {{ n.label }}
      </text>
    </g>
  </svg>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  nodes: { type: Array, default: () => [] },
  edges: { type: Array, default: () => [] }
})

const width = 520
const height = 300
const cx = width / 2
const cy = height / 2

const pos = computed(() => {
  const map = {}
  if (!props.nodes.length) return () => ({ x: cx, y: cy })
  const self = props.nodes[0]
  map[self.id] = { x: cx, y: cy }
  const others = props.nodes.slice(1)
  others.forEach((n, i) => {
    const angle = -Math.PI / 2 + (2 * Math.PI * i) / Math.max(others.length, 1)
    const r = n.type === 'external' ? 130 : 90
    map[n.id] = {
      x: cx + r * Math.cos(angle),
      y: cy + r * Math.sin(angle)
    }
  })
  return (id) => map[id] || { x: cx, y: cy }
})
</script>

<style scoped lang="scss">
.dep-graph {
  width: 100%;
  max-width: 540px;
  background: var(--bg-soft);
  border-radius: 12px;
  border: 1px solid var(--border);
}

.edge {
  stroke: #3a4160;
  stroke-width: 1.5;
}

.node {
  circle {
    fill: #232a3d;
    stroke: var(--accent);
    stroke-width: 2;
    cursor: pointer;
  }

  &.external circle {
    fill: #1b2030;
    stroke: #5a627d;
    stroke-dasharray: 3 3;
    cursor: default;
  }

  &.mod:hover circle {
    fill: #2c3578;
  }

  .node-icon {
    font-size: 12px;
    pointer-events: none;
  }

  .label {
    font-size: 12px;
    fill: var(--text-dim);
    pointer-events: none;
  }
}
</style>
