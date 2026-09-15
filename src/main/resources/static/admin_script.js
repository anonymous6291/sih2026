/* =====================================================
   CONFIGURATION
   ===================================================== */

// REST API used to retrieve chat history.
const HISTORY_API = "/history/chats";

const subscribe_url = "/user/event";

const query_url = "/app/admin/query";

const profile_url = "/profile";

let notice_url = "/topic/notice/";

let profile_json = null;

// Change this if your WebSocket endpoint is different.
const WEBSOCKET_URL =
    (location.protocol === "https:" ? "wss://" : "ws://") +
    location.host +
    "/ws";


let csrf = null;

async function getCsrfToken() {
    const response = await fetch("/csrf");
    csrf = await response.json();
    console.log("CSRF Loaded:", csrf);
    return csrf;
}


const fileButton = document.getElementById("fileButton");
const fileInput = document.getElementById("fileInput");
const fileList = document.getElementById("fileList");

let uploadDocIds = [];

fileButton.addEventListener("click", () => {
    fileInput.click();
});
fileInput.addEventListener("change", async () => {
    const files = Array.from(fileInput.files);
    if (!files) return; // Add file to the list
    const listItem = document.createElement("li");
    fileList.appendChild(listItem); // Send file to //upload
    const formData = new FormData();
    files.forEach(file => {
        formData.append("files", file);
    });
    try {
        const response = await fetch("/upload",
            {
                method: "POST", headers: {
                    "X-CSRF-TOKEN": csrf.token
                }, body: formData
            });
        if (!response.ok) {
            throw new Error(`Upload failed: ${response.status}`);
        }
        const reply = await response.json();
        console.log(reply)
        uploadDocIds = reply.documents;
        console.log("Upload successful:", uploadDocIds);
    } catch (error) {
        console.error("Upload error:", error);
    } // Allow selecting the same file again fileInput.value = ""; });
});

function generateUUID() {
    if (crypto.randomUUID) {
        return crypto.randomUUID();
    }

    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(
        /[xy]/g,
        function (c) {
            const r = Math.random() * 16 | 0;
            const v = c === 'x'
                ? r
                : (r & 0x3 | 0x8);

            return v.toString(16);
        }
    );
}

/* =====================================================
   STATE
   ===================================================== */

// Currently opened chat.
//
// null means the user is currently in a brand-new chat
// that has not received a chatId from the server yet.
let activeChatId = null;


// WebSocket instance.
let socket = null;


// Prevent multiple WebSocket sends while connecting.
let socketReady = false;


// Used when the server sends events belonging to a
// message that is currently being generated.
const pendingMessages = new Map();


/*
 * Example:
 *
 * pendingMessages = {
 *
 *   "frontend-message-id": {
 *       chatId: "abc123",
 *       element: HTMLElement
 *   }
 *
 * }
 */


/* =====================================================
   DOM ELEMENTS
   ===================================================== */

const chatHistory = document.getElementById("chatHistory");

const messagesContainer =
    document.getElementById("messages");

const welcome =
    document.getElementById("welcome");

const messageInput =
    document.getElementById("messageInput");

const sendButton =
    document.getElementById("sendButton");

const newChatButton =
    document.getElementById("newChatButton");

const currentChatTitle =
    document.getElementById("currentChatTitle");

const connectionDot =
    document.getElementById("connectionDot");

const connectionText =
    document.getElementById("connectionText");

const sidebar =
    document.getElementById("sidebar");

const menuButton =
    document.getElementById("menuButton");

const closeSidebar =
    document.getElementById("closeSidebar");


/* =====================================================
   INITIALIZATION
   ===================================================== */

document.addEventListener("DOMContentLoaded", () => {

    loadProfile().then(() => {

        notice_url = "/topic/notice/" + profile_json.role;

        loadChatHistory();

        initWebSocket();

        setupEventListeners();

    });

});

async function loadProfile() {
    try {

        const response =
            await fetch(profile_url);

        if (!response.ok) {
            throw new Error(
                `HTTP ${response.status}`
            );
        }

        profile_json =
            await response.json();

        console.log(profile_json);

    } catch (e) {
        console.log(e);
    }
}


