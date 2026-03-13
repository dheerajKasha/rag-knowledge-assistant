const uploadForm = document.getElementById("upload-form");
const questionForm = document.getElementById("question-form");
const uploadStatus = document.getElementById("upload-status");
const answerEmpty = document.getElementById("answer-empty");
const answerContent = document.getElementById("answer-content");
const queryId = document.getElementById("query-id");
const answerText = document.getElementById("answer-text");
const citations = document.getElementById("citations");

uploadForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    const fileInput = document.getElementById("file");
    const uploadedByInput = document.getElementById("uploadedBy");
    const submitButton = uploadForm.querySelector("button");

    if (!fileInput.files.length) {
        setUploadStatus("Select a document first.", true);
        return;
    }

    const formData = new FormData();
    formData.append("file", fileInput.files[0]);

    submitButton.disabled = true;
    setUploadStatus("Uploading and indexing document...");

    try {
        const response = await fetch(`/api/documents/upload?uploadedBy=${encodeURIComponent(uploadedByInput.value || "system")}`, {
            method: "POST",
            body: formData
        });

        if (!response.ok) {
            throw new Error(await readErrorMessage(response));
        }

        const payload = await response.json();
        setUploadStatus(`Indexed ${payload.filename} with ${payload.chunksIndexed} chunks.`, false, true);
        uploadForm.reset();
        uploadedByInput.value = "system";
    } catch (error) {
        setUploadStatus(error.message || "Upload failed.", true);
    } finally {
        submitButton.disabled = false;
    }
});

questionForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    const submitButton = questionForm.querySelector("button");
    const userId = document.getElementById("userId").value;
    const question = document.getElementById("question").value;
    const maxResults = Number(document.getElementById("maxResults").value || 5);

    submitButton.disabled = true;
    renderPendingAnswer();

    try {
        const response = await fetch("/api/qa/ask", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                userId,
                question,
                maxResults
            })
        });

        if (!response.ok) {
            throw new Error(await readErrorMessage(response));
        }

        const payload = await response.json();
        renderAnswer(payload);
    } catch (error) {
        renderError(error.message || "Question request failed.");
    } finally {
        submitButton.disabled = false;
    }
});

function setUploadStatus(message, isError = false, isSuccess = false) {
    uploadStatus.textContent = message;
    uploadStatus.className = "status";
    if (isError) {
        uploadStatus.classList.add("error");
    }
    if (isSuccess) {
        uploadStatus.classList.add("success");
    }
}

function renderPendingAnswer() {
    answerEmpty.classList.add("hidden");
    answerContent.classList.remove("hidden");
    queryId.textContent = "Working...";
    answerText.textContent = "Retrieving relevant chunks and generating an answer.";
    citations.innerHTML = "";
}

function renderAnswer(payload) {
    answerEmpty.classList.add("hidden");
    answerContent.classList.remove("hidden");
    queryId.textContent = `Query ID: ${payload.queryId}`;
    answerText.textContent = payload.answer;
    citations.innerHTML = "";

    if (!payload.citations.length) {
        citations.innerHTML = "<div class=\"citation-card\"><p>No source chunks matched this question.</p></div>";
        return;
    }

    payload.citations.forEach((citation) => {
        const card = document.createElement("article");
        card.className = "citation-card";
        card.innerHTML = `
            <h4>${escapeHtml(citation.documentName)}</h4>
            <p>${escapeHtml(citation.excerpt)}</p>
            <div class="citation-meta">Chunk ${citation.chunkIndex} • Similarity ${citation.score.toFixed(3)}</div>
        `;
        citations.appendChild(card);
    });
}

function renderError(message) {
    answerEmpty.classList.add("hidden");
    answerContent.classList.remove("hidden");
    queryId.textContent = "Request failed";
    answerText.textContent = message;
    citations.innerHTML = "";
}

async function readErrorMessage(response) {
    const contentType = response.headers.get("content-type") || "";
    if (contentType.includes("application/json")) {
        const payload = await response.json();
        return payload.message || JSON.stringify(payload);
    }
    return (await response.text()) || `Request failed with status ${response.status}`;
}

function escapeHtml(value) {
    return value
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
}
