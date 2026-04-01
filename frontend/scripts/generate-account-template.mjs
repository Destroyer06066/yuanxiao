import * as XLSX from 'xlsx'
import { fileURLToPath } from 'url'
import { dirname, join } from 'path'
import fs from 'fs'

const __filename = fileURLToPath(import.meta.url)
const __dirname = dirname(__filename)

const publicDir = join(__dirname, '../public')

// 创建工作簿
const wb = XLSX.utils.book_new()

// 创建数据
const data = [
  ['账号批量导入模板'],
  [],
  ['说明：'],
  ['1. 带 * 号的字段为必填项'],
  ['2. 角色可选值：院校管理员、院校工作人员、运营管理员'],
  ['3. 所属院校：填写院校全称，如"北京语言大学"'],
  ['4. 初始密码：选填，不填则默认为 Aa123456!'],
  [],
  ['用户名*', '姓名*', '角色*', '所属院校', '初始密码'],
  ['zhangsan', '张三', '院校管理员', '北京语言大学', ''],
  ['lisi', '李四', '院校工作人员', '北京大学', 'MyPass123!'],
  ['wangwu', '王五', '院校工作人员', '清华大学', ''],
]

// 创建工作表
const ws = XLSX.utils.aoa_to_sheet(data)

// 设置列宽
ws['!cols'] = [
  { wch: 20 },  // 用户名
  { wch: 15 },  // 姓名
  { wch: 20 },  // 角色
  { wch: 25 },  // 所属院校
  { wch: 20 },  // 初始密码
]

// 合并标题单元格
ws['!merges'] = [
  { s: { r: 0, c: 0 }, e: { r: 0, c: 4 } },  // 标题行
  { s: { r: 2, c: 0 }, e: { r: 6, c: 4 } },  // 说明区域
]

// 设置标题样式（通过设置行高）
ws['!rows'] = [
  { hpt: 30 },  // 标题行高
  { hpt: 20 },  // 空行
  { hpt: 18 },  // 说明行高
  { hpt: 18 },
  { hpt: 18 },
  { hpt: 18 },
  { hpt: 18 },
  { hpt: 20 },  // 空行
  { hpt: 25 },  // 表头行高
]

// 添加工作表
XLSX.utils.book_append_sheet(wb, ws, '账号导入模板')

// 写入文件
const outputPath = join(publicDir, 'account-import-template.xlsx')
XLSX.writeFile(wb, outputPath)
console.log('Excel模板已生成:', outputPath)
