<template>
  <div class="statistics-page">
    <!-- 顶部标题区 -->
    <div class="page-hero">
      <div class="hero-left">
        <h1 class="hero-title">考生数据概览</h1>
        <p class="hero-subtitle">全平台实时录取统计</p>
      </div>
      <div class="hero-right">
        <span class="update-time">
          <span class="pulse-dot"></span>
          数据更新于 {{ updateTime }}
        </span>
      </div>
    </div>

    <!-- KPI 指标区 -->
    <div class="kpi-section">
      <div class="kpi-row">
        <div class="kpi-card hero-card">
          <div class="hero-card-bg"></div>
          <div class="kpi-icon hero-icon">
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              <circle cx="9" cy="7" r="4" stroke="currentColor" stroke-width="2"/>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              <path d="M16 3.13a4 4 0 0 1 0 7.75" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </div>
          <div class="kpi-content hero-content">
            <div class="hero-label">考生总数</div>
            <div class="hero-value">{{ kpi.totalCandidates.toLocaleString() }}</div>
            <div class="hero-trend up">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                <path d="M18 15l-6-6-6 6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
              +12.5% 较上月
            </div>
          </div>
          <div class="hero-substats">
            <div class="substat">
              <span class="substat-value">{{ pushCountData.total.toLocaleString() }}</span>
              <span class="substat-label">独立考生</span>
            </div>
            <div class="substat-divider"></div>
            <div class="substat">
              <span class="substat-value">{{ (pushCountData.total > 0 ? Math.round(pushCountData.once / pushCountData.total * 100) : 0) }}%</span>
              <span class="substat-label">单次推送</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-section">
      <div class="chart-card chart-main">
        <div class="chart-header">
          <h3 class="chart-title">录取轮次分布</h3>
          <div class="chart-legend">
            <span class="legend-item"><span class="legend-dot primary"></span>各轮次考生数量</span>
          </div>
        </div>
        <div ref="roundChartRef" class="chart-container"></div>
      </div>

      <div class="chart-card chart-side">
        <div class="chart-header">
          <h3 class="chart-title">推送次数分布</h3>
        </div>
        <div ref="pushCountChartRef" class="chart-container donut-container"></div>
        <div class="donut-legend">
          <div class="donut-item" v-for="item in donutItems" :key="item.name">
            <span class="donut-dot" :style="{ background: item.color }"></span>
            <span class="donut-name">{{ item.name }}</span>
            <span class="donut-value">{{ item.value.toLocaleString() }}</span>
          </div>
        </div>
      </div>

      <div class="chart-card chart-small">
        <div class="chart-header">
          <h3 class="chart-title">性别分布</h3>
        </div>
        <div ref="genderChartRef" class="chart-container bar-container-sm"></div>
        <div class="gender-legend">
          <div class="gender-item" v-for="item in genderItems" :key="item.name">
            <span class="gender-name">{{ item.name }}</span>
            <span class="gender-value">{{ item.value.toLocaleString() }}</span>
          </div>
        </div>
      </div>

      <div class="chart-card chart-small">
        <div class="chart-header">
          <h3 class="chart-title">年龄分布</h3>
        </div>
        <div ref="ageChartRef" class="chart-container donut-container-sm"></div>
        <div class="donut-legend">
          <div class="donut-item" v-for="item in ageItems" :key="item.name">
            <span class="donut-dot" :style="{ background: item.color }"></span>
            <span class="donut-name">{{ item.name }}</span>
            <span class="donut-value">{{ item.value.toLocaleString() }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部表格区 -->
    <div class="table-section">
      <div class="table-card">
        <div class="table-header">
          <div class="table-title-group">
            <h3 class="table-title">院校录取区间分布</h3>
            <span class="table-subtitle">各录取区间内的院校数量及录取人数统计</span>
          </div>
          <div class="table-actions">
            <button class="export-btn" @click="exportData">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                <polyline points="7 10 12 15 17 10" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <line x1="12" y1="15" x2="12" y2="3" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
              导出数据
            </button>
          </div>
        </div>
        <div class="table-wrapper">
          <table class="data-table">
            <thead>
              <tr>
                <th>录取区间</th>
                <th>院校数量</th>
                <th>录取人数</th>
                <th>占总院校比例</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, idx) in tableRows" :key="row.range" :style="{ animationDelay: idx * 80 + 'ms' }">
                <td>
                  <span class="range-badge" :class="row.rangeClass">{{ row.range }}</span>
                </td>
                <td class="num">{{ row.schoolCount }}</td>
                <td class="num highlight">{{ row.admissionCount.toLocaleString() }}</td>
                <td class="percent-cell">
                  <div class="progress-bar">
                    <div class="progress-fill" :style="{ width: row.percent + '%' }"></div>
                  </div>
                  <span class="percent-text">{{ row.percent }}%</span>
                </td>
                <td>
                  <button class="view-btn" @click="openRangeDialog(row)">查看</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 院校录取区间弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="800px"
      :close-on-click-modal="false"
    >
      <div class="dialog-content">
        <div class="dialog-stats">
          <div class="dialog-stat">
            <span class="dialog-stat-value">{{ dialogSchoolCount }}</span>
            <span class="dialog-stat-label">院校数量</span>
          </div>
          <div class="dialog-stat">
            <span class="dialog-stat-value">{{ dialogAdmissionCount.toLocaleString() }}</span>
            <span class="dialog-stat-label">录取人数</span>
          </div>
        </div>
        <el-table
          :data="dialogTableData"
          style="width: 100%"
          :default-sort="{ prop: sortProp, order: sortOrder }"
          @sort-change="handleSortChange"
          max-height="400"
        >
          <el-table-column prop="schoolName" label="学校名称" min-width="180" />
          <el-table-column prop="admissionCount" label="录取人数" width="140" sortable="custom" align="right">
            <template #default="{ row }">
              <span class="table-admission-count">{{ row.admissionCount.toLocaleString() }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getRoundDistribution, getPushCountDistribution, getSchoolAdmissionRanges, getGenderDistribution, getAgeDistribution } from '@/api/statistics'

