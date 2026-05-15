import { useEffect, useRef, useState } from 'react'
import { useAuth } from '../context/useAuth'
import { getSocketUrl, messageApi, roomApi } from '../lib/api'

function BackIcon() {
  return (
    <svg className="icon-inline" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M19 12H5" />
      <path d="M11 6l-6 6 6 6" />
    </svg>
  )
}

function LogoutIcon() {
  return (
    <svg className="icon-inline" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M10 17l5-5-5-5" />
      <path d="M15 12H4" />
      <path d="M14 5h4a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2h-4" />
    </svg>
  )
}

function SendIcon() {
  return (
    <svg className="icon-inline" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M22 2L11 13" />
      <path d="M22 2L15 22l-4-9-9-4 20-7Z" />
    </svg>
  )
}

function formatTime(value) {
  return new Date(value).toLocaleTimeString([], {
    hour: 'numeric',
    minute: '2-digit',
  })
}

function formatJoined(value) {
  return new Date(value).toLocaleDateString([], {
    month: 'short',
    day: 'numeric',
  })
}

function getInitial(value = '') {
  return value.trim().charAt(0).toUpperCase() || 'U'
}

function getUsername(value = '') {
  const normalized = String(value || '').trim()
  if (!normalized) return ''
  const atIndex = normalized.indexOf('@')
  return atIndex > 0 ? normalized.slice(0, atIndex) : normalized
}

