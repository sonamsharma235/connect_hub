const statusEl = document.getElementById("status");
const roomCodeEl = document.getElementById("roomCode");
const tokenEl = document.getElementById("token");
const connectBtn = document.getElementById("connectBtn");
const disconnectBtn = document.getElementById("disconnectBtn");
const leaveBtn = document.getElementById("leaveBtn");
const deleteBtn = document.getElementById("deleteBtn");
const messagesEl = document.getElementById("messages");
const messageInputEl = document.getElementById("messageInput");
const sendBtn = document.getElementById("sendBtn");
const refreshRoomsBtn = document.getElementById("refreshRoomsBtn");
const roomsNoticeEl = document.getElementById("roomsNotice");
const roomsListEl = document.getElementById("roomsList");
const roomNameEl = document.getElementById("roomName");
const roomDescEl = document.getElementById("roomDesc");
const roomIdEl = document.getElementById("roomId");
const createRoomBtn = document.getElementById("createRoomBtn");
const createRoomNoticeEl = document.getElementById("createRoomNotice");

let socket = null;
let canDeleteRoom = false;
let roomsCache = [];
let roomsRefreshTimer = null;

function setStatus(text) {
  statusEl.textContent = text;
}

function setNotice(el, message, kind = "") {
  if (!el) return;
  el.textContent = message || "";
  el.hidden = !message;
  el.classList.remove("notice--error", "notice--success");
  if (kind === "error") el.classList.add("notice--error");
  if (kind === "success") el.classList.add("notice--success");
}

function setDeleteRoomUi(allowed) {
  canDeleteRoom = Boolean(allowed);
  deleteBtn.hidden = !canDeleteRoom;
  deleteBtn.disabled = !canDeleteRoom;
}

function decodeBase64Url(value) {
  const normalized = String(value || "").replace(/-/g, "+").replace(/_/g, "/");
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, "=");
  const binary = atob(padded);
  const bytes = Uint8Array.from(binary, (c) => c.charCodeAt(0));
  return new TextDecoder().decode(bytes);
}

function getEmailFromJwt(token) {
  try {
    const parts = String(token || "").split(".");
    if (parts.length < 2) return null;
    const payload = JSON.parse(decodeBase64Url(parts[1]));
    return payload?.sub || payload?.email || payload?.username || null;
  } catch {
    return null;
  }
}

function isNearBottom(container, thresholdPx = 24) {
  return container.scrollTop + container.clientHeight >= container.scrollHeight - thresholdPx;
}

function scrollToBottom(container) {
  container.scrollTop = container.scrollHeight;
}

function appendSystemMessage(text) {
  const stick = isNearBottom(messagesEl);
  const div = document.createElement("div");
  div.className = "message message--system";
  div.textContent = text;
  messagesEl.appendChild(div);
  if (stick) scrollToBottom(messagesEl);
}

function formatTime(isoInstant) {
  try {
    const date = new Date(isoInstant);
    return new Intl.DateTimeFormat(undefined, {
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      year: "numeric",
      month: "short",
      day: "2-digit",
    }).format(date);
  } catch {
    return String(isoInstant ?? "");
  }
}

function appendChatMessage(msg) {
  const stick = isNearBottom(messagesEl);

  const wrapper = document.createElement("div");
  wrapper.className = "message";

  const meta = document.createElement("div");
  meta.className = "message__meta";

  const sender = document.createElement("div");
  sender.className = "message__sender";
  sender.textContent = msg.senderName || msg.senderEmail || "Unknown";

  const time = document.createElement("div");
  time.className = "message__time";
  time.textContent = msg.sentAt ? formatTime(msg.sentAt) : "";

  meta.appendChild(sender);
  meta.appendChild(time);

  const content = document.createElement("div");
  content.className = "message__content";
  content.textContent = msg.content ?? "";

  wrapper.appendChild(meta);
  wrapper.appendChild(content);
  messagesEl.appendChild(wrapper);

  if (stick) scrollToBottom(messagesEl);
}

function clearMessages() {
  messagesEl.innerHTML = "";
}

function wsUrl(roomCode, token) {
  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  const params = new URLSearchParams({
    roomCode,
    token,
  });
  return `${protocol}//${window.location.host}/ws/chat?${params.toString()}`;
}

async function loadHistory(roomCode, token) {
  const resp = await fetch(`/api/messages/rooms/${encodeURIComponent(roomCode)}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });

  if (!resp.ok) {
    const body = await resp.text().catch(() => "");
    throw new Error(`History request failed: ${resp.status} ${resp.statusText}${body ? ` - ${body}` : ""}`);
  }

  return resp.json();
}

async function loadRoom(roomCode, token) {
  const resp = await fetch(`/api/rooms/${encodeURIComponent(roomCode)}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });

  if (!resp.ok) {
    const body = await resp.text().catch(() => "");
    throw new Error(`Room request failed: ${resp.status} ${resp.statusText}${body ? ` - ${body}` : ""}`);
  }

  return resp.json();
}