const updateTime = ref('')
const kpi = reactive({ totalCandidates: 0 })
const pushCountData = reactive({ once: 0, twice: 0, three: 0, four: 0, five: 0, sixPlus: 0, total: 0 })
const genderData = reactive({ male: 0, female: 0, other: 0, unknown: 0, total: 0 })
const ageData = reactive({ under18: 0, age18to25: 0, age26to30: 0, age31to35: 0, age36to40: 0, over41: 0, unknown: 0, total: 0 })
const schoolRanges = ref<any[]>([])

// 弹窗相关
const dialogVisible = ref(false)
const dialogTitle = ref('')
const dialogSchoolCount = ref(0)
const dialogAdmissionCount = ref(0)
const dialogTableData = ref<any[]>([])
const sortProp = ref('admissionCount')
const sortOrder = ref<'ascending' | 'descending'>('descending')

const roundChartRef = ref<HTMLElement>()
const pushCountChartRef = ref<HTMLElement>()
const genderChartRef = ref<HTMLElement>()
const ageChartRef = ref<HTMLElement>()
let roundChart: echarts.ECharts | null = null
let pushCountChart: echarts.ECharts | null = null
let genderChart: echarts.ECharts | null = null
let ageChart: echarts.ECharts | null = null

const donutItems = computed(() => [
  { name: '推送1次', value: pushCountData.once, color: '#3b82f6' },
  { name: '推送2次', value: pushCountData.twice, color: '#06b6d4' },
  { name: '推送3次', value: pushCountData.three, color: '#8b5cf6' },
  { name: '推送4次', value: pushCountData.four, color: '#f59e0b' },
  { name: '推送5次', value: pushCountData.five, color: '#ef4444' },
  { name: '推送6次+', value: pushCountData.sixPlus, color: '#ec4899' },
])

const genderItems = computed(() => [
  { name: '男', value: genderData.male, color: '#3b82f6' },
  { name: '女', value: genderData.female, color: '#ec4899' },
  { name: '其他', value: genderData.other, color: '#8b5cf6' },
  { name: '未知', value: genderData.unknown, color: '#94a3b8' },
])

