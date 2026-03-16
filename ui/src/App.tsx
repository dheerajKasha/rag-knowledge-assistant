import { FormEvent, useEffect, useMemo, useState } from "react";

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
  excerpt: string;
  score: number;
};

type AskResponse = {
  queryId: string;
  answer: string;
  citations: Citation[];
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
    text: "Upload a few documents into the knowledge base, then ask a question here. Answers will include source citations."
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
  const [asking, setAsking] = useState(false);
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
      setKnowledgeStatus(payload.length ? "Knowledge base is ready." : "Upload documents to start building the knowledge base.");
    } catch (error) {
      setKnowledgeStatus(error instanceof Error ? error.message : "Failed to load the knowledge base.");
    }
  }

  async function handleUpload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const fileInput = event.currentTarget.elements.namedItem("file") as HTMLInputElement | null;
    if (!fileInput?.files?.length) {
      setKnowledgeStatus("Choose a file to add to the knowledge base.");
      return;
    }

    const formData = new FormData();
    formData.append("file", fileInput.files[0]);

    setUploading(true);
    setKnowledgeStatus(`Indexing ${fileInput.files[0].name}...`);

    try {
      const response = await fetch(`/api/documents/upload?uploadedBy=${encodeURIComponent(uploadedBy || "system")}`, {
        method: "POST",
        body: formData
      });

      if (!response.ok) {
        throw new Error(await readErrorMessage(response));
      }

      const payload = await response.json();
      setKnowledgeStatus(`Indexed ${payload.filename} with ${payload.chunksIndexed} chunks.`);
      event.currentTarget.reset();
      await refreshDocuments();
    } catch (error) {
      setKnowledgeStatus(error instanceof Error ? error.message : "Upload failed.");
    } finally {
      setUploading(false);
    }
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
            Add documents to the knowledge base, then ask questions in a clean, focused QA space.
          </p>
        </div>

        <section className="surface-card">
          <div className="section-heading">
            <div>
              <h2>Knowledge base</h2>
              <p>{indexedCountLabel}</p>
            </div>
            <button className="ghost-button" type="button" onClick={() => void refreshDocuments()}>
              Refresh
            </button>
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
              {uploading ? "Indexing..." : "Add to knowledge base"}
            </button>
          </form>

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
                          <span>Chunk {citation.chunkIndex}</span>
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
              <p>Answers include document-level and chunk-level citations.</p>
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

async function readErrorMessage(response: Response) {
  const contentType = response.headers.get("content-type") ?? "";
  if (contentType.includes("application/json")) {
    const payload = (await response.json()) as { message?: string };
    return payload.message ?? JSON.stringify(payload);
  }

  return (await response.text()) || `Request failed with status ${response.status}`;
}
