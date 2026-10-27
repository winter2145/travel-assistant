import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'workbench',
      alias: '/travel',
      component: () => import('@/views/WorkbenchView.vue'),
      meta: { title: '旅行工作台' },
    },
    {
      path: '/trips',
      name: 'trips',
      alias: '/travel/history',
      component: () => import('@/views/MyTripsView.vue'),
      meta: { title: '我的旅行' },
    },
    {
      path: '/plans/:id',
      name: 'plan-detail',
      component: () => import('@/views/PlanDetailView.vue'),
      props: true,
      meta: { title: '行程详情' },
    },
    {
      path: '/travel/:id/itinerary',
      name: 'travel-itinerary',
      component: () => import('@/views/PlanDetailView.vue'),
      props: true,
      meta: { title: '行程详情' },
    },
    {
      path: '/travel/:id/export',
      name: 'travel-export',
      component: () => import('@/views/GuidePreviewView.vue'),
      props: true,
      meta: { title: '攻略预览与导出' },
    },
    {
      path: '/guide/:id',
      name: 'guide-preview',
      component: () => import('@/views/GuidePreviewView.vue'),
      props: true,
      meta: { title: '旅游攻略' },
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})

router.afterEach((to) => {
  const title = to.meta.title
  document.title = title ? `${title} · 行旅` : '行旅 · AI 旅游规划工作台'
})

export default router