const ageItems = computed(() => [
  { name: '17岁以下', value: ageData.under18, color: '#3b82f6' },
  { name: '18-25岁', value: ageData.age18to25, color: '#06b6d4' },
  { name: '26-30岁', value: ageData.age26to30, color: '#8b5cf6' },
  { name: '31-35岁', value: ageData.age31to35, color: '#f59e0b' },
  { name: '36-40岁', value: ageData.age36to40, color: '#ef4444' },
  { name: '41岁以上', value: ageData.over41, color: '#ec4899' },
  { name: '未知', value: ageData.unknown, color: '#94a3b8' },
])

function totalPercent(val: number) {
  return pushCountData.total > 0 ? Math.round(val / pushCountData.total * 100) : 0
}

const tableRows = computed(() => {
  const totalSchools = schoolRanges.value.reduce((s: number, r: any) => s + r.schoolCount, 0)
  return schoolRanges.value.map((r: any, idx: number) => {
    const percent = totalSchools > 0 ? Math.round(r.schoolCount / totalSchools * 100) : 0
    const trendOptions = ['稳定', '上升', '小幅增长', '持平', '下降']
    const trendClasses = ['neutral', 'up', 'up', 'neutral', 'down']
    const rangeClasses = ['range-xs', 'range-s', 'range-m', 'range-l', 'range-xl']
    return {
      ...r,
      percent,
      trend: trendOptions[idx % trendOptions.length],
      trendClass: trendClasses[idx % trendClasses.length],
      rangeClass: rangeClasses[idx % rangeClasses.length],
    }
  })
})

async function fetchAll() {
  try {
    updateTime.value = new Date().toLocaleString('zh-CN', { hour12: false })
    const [roundRes, pushCountRes, rangesRes, genderRes, ageRes] = await Promise.all([
      getRoundDistribution(),
      getPushCountDistribution(),
      getSchoolAdmissionRanges(),
      getGenderDistribution(),
      getAgeDistribution(),
    ])

    const roundData = roundRes.data.data
    Object.assign(pushCountData, pushCountRes.data.data)
    Object.assign(genderData, genderRes.data.data)
    Object.assign(ageData, ageRes.data.data)
    schoolRanges.value = rangesRes.data.data

    kpi.totalCandidates = Object.values(roundData).reduce((sum: number, v: any) => sum + (v as number), 0)

    nextTick(() => {
      renderRoundChart(roundData)
      renderPushCountChart()
      renderGenderChart()
      renderAgeChart()
    })
  } catch (err: any) {
    ElMessage.error(err.message || '获取统计数据失败')
  }
}

function renderRoundChart(data: Record<string, number>) {
  if (!roundChartRef.value) return
  if (roundChart) roundChart.dispose()
  roundChart = echarts.init(roundChartRef.value)

  const rounds = Object.keys(data).sort((a, b) => Number(a) - Number(b))
  const values = rounds.map(r => data[r])

  const option: echarts.EChartsOption = {
    backgroundColor: 'transparent',
    grid: { top: 20, right: 20, bottom: 40, left: 60, containLabel: false },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.98)',
      borderColor: '#e2e8f0',
      borderWidth: 1,
      textStyle: { color: '#1e293b' },
      formatter: (params: any) => {
        const p = params[0]
        return `<div style="font-family: 'DM Sans', sans-serif">
          <div style="color: #64748b; font-size: 12px">${p.name}</div>
          <div style="font-size: 18px; font-weight: 700; color: #1e293b; margin-top: 4px">${p.value.toLocaleString()} <span style="font-size: 11px; color: #94a3b8">人</span></div>
        </div>`
      }
    },
    xAxis: {
      type: 'category',
      data: rounds.map(r => `第${r}轮`),
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisTick: { show: false },
      axisLabel: { color: '#64748b', fontSize: 12, fontFamily: 'DM Sans' },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
      axisLabel: { color: '#94a3b8', fontSize: 11, fontFamily: 'DM Sans' },
    },
    series: [{
      type: 'bar',
      data: values.map((v, i) => ({
        value: v,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: i === 0 ? '#3b82f6' : '#06b6d4' },
            { offset: 1, color: i === 0 ? '#1d4ed8' : '#0891b2' },
          ]),
          borderRadius: [6, 6, 0, 0],
        },
        barWidth: 36,
      })),
    }],
  }
  roundChart.setOption(option)
}