async function loadMe(token) {
  const resp = await fetch(`/api/auth/me`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });

  if (!resp.ok) {
    const body = await resp.text().catch(() => "");
    throw new Error(`Profile request failed: ${resp.status} ${resp.statusText}${body ? ` - ${body}` : ""}`);
  }

  return resp.json();
}

async function readErrorMessage(resp) {
  const contentType = resp.headers?.get?.("content-type") || "";

  if (contentType.includes("application/json")) {
    const json = await resp.json().catch(() => null);
    const message =
      json?.message ||
      json?.error ||
      json?.reason ||
      json?.detail ||
      (typeof json === "string" ? json : null);
    if (message) return String(message);
    if (json) return JSON.stringify(json);
  }

  const text = await resp.text().catch(() => "");
  return text || `${resp.status} ${resp.statusText}`;
}

async function fetchRooms(token) {
  const resp = await fetch(`/api/rooms`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });

  if (!resp.ok) {
    const message = await readErrorMessage(resp);
    throw new Error(message || `Rooms request failed: ${resp.status} ${resp.statusText}`);
  }

  return resp.json();
}

async function createRoom(payload, token) {
  const resp = await fetch(`/api/rooms`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(payload),
  });

  if (!resp.ok) {
    const message = await readErrorMessage(resp);
    throw new Error(message || `Create room failed: ${resp.status} ${resp.statusText}`);
  }

  return resp.json();
}

function renderRooms(rooms) {
  if (!roomsListEl) return;

  roomsListEl.innerHTML = "";

  if (!Array.isArray(rooms) || rooms.length === 0) {
    setNotice(roomsNoticeEl, "No rooms yet. Create one to start chatting.", "success");
    return;
  }

  setNotice(roomsNoticeEl, "");

  for (const room of rooms) {
    const card = document.createElement("div");
    card.className = "room-card";

    const main = document.createElement("div");
    main.className = "room-card__main";

    const name = document.createElement("p");
    name.className = "room-card__name";
    name.textContent = room?.name || "Untitled room";

    const meta = document.createElement("div");
    meta.className = "room-card__meta";

    const codePill = document.createElement("span");
    codePill.className = "pill";
    codePill.textContent = room?.roomCode || "";

    meta.appendChild(codePill);

    main.appendChild(name);
    main.appendChild(meta);

    if (room?.description) {
      const desc = document.createElement("div");
      desc.className = "room-card__desc";
      desc.textContent = room.description;
      main.appendChild(desc);
    }

    const openBtn = document.createElement("button");
    openBtn.className = "btn btn--primary";
    openBtn.type = "button";
    openBtn.textContent = "Open chat";
    openBtn.addEventListener("click", () => openRoomChat(room?.roomCode));

    const leaveRoomBtn = document.createElement("button");
    leaveRoomBtn.className = "btn";
    leaveRoomBtn.type = "button";
    leaveRoomBtn.textContent = "Leave";
    leaveRoomBtn.addEventListener("click", (event) => {
      event.stopPropagation();
      leaveRoom(room?.roomCode);
    });

    card.addEventListener("click", (event) => {
      if (event.target === openBtn || event.target === leaveRoomBtn) return;
      openRoomChat(room?.roomCode);
    });

    card.appendChild(main);
    const actions = document.createElement("div");
    actions.className = "room-card__actions";
    actions.appendChild(openBtn);
    actions.appendChild(leaveRoomBtn);
    card.appendChild(actions);

    roomsListEl.appendChild(card);
  }
}

async function refreshRooms() {
  const token = tokenEl?.value?.trim?.() || "";

  setNotice(createRoomNoticeEl, "");

  if (!token) {
    roomsCache = [];
    renderRooms(roomsCache);
    setNotice(roomsNoticeEl, "Paste your JWT token to load rooms.");
    return;
  }

  try {
    const rooms = await fetchRooms(token);
    roomsCache = Array.isArray(rooms) ? rooms : [];
    roomsCache.sort((a, b) => String(a?.name || "").localeCompare(String(b?.name || "")));
    renderRooms(roomsCache);
  } catch (e) {
    roomsCache = [];
    renderRooms(roomsCache);
    setNotice(roomsNoticeEl, e?.message || "Failed to load rooms.", "error");
  }
}