function ChatRoomPage({ roomCode, onNavigate }) {
  const { user, token, logout } = useAuth()
  const [room, setRoom] = useState(null)
  const [members, setMembers] = useState([])
  const [messages, setMessages] = useState([])
  const [draft, setDraft] = useState('')
  const [socketState, setSocketState] = useState('connecting')
  const [status, setStatus] = useState({ type: '', message: '' })
  const [isLoading, setIsLoading] = useState(true)
  const [isSending, setIsSending] = useState(false)
  const socketRef = useRef(null)
  const chatScrollRef = useRef(null)
  const shouldAutoScrollRef = useRef(true)
  const messageEndRef = useRef(null)

  useEffect(() => {
    const bootstrapRoom = async () => {
      setIsLoading(true)
      setSocketState('connecting')
      setStatus({ type: '', message: '' })

      try {        const [roomData, memberList, history] = await Promise.all([
          roomApi.getByCode(roomCode, token),
          roomApi.members(roomCode, token),
          messageApi.list(roomCode, token),
        ])

        setRoom(roomData)
        setMembers(memberList)
        setMessages(history)
      } catch (error) {
        setStatus({ type: 'error', message: error.message })
      } finally {
        setIsLoading(false)
      }
    }

    bootstrapRoom()
  }, [roomCode, token])

  useEffect(() => {
    if (isLoading || !token || !room) {
      return undefined
    }

    const socket = new WebSocket(getSocketUrl(roomCode, token))
    socketRef.current = socket

    socket.onopen = () => {
      setSocketState('live')
    }

    socket.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data)

        if (payload.error) {
          setStatus({ type: 'error', message: payload.error })
          return
        }

        setMessages((current) => {
          if (current.some((item) => item.id === payload.id)) {
            return current
          }

          return [...current, payload]
        })
      } catch {
        setStatus({ type: 'error', message: 'Received an unreadable chat message.' })
      }
    }

    socket.onerror = () => {
      setSocketState('error')
    }

    socket.onclose = () => {
      setSocketState('offline')
    }

    return () => {
      socket.close()
      socketRef.current = null
    }
  }, [isLoading, room, roomCode, token])

  useEffect(() => {
    const element = chatScrollRef.current
    if (!element) {
      return undefined
    }

    const update = () => {
      const threshold = 120
      shouldAutoScrollRef.current =
        element.scrollTop + element.clientHeight >= element.scrollHeight - threshold
    }

    update()
    element.addEventListener('scroll', update)
    return () => element.removeEventListener('scroll', update)
  }, [])

  useEffect(() => {
    shouldAutoScrollRef.current = true
  }, [roomCode])

  useEffect(() => {
    if (shouldAutoScrollRef.current) {
      messageEndRef.current?.scrollIntoView({ behavior: 'smooth' })
    }
  }, [messages])

  const handleLogout = () => {
    logout()
    onNavigate('/auth', true)
  }

  const handleSendMessage = async (event) => {
    event.preventDefault()

    const content = draft.trim()
    if (!content) {
      return
    }

    setStatus({ type: '', message: '' })
    setIsSending(true)

    try {
      const activeSocket = socketRef.current

      if (activeSocket && activeSocket.readyState === WebSocket.OPEN) {
        activeSocket.send(JSON.stringify({ content }))
      } else {
        const savedMessage = await messageApi.send(roomCode, { content }, token)
        setMessages((current) => [...current, savedMessage])
      }

      setDraft('')
    } catch (error) {
      setStatus({ type: 'error', message: error.message })
    } finally {
      setIsSending(false)
    }
  }

  if (isLoading) {
    return (
      <div className="screen-shell loading-shell">
        <div className="loading-card">
          <div className="brand-mark brand-mark-large">
            <span className="brand-mark-bubble" />
          </div>
          <p>Loading room details...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="screen-shell screen-shell--fixed">
      <div className="chat-layout">
        <header className="chat-topbar">
          <button className="back-button" type="button" onClick={() => onNavigate('/dashboard')}>
            <BackIcon />
            Back to rooms
          </button>

          <div className="user-pill">
            <div className="avatar">{getInitial(user?.name || user?.email || 'U')}</div>
            <div className="user-copy">
              <strong>{user?.name}</strong>
              <span>{user?.email}</span>
            </div>
            <button className="icon-button" type="button" onClick={handleLogout} aria-label="Sign out">
              <LogoutIcon />
            </button>
          </div>
        </header>

        {status.message ? (
          <div className={`status-banner ${status.type || 'error'}`}>{status.message}</div>
        ) : null}

        <section className="chat-primary">
          <div className="chat-columns">
            <aside className="chat-aside">
              <section className="member-panel">
                <div className="space-between">
                  <h2 className="panel-title">Members</h2>
                  <span className="tiny-copy">{members.length} total</span>
                </div>

                <div className="member-list">
                  {members.map((member) => {
                    const email = member?.userEmail || member?.email || ''
                    const username = member?.username || member?.userName || member?.name || getUsername(email) || email

                    return (
                      <div className="member-item" key={`${email}-${member?.joinedAt || ''}`}>
                        <div className="member-avatar">{getInitial(username || email)}</div>
                        <div className="member-copy">
                          <strong>{username}</strong>
                          {email ? <span className="tiny-copy">{email}</span> : null}
                          {member?.joinedAt ? (
                            <span className="tiny-copy">Joined {formatJoined(member.joinedAt)}</span>
                          ) : null}
                        </div>
                      </div>
                    )
                  })}
                </div>
              </section>

              <div className="chat-sidebar">
                <div className="space-between">
                  <h2 className="panel-title">Room Details</h2>
                  <span className="code-pill">{room?.roomCode}</span>
                </div>

                <div className="sidebar-group">
                  <div className="sidebar-card">
                    <strong>Created by</strong>
                    <span>{room?.createdBy}</span>
                  </div>
                  <div className="sidebar-card">
                    <strong>Room id</strong>
                    <span>{room?.roomCode}</span>
                  </div>
                  <div className="sidebar-card">
                    <strong>Connection</strong>
                    <span className={`status-pill ${socketState}`}>{socketState}</span>
                  </div>
                </div>
              </div>
            </aside>
            <main className="chat-stage">
              <div className="chat-title-row">
                <div>
                  <div className="chat-title">
                    <h1>{room?.name || 'Room chat'}</h1>
                    <p className="chat-subtitle">{room?.description || 'Live conversation is ready to go.'}</p>
                  </div>
                  <p className="room-meta">
                    {messages.length === 0 ? 'No messages yet.' : `${messages.length} message(s) in this room`}
                  </p>
                </div>
                <div className="chat-stage-meta">
                  <span className="code-pill">{room?.roomCode}</span>
                  <span className={`status-pill ${socketState}`}>{socketState}</span>
                </div>
              </div>

              <div className="chat-scroll" ref={chatScrollRef}>
                {messages.length === 0 ? (
                  <div className="empty-state">Say hello to start the conversation.</div>
                ) : (
                  messages.map((message) => {
                    const isOwnMessage = message.senderEmail === user?.email

                    return (
                      <div
                        className={`message-row ${isOwnMessage ? 'self' : ''}`}
                        key={message.id || `${message.senderEmail}-${message.sentAt}`}
                      >
                        <article className="message-card">
                          <div className="message-meta">
                            <strong>{isOwnMessage ? 'You' : message.senderName || message.senderEmail}</strong>
                            <span>{formatTime(message.sentAt)}</span>
                          </div>
                          <div className="message-body">{message.content}</div>
                        </article>
                      </div>
                    )
                  })
                )}
                <div ref={messageEndRef} />
              </div>

              <form className="chat-composer" onSubmit={handleSendMessage}>
                <textarea
                  placeholder="Type your message here..."
                  value={draft}
                  onChange={(event) => setDraft(event.target.value)}
                />
                <button className="primary-button" type="submit" disabled={isSending}>
                  <SendIcon />
                  {isSending ? 'Sending...' : 'Send'}
                </button>
              </form>
            </main>
          </div>
        </section>
      </div>
    </div>
  )
}

export default ChatRoomPage