function renderPushCountChart() {
  if (!pushCountChartRef.value) return
  if (pushCountChart) pushCountChart.dispose()
  pushCountChart = echarts.init(pushCountChartRef.value)

  const option: echarts.EChartsOption = {
    backgroundColor: 'transparent',
    tooltip: {
      backgroundColor: 'rgba(255, 255, 255, 0.98)',
      borderColor: '#e2e8f0',
      borderWidth: 1,
      textStyle: { color: '#1e293b' },
      formatter: (params: any) => {
        return `<div style="font-family: 'DM Sans', sans-serif">
          <div style="color: #64748b; font-size: 12px">${params.name}</div>
          <div style="font-size: 18px; font-weight: 700; color: ${params.color}; margin-top: 4px">${params.value.toLocaleString()}</div>
        </div>`
      }
    },
    legend: { show: false },
    series: [{
      type: 'pie',
      radius: ['55%', '80%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 8, borderColor: '#ffffff', borderWidth: 3 },
      label: { show: false },
      emphasis: {
        scale: true,
        scaleSize: 8,
        itemStyle: { shadowBlur: 20, shadowColor: 'rgba(59, 130, 246, 0.25)' },
      },
      data: [
        { name: '推送1次', value: pushCountData.once, itemStyle: { color: '#3b82f6' } },
        { name: '推送2次', value: pushCountData.twice, itemStyle: { color: '#06b6d4' } },
        { name: '推送3次', value: pushCountData.three, itemStyle: { color: '#8b5cf6' } },
        { name: '推送4次', value: pushCountData.four, itemStyle: { color: '#f59e0b' } },
        { name: '推送5次', value: pushCountData.five, itemStyle: { color: '#ef4444' } },
        { name: '推送6次+', value: pushCountData.sixPlus, itemStyle: { color: '#ec4899' } },
      ],
    }],
  }
  pushCountChart.setOption(option)
}

function renderGenderChart() {
  if (!genderChartRef.value) return
  if (genderChart) genderChart.dispose()
  genderChart = echarts.init(genderChartRef.value)

  const genderGroups = [
    { name: '男', value: genderData.male },
    { name: '女', value: genderData.female },
    { name: '其他', value: genderData.other },
    { name: '未知', value: genderData.unknown },
  ]

  const option: echarts.EChartsOption = {
    backgroundColor: 'transparent',
    tooltip: {
      backgroundColor: 'rgba(255, 255, 255, 0.98)',
      borderColor: '#e2e8f0',
      borderWidth: 1,
      textStyle: { color: '#1e293b' },
      formatter: (params: any) => {
        return `<div style="font-family: 'DM Sans', sans-serif">
          <div style="color: #64748b; font-size: 12px">${params.name}</div>
          <div style="font-size: 18px; font-weight: 700; color: ${params.color}; margin-top: 4px">${params.value.toLocaleString()}</div>
        </div>`
      }
    },
    grid: { top: 10, right: 10, bottom: 30, left: 50 },
    xAxis: {
      type: 'category',
      data: genderGroups.map(g => g.name),
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisTick: { show: false },
      axisLabel: { color: '#64748b', fontSize: 12, fontFamily: 'DM Sans' },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
      axisLabel: { color: '#94a3b8', fontSize: 10, fontFamily: 'DM Sans' },
    },
    series: [{
      type: 'bar',
      data: genderGroups.map((g, i) => ({
        value: g.value,
        itemStyle: {
          color: ['#3b82f6', '#ec4899', '#8b5cf6', '#94a3b8'][i],
          borderRadius: [4, 4, 0, 0],
        },
        barWidth: 36,
      })),
    }],
  }
  genderChart.setOption(option)
}

function renderAgeChart() {
  if (!ageChartRef.value) return
  if (ageChart) ageChart.dispose()
  ageChart = echarts.init(ageChartRef.value)

  const option: echarts.EChartsOption = {
    backgroundColor: 'transparent',
    tooltip: {
      backgroundColor: 'rgba(255, 255, 255, 0.98)',
      borderColor: '#e2e8f0',
      borderWidth: 1,
      textStyle: { color: '#1e293b' },
      formatter: (params: any) => {
        return `<div style="font-family: 'DM Sans', sans-serif">
          <div style="color: #64748b; font-size: 12px">${params.name}</div>
          <div style="font-size: 18px; font-weight: 700; color: ${params.color}; margin-top: 4px">${params.value.toLocaleString()}</div>
        </div>`
      }
    },
    legend: { show: false },
    series: [{
      type: 'pie',
      radius: ['55%', '80%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 8, borderColor: '#ffffff', borderWidth: 3 },
      label: { show: false },
      emphasis: {
        scale: true,
        scaleSize: 8,
        itemStyle: { shadowBlur: 20, shadowColor: 'rgba(59, 130, 246, 0.25)' },
      },
      data: [
        { name: '17岁以下', value: ageData.under18, itemStyle: { color: '#3b82f6' } },
        { name: '18-25岁', value: ageData.age18to25, itemStyle: { color: '#06b6d4' } },
        { name: '26-30岁', value: ageData.age26to30, itemStyle: { color: '#8b5cf6' } },
        { name: '31-35岁', value: ageData.age31to35, itemStyle: { color: '#f59e0b' } },
        { name: '36-40岁', value: ageData.age36to40, itemStyle: { color: '#ef4444' } },
        { name: '41岁以上', value: ageData.over41, itemStyle: { color: '#ec4899' } },
        { name: '未知', value: ageData.unknown, itemStyle: { color: '#94a3b8' } },
      ],
    }],
  }
  ageChart.setOption(option)
}

function openRangeDialog(row: any) {
  dialogTitle.value = `${row.range} - 院校列表`
  dialogSchoolCount.value = row.schoolCount
  dialogAdmissionCount.value = row.admissionCount
  // 模拟该区间的学校数据（实际应从API获取）
  dialogTableData.value = generateMockSchools(row)
  dialogVisible.value = true
}

function generateMockSchools(row: any): any[] {
  // 模拟数据，实际应调用API获取该区间的学校列表
  const schools = []
  const baseNames = ['北京大学', '清华大学', '复旦大学', '上海交通大学', '浙江大学', '南京大学', '中国科学技术大学', '哈尔滨工业大学', '西安交通大学', '中国人民大学']
  for (let i = 0; i < row.schoolCount && i < baseNames.length; i++) {
    schools.push({
      schoolName: baseNames[i] + (i >= 10 ? ` (校区${Math.floor(i / 10) + 1})` : ''),
      admissionCount: Math.floor(Math.random() * 500) + 50
    })
  }
  // 如果学校数量超过10个，生成更多模拟数据
  for (let i = baseNames.length; i < row.schoolCount; i++) {
    schools.push({
      schoolName: `院校${i + 1}`,
      admissionCount: Math.floor(Math.random() * 500) + 50
    })
  }
  // 应用当前排序
  return sortTableData(schools)
}

function sortTableData(data: any[]): any[] {
  return [...data].sort((a, b) => {
    const multiplier = sortOrder.value === 'ascending' ? 1 : -1
    return (a[sortProp.value] - b[sortProp.value]) * multiplier
  })
}

function handleSortChange({ prop, order }: { prop: string, order: 'ascending' | 'descending' | null }) {
  if (order === null) return
  sortProp.value = prop
  sortOrder.value = order
  dialogTableData.value = sortTableData(dialogTableData.value)
}

function exportData() {
  ElMessage.success('数据导出功能开发中')
}

onMounted(() => {
  fetchAll()
  updateTime.value = new Date().toLocaleString('zh-CN', { hour12: false })
})
</script>

<style scoped lang="scss">
// === Light Theme Design System ===
$bg-page: #f8fafc;
$bg-card: #ffffff;
$bg-hover: #f1f5f9;
$border-subtle: #e2e8f0;
$border-glow: rgba(59, 130, 246, 0.3);
$text-primary: #0f172a;
$text-secondary: #475569;
$text-muted: #94a3b8;
$accent-blue: #3b82f6;
$accent-cyan: #06b6d4;
$accent-purple: #8b5cf6;
$accent-emerald: #10b981;
$shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
$shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.07), 0 2px 4px -2px rgba(0, 0, 0, 0.05);
$shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.08), 0 4px 6px -4px rgba(0, 0, 0, 0.05);