function scheduleRefreshRooms() {
  if (roomsRefreshTimer) window.clearTimeout(roomsRefreshTimer);
  roomsRefreshTimer = window.setTimeout(() => refreshRooms(), 600);
}

function openRoomChat(roomCode) {
  const normalized = String(roomCode || "").trim();
  if (!normalized) {
    setNotice(roomsNoticeEl, "Room id is missing for this room.", "error");
    return;
  }

  roomCodeEl.value = normalized;
  connect();
  messagesEl?.scrollIntoView?.({ behavior: "smooth", block: "start" });
}

function setConnectedUi(connected) {
  connectBtn.disabled = connected;
  disconnectBtn.disabled = !connected;
  if (leaveBtn) {
    leaveBtn.hidden = !connected;
    leaveBtn.disabled = !connected;
  }
  sendBtn.disabled = !connected;
  roomCodeEl.disabled = connected;
  tokenEl.disabled = connected;
  if (!connected) setDeleteRoomUi(false);
}

async function leaveRoom(roomCodeOverride) {
  const targetRoomCode = String(roomCodeOverride || roomCodeEl?.value || "").trim();
  const token = tokenEl?.value?.trim?.() || "";

  if (!targetRoomCode) {
    appendSystemMessage("Room code is required.");
    return;
  }
  if (!token) {
    appendSystemMessage("JWT token is required (paste it without 'Bearer ').");
    return;
  }

  const ok = window.confirm(`Leave room '${targetRoomCode}'?`);
  if (!ok) return;

  try {
    const resp = await fetch(`/api/rooms/${encodeURIComponent(targetRoomCode)}/leave`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
    });

    if (!resp.ok) {
      const body = await resp.text().catch(() => "");
      throw new Error(`Leave failed: ${resp.status} ${resp.statusText}${body ? ` - ${body}` : ""}`);
    }

    roomsCache = Array.isArray(roomsCache)
      ? roomsCache.filter((room) => String(room?.roomCode || "") !== targetRoomCode)
      : [];
    renderRooms(roomsCache);

    if (String(roomCodeEl?.value || "").trim() === targetRoomCode) {
      disconnect();
    }

    setNotice(roomsNoticeEl, `Left room '${targetRoomCode}'.`, "success");
    refreshRooms();
  } catch (e) {
    appendSystemMessage(e?.message || "Failed to leave room.");
  }
}

