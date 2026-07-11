import { createRouter, createWebHistory } from 'vue-router'
import { isLoggedIn } from '../utils/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/login/LoginView.vue')
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/register/RegisterView.vue')
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('../views/dashboard/DashboardView.vue')
    },
    {
      path: '/bills',
      name: 'bills',
      component: () => import('../views/bills/BillList.vue')
    },
    {
      path: '/categories',
      name: 'categories',
      component: () => import('../views/categories/CategoryManage.vue')
    },
    {
      path: '/statistics',
      name: 'statistics',
      component: () => import('../views/statistics/MonthlyStats.vue')
    },
    {
      path: '/trends',
      name: 'trends',
      component: () => import('../views/trends/TrendAnalysis.vue')
    },
    {
      path: '/budget',
      name: 'budget',
      component: () => import('../views/budget/BudgetManage.vue')
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/login'
    }
  ]
})

const publicPaths = ['/login', '/register']

router.beforeEach((to, _from, next) => {
  // 已登录 → 不允许停留在登录/注册页
  if (isLoggedIn() && publicPaths.includes(to.path)) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