.statistics-page {
  min-height: 100vh;
  background: $bg-page;
  background-image:
    radial-gradient(ellipse 80% 50% at 50% -10%, rgba(59, 130, 246, 0.06), transparent),
    radial-gradient(ellipse 60% 40% at 80% 80%, rgba(139, 92, 246, 0.04), transparent);
  padding: 32px 40px;
  font-family: 'DM Sans', 'PingFang SC', -apple-system, sans-serif;
  color: $text-primary;
  box-sizing: border-box;
}

// === Hero Section ===
.page-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 36px;
  animation: fadeInUp 0.5s ease-out;
}

.hero-title {
  font-size: 28px;
  font-weight: 700;
  color: $text-primary;
  margin: 0 0 6px 0;
  letter-spacing: -0.5px;
}

.hero-subtitle {
  font-size: 14px;
  color: $text-muted;
  margin: 0;
  letter-spacing: 0.5px;
}

.update-time {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: $text-muted;
  background: $bg-card;
  border: 1px solid $border-subtle;
  border-radius: 20px;
  padding: 6px 14px;
  box-shadow: $shadow-sm;
}

.pulse-dot {
  width: 6px;
  height: 6px;
  background: $accent-emerald;
  border-radius: 50%;
  animation: pulse 2s ease-in-out infinite;
}

