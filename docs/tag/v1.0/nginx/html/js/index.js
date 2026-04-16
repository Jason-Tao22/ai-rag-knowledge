const chatArea = document.getElementById("chatArea");
const messageInput = document.getElementById("messageInput");
const submitBtn = document.getElementById("submitBtn");
const newChatBtn = document.getElementById("newChatBtn");
const chatList = document.getElementById("chatList");
const welcomeMessage = document.getElementById("welcomeMessage");
const toggleSidebarBtn = document.getElementById("toggleSidebar");
const sidebar = document.getElementById("sidebar");
const retrievalOnlyToggle = document.getElementById("retrievalOnlyToggle");

let currentChatId = null;

document.addEventListener("DOMContentLoaded", function () {
  loadRagOptions();
});

async function loadRagOptions() {
  const ragSelect = document.getElementById("ragSelect");

  try {
    const response = await fetch("/api/v1/rag/query_rag_tag_list");
    const data = await response.json();

    if (data.code === "0000" && Array.isArray(data.data)) {
      while (ragSelect.options.length > 1) {
        ragSelect.remove(1);
      }

      data.data.forEach((tag) => {
        const option = new Option(tag, tag);
        ragSelect.add(option);
      });
    }
  } catch (error) {
    console.error("Failed to fetch knowledge base list:", error);
  }
}

function createNewChat() {
  const chatId = Date.now().toString();
  currentChatId = chatId;
  localStorage.setItem("currentChatId", chatId);
  localStorage.setItem(
    `chat_${chatId}`,
    JSON.stringify({
      name: "New Chat",
      messages: [],
    })
  );
  updateChatList();
  clearChatArea();
}

function deleteChat(chatId) {
  if (confirm("Delete this chat?")) {
    localStorage.removeItem(`chat_${chatId}`);
    if (currentChatId === chatId) {
      createNewChat();
    }
    updateChatList();
  }
}

function updateChatList() {
  chatList.innerHTML = "";
  const chats = Object.keys(localStorage).filter((key) => key.startsWith("chat_"));

  const currentChatIndex = chats.findIndex((key) => key.split("_")[1] === currentChatId);
  if (currentChatIndex !== -1) {
    const currentChat = chats[currentChatIndex];
    chats.splice(currentChatIndex, 1);
    chats.unshift(currentChat);
  }

  chats.forEach((chatKey) => {
    let chatData = JSON.parse(localStorage.getItem(chatKey));
    const chatId = chatKey.split("_")[1];

    if (Array.isArray(chatData)) {
      chatData = {
        name: `Chat ${new Date(parseInt(chatId, 10)).toLocaleDateString()}`,
        messages: chatData,
      };
      localStorage.setItem(chatKey, JSON.stringify(chatData));
    }

    const li = document.createElement("li");
    li.className = `chat-item flex items-center justify-between p-2 hover:bg-gray-100 rounded-lg cursor-pointer transition-colors ${chatId === currentChatId ? "bg-blue-50" : ""}`;
    li.innerHTML = `
      <div class="flex-1">
        <div class="text-sm font-medium">${chatData.name}</div>
        <div class="text-xs text-gray-400">${new Date(parseInt(chatId, 10)).toLocaleDateString()}</div>
      </div>
      <div class="chat-actions flex items-center gap-1 opacity-0 transition-opacity duration-200">
        <button class="p-1 hover:bg-gray-200 rounded text-gray-500" onclick="renameChat('${chatId}')">Rename</button>
        <button class="p-1 hover:bg-red-200 rounded text-red-500" onclick="deleteChat('${chatId}')">Delete</button>
      </div>
    `;

    li.addEventListener("click", (e) => {
      if (!e.target.closest(".chat-actions")) {
        loadChat(chatId);
      }
    });
    li.addEventListener("mouseenter", () => {
      li.querySelector(".chat-actions").classList.remove("opacity-0");
    });
    li.addEventListener("mouseleave", () => {
      li.querySelector(".chat-actions").classList.add("opacity-0");
    });
    chatList.appendChild(li);
  });
}

