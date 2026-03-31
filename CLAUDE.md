# Claude Code — Project Rules

## Git Workflow

- **Never commit directly to `main`.** All work must happen on a feature branch.
- Branch naming: `feature/<short-description>` (e.g. `feature/chunked-upload`).
- After implementing a feature, always create a PR targeting `main`.
- If `gh` CLI is available, create the PR automatically. If not, push the branch and print the GitHub PR creation URL.
- One PR per logical feature or fix — do not batch unrelated changes.