/* =====================================================
   EVENT LISTENERS
   ===================================================== */

function setupEventListeners() {

    /*
     * Send message when clicking send.
     */
    sendButton.addEventListener("click", sendMessage);


    /*
     * Enter sends message.
     *
     * Shift + Enter creates a new line.
     */
    messageInput.addEventListener("keydown", event => {

        if (
            event.key === "Enter" &&
            !event.shiftKey
        ) {

            event.preventDefault();

            sendMessage();
        }

    });


    /*
     * Automatically increase textarea height.
     */
    messageInput.addEventListener("input", autoResizeTextarea);


    /*
     * New chat.
     */
    newChatButton.addEventListener(
        "click",
        createNewChat
    );


    /*
     * Mobile sidebar.
     */
    menuButton.addEventListener("click", () => {
        sidebar.classList.add("open");
    });


    closeSidebar.addEventListener("click", () => {
        sidebar.classList.remove("open");
    });

}


/* =====================================================
   LOAD CHAT HISTORY
   ===================================================== */

async function loadChatHistory() {

    try {

        const response =
            await fetch(HISTORY_API);

        if (!response.ok) {
            throw new Error(
                `HTTP ${response.status}`
            );
        }

        const chats =
            await response.json();

        console.log(chats);

        renderChatHistory(chats);

    } catch (error) {

        console.error(
            "Failed to load chat history:",
            error
        );

        chatHistory.innerHTML = `
            <div style="
                padding: 15px;
                color: #999;
                font-size: 13px;
            ">
                Failed to load chats.
            </div>
        `;
    }
}


/* =====================================================
   RENDER CHAT HISTORY
   ===================================================== */

function renderChatHistory(chats) {

    chatHistory.innerHTML = "";

    chats = chats.chats;

    if (!Array.isArray(chats) || chats.length === 0) {

        chatHistory.innerHTML = `
            <div style="
                padding: 15px;
                color: #999;
                font-size: 13px;
            ">
                No chats yet.
            </div>
        `;

        return;
    }


    chats.forEach(chat => {

        const item =
            document.createElement("div");

        item.className = "chat-item";

        /*
         * We store chatId directly on the element.
         */
        item.dataset.chatId = chat.chat_id;

        /*
         * Description is what the user sees.
         */
        item.textContent =
            chat.description || "Untitled chat";


        item.addEventListener(
            "click",
            () => openChat(chat.chat_id, chat.description)
        );


        chatHistory.appendChild(item);

    });
}


/* =====================================================
   OPEN EXISTING CHAT
   ===================================================== */

function openChat(chatId, description) {

    /*
     * This is extremely important.
     *
     * Once this value changes, incoming WebSocket
     * events will only be accepted if their chatId
     * matches this value.
     */
    activeChatId = chatId;


    currentChatTitle.textContent =
        description || "Chat";


    /*
     * Highlight selected chat.
     */
    document
        .querySelectorAll(".chat-item")
        .forEach(item => {

            item.classList.toggle(
                "active",
                item.dataset.chatId === chatId
            );

        });


    /*
     * Clear currently displayed messages.
     *
     * In a real application you may instead fetch:
     *
     * GET /history/chats/{chatId}/messages
     *
     * and render the previous messages.
     */
    clearMessages();

    /*
     * Close mobile sidebar.
     */
    sidebar.classList.remove("open");

}


/* =====================================================
   CREATE NEW CHAT
   ===================================================== */

function createNewChat() {

    /*
     * Empty/null chatId tells the backend:
     *
     * "This is a new conversation."
     */
    activeChatId = null;


    currentChatTitle.textContent =
        "New chat";


    /*
     * Remove active history selection.
     */
    document
        .querySelectorAll(".chat-item")
        .forEach(item => {
            item.classList.remove("active");
        });


    clearMessages();


    sidebar.classList.remove("open");


    messageInput.focus();
}


/* =====================================================
   CLEAR MESSAGES
   ===================================================== */

