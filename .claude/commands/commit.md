# /commit

Generate a git commit message and create a commit for the user's changes.

## Instructions

1. Run `git status` to see all untracked files
2. Run `git diff` to see staged and unstaged changes
3. Run `git log --oneline -5` to see recent commit style
4. Analyze the changes and draft a commit message:
   - Use **Vietnamese** for the commit message
   - Follow conventional commits format: `type: message` (feat:, fix:, refactor:, etc.)
   - Keep the summary concise and focused on the "why" not the "what"
   - Use a HEREDOC for proper formatting
5. Stage the relevant files by name (NOT `git add -A` or `git add .`)
6. Create the commit with the drafted message
7. Show the final git status to confirm success

## Rules

- NEVER stage files that likely contain secrets (.env, credentials.json, etc) - warn the user
- Follow the repository's existing commit message style
- If no changes to commit, inform the user
- Do NOT push unless explicitly asked
