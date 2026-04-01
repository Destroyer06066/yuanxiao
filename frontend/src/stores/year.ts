import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { getAvailableYears } from '@/api/statistics'

export const useYearStore = defineStore('year', () => {
  const selectedYear = ref(new Date().getFullYear())
  const availableYears = ref<number[]>([])

  // 持久化到 localStorage
  watch(selectedYear, (y) => {
    localStorage.setItem('selected_year', String(y))
  })

  async function loadYears() {
    try {
      const res = await getAvailableYears()
      availableYears.value = res.data.data || []
      // 如果持久化的年份不在列表中，使用当前年份
      const saved = localStorage.getItem('selected_year')
      if (saved && availableYears.value.includes(Number(saved))) {
        selectedYear.value = Number(saved)
      } else if (availableYears.value.length > 0) {
        selectedYear.value = availableYears.value[0]
      }
    } catch {
      availableYears.value = [new Date().getFullYear()]
    }
  }

  return { selectedYear, availableYears, loadYears }
})