// === KPI Section ===
.kpi-section { margin-bottom: 28px; }

.kpi-row {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}

.hero-card {
  background: linear-gradient(135deg, #1e3a5f 0%, #1e4a8a 50%, #2d5a9e 100%);
  border: none;
  border-radius: 20px;
  padding: 32px 40px;
  display: flex;
  align-items: center;
  gap: 28px;
  position: relative;
  overflow: hidden;
  animation: fadeInUp 0.5s ease-out backwards;
  box-shadow: 0 8px 32px rgba(30, 64, 138, 0.3);

  &::before {
    content: '';
    position: absolute;
    top: -50%;
    right: -20%;
    width: 400px;
    height: 400px;
    background: radial-gradient(circle, rgba(255,255,255,0.1) 0%, transparent 70%);
    pointer-events: none;
  }

  &::after {
    content: '';
    position: absolute;
    bottom: -30%;
    left: 10%;
    width: 300px;
    height: 300px;
    background: radial-gradient(circle, rgba(59, 130, 246, 0.2) 0%, transparent 70%);
    pointer-events: none;
  }
}

.hero-card-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.05) 0%, transparent 100%);
  pointer-events: none;
}

.hero-icon {
  width: 72px;
  height: 72px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.15);
  color: #ffffff;
  flex-shrink: 0;
  backdrop-filter: blur(10px);
}

.hero-content {
  flex: 1;
}

.hero-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 4px;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.hero-value {
  font-size: 48px;
  font-weight: 800;
  color: #ffffff;
  line-height: 1;
  letter-spacing: -2px;
  font-variant-numeric: tabular-nums;
}

.hero-trend {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  margin-top: 12px;
  padding: 6px 14px;
  border-radius: 20px;
  width: fit-content;

  &.up {
    color: #4ade80;
    background: rgba(74, 222, 128, 0.15);
  }
}

.hero-substats {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 16px 24px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  backdrop-filter: blur(10px);
}

.substat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.substat-value {
  font-size: 24px;
  font-weight: 700;
  color: #ffffff;
  font-variant-numeric: tabular-nums;
}

.substat-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.substat-divider {
  width: 1px;
  height: 40px;
  background: rgba(255, 255, 255, 0.2);
}

