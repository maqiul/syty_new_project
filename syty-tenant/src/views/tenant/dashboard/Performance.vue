<template>
  <div class="performance-page">
    <h2 class="page-title">穿线师绩效看板</h2>

    <!-- 指标卡片 -->
    <a-row :gutter="[16, 16]" class="stat-row">
      <a-col :xs="12" :sm="6">
        <a-card hoverable class="stat-card">
          <a-statistic title="总单量" :value="stats.totalOrders" :precision="0" :value-style="{ color: '#1677ff' }">
            <template #prefix><FileTextOutlined /></template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="6">
        <a-card hoverable class="stat-card">
          <a-statistic title="总穿线费" :value="stats.totalStringingFee" :precision="2" prefix="¥" :value-style="{ color: '#52c41a' }">
            <template #prefix><DollarOutlined /></template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="6">
        <a-card hoverable class="stat-card">
          <a-statistic title="平均耗时" :value="stats.avgDuration" suffix="分钟" :value-style="{ color: '#faad14' }">
            <template #prefix><ClockCircleOutlined /></template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="6">
        <a-card hoverable class="stat-card">
          <a-statistic title="本月新增会员" :value="stats.newMembersThisMonth" :value-style="{ color: '#722ed1' }">
            <template #prefix><UserAddOutlined /></template>
          </a-statistic>
        </a-card>
      </a-col>
    </a-row>

    <!-- 图表区 -->
    <a-row :gutter="[16, 16]" class="chart-row">
      <a-col :xs="24" :lg="14">
        <a-card title="近 30 天每日单量趋势" :loading="chartLoading">
          <div ref="lineChartRef" class="chart-container"></div>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="10">
        <a-card title="穿线师单量 Top 5" :loading="chartLoading">
          <div ref="barChartRef" class="chart-container"></div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 排行榜 -->
    <a-card title="穿线师排行榜" :loading="rankLoading" class="rank-card">
      <a-table
        :columns="rankColumns"
        :dataSource="rankData"
        :pagination="false"
        size="small"
        :row-class-name="(_r: any, idx: number) => `rank-${idx + 1}`"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.dataIndex === 'rank'">
            <a-tag :color="index === 0 ? 'gold' : index === 1 ? 'silver' : index === 2 ? 'bronze' : 'default'" style="min-width: 32px; text-align: center;">
              {{ index + 1 }}
            </a-tag>
          </template>
          <template v-if="column.dataIndex === 'avgDuration'">
            {{ record.avgDuration }} 分钟
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { FileTextOutlined, DollarOutlined, ClockCircleOutlined, UserAddOutlined } from '@antdv-next/icons'
import * as echarts from 'echarts'
import { getPerformanceStats, getStringerRank, type PerformanceStats, type StringerRank } from '@/api/tenant/performance'

const stats = reactive<PerformanceStats>({
  totalOrders: 0,
  totalStringingFee: 0,
  avgDuration: 0,
  newMembersThisMonth: 0,
  dailyOrdersTrend: [],
  stringerRanking: [],
})

const rankData = ref<StringerRank[]>([])
const rankLoading = ref(false)
const chartLoading = ref(false)

const rankColumns = [
  { title: '排名', dataIndex: 'rank', key: 'rank', width: 70 },
  { title: '姓名', dataIndex: 'name', key: 'name', width: 120 },
  { title: '单量', dataIndex: 'orders', key: 'orders', width: 100, sorter: true },
  { title: '总金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 120 },
  { title: '平均耗时', dataIndex: 'avgDuration', key: 'avgDuration', width: 120 },
]

// ECharts 实例
const lineChartRef = ref<HTMLElement>()
const barChartRef = ref<HTMLElement>()
let lineChart: echarts.ECharts | null = null
let barChart: echarts.ECharts | null = null

async function fetchStats() {
  chartLoading.value = true
  try {
    const res: any = await getPerformanceStats({ period: 'month' })
    const data = res?.data?.data
    if (data) {
      Object.assign(stats, data)
      renderLineChart(data.dailyOrdersTrend || [])
    } else {
      renderLineChart([])
    }
  } catch {
    renderLineChart([])
  } finally {
    chartLoading.value = false
  }
}

async function fetchRank() {
  rankLoading.value = true
  try {
    const res: any = await getStringerRank({ period: 'month', limit: 5 })
    const data = res?.data?.data
    rankData.value = Array.isArray(data) ? data : []
  } catch {
    rankData.value = []
  } finally {
    rankLoading.value = false
  }
}

function renderLineChart(trend: { date: string; count: number }[]) {
  if (!lineChartRef.value) return
  if (!lineChart) {
    lineChart = echarts.init(lineChartRef.value)
  }
  lineChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: {
      type: 'category',
      data: trend.map(d => d.date),
      axisLabel: { rotate: 45, fontSize: 10 },
    },
    yAxis: { type: 'value', name: '单量', minInterval: 1 },
    series: [{
      data: trend.map(d => d.count),
      type: 'line',
      smooth: true,
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(22,119,255,0.3)' }, { offset: 1, color: 'rgba(22,119,255,0.02)' }] } },
      itemStyle: { color: '#1677ff' },
    }],
  })
}

function renderBarChart(ranking: { name: string; orders: number }[]) {
  if (!barChartRef.value) return
  if (!barChart) {
    barChart = echarts.init(barChartRef.value)
  }
  const reversed = [...ranking].reverse()
  barChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 80, right: 30, top: 20, bottom: 20 },
    xAxis: { type: 'value', name: '单量' },
    yAxis: { type: 'category', data: reversed.map(r => r.name) },
    series: [{
      data: reversed.map(r => r.orders),
      type: 'bar',
      itemStyle: { color: '#52c41a', borderRadius: [0, 4, 4, 0] },
      label: { show: true, position: 'right' },
    }],
  })
}

// ==================== 生命周期 ====================
onMounted(async () => {
  await Promise.all([fetchStats(), fetchRank()])
  // 渲染柱状图（依赖 ranking 数据）
  setTimeout(() => renderBarChart(stats.stringerRanking.length ? stats.stringerRanking : []), 100)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  lineChart?.dispose()
  barChart?.dispose()
  window.removeEventListener('resize', handleResize)
})

function handleResize() {
  lineChart?.resize()
  barChart?.resize()
}
</script>

<style scoped>
.performance-page {
  padding: 20px;
}

.page-title {
  margin: 0 0 20px;
  font-size: 22px;
  font-weight: 600;
  color: #1d2129;
}

.stat-row {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.chart-row {
  margin-bottom: 20px;
}

.chart-container {
  width: 100%;
  height: 320px;
}

.rank-card {
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

:deep(.rank-1) {
  background-color: #fffbe6;
}
:deep(.rank-2) {
  background-color: #f5f5f5;
}
</style>