function renameChat(chatId) {
  const chatKey = `chat_${chatId}`;
  const chatData = JSON.parse(localStorage.getItem(chatKey));
  const currentName = chatData.name || `Chat ${new Date(parseInt(chatId, 10)).toLocaleString()}`;
  const newName = prompt("Enter a new chat name", currentName);

  if (newName) {
    chatData.name = newName;
    localStorage.setItem(chatKey, JSON.stringify(chatData));
    updateChatList();
  }
}

function loadChat(chatId) {
  currentChatId = chatId;
  localStorage.setItem("currentChatId", chatId);
  clearChatArea();
  const chatData = JSON.parse(localStorage.getItem(`chat_${chatId}`) || '{"messages": []}');
  chatData.messages.forEach((msg) => {
    appendMessage(msg.content, msg.isAssistant, false, msg.citations || [], msg.meta || null);
  });
  updateChatList();
}

function clearChatArea() {
  chatArea.innerHTML = "";
  welcomeMessage.style.display = "flex";
}

function createMessageContainer(isAssistant = false) {
  const messageDiv = document.createElement("div");
  messageDiv.className = `max-w-4xl mx-auto mb-4 p-4 rounded-lg ${isAssistant ? "bg-gray-100" : "bg-white border"} markdown-body relative`;
  chatArea.appendChild(messageDiv);
  chatArea.scrollTop = chatArea.scrollHeight;
  return messageDiv;
}

function renderCitations(citations) {
  if (!citations || citations.length === 0) {
    return "";
  }

  const cards = citations
    .map((citation) => {
      const sourceMeta = [citation.sourceName, citation.filePath].filter(Boolean).join(" · ");
      const safeTitle = escapeHtml(citation.title || "Untitled Source");
      const safeSourceMeta = escapeHtml(sourceMeta || "Indexed source");
      const safePassage = escapeHtml(citation.passage || "");
      const safeLink = citation.sourceUrl ? encodeURI(citation.sourceUrl) : "";
      const sourceLink = citation.sourceUrl
        ? `<a class="citation-link" href="${safeLink}" target="_blank" rel="noopener noreferrer">Open source</a>`
        : "";

      return `
        <div class="citation-card">
          <div class="citation-head">
            <div class="flex gap-3">
              <span class="citation-rank">${citation.rank}</span>
              <div>
                <div class="citation-title">${safeTitle}</div>
                <div class="citation-source">${safeSourceMeta}</div>
              </div>
            </div>
            ${sourceLink}
          </div>
          <p class="citation-passage">${safePassage}</p>
        </div>
      `;
    })
    .join("");

  return `
    <section class="citations-panel">
      <div class="citations-title">Retrieved Evidence</div>
      ${cards}
    </section>
  `;
}

function renderAssistantMeta(meta) {
  if (!meta) {
    return "";
  }

  const parts = [];
  if (meta.model) {
    parts.push(`Model: ${meta.model}`);
  }
  if (typeof meta.retrievedCount === "number") {
    parts.push(`Passages: ${meta.retrievedCount}`);
  }
  if (typeof meta.latencyMs === "number") {
    parts.push(`Latency: ${meta.latencyMs} ms`);
  }

  if (parts.length === 0) {
    return "";
  }

  return `<div class="assistant-meta">${escapeHtml(parts.join(" · "))}</div>`;
}

function appendMessage(content, isAssistant = false, saveToStorage = true, citations = [], meta = null) {
  welcomeMessage.style.display = "none";
  const messageDiv = createMessageContainer(isAssistant);

  if (isAssistant) {
    const renderedContent = DOMPurify.sanitize(marked.parse(content));
    messageDiv.innerHTML = renderedContent + renderAssistantMeta(meta) + renderCitations(citations);
  } else {
    const renderedContent = DOMPurify.sanitize(marked.parse(content));
    messageDiv.innerHTML = renderedContent;
  }

  const copyBtn = document.createElement("button");
  copyBtn.className = "absolute top-2 right-2 p-1 bg-gray-200 rounded-md text-xs";
  copyBtn.textContent = "Copy";
  copyBtn.onclick = () => {
    navigator.clipboard.writeText(content).then(() => {
      copyBtn.textContent = "Copied";
      setTimeout(() => (copyBtn.textContent = "Copy"), 1500);
    });
  };
  messageDiv.appendChild(copyBtn);

  if (saveToStorage && currentChatId) {
    const chatData = JSON.parse(localStorage.getItem(`chat_${currentChatId}`) || '{"name": "New Chat", "messages": []}');
    chatData.messages.push({ content, isAssistant, citations, meta });
    localStorage.setItem(`chat_${currentChatId}`, JSON.stringify(chatData));
  }
}

