import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { Conversation } from '@/types/conversation'
import { createConversation, deleteConversation, listConversations } from '@/api/conversation'

export const useConversationStore = defineStore('conversation', () => {
  const conversations = ref<Conversation[]>([])
  const activeId = ref<string | null>(null)
  const isLoading = ref(false)

  const activeConversation = computed(() => conversations.value.find((c) => c.id === activeId.value) ?? null)

  async function refresh(): Promise<void> {
    isLoading.value = true
    try {
      const result = await listConversations({ page: 1, pageSize: 100 })
      conversations.value = result.list
    } finally {
      isLoading.value = false
    }
  }

  async function newConversation(): Promise<Conversation> {
    const conversation = await createConversation({})
    conversations.value = [conversation, ...conversations.value.filter((c) => c.id !== conversation.id)]
    activeId.value = conversation.id
    return conversation
  }

  async function ensureActiveConversation(): Promise<string> {
    if (activeId.value && conversations.value.some((c) => c.id === activeId.value)) return activeId.value
    return (await newConversation()).id
  }

  function selectConversation(id: string): void {
    activeId.value = id
  }

  async function removeConversation(id: string): Promise<void> {
    await deleteConversation(id)
    conversations.value = conversations.value.filter((c) => c.id !== id)
    if (activeId.value === id) activeId.value = null
  }

  return { conversations, activeId, activeConversation, isLoading, refresh, newConversation, ensureActiveConversation, selectConversation, removeConversation }
})
