<template>
  <div class="ai-chat-wrapper">
    <!-- Floating button -->
    <div class="ai-fab" @click="toggleChat" v-show="!isOpen">
      <el-icon :size="24"><ChatDotRound /></el-icon>
      <span class="fab-label">AI助手</span>
    </div>

    <!-- Chat panel -->
    <transition name="slide-up">
      <div class="ai-chat-panel" v-show="isOpen">
        <!-- Header -->
        <div class="chat-header">
          <div class="header-info">
            <el-icon :size="20" color="#fff"><ChatDotRound /></el-icon>
            <span class="chat-title">HRMS AI 助手</span>
            <el-tag size="small" type="success" effect="dark" class="status-tag">在线</el-tag>
          </div>
          <div class="header-actions">
            <el-tooltip content="清空对话" placement="bottom">
              <el-icon class="action-icon" :size="18" @click="clearMessages"><Delete /></el-icon>
            </el-tooltip>
            <el-tooltip content="关闭" placement="bottom">
              <el-icon class="action-icon" :size="18" @click="toggleChat"><Close /></el-icon>
            </el-tooltip>
          </div>
        </div>

        <!-- Messages -->
        <div class="chat-messages" ref="messagesRef">
          <div v-if="messages.length === 0" class="welcome-area">
            <el-icon :size="48" color="#409EFF"><ChatDotRound /></el-icon>
            <h3>HRMS AI 助手</h3>
            <p>我可以帮您分析员工数据、预测人才趋势、生成管理建议</p>
            <div class="suggestion-chips">
              <div class="chip" v-for="s in suggestions" :key="s" @click="sendMessage(s)">
                {{ s }}
              </div>
            </div>
          </div>

          <div v-for="(msg, idx) in messages" :key="idx" class="message-row" :class="msg.role">
            <div class="message-avatar">
              <el-icon v-if="msg.role === 'user'" :size="20"><User /></el-icon>
              <el-icon v-else :size="20" color="#409EFF"><ChatDotRound /></el-icon>
            </div>
            <div class="message-content">
              <div class="message-text" v-html="formatMessage(msg.content)"></div>
              <div v-if="msg.role === 'assistant' && msg.streaming" class="typing-indicator">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>
        </div>

        <!-- Input -->
        <div class="chat-input-area">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="1"
            autosize
            placeholder="输入您的问题..."
            resize="none"
            @keydown.enter.exact.prevent="handleEnter"
            :disabled="isStreaming"
          />
          <el-button
            type="primary"
            :icon="Promotion"
            circle
            @click="handleSend"
            :loading="isStreaming"
            :disabled="!inputText.trim()"
          />
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, nextTick, onUnmounted } from 'vue'
import { ChatDotRound, Delete, Close, User, Promotion } from '@element-plus/icons-vue'

const isOpen = ref(false)
const inputText = ref('')
const isStreaming = ref(false)
const messagesRef = ref(null)
const messages = ref([])

const suggestions = [
  '公司目前有多少员工？',
  '分析一下各部门人员分布',
  '哪些员工有离职风险？',
  '薪资水平分析',
  '绩效表现如何？'
]

const toggleChat = () => {
  isOpen.value = !isOpen.value
}

const clearMessages = () => {
  messages.value = []
}

const handleEnter = () => {
  if (!isStreaming.value && inputText.value.trim()) {
    handleSend()
  }
}

const handleSend = () => {
  const text = inputText.value.trim()
  if (!text || isStreaming.value) return

  messages.value.push({ role: 'user', content: text, streaming: false })
  inputText.value = ''

  const assistantMsg = ref({ role: 'assistant', content: '', streaming: true })
  messages.value.push(assistantMsg.value)

  isStreaming.value = true
  scrollToBottom()

  const controller = new AbortController()

  fetch('/api/ai/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message: text, conversationId: Date.now().toString() }),
    signal: controller.signal
  }).then(response => {
    if (!response.ok) throw new Error('Request failed: ' + response.status)

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    const read = () => {
      reader.read().then(({ done, value }) => {
        if (done) {
          assistantMsg.value.streaming = false
          isStreaming.value = false
          return
        }

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (!line.trim()) continue

          if (line.startsWith('event:')) continue

          if (line.startsWith('data:')) {
            const data = line.substring(5).trim()
            if (data === '') continue

            try {
              const parsed = JSON.parse(data)
              if (parsed.content) {
                assistantMsg.value.content += parsed.content
                scrollToBottom()
              }
            } catch (e) {
              // Not JSON — fallback: append raw text directly
              if (data && data.trim() && !data.includes('[DONE]')) {
                assistantMsg.value.content += data
                scrollToBottom()
              }
            }
          }
        }

        read()
      }).catch(err => {
        if (err.name === 'AbortError') return
        assistantMsg.value.content += '\n[连接错误: ' + err.message + ']'
        assistantMsg.value.streaming = false
        isStreaming.value = false
      })
    }

    read()
  }).catch(err => {
    assistantMsg.value.content = '连接失败: ' + err.message + '\n\n请确认后端服务已启动。'
    assistantMsg.value.streaming = false
    isStreaming.value = false
  })
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