function appendLoadingMessage(isRetrievalOnly = false) {
  welcomeMessage.style.display = "none";
  const messageDiv = createMessageContainer(true);
  messageDiv.innerHTML = `<p class="loading-message">${isRetrievalOnly ? "Retrieving supporting evidence..." : "Retrieving evidence and generating an answer..."}</p>`;
  return messageDiv;
}

async function submitRagQuery(message) {
  const ragTag = document.getElementById("ragSelect").value;
  const model = document.getElementById("aiModel").value;
  const isRetrievalOnly = retrievalOnlyToggle.checked;

  if (!ragTag) {
    alert("Please select a knowledge base first.");
    return;
  }

  const loadingMessage = appendLoadingMessage(isRetrievalOnly);

  try {
    const params = new URLSearchParams({
      message,
      ragTag,
      topK: isRetrievalOnly ? "5" : "1",
    });
    if (!isRetrievalOnly) {
      params.set("model", model);
    }

    const endpoint = isRetrievalOnly ? "/api/v1/rag/retrieve" : "/api/v1/rag/query";
    const response = await fetch(`${endpoint}?${params.toString()}`);
    const payload = await response.json();

    if (!response.ok || payload.code !== "0000") {
      throw new Error(payload.info || "The RAG request failed.");
    }

    const result = payload.data;
    loadingMessage.remove();
    appendMessage(result.answer, true, true, result.citations || [], result);
  } catch (error) {
    console.error("RAG query failed:", error);
    loadingMessage.remove();
    appendMessage(`Request failed: ${error.message}`, true, true, [], null);
  }
}

submitBtn.addEventListener("click", () => {
  const message = messageInput.value.trim();
  if (!message) {
    return;
  }

  if (!currentChatId) {
    createNewChat();
  }

  appendMessage(message, false);
  messageInput.value = "";
  submitRagQuery(message);
});

messageInput.addEventListener("keypress", (e) => {
  if (e.key === "Enter" && !e.shiftKey) {
    e.preventDefault();
    submitBtn.click();
  }
});

newChatBtn.addEventListener("click", createNewChat);

toggleSidebarBtn.addEventListener("click", () => {
  sidebar.classList.toggle("-translate-x-full");
  updateSidebarIcon();
});

function updateSidebarIcon() {
  const iconPath = document.getElementById("sidebarIconPath");
  if (sidebar.classList.contains("-translate-x-full")) {
    iconPath.setAttribute("d", "M4 6h16M4 12h4m12 0h-4M4 18h16");
  } else {
    iconPath.setAttribute("d", "M4 6h16M4 12h16M4 18h16");
  }
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

updateChatList();
const savedChatId = localStorage.getItem("currentChatId");
if (savedChatId) {
  loadChat(savedChatId);
}

window.addEventListener("resize", () => {
  if (window.innerWidth > 768) {
    sidebar.classList.remove("-translate-x-full");
  } else {
    sidebar.classList.add("-translate-x-full");
  }
});

if (window.innerWidth <= 768) {
  sidebar.classList.add("-translate-x-full");
}

updateSidebarIcon();

const uploadMenuButton = document.getElementById("uploadMenuButton");
const uploadMenu = document.getElementById("uploadMenu");

uploadMenuButton.addEventListener("click", (e) => {
  e.stopPropagation();
  uploadMenu.classList.toggle("hidden");
});

document.addEventListener("click", (e) => {
  if (!uploadMenu.contains(e.target) && e.target !== uploadMenuButton) {
    uploadMenu.classList.add("hidden");
  }
});

document.querySelectorAll("#uploadMenu a").forEach((item) => {
  item.addEventListener("click", () => {
    uploadMenu.classList.add("hidden");
  });
});