async function deleteRoom() {
  const roomCode = roomCodeEl.value.trim();
  const token = tokenEl.value.trim();

  if (!roomCode) {
    appendSystemMessage("Room code is required.");
    return;
  }
  if (!token) {
    appendSystemMessage("JWT token is required (paste it without 'Bearer ').");
    return;
  }
  if (!canDeleteRoom) {
    appendSystemMessage("Only the room creator can delete this room.");
    return;
  }

  const ok = window.confirm(`Delete room '${roomCode}' for everyone? This cannot be undone.`);
  if (!ok) return;

  try {
    const resp = await fetch(`/api/rooms/${encodeURIComponent(roomCode)}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
    });

    if (!resp.ok) {
      const body = await resp.text().catch(() => "");
      throw new Error(`Delete failed: ${resp.status} ${resp.statusText}${body ? ` - ${body}` : ""}`);
    }

    appendSystemMessage("Room deleted.");
    disconnect();
    refreshRooms();
  } catch (e) {
    appendSystemMessage(e?.message || "Failed to delete room.");
  }
}

async function connect() {
  const roomCode = roomCodeEl.value.trim();
  const token = tokenEl.value.trim();

  if (!roomCode) {
    appendSystemMessage("Room code is required.");
    return;
  }
  if (!token) {
    appendSystemMessage("JWT token is required (paste it without 'Bearer ').");
    return;
  }

  if (socket) {
    disconnect();
  }

  localStorage.setItem("connecthub.roomCode", roomCode);
  localStorage.setItem("connecthub.token", token);

  setConnectedUi(true);
  setStatus("Connecting…");
  clearMessages();
  setDeleteRoomUi(false);

  try {
    const [room, me] = await Promise.all([loadRoom(roomCode, token), loadMe(token)]);
    const currentUserEmail = me?.email || getEmailFromJwt(token);
    const createdBy = room?.createdBy || room?.createdByEmail || room?.ownerEmail || room?.owner || "";
    const isCreator =
      currentUserEmail &&
      createdBy &&
      String(createdBy).toLowerCase() === String(currentUserEmail).toLowerCase();
    setDeleteRoomUi(isCreator);
  } catch (e) {
    setDeleteRoomUi(false);
    appendSystemMessage(e?.message || "Failed to load room details.");
  }

  try {
    const history = await loadHistory(roomCode, token);
    if (Array.isArray(history)) {
      for (const msg of history) appendChatMessage(msg);
      scrollToBottom(messagesEl);
    }
  } catch (e) {
    appendSystemMessage(e?.message || "Failed to load message history.");
  }

  const ws = new WebSocket(wsUrl(roomCode, token));
  socket = ws;

  ws.addEventListener("open", () => {
    if (socket !== ws) return;
    setStatus(`Connected · Room ${roomCode}`);
    appendSystemMessage("Connected.");
  });

  ws.addEventListener("message", (event) => {
    if (socket !== ws) return;
    try {
      const parsed = JSON.parse(event.data);
      if (parsed && parsed.error) {
        appendSystemMessage(parsed.error);
        return;
      }
      if (parsed && parsed.type === "ROOM_DELETED") {
        appendSystemMessage(parsed.message || "Room was deleted by the creator.");
        disconnect();
        return;
      }
      appendChatMessage(parsed);
    } catch {
      appendSystemMessage(String(event.data));
    }
  });

  ws.addEventListener("close", () => {
    if (socket !== ws) return;
    setStatus("Disconnected");
    appendSystemMessage("Disconnected.");
    socket = null;
    setConnectedUi(false);
  });

  ws.addEventListener("error", () => {
    if (socket !== ws) return;
    appendSystemMessage("WebSocket error.");
  });
}

function disconnect() {
  if (!socket) return;
  const ws = socket;
  socket = null;
  setStatus("Disconnected");
  setConnectedUi(false);
  setDeleteRoomUi(false);
  try {
    ws.close();
  } catch {
    // ignore
  }
  appendSystemMessage("Disconnected.");
}

function autosizeTextarea(el) {
  el.style.height = "auto";
  el.style.height = `${Math.min(el.scrollHeight, 140)}px`;
}

function sendMessage() {
  if (!socket || socket.readyState !== WebSocket.OPEN) {
    appendSystemMessage("Not connected.");
    return;
  }
  const content = messageInputEl.value.trim();
  if (!content) return;

  socket.send(JSON.stringify({ content }));
  messageInputEl.value = "";
  autosizeTextarea(messageInputEl);
  messageInputEl.focus();
}

connectBtn.addEventListener("click", () => connect());
disconnectBtn.addEventListener("click", () => disconnect());
leaveBtn?.addEventListener("click", () => leaveRoom());
deleteBtn.addEventListener("click", () => deleteRoom());
sendBtn.addEventListener("click", () => sendMessage());
refreshRoomsBtn?.addEventListener("click", () => refreshRooms());

messageInputEl.addEventListener("input", () => autosizeTextarea(messageInputEl));

tokenEl?.addEventListener("input", () => {
  const token = tokenEl.value.trim();
  if (token) localStorage.setItem("connecthub.token", token);
  scheduleRefreshRooms();
});

messageInputEl.addEventListener("keydown", (e) => {
  if (e.key === "Enter" && !e.shiftKey) {
    e.preventDefault();
    sendMessage();
  }
});

createRoomBtn?.addEventListener("click", async () => {
  const token = tokenEl?.value?.trim?.() || "";
  const name = roomNameEl?.value?.trim?.() || "";
  const description = roomDescEl?.value?.trim?.() || "";
  const roomCode = roomIdEl?.value?.trim?.() || "";

  setNotice(createRoomNoticeEl, "");

  if (!token) {
    setNotice(createRoomNoticeEl, "JWT token is required to create a room.", "error");
    return;
  }
  if (!name) {
    setNotice(createRoomNoticeEl, "Room name is required.", "error");
    return;
  }

  const payload = { name };
  if (description) payload.description = description;
  if (roomCode) payload.roomCode = roomCode;

  createRoomBtn.disabled = true;
  try {
    await createRoom(payload, token);
    if (roomNameEl) roomNameEl.value = "";
    if (roomDescEl) roomDescEl.value = "";
    if (roomIdEl) roomIdEl.value = "";
    setNotice(createRoomNoticeEl, "Room created. Select it from the list to start chatting.", "success");
    await refreshRooms();
  } catch (e) {
    setNotice(createRoomNoticeEl, e?.message || "Failed to create room.", "error");
  } finally {
    createRoomBtn.disabled = false;
  }
});

window.addEventListener("beforeunload", () => {
  if (socket) socket.close();
});

(() => {
  roomCodeEl.value = localStorage.getItem("connecthub.roomCode") || "";
  tokenEl.value = localStorage.getItem("connecthub.token") || "";
  autosizeTextarea(messageInputEl);
  setConnectedUi(false);
  setDeleteRoomUi(false);
  refreshRooms();
})();
