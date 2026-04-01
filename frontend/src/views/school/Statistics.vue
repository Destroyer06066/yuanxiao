<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">数据统计</h2>
      <div class="header-actions">
        <el-select v-model="selectedYear" style="width: 120px" @change="handleYearChange">
          <el-option v-for="y in years" :key="y" :label="y + '年'" :value="y" />
        </el-select>
      </div>
    </div>

    <!-- KPI 指标卡 -->
    <el-row :gutter="20" class="kpi-row">
      <el-col :span="6" v-for="kpi in computedKpis" :key="kpi.label">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-value" :style="{ color: kpi.color }">{{ kpi.display }}</div>
          <div class="kpi-label">{{ kpi.label }}</div>
          <div class="kpi-trend" :class="kpi.trend > 0 ? 'up' : 'down'" v-if="kpi.trend !== 0">
            {{ kpi.trend > 0 ? '↑' : '↓' }} {{ Math.abs(kpi.trend) }}% vs 上年
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表行 -->
    <el-row :gutter="20" class="chart-row">
      <!-- 录取趋势折线图 -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <span>录取趋势</span>
            <el-date-picker
              v-model="selectedMonth"
              type="month"
              placeholder="选择月份"
              style="width: 140px; margin-left: 12px"
              format="MM月"
              value-format="YYYY-MM"
              @change="loadTrend"
              :clearable="false"
            />
          </template>
          <div v-if="trendLoading" style="height:260px;display:flex;align-items:center;justify-content:center;color:#909399">
            <el-icon class="is-loading"><Loading /></el-icon>&nbsp;加载中...
          </div>
          <v-chart v-else :option="trendOption" style="height:260px" :autoresize="true" />
        </el-card>
      </el-col>

      <!-- 状态分布饼图 -->
      <el-col :span="8">
        <el-card>
          <template #header><span>考生状态分布</span></template>
          <div v-if="distLoading" style="height:260px;display:flex;align-items:center;justify-content:center;color:#909399">
            <el-icon class="is-loading"><Loading /></el-icon>&nbsp;加载中...
          </div>
          <v-chart v-else :option="distOption" style="height:260px" :autoresize="true" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 专业录取排名 -->
    <el-card class="rank-card">
      <template #header>
        <span>专业录取排名（Top 10）</span>
      </template>
      <el-table :data="majorRanking" stripe v-loading="rankingLoading">
        <el-table-column type="index" label="排名" width="80" />
        <el-table-column prop="majorName" label="专业名称" min-width="160" />
        <el-table-column prop="admitted" label="已录取" width="100" />
        <el-table-column prop="confirmed" label="已确认" width="100" />
        <el-table-column prop="checkedIn" label="已报到" width="100" />
        <el-table-column label="总计" width="100">
          <template #default="{ row }">
            {{ (row.admitted || 0) + (row.confirmed || 0) + (row.checkedIn || 0) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart, BarChart } from 'echarts/charts'
import {
  TitleComponent, TooltipComponent, LegendComponent,
  GridComponent, DataZoomComponent,
} from 'echarts/components'
import * as echarts from 'echarts'
import {
  getKpis, getTrend, getStatusDistribution, getMajorRanking, getAvailableYears,
  type KpiData, type MonthlyTrend, type DailyTrend, type StatusDistItem, type MajorRankingItem,
} from '@/api/statistics'

use([CanvasRenderer, LineChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, DataZoomComponent])

// ========== 年份和月份选择 ==========
const selectedYear = ref(new Date().getFullYear())
const selectedMonth = ref(new Date().toISOString().slice(0, 7))
const years = ref<number[]>([])

async function loadYears() {
  try {
    const res = await getAvailableYears()
    years.value = res.data.data || []
    if (years.value.length > 0 && !years.value.includes(selectedYear.value)) {
      selectedYear.value = years.value[0]
    }
  } catch {
    // fallback to current year only
    years.value = [new Date().getFullYear()]
  }
}

function handleYearChange() {
  // 重置月份为选择年份的1月
  const [year] = selectedMonth.value.split('-').map(Number)
  if (year !== selectedYear.value) {
    selectedMonth.value = `${selectedYear.value}-01`
  }
  loadTrend()
}

// ========== KPI ==========
const kpiData = ref<KpiData | null>(null)
const kpiLoading = ref(false)

interface KpiItem {
  label: string
  value: number
  color: string
  trend: number
  display: string
}

const computedKpis = computed<KpiItem[]>(() => {
  if (!kpiData.value) {
    return [
      { label: '推送考生总数', value: 0, color: '#409eff', trend: 0, display: '-' },
      { label: '已录取人数', value: 0, color: '#67c23a', trend: 0, display: '-' },
      { label: '已确认人数', value: 0, color: '#e6a23c', trend: 0, display: '-' },
      { label: '已报到人数', value: 0, color: '#25a861', trend: 0, display: '-' },
    ]
  }
  const d = kpiData.value
  const fmt = (n: number) => n >= 10000 ? (n / 10000).toFixed(1) + '万' : String(n)
  const trend = (cur: number, last: number) =>
    last === 0 ? 0 : Math.round(((cur - last) / last) * 100)
  return [
    { label: '推送考生总数', value: d.totalPushed, color: '#409eff', trend: trend(d.totalPushed, d.totalLastYear), display: fmt(d.totalPushed) },
    { label: '已录取人数', value: d.admitted, color: '#67c23a', trend: trend(d.admitted, d.admittedLastYear), display: fmt(d.admitted) },
    { label: '已确认人数', value: d.confirmed, color: '#e6a23c', trend: trend(d.confirmed, d.confirmedLastYear), display: fmt(d.confirmed) },
    { label: '已报到人数', value: d.checkedIn, color: '#25a861', trend: trend(d.checkedIn, d.checkedInLastYear), display: fmt(d.checkedIn) },
  ]
})

// ========== 趋势图 ==========
const trendData = ref<(MonthlyTrend | DailyTrend)[]>([])
const trendLoading = ref(false)
const isDailyMode = computed(() => {
  return trendData.value.length > 0 && 'date' in trendData.value[0]
})

const trendOption = computed(() => {
  if (isDailyMode.value) {
    // 每日趋势：柱状图
    const dailyData = trendData.value as DailyTrend[]
    return {
      tooltip: { trigger: 'axis' },
      grid: { top: 10, bottom: 30, left: 50, right: 20 },
      xAxis: { type: 'category', data: dailyData.map(d => d.date) },
      yAxis: { type: 'value', minInterval: 1, name: '录取人数' },
      series: [{
        name: '录取人数',
        type: 'bar' as const,
        data: dailyData.map(d => d.admitted),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#409eff' },
            { offset: 1, color: '#67c23a' },
          ]),
          borderRadius: [4, 4, 0, 0],
        },
        barWidth: '60%',
      }],
    }
  } else {
    // 月度趋势：折线图
    const monthlyData = trendData.value as MonthlyTrend[]
    const months = monthlyData.map(d => d.month)
    const series = [
      { name: '待处理', key: 'pending' as const, color: '#909399' },
      { name: '已录取', key: 'admitted' as const, color: '#409eff' },
      { name: '已确认', key: 'confirmed' as const, color: '#67c23a' },
      { name: '已报到', key: 'checkedIn' as const, color: '#25a861' },
      { name: '已拒绝', key: 'rejected' as const, color: '#f56c6c' },
    ]
    return {
      tooltip: { trigger: 'axis' },
      legend: { bottom: 0, data: series.map(s => s.name) },
      grid: { top: 10, bottom: 40, left: 50, right: 20 },
      xAxis: { type: 'category', data: months, boundaryGap: false },
      yAxis: { type: 'value', minInterval: 1 },
      series: series.map(s => ({
        name: s.name,
        type: 'line' as const,
        data: monthlyData.map(d => d[s.key]),
        smooth: true,
        itemStyle: { color: s.color },
      })),
    }
  }
})

