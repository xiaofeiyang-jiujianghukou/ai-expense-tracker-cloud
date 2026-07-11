<template>
  <AppLayout>
    <div class="cat-page">
      <div class="toolbar">
        <el-button type="primary" @click="openDialog()">新增分类</el-button>
      </div>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="支出分类" name="EXPENSE" />
        <el-tab-pane label="收入分类" name="INCOME" />
      </el-tabs>

      <el-table :data="filteredList" stripe v-loading="loading" style="width:100%">
        <el-table-column prop="name" label="名称" min-width="200" />
        <el-table-column label="操作" width="160" align="center">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog :title="editingId ? '编辑分类' : '新增分类'" v-model="dialogVisible" width="360px">
        <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
          <el-form-item prop="type">
            <el-radio-group v-model="form.type" :disabled="!!editingId">
              <el-radio value="INCOME">收入</el-radio>
              <el-radio value="EXPENSE">支出</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item prop="name">
            <el-input v-model="form.name" placeholder="分类名称" />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppLayout from '../../components/AppLayout.vue'
import { listCategories, createCategory, updateCategory, deleteCategory, type CategoryVO } from '../../api/category'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const activeTab = ref('EXPENSE')
const list = ref<CategoryVO[]>([])
const editingId = ref<number | null>(null)
const formRef = ref()
const form = reactive({ name: '', type: 'EXPENSE' })
const rules = { name: [{ required: true, message: '请输入分类名称' }] }

const filteredList = computed(() => list.value.filter(c => c.type === activeTab.value))

onMounted(() => fetchList())
async function fetchList() {
  loading.value = true
  try { const res = await listCategories(); list.value = res.data } finally { loading.value = false }
}

function openDialog(row?: CategoryVO) {
  editingId.value = row?.id ?? null
  form.name = row?.name ?? ''
  form.type = row?.type ?? activeTab.value
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editingId.value) {
      await updateCategory({ id: editingId.value, name: form.name, type: form.type })
      ElMessage.success('修改成功')
    } else {
      await createCategory({ name: form.name, type: form.type })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定删除？', '确认', { type: 'warning' })
  await deleteCategory(id)
  ElMessage.success('删除成功')
  fetchList()
}
</script>

<style scoped>
.toolbar { margin-bottom: 12px; }
</style>
