import { FormEvent, KeyboardEvent, useCallback, useEffect, useMemo, useRef, useState } from "react";

type DocumentSummary = {
  documentId: string;
  filename: string;
  uploadedBy: string;
  contentType: string;
  chunkCount: number;
  createdAt: string;
};

type Citation = {
  documentId: string;
  documentName: string;
  chunkIndex: number;
  pageStart: number;
  pageEnd: number;
  paragraphStart: number;
  paragraphEnd: number;
  excerpt: string;
  score: number;
};

type AskResponse = {
  queryId: string;
  answer: string;
  citations: Citation[];
};

type RefreshIndexResponse = {
  indexedCount: number;
  updatedCount: number;
  removedCount: number;
  repositoryPath: string;
};

type Message = {
  id: string;
  role: "user" | "assistant";
  text: string;
  citations?: Citation[];
  pending?: boolean;
};

type Toast = {
  id: string;
  message: string;
  kind: "success" | "error" | "info";
};

const startingMessages: Message[] = [
  {
    id: "welcome",
    role: "assistant",
    text: "Upload a few documents or place them in data/documents, then ask a question here. Answers include paper-style citations."
  }
];

export default function App() {
  const [documents, setDocuments] = useState<DocumentSummary[]>([]);
  const [documentsLoading, setDocumentsLoading] = useState(true);
  const [messages, setMessages] = useState<Message[]>(startingMessages);
  const [question, setQuestion] = useState("");
  const [userId, setUserId] = useState("analyst-1");
  const [maxResults, setMaxResults] = useState(5);
  const [uploadedBy, setUploadedBy] = useState("system");
  const [uploading, setUploading] = useState(false);
  const [asking, setAsking] = useState(false);
  const [refreshingIndex, setRefreshingIndex] = useState(false);
  const [knowledgeStatus, setKnowledgeStatus] = useState("Loading indexed documents...");
  const [isDragging, setIsDragging] = useState(false);
  const [toasts, setToasts] = useState<Toast[]>([]);

  const fileInputRef = useRef<HTMLInputElement>(null);
  const messageListRef = useRef<HTMLDivElement>(null);

  const indexedCountLabel = useMemo(() => {
    if (documentsLoading) return "Loading…";
    if (!documents.length) return "No documents indexed yet";
    return `${documents.length} document${documents.length === 1 ? "" : "s"} indexed`;
  }, [documents, documentsLoading]);

  // Auto-scroll message list to bottom when new messages arrive
  useEffect(() => {
    const el = messageListRef.current;
    if (el) {
      el.scrollTop = el.scrollHeight;
    }
  }, [messages]);

  useEffect(() => {
    void refreshDocuments();
  }, []);

  function addToast(message: string, kind: Toast["kind"] = "info") {
    const id = crypto.randomUUID();
    setToasts((prev) => [...prev, { id, message, kind }]);
    setTimeout(() => setToasts((prev) => prev.filter((t) => t.id !== id)), 4500);
  }

  function dismissToast(id: string) {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }

  async function refreshDocuments() {
    setDocumentsLoading(true);
    try {
      const response = await fetch("/api/documents");
      if (!response.ok) throw new Error(await readErrorMessage(response));
      const payload = (await response.json()) as DocumentSummary[];
      setDocuments(payload);
      setKnowledgeStatus(
        payload.length ? "Knowledge base is ready." : "Upload documents or add them to data/documents."
      );
    } catch (error) {
      const message = error instanceof Error ? error.message : "Failed to load the knowledge base.";
      setKnowledgeStatus(message);
      addToast(message, "error");
    } finally {
      setDocumentsLoading(false);
    }
  }

  async function refreshRepositoryIndex() {
    setRefreshingIndex(true);
    setKnowledgeStatus("Refreshing repository documents into the vector index...");
    try {
      const response = await fetch("/api/documents/refresh", { method: "POST" });
      if (!response.ok) throw new Error(await readErrorMessage(response));
      const payload = (await response.json()) as RefreshIndexResponse;
      const msg = `Refresh complete: ${payload.indexedCount} indexed, ${payload.updatedCount} updated, ${payload.removedCount} removed.`;
      setKnowledgeStatus(msg);
      addToast(msg, "success");
      await refreshDocuments();
    } catch (error) {
      const message = error instanceof Error ? error.message : "Repository refresh failed.";
      setKnowledgeStatus(message);
      addToast(message, "error");
    } finally {
      setRefreshingIndex(false);
    }
  }

  async function handleUpload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const fileInput = event.currentTarget.elements.namedItem("file") as HTMLInputElement | null;
    if (!fileInput?.files?.length) {
      addToast("Choose a file to add to the knowledge base.", "info");
      return;
    }

    const file = fileInput.files[0];
    const formData = new FormData();
    formData.append("file", file);

    setUploading(true);
    setKnowledgeStatus(`Indexing ${file.name}…`);

    try {
      const response = await fetch(
        `/api/documents/upload?uploadedBy=${encodeURIComponent(uploadedBy || "system")}`,
        { method: "POST", body: formData }
      );
      if (!response.ok) throw new Error(await readErrorMessage(response));
      const payload = await response.json();
      const msg = `Indexed ${payload.filename} with ${payload.chunksIndexed} chunks.`;
      setKnowledgeStatus(msg);
      addToast(msg, "success");
      event.currentTarget.reset();
      await refreshDocuments();
    } catch (error) {
      const message = error instanceof Error ? error.message : "Upload failed.";
      setKnowledgeStatus(message);
      addToast(message, "error");
    } finally {
      setUploading(false);
    }
  }

  // Drag-and-drop handlers
  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  }, []);

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  }, []);

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      setIsDragging(false);
      const files = e.dataTransfer.files;
      if (files.length && fileInputRef.current) {
        const dt = new DataTransfer();
        dt.items.add(files[0]);
        fileInputRef.current.files = dt.files;
        addToast(`${files[0].name} ready to index. Click "Add to knowledge base".`, "info");
      }
    },
    []
  );

  async function handleAsk(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await submitQuestion();
  }

  function handleTextareaKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
    if ((e.ctrlKey || e.metaKey) && e.key === "Enter") {
      e.preventDefault();
      void submitQuestion();
    }
  }

  async function submitQuestion() {
    const trimmedQuestion = question.trim();
    if (!trimmedQuestion || asking) return;

    const userMessage: Message = { id: crypto.randomUUID(), role: "user", text: trimmedQuestion };
    const pendingMessage: Message = {
      id: "pending",
      role: "assistant",
      text: "Searching the knowledge base…",
      pending: true
    };

    setMessages((prev) => [...prev, userMessage, pendingMessage]);
    setQuestion("");
    setAsking(true);

    try {
      const response = await fetch("/api/qa/ask", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ userId, question: trimmedQuestion, maxResults })
      });
      if (!response.ok) throw new Error(await readErrorMessage(response));
      const payload = (await response.json()) as AskResponse;
      setMessages((prev) => [
        ...prev.filter((m) => !m.pending),
        { id: payload.queryId, role: "assistant", text: payload.answer, citations: payload.citations }
      ]);
    } catch (error) {
      const text = error instanceof Error ? error.message : "The question request failed.";
      setMessages((prev) => [
        ...prev.filter((m) => !m.pending),
        { id: crypto.randomUUID(), role: "assistant", text }
      ]);
    } finally {
      setAsking(false);
    }
  }

  return (
    <>
      {/* Toast stack */}
      <div className="toast-stack" aria-live="polite">
        {toasts.map((toast) => (
          <div key={toast.id} className={`toast toast--${toast.kind}`}>
            <span>{toast.message}</span>
            <button className="toast-dismiss" onClick={() => dismissToast(toast.id)} aria-label="Dismiss">
              ×
            </button>
          </div>
        ))}
      </div>

      <div className="layout-shell">
        <aside className="knowledge-panel">
          <div className="brand-block">
            <p className="eyebrow">DocIntel AI</p>
            <h1>Enterprise document intelligence with a chat-first workspace.</h1>
            <p className="supporting-copy">
              Add documents to the knowledge base, place them in the repo folder, and ask questions in a clean QA space.
            </p>
          </div>

          <section className="surface-card">
            <div className="section-heading">
              <div>
                <h2>Knowledge base</h2>
                <p>{indexedCountLabel}</p>
              </div>
              <button className="ghost-button" type="button" onClick={() => void refreshDocuments()}>
                Refresh list
              </button>
            </div>

            <div className="repo-actions">
              <button
                className="ghost-button"
                type="button"
                onClick={() => void refreshRepositoryIndex()}
                disabled={refreshingIndex}
              >
                {refreshingIndex ? "Refreshing index…" : "Refresh repo documents"}
              </button>
              <p>
                Place files in <code>data/documents</code>. Spring Boot scans that folder on startup, and this action
                reindexes it on demand.
              </p>
            </div>

            <form className="upload-form" onSubmit={handleUpload}>
              <label className="field">
                <span>Uploaded by</span>
                <input value={uploadedBy} onChange={(e) => setUploadedBy(e.target.value)} />
              </label>
              <label
                className={`drop-zone${isDragging ? " drop-zone--active" : ""}`}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
              >
                <span>{isDragging ? "Drop to select" : "Add file"}</span>
                <small>PDF, DOCX, TXT, or Markdown · drag & drop or click</small>
                <input ref={fileInputRef} name="file" type="file" accept=".pdf,.docx,.txt,.md,.markdown" />
              </label>
              <button className="primary-button" type="submit" disabled={uploading}>
                {uploading ? "Indexing…" : "Add to knowledge base"}
              </button>
            </form>

            <p className="status-line">{knowledgeStatus}</p>

            <div className="document-list">
              {documentsLoading ? (
                <>
                  <div className="document-card skeleton-card">
                    <div className="skeleton skeleton-title" />
                    <div className="skeleton skeleton-meta" />
                  </div>
                  <div className="document-card skeleton-card">
                    <div className="skeleton skeleton-title" />
                    <div className="skeleton skeleton-meta" />
                  </div>
                </>
              ) : documents.length === 0 ? (
                <div className="empty-state">
                  <p>No documents indexed yet.</p>
                  <p>Upload a file above or place files in <code>data/documents</code>.</p>
                </div>
              ) : (
                documents.map((document) => (
                  <article className="document-card" key={document.documentId}>
                    <div className="document-title-row">
                      <h3>{document.filename}</h3>
                      <span className="pill">{document.chunkCount} chunks</span>
                    </div>
                    <p className="document-meta">
                      Added by {document.uploadedBy} · {formatDate(document.createdAt)}
                    </p>
                  </article>
                ))
              )}
            </div>
          </section>
        </aside>

        <section className="chat-shell">
          <header className="chat-header">
            <div>
              <p className="eyebrow">QA Space</p>
              <h2>Ask questions like a conversation.</h2>
            </div>
            <div className="chat-controls">
              <label className="compact-field">
                <span>User</span>
                <input value={userId} onChange={(e) => setUserId(e.target.value)} />
              </label>
              <label className="compact-field compact-number">
                <span>Results</span>
                <input
                  type="number"
                  min={1}
                  max={10}
                  value={maxResults}
                  onChange={(e) => setMaxResults(Number(e.target.value))}
                />
              </label>
            </div>
          </header>

          <div className="chat-surface">
            <div className="message-list" ref={messageListRef}>
              {messages.map((message) => (
                <article
                  key={message.id}
                  className={`message-bubble ${message.role === "user" ? "user-bubble" : "assistant-bubble"} ${message.pending ? "pending-bubble" : ""}`}
                >
                  <p className="message-role">{message.role === "user" ? "You" : "DocIntel AI"}</p>
                  {message.pending ? (
                    <p className="message-text typing-indicator">
                      <span /><span /><span />
                    </p>
                  ) : (
                    <p className="message-text">{message.text}</p>
                  )}
                  {message.citations?.length ? (
                    <div className="citation-stack">
                      {message.citations.map((citation) => (
                        <div className="citation-card" key={`${citation.documentId}-${citation.chunkIndex}`}>
                          <div className="citation-heading">
                            <strong>{citation.documentName}</strong>
                            <span>{formatCitationLocation(citation)}</span>
                          </div>
                          <p>{citation.excerpt}</p>
                        </div>
                      ))}
                    </div>
                  ) : null}
                </article>
              ))}
            </div>

            <form className="composer" onSubmit={handleAsk}>
              <textarea
                value={question}
                onChange={(e) => setQuestion(e.target.value)}
                onKeyDown={handleTextareaKeyDown}
                placeholder="Ask a question about the indexed documents…"
                rows={4}
              />
              <div className="composer-footer">
                <p>Answers include document, page, and paragraph citations. Press Ctrl+Enter to send.</p>
                <button className="primary-button" type="submit" disabled={asking}>
                  {asking ? "Thinking…" : "Send"}
                </button>
              </div>
            </form>
          </div>
        </section>
      </div>
    </>
  );
}

function formatDate(value: string) {
  return new Date(value).toLocaleString(undefined, {
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit"
  });
}

function formatCitationLocation(citation: Citation) {
  const pageLabel =
    citation.pageStart === citation.pageEnd ? `p. ${citation.pageStart}` : `pp. ${citation.pageStart}-${citation.pageEnd}`;
  const paragraphLabel =
    citation.paragraphStart === citation.paragraphEnd
      ? `para. ${citation.paragraphStart}`
      : `paras. ${citation.paragraphStart}-${citation.paragraphEnd}`;
  return `${pageLabel}, ${paragraphLabel}`;
}

async function readErrorMessage(response: Response) {
  const contentType = response.headers.get("content-type") ?? "";
  if (contentType.includes("application/json")) {
    const payload = (await response.json()) as { message?: string };
    return payload.message ?? JSON.stringify(payload);
  }
  return (await response.text()) || `Request failed with status ${response.status}`;
}
