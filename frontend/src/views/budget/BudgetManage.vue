<template>
  <AppLayout>
    <div class="budget-page">
      <div class="toolbar">
        <el-date-picker v-model="selectedMonth" type="month" format="YYYY年M月" value-format="YYYY-MM"
          placeholder="选择月份" @change="fetchBudgets" />
        <el-button type="primary" @click="openDialog()">设置预算</el-button>
        <el-button :loading="aiLoading" @click="fetchAiAdvice">AI 建议</el-button>
      </div>

      <el-card v-if="budgets.length">
        <el-table :data="budgets" stripe size="small" v-loading="loading">
          <el-table-column prop="categoryName" label="分类" width="100" />
          <el-table-column prop="targetAmount" label="预算" align="right" width="120">
            <template #default="{ row }">¥{{ row.targetAmount }}</template>
          </el-table-column>
          <el-table-column prop="actualAmount" label="实际" align="right" width="120">
            <template #default="{ row }">¥{{ row.actualAmount }}</template>
          </el-table-column>
          <el-table-column label="进度" min-width="200">
            <template #default="{ row }">
              <el-progress :percentage="Math.min(row.percentage, 100)" :color="row.percentage > 100 ? '#f56c6c' : '#409eff'" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button type="primary" size="small" link @click="openDialog(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
      <el-empty v-else-if="!loading" description="暂无预算，点击「设置预算」或「AI 建议」开始" :image-size="80" />

      <el-dialog v-model="dialogVisible" :title="editingBudget ? '编辑预算' : '新增预算'" width="400px">
        <el-form :model="form" label-width="80px">
          <el-form-item label="分类">
            <el-select v-model="form.categoryId" placeholder="选择分类" style="width:100%">
              <el-option v-for="c in expenseCategories" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="预算金额">
            <el-input-number v-model="form.targetAmount" :min="0" :precision="2" style="width:100%" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="saveBudget">保存</el-button>
        </template>
      </el-dialog>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import AppLayout from '../../components/AppLayout.vue'
import { listBudgets, setBudget, type BudgetVO } from '../../api/budget'
import { listCategories, type CategoryVO } from '../../api/category'
import { getBudgetAdvice } from '../../api/ai'
import { ElMessage } from 'element-plus'

const selectedMonth = ref(`${new Date().getFullYear()}-${String(new Date().getMonth() + 1).padStart(2, '0')}`)
const budgets = ref<BudgetVO[]>([])
const expenseCategories = ref<CategoryVO[]>([])
const loading = ref(false)
const saving = ref(false)
const aiLoading = ref(false)
const dialogVisible = ref(false)
const editingBudget = ref<BudgetVO | null>(null)
const form = reactive({ categoryId: null as number | null, targetAmount: 0 })

onMounted(async () => {
  try {
    const cats = await listCategories('EXPENSE')
    expenseCategories.value = cats.data
  } catch { /* ignore */ }
  fetchBudgets()
})

async function fetchBudgets() {
  loading.value = true
  try {
    const [y, m] = selectedMonth.value.split('-')
    const res = await listBudgets({ year: Number(y), month: Number(m) })
    budgets.value = res.data
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function openDialog(budget?: BudgetVO) {
  editingBudget.value = budget || null
  if (budget) {
    form.categoryId = budget.categoryId
    form.targetAmount = budget.targetAmount
  } else {
    form.categoryId = null
    form.targetAmount = 0
  }
  dialogVisible.value = true
}

async function saveBudget() {
  if (!form.categoryId) return
  saving.value = true
  try {
    const [y, m] = selectedMonth.value.split('-')
    await setBudget({ categoryId: form.categoryId, year: Number(y), month: Number(m), targetAmount: form.targetAmount })
    dialogVisible.value = false
    fetchBudgets()
  } catch { /* ignore */ }
  finally { saving.value = false }
}

async function fetchAiAdvice() {
  aiLoading.value = true
  try {
    const [y, m] = selectedMonth.value.split('-')
    const res = await getBudgetAdvice({ year: Number(y), month: Number(m) })
    const items = res.data.items || []
    let applied = 0
    for (const item of items) {
      const cat = expenseCategories.value.find(c => c.name === item.categoryName)
      if (cat) {
        await setBudget({ categoryId: cat.id, year: Number(y), month: Number(m), targetAmount: item.amount })
        applied++
      }
    }
    fetchBudgets()
    ElMessage.success(`AI 已为 ${applied} 个分类设置预算建议`)
  } catch {
    ElMessage.warning('AI 建议获取失败，请稍后重试')
  }
  finally { aiLoading.value = false }
}
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
</style>