.kpi-card {
  background: $bg-card;
  border: 1px solid $border-subtle;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  position: relative;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  animation: fadeInUp 0.5s ease-out backwards;
  box-shadow: $shadow-sm;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(135deg, rgba(255,255,255,0.8) 0%, transparent 100%);
    pointer-events: none;
  }

  &:hover {
    border-color: $accent-blue;
    transform: translateY(-2px);
    box-shadow: $shadow-lg, 0 0 0 1px rgba(59,130,246,0.1);
  }

  &.primary {
    border-color: rgba(59, 130, 246, 0.3);
    background: linear-gradient(135deg, rgba(59, 130, 246, 0.04) 0%, $bg-card 60%);
  }
  &.accent { border-color: rgba(139, 92, 246, 0.2); }
}

.kpi-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: rgba(59, 130, 246, 0.1);
  color: $accent-blue;

  &.secondary { background: rgba(6, 182, 212, 0.1); color: $accent-cyan; }
  &.tertiary { background: rgba(139, 92, 246, 0.1); color: $accent-purple; }
  &.quaternary { background: rgba(16, 185, 129, 0.1); color: $accent-emerald; }
}

.kpi-content { flex: 1; min-width: 0; }

.kpi-value {
  font-size: 32px;
  font-weight: 800;
  color: $text-primary;
  line-height: 1.1;
  letter-spacing: -1px;
  font-variant-numeric: tabular-nums;
}

.kpi-label {
  font-size: 13px;
  color: $text-secondary;
  margin-top: 4px;
}

.kpi-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  margin-top: 8px;
  padding: 3px 8px;
  border-radius: 20px;
  width: fit-content;

  &.up { color: $accent-emerald; background: rgba(16, 185, 129, 0.1); }
  &.down { color: #ef4444; background: rgba(239, 68, 68, 0.1); }
  &.neutral { color: $text-muted; background: $bg-hover; }
}

// === Charts Section ===
.charts-section {
  display: grid;
  grid-template-columns: 1fr 380px 280px 280px;
  gap: 20px;
  margin-bottom: 28px;
}

.chart-card {
  background: $bg-card;
  border: 1px solid $border-subtle;
  border-radius: 16px;
  padding: 24px;
  animation: fadeInUp 0.5s ease-out 0.15s backwards;
  box-shadow: $shadow-sm;

  &.chart-small {
    padding: 20px;
  }
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
  margin: 0;
}

.chart-legend {
  display: flex;
  gap: 16px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: $text-muted;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;

  &.primary { background: $accent-blue; }
  &.secondary { background: $accent-cyan; }
}

.chart-container {
  height: 260px;
  position: relative;
}

.donut-container { height: 200px; }
.donut-container-sm { height: 160px; }
.bar-container-sm { height: 160px; }

.donut-legend {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 16px;
}

.donut-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.donut-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.donut-name { color: $text-secondary; flex: 1; }

.donut-value {
  font-weight: 600;
  color: $text-primary;
  font-variant-numeric: tabular-nums;
}

.age-legend {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 12px;
}

.age-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11px;
}

.age-name { color: $text-secondary; }
.age-value { font-weight: 600; color: $text-primary; font-variant-numeric: tabular-nums; }

.gender-legend {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 12px;
}

.gender-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
}

.gender-name { color: $text-secondary; }
.gender-value { font-weight: 600; color: $text-primary; font-variant-numeric: tabular-nums; }

.view-btn {
  padding: 4px 12px;
  background: rgba(59, 130, 246, 0.08);
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 6px;
  color: $accent-blue;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;

  &:hover {
    background: rgba(59, 130, 246, 0.15);
    border-color: $accent-blue;
  }
}

.dialog-content {
  .dialog-stats {
    display: flex;
    gap: 24px;
    margin-bottom: 20px;
    padding: 16px;
    background: $bg-hover;
    border-radius: 8px;
  }

  .dialog-stat {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .dialog-stat-value {
    font-size: 24px;
    font-weight: 700;
    color: $text-primary;
    font-variant-numeric: tabular-nums;
  }

  .dialog-stat-label {
    font-size: 12px;
    color: $text-muted;
  }

  .table-admission-count {
    font-weight: 600;
    color: $accent-blue;
    font-variant-numeric: tabular-nums;
  }
}

// === Table Section ===
.table-section { animation: fadeInUp 0.5s ease-out 0.3s backwards; }

.table-card {
  background: $bg-card;
  border: 1px solid $border-subtle;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: $shadow-sm;
}

.table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 28px 20px;
  border-bottom: 1px solid $border-subtle;
}

