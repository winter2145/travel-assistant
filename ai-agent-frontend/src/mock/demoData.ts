/**
 * DEMO DATA —— 仅用于布局与空态开发，与真实后端数据严格分离。
 *
 * 使用规则：
 * 1. 真实业务逻辑（对话、行程解析）绝不引用本文件；
 * 2. 引用本文件的组件必须在界面上显示"演示数据"标记；
 * 3. 后端对应接口接入后，优先删除对应 demo 块。
 */

import type { Conversation } from '@/types/conversation'
import type { Itinerary, ItineraryDay } from '@/types/plan'

/** 演示会话列表（用于会话侧边栏布局，P1 接入后删除） */
export const demoConversations: Conversation[] = [
  { id: 'demo-c1', title: '大连 3 天 2 晚 · 情侣出行', createdAt: Date.now() - 1000 * 60 * 60 * 26, updatedAt: Date.now() - 1000 * 60 * 30 },
  { id: 'demo-c2', title: '青岛 4 天 3 晚 · 家庭亲子', createdAt: Date.now() - 1000 * 60 * 60 * 50, updatedAt: Date.now() - 1000 * 60 * 60 * 2 },
  { id: 'demo-c3', title: '成都 5 天 · 美食之旅', createdAt: Date.now() - 1000 * 60 * 60 * 80, updatedAt: Date.now() - 1000 * 60 * 60 * 24 },
]

/** 演示行程（用于行程详情 / 攻略预览布局，P2 接入后删除） */
export const demoItinerary: Itinerary = {
  city: '大连',
  startDate: '2026-10-24',
  days: [
    {
      day: 1,
      date: '2026-10-24',
      activities: [
        {
          name: '星海广场',
          location: '中山路572号（沙河口区）',
          startTime: '09:00',
          endTime: '11:00',
          estimatedCost: 0,
          travelMinutesFromPrevious: 0,
          notes: '亚洲最大城市广场，可远眺星海湾大桥',
        },
        {
          name: '付家庄海滨浴场',
          location: '滨海路50号付家庄公园内',
          startTime: '14:00',
          endTime: '16:30',
          estimatedCost: 0,
          travelMinutesFromPrevious: 30,
          notes: '沙滩漫步、看日落',
        },
        {
          name: '大连森林动物园',
          location: '滨海西路79号',
          startTime: '17:00',
          endTime: '19:00',
          estimatedCost: 120,
          travelMinutesFromPrevious: 25,
          notes: '晚场开放，适合亲子',
        },
      ],
    },
    {
      day: 2,
      date: '2026-10-25',
      activities: [
        {
          name: '海昌·东方水城',
          location: '港浦路114号',
          startTime: '09:30',
          endTime: '12:00',
          estimatedCost: 60,
          travelMinutesFromPrevious: 0,
          notes: '威尼斯风情街区，贡多拉游船',
        },
        {
          name: '莲花山观景台',
          location: '迎春路60号森林动物园西山',
          startTime: '14:00',
          endTime: '16:00',
          estimatedCost: 0,
          travelMinutesFromPrevious: 40,
          notes: '俯瞰大连全城与海岸线',
        },
      ],
    },
    {
      day: 3,
      date: '2026-10-26',
      activities: [
        {
          name: '滨海大道骑行',
          location: '西岗区-沙河口区滨海路',
          startTime: '09:00',
          endTime: '11:30',
          estimatedCost: 30,
          travelMinutesFromPrevious: 0,
          notes: '中国最美海岸公路之一',
        },
        {
          name: '大连自然博物馆',
          location: '黑石礁西村街40号',
          startTime: '14:00',
          endTime: '16:30',
          estimatedCost: 0,
          travelMinutesFromPrevious: 20,
          notes: '免费开放，海洋生物标本',
        },
      ],
    },
  ] as ItineraryDay[],
  budget: 3000,
  pace: 'normal',
  estimatedActivityCost: 210,
  notes: ['仅汇总活动费用，不含住宿、餐饮和交通', '建议继续进行预算与时间冲突校验'],
}
