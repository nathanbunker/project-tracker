# Project Language Import Format

Create JSON using only these keys:

- `projectName` (required): Exact project name; matching is case-insensitive within the current workspace.
- `description` (optional): Project description text.
- `currentFocus` (optional): Current focus text.
- `projectOutcome` (optional): Intended outcome text.
- `successCriteria` (optional): Array of strings, one string per success criterion. A newline-separated string is also accepted.

Omit a field to leave its existing value unchanged. Use `null`, an empty string, or an empty `successCriteria` array to clear a value. Include at least one optional field in every project object. Do not add other keys.

Preferred format for one or more projects:

```json
[
  {
    "projectName": "Website Refresh",
    "description": "Refresh the public website.",
    "currentFocus": "Approve final copy.",
    "projectOutcome": "A clear, current website is live.",
    "successCriteria": [
      "Stakeholders approve the copy",
      "The new site is live"
    ]
  }
]
```

A single object is valid for one project. JSON Lines is also valid for bulk imports: put one complete, single-line JSON object on each line. Each project may appear only once per import; the entire import is rejected if any project is missing, ambiguous, duplicated, or invalid.