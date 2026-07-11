<template>
  <AppLayout>
    <div class="txn-page">
      <div class="toolbar">
        <el-button type="primary" @click="openDialog()">新增账单</el-button>
        <el-button @click="exportCsv">导出 CSV</el-button>
        <div class="filters">
          <el-select v-model="filters.type" placeholder="类型" clearable style="width:100px">
            <el-option label="收入" value="INCOME" /><el-option label="支出" value="EXPENSE" />
          </el-select>
          <el-select v-model="filters.categoryId" placeholder="分类" clearable style="width:140px">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <el-date-picker v-model="filters.dateRange" type="daterange" range-separator="至"
            start-placeholder="开始" end-placeholder="结束" format="YYYY-MM-DD" value-format="YYYY-MM-DD"
            style="width:260px" />
          <el-button @click="fetchList">查询</el-button>
        </div>
      </div>

      <el-table :data="list" stripe v-loading="loading" style="width:100%">
        <el-table-column prop="billDate" label="日期" width="110" />
        <el-table-column prop="categoryName" label="分类" width="100" />
        <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
        <el-table-column label="金额" width="130" align="right">
          <template #default="{ row }">
            <span :class="row.type === 'INCOME' ? 'income-text' : 'expense-text'">
              {{ row.type === 'INCOME' ? '+' : '-' }}¥{{ row.amount }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pager" v-model:current-page="filters.page" v-model:page-size="filters.size"
        :total="total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" @change="fetchList" />

      <el-dialog :title="editingId ? '编辑账单' : '新增账单'" v-model="dialogVisible" width="420px">
        <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
          <el-form-item label="类型" prop="type">
            <el-radio-group v-model="form.type"><el-radio value="INCOME">收入</el-radio><el-radio value="EXPENSE">支出</el-radio></el-radio-group>
          </el-form-item>
          <el-form-item label="分类" prop="categoryId">
            <el-select v-model="form.categoryId" style="width:100%">
              <el-option v-for="c in filteredCategories" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="aiSuggestion" label="AI建议">
            <el-tag type="success" :loading="aiSuggesting" class="ai-tag"
              @click="applyAiSuggestion">
              {{ aiSuggestion.categoryName }} ({{ (aiSuggestion.confidence * 100).toFixed(0) }}%)
            </el-tag>
            <span class="ai-reason">{{ aiSuggestion.reason }}</span>
          </el-form-item>
          <el-form-item label="金额" prop="amount">
            <el-input-number v-model="form.amount" :min="0.01" :precision="2" style="width:100%" />
          </el-form-item>
          <el-form-item label="日期" prop="billDate">
            <el-date-picker v-model="form.billDate" type="date" format="YYYY-MM-DD" value-format="YYYY-MM-DD" style="width:100%" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.description" placeholder="选填" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
        </template>
      </el-dialog>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppLayout from '../../components/AppLayout.vue'
import { listBills, createBill, updateBill, deleteBill, exportBillsCsv, type BillVO } from '../../api/bill'
import { listCategories, type CategoryVO } from '../../api/category'
import { categorize, type CategorizeResponse } from '../../api/ai'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const list = ref<BillVO[]>([])
const total = ref(0)
const categories = ref<CategoryVO[]>([])
const editingId = ref<number | null>(null)
const formRef = ref()
const aiSuggesting = ref(false)
const aiSuggestion = ref<CategorizeResponse | null>(null)

const filters = reactive({ type: '', categoryId: null as number | null, dateRange: null as [string, string] | null, page: 1, size: 10 })
const form = reactive({ type: 'EXPENSE', categoryId: null as number | null, amount: 0, billDate: '', description: '' })
const rules = {
  type: [{ required: true, message: '请选择类型' }],
  categoryId: [{ required: true, message: '请选择分类' }],
  amount: [{ required: true, message: '请输入金额' }],
  billDate: [{ required: true, message: '请选择日期' }]
}

const filteredCategories = computed(() => categories.value.filter(c => c.type === form.type))

// Debounced AI categorization watcher
let aiTimer: ReturnType<typeof setTimeout> | null = null
watch([() => form.description, () => form.type], ([desc, type]) => {
  aiSuggestion.value = null
  if (!desc || desc.trim().length < 2) return
  if (aiTimer) clearTimeout(aiTimer)
  aiTimer = setTimeout(() => fetchAiSuggestion(desc, type), 800)
})

async function fetchAiSuggestion(description: string, type: string) {
  aiSuggesting.value = true
  try {
    const res = await categorize({ description, amount: form.amount || 1, type })
    if (res.data?.categoryId) {
      aiSuggestion.value = res.data
    }
  } catch { /* AI failure is non-blocking */ }
  finally { aiSuggesting.value = false }
}

function applyAiSuggestion() {
  if (!aiSuggestion.value) return
  form.categoryId = aiSuggestion.value.categoryId
}

onMounted(async () => {
  const cats = await listCategories()
  categories.value = cats.data
  fetchList()
})

async function fetchList() {
  loading.value = true
  try {
    const res = await listBills({
      type: filters.type || undefined,
      categoryId: filters.categoryId ?? undefined,
      startDate: filters.dateRange?.[0],
      endDate: filters.dateRange?.[1],
      page: filters.page,
      size: filters.size
    })
    list.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

function openDialog(row?: BillVO) {
  editingId.value = row?.id ?? null
  if (row) {
    form.type = row.type; form.categoryId = row.categoryId; form.amount = row.amount
    form.billDate = row.billDate; form.description = row.description || ''
  } else {
    form.type = 'EXPENSE'; form.categoryId = null; form.amount = 0
    form.billDate = ''; form.description = ''
  }
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const data = { ...form, categoryId: form.categoryId! }
    if (editingId.value) {
      await updateBill({ id: editingId.value, ...data })
      ElMessage.success('修改成功')
    } else {
      await createBill(data)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定删除？', '确认', { type: 'warning' })
  await deleteBill(id)
  ElMessage.success('删除成功')
  fetchList()
}

async function exportCsv() {
  try {
    const blob = await exportBillsCsv({
      type: filters.type || undefined,
      categoryId: filters.categoryId ?? undefined,
      startDate: filters.dateRange?.[0],
      endDate: filters.dateRange?.[1]
    })
    const url = URL.createObjectURL(blob as Blob)
    const a = document.createElement('a')
    a.href = url; a.download = 'bills.csv'; a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('CSV 导出成功')
  } catch { ElMessage.warning('导出失败') }
}
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 8px; }
.filters { display: flex; gap: 8px; align-items: center; }
.pager { margin-top: 16px; display: flex; justify-content: flex-end; }
.income-text { color: #67c23a; }
.expense-text { color: #f56c6c; }
.ai-tag { cursor: pointer; }
.ai-reason { margin-left: 8px; color: #909399; font-size: 12px; }
</style>
