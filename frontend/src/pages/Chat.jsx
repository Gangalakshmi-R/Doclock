import { useEffect, useRef, useState } from "react";
import "../App.css";
import api from "../services/api";

const QUICK_QUESTIONS = [
  {
    icon: "✦",
    text: "What certification did I complete?",
  },
  {
    icon: "◫",
    text: "What is in my documents?",
  },
];

function Chat({
  selectedConversationId,
  onNewChat,
  onConversationCreated,
}) {
  const [messages, setMessages] = useState([]);
  const [question, setQuestion] = useState("");
  const [loading, setLoading] = useState(false);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [error, setError] = useState("");

  const scrollRef = useRef(null);
  const inputRef = useRef(null);

  useEffect(() => {
    let cancelled = false;

    const loadMessages = async () => {
      setError("");

      if (!selectedConversationId) {
        setMessages([]);
        setQuestion("");
        return;
      }

      try {
        setLoadingMessages(true);
        const response = await api.get(
          `/conversations/${selectedConversationId}/messages`
        );

        if (!cancelled) {
          setMessages(Array.isArray(response.data) ? response.data : []);
        }
      } catch (err) {
        console.error("Failed to load messages:", err);
        if (!cancelled) {
          setMessages([]);
          setError("Unable to load this conversation.");
        }
      } finally {
        if (!cancelled) {
          setLoadingMessages(false);
        }
      }
    };

    loadMessages();

    return () => {
      cancelled = true;
    };
  }, [selectedConversationId]);

  useEffect(() => {
    const element = scrollRef.current;
    if (!element) return;

    requestAnimationFrame(() => {
      element.scrollTo({
        top: element.scrollHeight,
        behavior: "smooth",
      });
    });
  }, [messages, loading]);

  useEffect(() => {
    if (!loadingMessages) {
      window.setTimeout(() => inputRef.current?.focus(), 80);
    }
  }, [selectedConversationId, loadingMessages]);

  const sendMessage = async () => {
    const trimmed = question.trim();

    if (!trimmed || loading) return;

    setError("");

    const optimisticMessage = {
      id: `temp-user-${Date.now()}`,
      role: "USER",
      content: trimmed,
      createdAt: new Date().toISOString(),
    };

    setMessages((current) => [...current, optimisticMessage]);
    setQuestion("");
    setLoading(true);

    try {
      const payload = { question: trimmed };

      if (selectedConversationId) {
        payload.conversationId = selectedConversationId;
      }

      const response = await api.post("/chat", payload);
      const data = response.data || {};

      setMessages((current) => [
        ...current,
        {
          id: `temp-assistant-${Date.now()}`,
          role: "ASSISTANT",
          content: data.answer || "I couldn't generate an answer.",
          createdAt: new Date().toISOString(),
          sources: Array.isArray(data.sources) ? data.sources : [],
          answerMode: data.answerMode,
        },
      ]);

      await onConversationCreated?.();
    } catch (err) {
      console.error("Chat error:", err);

      setMessages((current) =>
        current.filter((message) => message.id !== optimisticMessage.id)
      );

      setError(
        err?.response?.data?.message ||
          "Sorry, I couldn't process your question. Please try again."
      );
    } finally {
      setLoading(false);
      window.setTimeout(() => inputRef.current?.focus(), 80);
    }
  };

  const handleKeyDown = (event) => {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      sendMessage();
    }
  };

  const askQuickQuestion = (text) => {
    setQuestion(text);
    window.setTimeout(() => inputRef.current?.focus(), 50);
  };

  const formatRelevance = (value) => {
    if (value == null) return null;
    return `${(Number(value) * 100).toFixed(1)}%`;
  };

  const showWelcome = !selectedConversationId && messages.length === 0 && !loadingMessages;

  return (
    <section className="chat-page">
      <div ref={scrollRef} className="chat-scroll-area">
        {showWelcome ? (
          <div className="chat-empty-state">
            <div className="chat-empty-icon">🔐</div>
            <span className="chat-empty-label">YOUR PRIVATE AI ASSISTANT</span>
            <h2>
              Ask anything about
              <br />
              your documents.
            </h2>
            <p>
              DocLock searches your uploaded documents and generates answers
              using your private knowledge base.
            </p>

            <div className="quick-question-grid">
              {QUICK_QUESTIONS.map((item) => (
                <button
                  type="button"
                  className="quick-question"
                  key={item.text}
                  onClick={() => askQuickQuestion(item.text)}
                >
                  <span className="quick-icon">{item.icon}</span>
                  <span>{item.text}</span>
                  <span className="quick-arrow">→</span>
                </button>
              ))}
            </div>
          </div>
        ) : loadingMessages ? (
          <div className="chat-loading-state">
            <div className="loading-spinner" />
            <span>Loading conversation...</span>
          </div>
        ) : (
          <div className="messages-container">
            {messages.map((message) => (
              <div
                key={message.id}
                className={`message-row ${
                  message.role === "USER" ? "user-message" : "assistant-message"
                }`}
              >
                <div className="message-avatar">
                  {message.role === "USER" ? "You" : "🔐"}
                </div>

                <div className="message-content">
                  <div className="message-name">
                    {message.role === "USER" ? "You" : "DocLock AI"}
                  </div>

                  <div className="message-bubble">{message.content}</div>

                  {message.answerMode === "extractive-fallback" && (
                    <p className="answer-note">
                      Showing the most relevant document passage while AI generation reconnects.
                    </p>
                  )}

                  {message.sources?.length > 0 && (
                    <div className="message-sources">
                      <div className="sources-heading">
                        <span>Sources</span>
                        <span>{message.sources.length}</span>
                      </div>

                      <div className="source-list">
                        {message.sources.map((source, index) => (
                          <div
                            className="source-card"
                            key={
                              source.id ||
                              `${source.documentId}-${source.chunkNumber}-${index}`
                            }
                          >
                            <div className="source-file-icon">PDF</div>
                            <div className="source-details">
                              <strong>{source.documentName || `Document #${source.documentId}`}</strong>
                              <span>Chunk {source.chunkNumber}</span>
                            </div>
                            {source.relevance != null && (
                              <span className="source-score">
                                {formatRelevance(source.relevance)}
                              </span>
                            )}
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              </div>
            ))}

            {loading && (
              <div className="message-row assistant-message">
                <div className="message-avatar">🔐</div>
                <div className="message-content">
                  <div className="message-name">DocLock AI</div>
                  <div className="typing-bubble">
                    <span />
                    <span />
                    <span />
                  </div>
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {error && (
        <div className="chat-error" role="alert">
          <span>!</span>
          <p>{error}</p>
          <button type="button" onClick={() => setError("")} aria-label="Dismiss error">
            ×
          </button>
        </div>
      )}

      <div className="chat-composer-area">
        <div className="chat-composer">
          <textarea
            ref={inputRef}
            value={question}
            onChange={(event) => setQuestion(event.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Ask something about your documents..."
            rows={1}
            disabled={loading}
            aria-label="Ask your documents"
          />

          <button
            type="button"
            className="chat-send-button"
            onClick={sendMessage}
            disabled={loading || !question.trim()}
            title="Send message"
            aria-label="Send message"
          >
            {loading ? (
              <span className="send-spinner" />
            ) : (
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path
                  d="M12 19V5"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                />
                <path
                  d="M6 11l6-6 6 6"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
            )}
          </button>
        </div>

        <p className="composer-hint">
          Enter to send · Shift + Enter for a new line
        </p>
      </div>
    </section>
  );
}

export default Chat;