function clearMessages() {

    messagesContainer.innerHTML = "";

    /*
     * Recreate welcome screen.
     */
    const welcomeElement =
        document.createElement("div");

    welcomeElement.className = "welcome";

    welcomeElement.innerHTML = `
        <div class="welcome-icon">
            AI
        </div>

        <h1>How can I help you?</h1>

        <p>
            Ask anything or start a new conversation.
        </p>
    `;

    messagesContainer.appendChild(
        welcomeElement
    );
}


/* =====================================================
   WEBSOCKET
   ===================================================== */


async function initWebSocket() {
    csrf = await getCsrfToken();
    // 2. Initialize the STOMP client with the resolved token data
    socket = new StompJs.Client({
        brokerURL: WEBSOCKET_URL,
        reconnectDelay: 5000,

        // Dynamically compute the header object using the resolved values
        connectHeaders: {
            [csrf.headerName]: csrf.token
        },

        onConnect: function () {

            console.log(
                "WebSocket connected"
            );

            socketReady = true;

            connectionDot.className =
                "connected";

            connectionText.textContent =
                "Connected";

            socket.subscribe(notice_url, message => {

                const event = JSON.parse(message.body);

                console.log("Notice:");
                console.log(event);

            });

            socket.subscribe(subscribe_url, message => {

                const event = JSON.parse(message.body);

                handleWebSocketEvent(event);

            });
        },

        onOpen: function () {

            console.log(
                "WebSocket connected"
            );

            socketReady = true;

            connectionDot.className =
                "connected";

            connectionText.textContent =
                "Connected";

        },

        onDisconnect: function () {

            console.log(
                "WebSocket disconnected"
            );

            socketReady = false;

            connectionDot.className =
                "disconnected";

            connectionText.textContent =
                "Disconnected";
        },

        onStompError: function (frame) {
            console.error("STOMP error:", frame);
            console.log('Broker reported error: ' + frame.headers['message']);
            console.log('Additional details: ' + frame.body);

            console.log(
                "WebSocket disconnected"
            );

            socketReady = false;

            connectionDot.className =
                "disconnected";

            connectionText.textContent =
                "Disconnected";
        },

        onmessage: function (event) {

            handleWebSocketEvent(
                event.data
            );

        },
        onerror: function (error) {

            console.error(
                "WebSocket error:",
                error
            );

        },

        onclose: function () {

            console.log(
                "WebSocket disconnected"
            );

            socketReady = false;

            connectionDot.className =
                "disconnected";

            connectionText.textContent =
                "Disconnected";


            /*
             * Automatically reconnect.
             */
            setTimeout(
                initWebSocket,
                3000
            );

        }
    });

    // 3. Start the connection
    socket.activate();
}

/* =====================================================
   SEND MESSAGE
   ===================================================== */

function sendMessage() {

    const message =
        messageInput.value.trim();


    /*
     * Don't send empty messages.
     */
    if (!message) {
        return;
    }


    /*
     * WebSocket must be connected.
     */


    if (!socket.connected) {
        alert("WebSocket is not connected");
        return;
    }

    if (!socket.connected) {
        alert("WebSocket is not connected");
        return;
    }


    /*
     * Generate a unique messageId on the frontend.
     *
     * crypto.randomUUID() produces something like:
     *
     * 550e8400-e29b-41d4-a716-446655440000
     */
    const messageId = generateUUID();


    /*
     * IMPORTANT:
     *
     * Capture the current chatId before sending.
     *
     * For a new chat this will be null.
     */
    const messageChatId =
        activeChatId || "";


    /*
     * Display user's message immediately.
     */
    addUserMessage(message);


    /*
     * Clear input.
     */
    messageInput.value = "";

    autoResizeTextarea();


    /*
     * Create an AI message placeholder.
     *
     * Server events will append content to this
     * element.
     */
    const aiMessage =
        createAIMessage();


    /*
     * Store information about this request.
     */
    pendingMessages.set(
        messageId,
        {
            chatId: messageChatId,
            element: aiMessage
        }
    );


    /*
     * Payload sent to your backend.
     */
    const payload = {

        query: message,

        chat_id: messageChatId,

        message_id: messageId,

        model_id: "1",

        documents: uploadDocIds,

        tools: [],
    };

    uploadDocIds = [];


    console.log(
        "Sending:",
        payload
    );

    // Send to Spring Boot @MessageMapping
    socket.publish({
        destination: query_url,
        body: JSON.stringify(payload)
    });

}