const formatMessage = (text) => {
  if (!text) return ''
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')
}

onUnmounted(() => {
  isOpen.value = false
})
</script>

<style scoped lang="scss">
.ai-chat-wrapper {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 9999;
}

.ai-fab {
  display: flex;
  align-items: center;
  gap: 8px;
  background: linear-gradient(135deg, #409EFF, #1d6fe0);
  color: #fff;
  padding: 12px 20px;
  border-radius: 28px;
  cursor: pointer;
  box-shadow: 0 4px 20px rgba(64, 158, 255, 0.4);
  transition: all 0.3s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 24px rgba(64, 158, 255, 0.5);
  }

  .fab-label {
    font-size: 14px;
    font-weight: 500;
  }
}

.ai-chat-panel {
  width: 420px;
  height: 600px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  background: linear-gradient(135deg, #409EFF, #1d6fe0);
  padding: 14px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;

  .header-info {
    display: flex;
    align-items: center;
    gap: 8px;

    .chat-title {
      color: #fff;
      font-size: 16px;
      font-weight: 600;
    }

    .status-tag {
      margin-left: 4px;
    }
  }

  .header-actions {
    display: flex;
    gap: 16px;

    .action-icon {
      color: rgba(255, 255, 255, 0.85);
      cursor: pointer;
      transition: color 0.3s;

      &:hover {
        color: #fff;
      }
    }
  }
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-thumb {
    background: #dcdfe6;
    border-radius: 2px;
  }
}

.welcome-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  padding: 20px;

  h3 {
    margin: 16px 0 8px;
    font-size: 20px;
    color: #303133;
  }

  p {
    color: #909399;
    font-size: 14px;
    margin-bottom: 24px;
  }

  .suggestion-chips {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    justify-content: center;

    .chip {
      padding: 8px 16px;
      background: #ecf5ff;
      color: #409EFF;
      border: 1px solid #d9ecff;
      border-radius: 20px;
      font-size: 13px;
      cursor: pointer;
      transition: all 0.3s;

      &:hover {
        background: #409EFF;
        color: #fff;
        border-color: #409EFF;
      }
    }
  }
}

.message-row {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;

  &.user {
    flex-direction: row-reverse;

    .message-content {
      align-items: flex-end;
    }

    .message-text {
      background: #409EFF;
      color: #fff;
      border-radius: 16px 16px 4px 16px;
    }
  }

  &.assistant {
    .message-text {
      background: #f4f4f5;
      color: #303133;
      border-radius: 16px 16px 16px 4px;
    }
  }

  .message-avatar {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    .user & {
      background: #409EFF;
      color: #fff;
    }

    .assistant & {
      background: #ecf5ff;
    }
  }

  .message-content {
    display: flex;
    flex-direction: column;
    max-width: 300px;
  }

  .message-text {
    padding: 10px 14px;
    font-size: 14px;
    line-height: 1.6;
    word-break: break-word;
  }
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 6px 14px;

  span {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #c0c4cc;
    animation: typing 1.4s infinite;

    &:nth-child(2) {
      animation-delay: 0.2s;
    }

    &:nth-child(3) {
      animation-delay: 0.4s;
    }
  }
}

@keyframes typing {
  0%, 60%, 100% {
    opacity: 0.3;
    transform: scale(0.8);
  }
  30% {
    opacity: 1;
    transform: scale(1);
  }
}

.chat-input-area {
  padding: 12px 16px;
  border-top: 1px solid #ebeef5;
  display: flex;
  gap: 10px;
  align-items: flex-end;
  flex-shrink: 0;

  :deep(.el-textarea__inner) {
    border-radius: 20px;
    padding: 8px 16px;
    font-size: 14px;
    resize: none;
    max-height: 100px;
  }
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.3s ease;
}

.slide-up-enter-from,
.slide-up-leave-to {
  opacity: 0;
  transform: translateY(20px);
}
</style>
