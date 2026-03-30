import { FormEvent, useEffect, useMemo, useRef, useState } from "react";

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

const startingMessages: Message[] = [
  {
    id: "welcome",
    role: "assistant",
    text: "Upload a few documents or place them in data/documents, then ask a question here. Answers include paper-style citations."
  }
];

export default function App() {
  const [documents, setDocuments] = useState<DocumentSummary[]>([]);
  const [messages, setMessages] = useState<Message[]>(startingMessages);
  const [question, setQuestion] = useState("");
  const [userId, setUserId] = useState("analyst-1");
  const [maxResults, setMaxResults] = useState(5);
  const [uploadedBy, setUploadedBy] = useState("system");
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState<number | null>(null);
  const [asking, setAsking] = useState(false);
  const [refreshingIndex, setRefreshingIndex] = useState(false);
  const [knowledgeStatus, setKnowledgeStatus] = useState("Loading indexed documents...");

  const indexedCountLabel = useMemo(() => {
    if (!documents.length) {
      return "No documents indexed yet";
    }
    return `${documents.length} document${documents.length === 1 ? "" : "s"} indexed`;
  }, [documents]);

  useEffect(() => {
    void refreshDocuments();
  }, []);

  async function refreshDocuments() {
    try {
      const response = await fetch("/api/documents");
      if (!response.ok) {
        throw new Error(await readErrorMessage(response));
      }

      const payload = (await response.json()) as DocumentSummary[];
      setDocuments(payload);
      setKnowledgeStatus(payload.length ? "Knowledge base is ready." : "Upload documents or add them to data/documents.");
    } catch (error) {
      setKnowledgeStatus(error instanceof Error ? error.message : "Failed to load the knowledge base.");
    }
  }

  async function refreshRepositoryIndex() {
    setRefreshingIndex(true);
    setKnowledgeStatus("Refreshing repository documents into the vector index...");

    try {
      const response = await fetch("/api/documents/refresh", {
        method: "POST"
      });

      if (!response.ok) {
        throw new Error(await readErrorMessage(response));
      }

      const payload = (await response.json()) as RefreshIndexResponse;
      setKnowledgeStatus(
        `Repository refresh complete: ${payload.indexedCount} indexed, ${payload.updatedCount} updated, ${payload.removedCount} removed from ${payload.repositoryPath}.`
      );
      await refreshDocuments();
    } catch (error) {
      setKnowledgeStatus(error instanceof Error ? error.message : "Repository refresh failed.");
    } finally {
      setRefreshingIndex(false);
    }
  }

  /** Files above this size are sent via the chunked upload API. */
  const CHUNKED_THRESHOLD_BYTES = 25 * 1024 * 1024; // 25 MB
  const CHUNK_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB per chunk

  async function handleUpload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const fileInput = event.currentTarget.elements.namedItem("file") as HTMLInputElement | null;
    if (!fileInput?.files?.length) {
      setKnowledgeStatus("Choose a file to add to the knowledge base.");
      return;
    }

    const file = fileInput.files[0];
    setUploading(true);
    setUploadProgress(null);

    try {
      let payload: { filename: string; chunksIndexed: number };

      if (file.size > CHUNKED_THRESHOLD_BYTES) {
        payload = await uploadInChunks(file, uploadedBy || "system");
      } else {
        setKnowledgeStatus(`Indexing ${file.name}...`);
        const formData = new FormData();
        formData.append("file", file);
        const response = await fetch(`/api/documents/upload?uploadedBy=${encodeURIComponent(uploadedBy || "system")}`, {
          method: "POST",
          body: formData
        });
        if (!response.ok) throw new Error(await readErrorMessage(response));
        payload = await response.json();
      }

      setKnowledgeStatus(`Indexed ${payload.filename} with ${payload.chunksIndexed} chunks.`);
      event.currentTarget.reset();
      await refreshDocuments();
    } catch (error) {
      setKnowledgeStatus(error instanceof Error ? error.message : "Upload failed.");
    } finally {
      setUploading(false);
      setUploadProgress(null);
    }
  }

  async function uploadInChunks(
    file: File,
    uploadedByValue: string
  ): Promise<{ filename: string; chunksIndexed: number }> {
    const totalChunks = Math.ceil(file.size / CHUNK_SIZE_BYTES);
    const fileSizeMB = (file.size / (1024 * 1024)).toFixed(1);

    setKnowledgeStatus(
      `${file.name} is ${fileSizeMB} MB — uploading in ${totalChunks} chunks. This may take a moment for large files.`
    );
    setUploadProgress(0);

    // Step 1: initiate session
    const initiateResponse = await fetch(
      `/api/documents/upload/session?filename=${encodeURIComponent(file.name)}&totalChunks=${totalChunks}`,
      { method: "POST" }
    );
    if (!initiateResponse.ok) throw new Error(await readErrorMessage(initiateResponse));
    const { sessionId } = (await initiateResponse.json()) as { sessionId: string };

    // Step 2: send each chunk
    for (let i = 0; i < totalChunks; i++) {
      const start = i * CHUNK_SIZE_BYTES;
      const end = Math.min(start + CHUNK_SIZE_BYTES, file.size);
      const slice = file.slice(start, end);

      const chunkForm = new FormData();
      chunkForm.append("data", new Blob([await slice.arrayBuffer()]), file.name);

      const chunkResponse = await fetch(
        `/api/documents/upload/session/${sessionId}/chunk?chunkIndex=${i}`,
        { method: "POST", body: chunkForm }
      );
      if (!chunkResponse.ok) throw new Error(await readErrorMessage(chunkResponse));

      const pct = Math.round(((i + 1) / totalChunks) * 90);
      setUploadProgress(pct);
      setKnowledgeStatus(`Uploading ${file.name}: ${pct}% (chunk ${i + 1} of ${totalChunks})…`);
    }

    // Step 3: finalize
    setKnowledgeStatus(`Processing ${file.name}…`);
    setUploadProgress(95);
    const finalizeResponse = await fetch(
      `/api/documents/upload/session/${sessionId}/finalize?uploadedBy=${encodeURIComponent(uploadedByValue)}`,
      { method: "POST" }
    );
    if (!finalizeResponse.ok) throw new Error(await readErrorMessage(finalizeResponse));
    setUploadProgress(100);
    return finalizeResponse.json();
  }

  async function handleAsk(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const trimmedQuestion = question.trim();
    if (!trimmedQuestion) {
      return;
    }

    const userMessage: Message = {
      id: crypto.randomUUID(),
      role: "user",
      text: trimmedQuestion
    };
    const pendingMessage: Message = {
      id: "pending",
      role: "assistant",
      text: "Searching the knowledge base and preparing an answer...",
      pending: true
    };

    setMessages((current) => [...current, userMessage, pendingMessage]);
    setQuestion("");
    setAsking(true);

    try {
      const response = await fetch("/api/qa/ask", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          userId,
          question: trimmedQuestion,
          maxResults
        })
      });

      if (!response.ok) {
        throw new Error(await readErrorMessage(response));
      }

      const payload = (await response.json()) as AskResponse;
      setMessages((current) => [
        ...current.filter((message) => !message.pending),
        {
          id: payload.queryId,
          role: "assistant",
          text: payload.answer,
          citations: payload.citations
        }
      ]);
    } catch (error) {
      setMessages((current) => [
        ...current.filter((message) => !message.pending),
        {
          id: crypto.randomUUID(),
          role: "assistant",
          text: error instanceof Error ? error.message : "The question request failed."
        }
      ]);
    } finally {
      setAsking(false);
    }
  }

  return (
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
            <button className="ghost-button" type="button" onClick={() => void refreshRepositoryIndex()} disabled={refreshingIndex}>
              {refreshingIndex ? "Refreshing index..." : "Refresh repo documents"}
            </button>
            <p>
              Place files in <code>data/documents</code>. Spring Boot scans that folder on startup, and this action reindexes it on demand.
            </p>
          </div>

          <form className="upload-form" onSubmit={handleUpload}>
            <label className="field">
              <span>Uploaded by</span>
              <input value={uploadedBy} onChange={(event) => setUploadedBy(event.target.value)} />
            </label>
            <label className="drop-zone">
              <span>Add file</span>
              <small>PDF, DOCX, TXT, or Markdown</small>
              <input name="file" type="file" accept=".pdf,.docx,.txt,.md,.markdown" />
            </label>
            <button className="primary-button" type="submit" disabled={uploading}>
              {uploading ? "Uploading..." : "Add to knowledge base"}
            </button>
          </form>

          {uploadProgress !== null && (
            <div className="upload-progress">
              <div className="upload-progress-bar" style={{ width: `${uploadProgress}%` }} />
              <span className="upload-progress-label">{uploadProgress}%</span>
            </div>
          )}

          <p className="status-line">{knowledgeStatus}</p>

          <div className="document-list">
            {documents.map((document) => (
              <article className="document-card" key={document.documentId}>
                <div className="document-title-row">
                  <h3>{document.filename}</h3>
                  <span className="pill">{document.chunkCount} chunks</span>
                </div>
                <p className="document-meta">
                  Added by {document.uploadedBy} • {formatDate(document.createdAt)}
                </p>
              </article>
            ))}
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
              <input value={userId} onChange={(event) => setUserId(event.target.value)} />
            </label>
            <label className="compact-field compact-number">
              <span>Results</span>
              <input
                type="number"
                min={1}
                max={10}
                value={maxResults}
                onChange={(event) => setMaxResults(Number(event.target.value))}
              />
            </label>
          </div>
        </header>

        <div className="chat-surface">
          <div className="message-list">
            {messages.map((message) => (
              <article
                key={message.id}
                className={`message-bubble ${message.role === "user" ? "user-bubble" : "assistant-bubble"} ${message.pending ? "pending-bubble" : ""}`}
              >
                <p className="message-role">{message.role === "user" ? "You" : "DocIntel AI"}</p>
                <p className="message-text">{message.text}</p>
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
              onChange={(event) => setQuestion(event.target.value)}
              placeholder="Ask a question about the indexed documents..."
              rows={4}
            />
            <div className="composer-footer">
              <p>Answers include document, page, and paragraph citations.</p>
              <button className="primary-button" type="submit" disabled={asking}>
                {asking ? "Thinking..." : "Send"}
              </button>
            </div>
          </form>
        </div>
      </section>
    </div>
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