/* =====================================================
   HANDLE WEBSOCKET EVENT
   ===================================================== */

function handleWebSocketEvent(event) {

    console.log(
        "Received event:",
        event
    );


    /*
     * -------------------------------------------------
     * MOST IMPORTANT PART
     * -------------------------------------------------
     *
     * Every event contains chatId.
     *
     * We only process it if that chat is currently
     * opened.
     */

    if (activeChatId === null) {

        activeChatId = event.chat_id;

    } else if (event.chat_id !== activeChatId) {

        /*
         * Event belongs to another chat.
         *
         * Ignore it.
         */
        console.log(
            "Ignoring event from chat:",
            event.chat_id
        );

        return;
    }


    /*
     * Find the message/request this event belongs to.
     */
    const pending =
        pendingMessages.get(
            event.message_id
        );


    /*
     * If we don't know this messageId,
     * we can still decide what to do based on event.
     */
    if (!pending) {

        console.log(
            "No pending message:",
            event.message_id
        );

        return;
    }


    /*
     * If the server assigned a chatId to a
     * brand-new conversation, update the frontend.
     *
     * However, this requires the backend event to
     * contain the new chatId.
     *
     * See the special case below.
     */


    /*
     * Handle different event types.
     */

    if (event.error) {
        appendAIText(
            pending.element,
            "\n\nError: " +
            (event.message || "Unknown error")
        );

        pendingMessages.delete(
            event.message_id
        );
    } else if (event.is_event) {
        appendAIText(
            pending.element,
            (event.message || "") + "\n\n"
        );
    } else {
        appendAIText(
            pending.element,
            event.message || ""
        );

        finishAIMessage(
            pending.element
        );

        pendingMessages.delete(
            event.message_id
        );
    }

    scrollToBottom();

}


/* =====================================================
   USER MESSAGE
   ===================================================== */

function addUserMessage(message) {

    removeWelcome();


    const row =
        document.createElement("div");

    row.className =
        "message-row user-message";


    row.innerHTML = `

        <div class="message-avatar user-avatar">
            You
        </div>

        <div class="message-content"></div>

    `;


    const content =
        row.querySelector(
            ".message-content"
        );


    /*
     * textContent prevents HTML injection.
     */
    content.textContent =
        message;


    messagesContainer.appendChild(
        row
    );


    scrollToBottom();
}


/* =====================================================
   AI MESSAGE
   ===================================================== */

function createAIMessage() {

    removeWelcome();


    const row =
        document.createElement("div");

    row.className =
        "message-row ai-message";


    row.innerHTML = `

        <div class="message-avatar ai-avatar">
            AI
        </div>

        <div class="message-content"></div>

    `;


    messagesContainer.appendChild(
        row
    );


    scrollToBottom();


    return row.querySelector(
        ".message-content"
    );
}


/* =====================================================
   APPEND AI TEXT
   ===================================================== */

function appendAIText(element, text) {

    /*
     * textContent would replace existing content,
     * so we append using a text node.
     *
     * This also prevents HTML injection.
     */
    element.appendChild(
        document.createTextNode(text)
    );


    scrollToBottom();
}


/* =====================================================
   FINISH AI MESSAGE
   ===================================================== */

function finishAIMessage(element) {

    element.classList.add(
        "completed"
    );
}


/* =====================================================
   REMOVE WELCOME
   ===================================================== */

function removeWelcome() {

    const welcomeElement =
        messagesContainer.querySelector(
            ".welcome"
        );

    if (welcomeElement) {

        welcomeElement.remove();

    }
}


/* =====================================================
   SCROLL
   ===================================================== */

function scrollToBottom() {

    requestAnimationFrame(() => {

        messagesContainer.scrollTop =
            messagesContainer.scrollHeight;

    });

}


/* =====================================================
   TEXTAREA RESIZE
   ===================================================== */

function autoResizeTextarea() {

    messageInput.style.height =
        "auto";

    messageInput.style.height =
        Math.min(
            messageInput.scrollHeight,
            180
        ) + "px";
}