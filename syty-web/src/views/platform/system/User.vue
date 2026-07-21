<template>
  <div class="platform-system-user">
    <a-card :bordered="false">
      <template #title>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>用户管理</span>
          <a-button type="primary">
            <template #icon><PlusOutlined /></template>
            新增用户
          </a-button>
        </div>
      </template>

      <a-table
        :columns="columns"
        :dataSource="tableData"
        rowKey="id"
        :pagination="{
          current: 1,
          pageSize: 10,
          total: tableData.length,
          showTotal: (total: number) => `共 ${total} 条`,
        }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'role'">
            <a-tag color="blue">{{ record.role }}</a-tag>
          </template>
          <template v-if="column.dataIndex === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'red'">
              {{ record.status === 1 ? '启用' : '禁用' }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { PlusOutlined } from '@antdv-next/icons'

const columns = [
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: '姓名', dataIndex: 'realName', key: 'realName' },
  { title: '手机号', dataIndex: 'phone', key: 'phone' },
  { title: '角色', dataIndex: 'role', key: 'role' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt' },
]

const tableData = ref([
  { id: 1, username: 'admin', realName: '管理员', phone: '13800138000', role: '超级管理员', status: 1, createdAt: '2024-01-01 10:00:00' },
  { id: 2, username: 'zhangsan', realName: '张三', phone: '13900139000', role: '普通用户', status: 1, createdAt: '2024-02-15 09:30:00' },
  { id: 3, username: 'lisi', realName: '李四', phone: '13700137000', role: '租户管理员', status: 1, createdAt: '2024-03-20 14:20:00' },
])
</script>

<style scoped>
.platform-system-user {
  padding: 24px;
}
</style>