// ========== 状态分布饼图 ==========
const statusDist = ref<StatusDistItem[]>([])
const distLoading = ref(false)

const distOption = computed(() => {
  const filtered = statusDist.value.filter(d => d.count > 0)
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
    legend: { orient: 'vertical', right: 10, top: 'center', formatter: (name: string) => {
      const item = statusDist.value.find(d => d.label === name)
      return `${name} ${item?.count ?? 0}人`
    } },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: filtered.map(d => ({ name: d.label, value: d.count, itemStyle: { color: d.color } })),
    }],
  }
})

// ========== 专业排名 ==========
const majorRanking = ref<MajorRankingItem[]>([])
const rankingLoading = ref(false)

// ========== 加载数据 ==========
async function loadKpis() {
  kpiLoading.value = true
  try {
    const res = await getKpis({ year: selectedYear.value })
    kpiData.value = res.data.data
  } catch { /* axios interceptor handles */ }
  finally { kpiLoading.value = false }
}

async function loadTrend() {
  trendLoading.value = true
  try {
    const [year, month] = selectedMonth.value.split('-').map(Number)
    const res = await getTrend({ year, month })
    trendData.value = res.data.data
  } catch {}
  finally { trendLoading.value = false }
}

async function loadDist() {
  distLoading.value = true
  try {
    const res = await getStatusDistribution()
    statusDist.value = res.data.data
  } catch {}
  finally { distLoading.value = false }
}

async function loadRanking() {
  rankingLoading.value = true
  try {
    const res = await getMajorRanking({ limit: 10 })
    majorRanking.value = res.data.data
  } catch {}
  finally { rankingLoading.value = false }
}

async function loadAll() {
  const prevYear = selectedYear.value
  await loadYears()
  // loadYears 可能改变 selectedYear，确保月份与年份一致
  if (prevYear !== selectedYear.value) {
    // 年份发生变化，月份重置为该年1月
    selectedMonth.value = `${selectedYear.value}-01`
  } else {
    // 年份没变，保持当前月份或设为当前年月
    const currentYear = new Date().getFullYear()
    if (selectedYear.value !== currentYear) {
      selectedMonth.value = `${selectedYear.value}-01`
    } else {
      selectedMonth.value = new Date().toISOString().slice(0, 7)
    }
  }
  await Promise.all([loadKpis(), loadTrend(), loadDist(), loadRanking()])
}

onMounted(() => {
  loadAll()
})
</script>

<style scoped lang="scss">
.kpi-row { margin-bottom: 20px; }
.kpi-card {
  text-align: center;
  padding: 8px;
  .kpi-value {
    font-size: 36px;
    font-weight: 700;
    line-height: 1.2;
  }
  .kpi-label {
    font-size: 14px;
    color: #606266;
    margin: 8px 0 4px;
  }
  .kpi-trend {
    font-size: 12px;
    &.up { color: #67c23a; }
    &.down { color: #f56c6c; }
  }
}
.chart-row { margin-bottom: 20px; }
.rank-card { }
</style>
