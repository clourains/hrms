export const chatWithAi = (message, conversationId) => {
  return fetch('/api/ai/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message, conversationId })
  })
}

export const getHrContextApi = () => {
  return fetch('/api/ai/context').then(res => res.text())
}
