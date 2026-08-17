import { useCallback, useEffect, useRef, useState } from "react";
import "./App.css";
import Chat from "./pages/Chat";
import Login from "./pages/Login";
import api from "./services/api";

function App() {
  // =========================================================
  // AUTHENTICATION
  // =========================================================

  const [isAuthenticated, setIsAuthenticated] = useState(() => {
    return Boolean(localStorage.getItem("doclock_token"));
  });

  // =========================================================
  // DOCUMENT STATE
  // =========================================================

  const [documents, setDocuments] = useState([]);
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [loadingDocuments, setLoadingDocuments] = useState(false);
  const [dragActive, setDragActive] = useState(false);
  const [currentPage, setCurrentPage] = useState("documents");

  // =========================================================
  // CHAT STATE
  // =========================================================

  const [conversations, setConversations] = useState([]);
  const [selectedConversationId, setSelectedConversationId] =
    useState(null);
  const [loadingConversations, setLoadingConversations] =
    useState(false);

  const fileInputRef = useRef(null);

  // =========================================================
  // LOGIN
  // =========================================================

  const handleLogin = () => {
    setIsAuthenticated(true);
    setCurrentPage("documents");
  };

  // =========================================================
  // FETCH DOCUMENTS
  // =========================================================

  const fetchDocuments = useCallback(async () => {
    if (!isAuthenticated) return;

    try {
      setLoadingDocuments(true);

      const response = await api.get("/documents");

      setDocuments(
        Array.isArray(response.data)
          ? response.data
          : []
      );
    } catch (error) {
      console.error("Failed to fetch documents:", error);
    } finally {
      setLoadingDocuments(false);
    }
  }, [isAuthenticated]);

  // =========================================================
  // FETCH CONVERSATIONS
  // =========================================================

  const fetchConversations = useCallback(async () => {
    if (!isAuthenticated) return [];

    try {
      setLoadingConversations(true);

      const response =
        await api.get("/conversations");

      const data = Array.isArray(response.data)
        ? [...response.data]
        : [];

      data.sort(
        (a, b) =>
          new Date(
            b.updatedAt || b.createdAt
          ) -
          new Date(
            a.updatedAt || a.createdAt
          )
      );

      setConversations(data);

      return data;
    } catch (error) {
      console.error(
        "Failed to fetch conversations:",
        error
      );

      return [];
    } finally {
      setLoadingConversations(false);
    }
  }, [isAuthenticated]);

  // =========================================================
  // LOAD DOCUMENTS AFTER LOGIN
  // =========================================================

  useEffect(() => {
    if (isAuthenticated) {
      fetchDocuments();
    }
  }, [isAuthenticated, fetchDocuments]);

  // =========================================================
  // LOAD CHAT HISTORY WHEN CHAT IS OPEN
  // =========================================================

  useEffect(() => {
    if (
      isAuthenticated &&
      currentPage === "chat"
    ) {
      fetchConversations();
    }
  }, [
    isAuthenticated,
    currentPage,
    fetchConversations
  ]);

  // =========================================================
  // CHAT NAVIGATION
  // =========================================================

  const openChat = (id) => {
    setCurrentPage("chat");
    setSelectedConversationId(id);
  };

  const startNewChat = () => {
    setCurrentPage("chat");
    setSelectedConversationId(null);
  };

  const handleConversationCreated = async () => {
    const updated =
      await fetchConversations();

    if (
      !selectedConversationId &&
      updated.length > 0
    ) {
      setSelectedConversationId(
        updated[0].id
      );
    }
  };

  // =========================================================
  // DELETE CONVERSATION
  // =========================================================

  const handleDeleteConversation = async (
    event,
    id
  ) => {
    event.stopPropagation();

    if (
      !window.confirm(
        "Delete this conversation?"
      )
    ) {
      return;
    }

    try {
      await api.delete(
        `/conversations/${id}`
      );

      setConversations((current) =>
        current.filter(
          (conversation) =>
            conversation.id !== id
        )
      );

      if (
        selectedConversationId === id
      ) {
        setSelectedConversationId(null);
      }
    } catch (error) {
      console.error(
        "Delete conversation error:",
        error
      );

      alert(
        "Failed to delete conversation."
      );
    }
  };

  // =========================================================
  // FILE SELECTION
  // =========================================================

  const selectFile = (file) => {
    if (!file) return;

    if (file.type !== "application/pdf") {
      alert("Only PDF files are allowed.");
      return;
    }

    setSelectedFile(file);
  };

  const handleFileChange = (event) => {
    selectFile(
      event.target.files?.[0]
    );
  };

  const handleDragOver = (event) => {
    event.preventDefault();
    setDragActive(true);
  };

  const handleDragLeave = (event) => {
    event.preventDefault();
    setDragActive(false);
  };

  const handleDrop = (event) => {
    event.preventDefault();
    setDragActive(false);

    selectFile(
      event.dataTransfer.files?.[0]
    );
  };

  // =========================================================
  // UPLOAD DOCUMENT
  // =========================================================

  const uploadDocument = async () => {
    if (!selectedFile) {
      alert("Please select a PDF first.");
      return;
    }

    const formData = new FormData();

    formData.append(
      "file",
      selectedFile
    );

    try {
      setUploading(true);

      await api.post(
        "/documents/upload",
        formData
      );

      setSelectedFile(null);

      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }

      await fetchDocuments();

      alert(
        "Document uploaded successfully!"
      );
    } catch (error) {
      console.error(
        "Upload error:",
        error
      );

      console.error(
        "Backend response:",
        error.response?.data
      );

      alert(
        "Upload failed. Check the browser console."
      );
    } finally {
      setUploading(false);
    }
  };

  // =========================================================
  // DELETE DOCUMENT
  // =========================================================

  const deleteDocument = async (id) => {
    if (
      !window.confirm(
        "Delete this document?"
      )
    ) {
      return;
    }

    try {
      await api.delete(
        `/documents/${id}`
      );

      setDocuments((current) =>
        current.filter(
          (document) =>
            document.id !== id
        )
      );
    } catch (error) {
      console.error(
        "Delete error:",
        error
      );

      alert(
        "Failed to delete document."
      );
    }
  };

  // =========================================================
  // FORMATTING
  // =========================================================

  const formatSize = (bytes) => {
    if (!bytes) return "0 KB";

    if (bytes < 1024) {
      return `${bytes} B`;
    }

    if (bytes < 1024 * 1024) {
      return `${(
        bytes / 1024
      ).toFixed(1)} KB`;
    }

    return `${(
      bytes /
      (1024 * 1024)
    ).toFixed(1)} MB`;
  };

  const formatDate = (date) => {
    if (!date) return "";

    return new Date(
      date
    ).toLocaleDateString(
      "en-IN",
      {
        day: "2-digit",
        month: "short",
        year: "numeric"
      }
    );
  };

  const formatConversationDate = (
    date
  ) => {
    if (!date) return "";

    return new Date(
      date
    ).toLocaleDateString(
      "en-IN",
      {
        day: "2-digit",
        month: "short"
      }
    );
  };

  // =========================================================
  // LOGOUT
  // =========================================================

  const handleLogout = () => {
    if (
      !window.confirm(
        "Are you sure you want to logout?"
      )
    ) {
      return;
    }

    localStorage.removeItem(
      "doclock_token"
    );

    localStorage.removeItem(
      "doclock_username"
    );

    localStorage.removeItem(
      "isAuthenticated"
    );

    setIsAuthenticated(false);
    setCurrentPage("documents");
    setSelectedConversationId(null);
    setConversations([]);
    setDocuments([]);
  };

  // =========================================================
  // SHOW LOGIN IF NOT AUTHENTICATED
  // =========================================================

  if (!isAuthenticated) {
    return (
      <Login
        onLogin={handleLogin}
      />
    );
  }

  // =========================================================
  // MAIN APPLICATION
  // =========================================================

  return (
    <div className="app">

      {/* ===================================================
          SIDEBAR
      =================================================== */}

      <aside className="sidebar">

        <div className="logo">

          <div className="logo-box">
            🔐
          </div>

          <div>
            <h2>DocLock</h2>
            <span>
              Personal AI Vault
            </span>
          </div>

        </div>

        <nav className="sidebar-nav">

          {/* DOCUMENTS */}

          <button
            type="button"
            className={`nav-button ${
              currentPage === "documents"
                ? "active"
                : ""
            }`}
            onClick={() =>
              setCurrentPage(
                "documents"
              )
            }
          >
            <span className="nav-icon">
              📁
            </span>

            <span>
              Documents
            </span>
          </button>

          {/* AI CHAT */}

          <button
            type="button"
            className={`nav-button ${
              currentPage === "chat"
                ? "active"
                : ""
            }`}
            onClick={() =>
              setCurrentPage("chat")
            }
          >
            <span className="nav-icon">
              🤖
            </span>

            <span>
              AI Chat
            </span>
          </button>

          {/* CHAT HISTORY */}

          {currentPage === "chat" && (
            <section
              className="conversation-sidebar"
              aria-label="Chat history"
            >

              <div className="conversation-header">

                <div>
                  <span className="conversation-label">
                    AI ASSISTANT
                  </span>

                  <strong>
                    Conversations
                  </strong>
                </div>

                <button
                  type="button"
                  className="new-conversation-button"
                  onClick={
                    startNewChat
                  }
                  title="New conversation"
                  aria-label="New conversation"
                >
                  +
                </button>

              </div>

              <div className="conversation-list">

                {loadingConversations ? (
                  <div className="conversation-empty">

                    <div className="mini-spinner" />

                    <span>
                      Loading chats...
                    </span>

                  </div>

                ) : conversations.length === 0 ? (

                  <div className="conversation-empty">

                    <span className="conversation-empty-icon">
                      ○
                    </span>

                    <p>
                      No conversations yet.
                    </p>

                    <small>
                      Start a chat to create one.
                    </small>

                  </div>

                ) : (

                  conversations.map(
                    (conversation) => (

                      <div
                        className={`conversation-item ${
                          selectedConversationId ===
                          conversation.id
                            ? "active"
                            : ""
                        }`}
                        key={
                          conversation.id
                        }
                      >

                        <button
                          type="button"
                          className="conversation-main"
                          onClick={() =>
                            openChat(
                              conversation.id
                            )
                          }
                        >

                          <span className="conversation-icon">
                            ◌
                          </span>

                          <span className="conversation-item-content">

                            <span className="conversation-title">
                              {conversation.title ||
                                "New conversation"}
                            </span>

                            <span className="conversation-date">
                              {formatConversationDate(
                                conversation.updatedAt ||
                                  conversation.createdAt
                              )}
                            </span>

                          </span>

                        </button>

                        <button
                          type="button"
                          className="conversation-delete"
                          onClick={(event) =>
                            handleDeleteConversation(
                              event,
                              conversation.id
                            )
                          }
                          title="Delete conversation"
                          aria-label={`Delete ${
                            conversation.title ||
                            "conversation"
                          }`}
                        >
                          ×
                        </button>

                      </div>

                    )
                  )

                )}

              </div>

            </section>
          )}

        </nav>

        <div className="privacy-box">

          <div className="privacy-title">
            🔒 Private Vault
          </div>

          <p>
            Your documents are stored
            privately on your system.
          </p>

        </div>

      </aside>

      {/* ===================================================
          MAIN
      =================================================== */}

      <main className="main">

        <header className="header">

          <div>

            <p className="header-label">
              {currentPage === "chat"
                ? "DOCLOCK AI"
                : "PERSONAL VAULT"}
            </p>

            <h1>
              {currentPage === "chat"
                ? "Ask your documents"
                : "My Documents"}
            </h1>

          </div>

          <div className="header-actions">

            {currentPage ===
            "documents" ? (

              <button
                type="button"
                className="header-action-button"
                onClick={
                  fetchDocuments
                }
                disabled={
                  loadingDocuments
                }
              >

                <span className="action-icon">
                  ↻
                </span>

                {loadingDocuments
                  ? "Refreshing..."
                  : "Refresh"}

              </button>

            ) : (

              <button
                type="button"
                className="header-action-button"
                onClick={
                  startNewChat
                }
              >

                <span className="action-icon">
                  +
                </span>

                New Chat

              </button>

            )}

            <button
              type="button"
              className="logout-navbar-button"
              onClick={
                handleLogout
              }
            >

              <span>
                ↪
              </span>

              Logout

            </button>

          </div>

        </header>

        {/* =================================================
            DOCUMENT PAGE
        ================================================= */}

        {currentPage ===
          "documents" && (

          <section className="content">

            <section className="hero">

              <div className="hero-content">

                <p className="hero-label">
                  YOUR PRIVATE KNOWLEDGE BASE
                </p>

                <h2>
                  Store your knowledge.
                  <br />
                  <span>
                    Ask your documents
                    anything.
                  </span>
                </h2>

                <p className="hero-description">
                  Upload your documents
                  and DocLock will prepare
                  them for AI-powered
                  search and conversations.
                </p>

              </div>

              <div className="hero-symbol">
                🔐
              </div>

            </section>

            {/* UPLOAD */}

            <section className="upload-section">

              <div className="section-title">

                <h3>
                  Upload Document
                </h3>

                <p>
                  Add a PDF to your
                  personal knowledge base.
                </p>

              </div>

              <div
                className={`upload-area ${
                  dragActive
                    ? "drag-active"
                    : ""
                }`}
                onDragOver={
                  handleDragOver
                }
                onDragLeave={
                  handleDragLeave
                }
                onDrop={
                  handleDrop
                }
              >

                {!selectedFile ? (

                  <>

                    <div className="upload-icon">
                      ↑
                    </div>

                    <h4>
                      Drag &amp; drop
                      your PDF here
                    </h4>

                    <p>
                      or choose a file
                      from your computer
                    </p>

                    <input
                      ref={fileInputRef}
                      type="file"
                      accept="application/pdf"
                      onChange={
                        handleFileChange
                      }
                      hidden
                    />

                    <button
                      type="button"
                      className="browse-button"
                      onClick={() =>
                        fileInputRef.current?.click()
                      }
                    >
                      + Browse Files
                    </button>

                    <span className="file-hint">
                      PDF files only
                    </span>

                  </>

                ) : (

                  <div className="selected-file">

                    <div className="file-icon">
                      PDF
                    </div>

                    <div className="file-details">

                      <strong>
                        {selectedFile.name}
                      </strong>

                      <span>
                        {formatSize(
                          selectedFile.size
                        )}
                      </span>

                    </div>

                    <button
                      type="button"
                      className="remove-button"
                      onClick={() =>
                        setSelectedFile(
                          null
                        )
                      }
                      title="Remove selected file"
                    >
                      ×
                    </button>

                    <button
                      type="button"
                      className="upload-button"
                      onClick={
                        uploadDocument
                      }
                      disabled={
                        uploading
                      }
                    >
                      {uploading
                        ? "Processing..."
                        : "Upload & Process"}
                    </button>

                  </div>

                )}

              </div>

            </section>

            {/* DOCUMENTS */}

            <section className="documents-section">

              <div className="documents-heading">

                <div>

                  <h3>
                    Your Documents
                  </h3>

                  <p>
                    {documents.length}{" "}
                    {documents.length === 1
                      ? "document"
                      : "documents"}{" "}
                    in your vault
                  </p>

                </div>

                <div className="count">
                  {documents.length}
                </div>

              </div>

              {loadingDocuments ? (

                <div className="empty">

                  <div className="loader" />

                  <p>
                    Loading documents...
                  </p>

                </div>

              ) : documents.length ===
                0 ? (

                <div className="empty">

                  <div className="empty-icon">
                    📁
                  </div>

                  <h4>
                    Your vault is empty
                  </h4>

                  <p>
                    Upload your first PDF
                    to start building your
                    knowledge base.
                  </p>

                </div>

              ) : (

                <div className="document-grid">

                  {documents.map(
                    (document) => (

                      <article
                        className="document-card"
                        key={
                          document.id
                        }
                      >

                        <div className="card-top">

                          <div className="pdf-badge">
                            PDF
                          </div>

                          <button
                            type="button"
                            className="delete-button"
                            onClick={() =>
                              deleteDocument(
                                document.id
                              )
                            }
                            title="Delete document"
                            aria-label={`Delete ${document.fileName}`}
                          >
                            🗑
                          </button>

                        </div>

                        <h4
                          className="document-name"
                          title={
                            document.fileName
                          }
                        >
                          {
                            document.fileName
                          }
                        </h4>

                        <p className="document-meta">
                          {formatSize(
                            document.fileSize
                          )}{" "}
                          •{" "}
                          {formatDate(
                            document.uploadedAt
                          )}
                        </p>

                        <div className="status">

                          <span
                            className={`status-dot ${
                              document.status?.toLowerCase() ||
                              ""
                            }`}
                          />

                          <span>
                            {document.status ===
                            "PROCESSED"
                              ? "Ready for AI"
                              : document.status ||
                                "Processing"}
                          </span>

                        </div>

                      </article>

                    )
                  )}

                </div>

              )}

            </section>

          </section>

        )}

        {/* =================================================
            CHAT PAGE
        ================================================= */}

        {currentPage === "chat" && (

          <Chat
            selectedConversationId={
              selectedConversationId
            }
            onNewChat={
              startNewChat
            }
            onConversationCreated={
              handleConversationCreated
            }
          />

        )}

      </main>

    </div>
  );
}

export default App;