#!/usr/bin/env bash
# Protect the main branch: PR required + the CI checks must pass before merging.
# Requires: GitHub CLI (gh), logged in with admin rights on the repo.
# Usage:    ./scripts/protect-main.sh OWNER/REPO
set -euo pipefail

REPO="${1:?Usage: $0 OWNER/REPO}"

gh api --method PUT "repos/${REPO}/branches/main/protection" --input - <<JSON
{
  "required_status_checks": {
    "strict": true,
    "contexts": ["Backend", "Frontend", "Docker Compose smoke test"]
  },
  "enforce_admins": true,
  "required_pull_request_reviews": {
    "required_approving_review_count": 1,
    "dismiss_stale_reviews": true
  },
  "restrictions": null,
  "required_linear_history": true,
  "allow_force_pushes": false,
  "allow_deletions": false
}
JSON

echo "main is now protected on ${REPO}"