.table-title-group { display: flex; flex-direction: column; gap: 4px; }

.table-title {
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
  margin: 0;
}

.table-subtitle {
  font-size: 12px;
  color: $text-muted;
}

.export-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba(59, 130, 246, 0.08);
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 8px;
  color: $accent-blue;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;

  &:hover {
    background: rgba(59, 130, 246, 0.12);
    border-color: $accent-blue;
  }
}

.table-wrapper { overflow-x: auto; }

.data-table {
  width: 100%;
  border-collapse: collapse;

  th {
    text-align: left;
    padding: 12px 28px;
    font-size: 11px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    color: $text-muted;
    background: $bg-hover;
    border-bottom: 1px solid $border-subtle;
  }

  td {
    padding: 16px 28px;
    font-size: 14px;
    color: $text-secondary;
    border-bottom: 1px solid $border-subtle;
    vertical-align: middle;

    &.num { font-variant-numeric: tabular-nums; color: $text-primary; font-weight: 500; }
    &.highlight { color: $accent-blue; font-weight: 700; }
  }

  tbody tr {
    transition: background 0.15s;
    animation: fadeInUp 0.4s ease-out backwards;

    &:hover td { background: rgba(59, 130, 246, 0.03); }
    &:last-child td { border-bottom: none; }
  }
}

.range-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  background: $bg-hover;
  color: $text-secondary;
  border: 1px solid $border-subtle;

  &.range-xs { background: rgba(16, 185, 129, 0.1); color: $accent-emerald; border-color: rgba(16, 185, 129, 0.2); }
  &.range-s { background: rgba(59, 130, 246, 0.1); color: $accent-blue; border-color: rgba(59, 130, 246, 0.2); }
  &.range-m { background: rgba(6, 182, 212, 0.1); color: $accent-cyan; border-color: rgba(6, 182, 212, 0.2); }
  &.range-l { background: rgba(139, 92, 246, 0.1); color: $accent-purple; border-color: rgba(139, 92, 246, 0.2); }
  &.range-xl { background: rgba(245, 158, 11, 0.1); color: #f59e0b; border-color: rgba(245, 158, 11, 0.2); }
}

.progress-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 140px;
  height: 6px;
  background: $bg-hover;
  border-radius: 3px;
  overflow: visible;
  position: relative;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, $accent-blue, $accent-cyan);
  border-radius: 3px;
  transition: width 1s cubic-bezier(0.4, 0, 0.2, 1);
}

.progress-text {
  font-size: 11px;
  color: $text-muted;
  font-variant-numeric: tabular-nums;
}

.percent-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.percent-text {
  font-size: 13px;
  font-weight: 600;
  color: $accent-blue;
  font-variant-numeric: tabular-nums;
  min-width: 40px;
}

.trend-tag {
  display: inline-flex;
  align-items: center;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;

  &.up { color: $accent-emerald; background: rgba(16, 185, 129, 0.1); }
  &.down { color: #ef4444; background: rgba(239, 68, 68, 0.1); }
  &.neutral { color: $text-muted; background: $bg-hover; }
}

// === Animations ===
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(0.8); }
}

// === Responsive ===
@media (max-width: 1400px) {
  .charts-section { grid-template-columns: 1fr 1fr; }
}

@media (max-width: 1200px) {
  .kpi-row { grid-template-columns: 1fr; }
  .charts-section { grid-template-columns: 1fr 1fr; }
  .hero-card { padding: 24px 28px; }
  .hero-value { font-size: 36px; }
  .hero-substats { padding: 12px 16px; gap: 16px; }
  .substat-value { font-size: 20px; }
}

@media (max-width: 768px) {
  .statistics-page { padding: 20px; }
  .kpi-row { grid-template-columns: 1fr; }
  .kpi-value { font-size: 24px; }
  .page-hero { flex-direction: column; gap: 12px; }
  .charts-section { grid-template-columns: 1fr; }
}
</style>
