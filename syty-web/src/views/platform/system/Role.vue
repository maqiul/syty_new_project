<template>
  <div class="platform-system-role">
    <a-card :bordered="false">
      <template #title>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>角色管理</span>
          <a-button type="primary">
            <template #icon><PlusOutlined /></template>
            新增角色
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
          <template v-if="column.dataIndex === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'red'">
              {{ record.status === 1 ? '启用' : '禁用' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a>编辑</a>
              <a-divider type="vertical" />
              <a type="danger">删除</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'

const columns = [
  { title: '角色名称', dataIndex: 'name', key: 'name' },
  { title: '角色编码', dataIndex: 'code', key: 'code' },
  { title: '描述', dataIndex: 'description', key: 'description' },
  { title: '用户数', dataIndex: 'userCount', key: 'userCount' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt' },
  { title: '操作', key: 'action', width: 160 },
]

const tableData = ref([
  { id: 1, name: '超级管理员', code: 'super_admin', description: '拥有所有权限', userCount: 1, status: 1, createdAt: '2024-01-01 10:00:00' },
  { id: 2, name: '租户管理员', code: 'tenant_admin', description: '管理租户下用户和资源', userCount: 5, status: 1, createdAt: '2024-01-10 11:30:00' },
  { id: 3, name: '普通用户', code: 'user', description: '基本使用权限', userCount: 120, status: 1, createdAt: '2024-01-10 11:30:00' },
])
</script>

<style scoped>
.platform-system-role {
  padding: 24px;
}
</style>
