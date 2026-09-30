# Connecting an AI assistant to Dandelion Daily via MCP

Dandelion Daily hosts its own MCP (Model Context Protocol) server inside the app
itself, at `/api/v1/mcp`. This lets an AI assistant read your project and planning
data and, once you've approved specific changes in conversation, write actions,
outlooks, and project language back to Dandelion on your behalf.

See `docs/Dandelion_Daily_AI_Integration_Assessment.md` for the full design and
decision record. This file is just the practical "how do I connect a client" guide.

This file never contains a real API key. Key values live only in each machine's
environment variables, never in this repo.

## What's exposed

13 tools, task-oriented rather than raw CRUD:

- **Read-only**: `get_project_context`, `get_planning_context`, `get_outlook`,
  `list_outlooks`, `get_time_allocation_context`, `get_day_availability`
- **Narrow writes** (single record, no staleness ceremony beyond a simple upsert):
  `set_outlook`, `set_day_availability`, `add_project_ai_thought`,
  `update_project_ai_thought`, `delete_project_ai_thought`
- **Staleness-checked writes**: `update_project_language` (project description/
  focus/outcome/success criteria), `apply_changes` (batch create/update/reschedule/
  split/remove/complete on actions, applied atomically -- all or nothing)

## Authentication

Every request needs an `X-Api-Key` header. Keys are managed per-client under
**Settings -> Web API Client Keys** in Dandelion itself:

1. Log in to Dandelion.
2. Go to Settings.
3. Under "Web API Client Keys", enter an agent name (e.g. `codex`, `claude-code`)
   and click Create API Key. Leave the workspace field blank -- it defaults to your
   own private workspace.
4. Copy the generated key immediately. It's also visible any time you're on that
   page later, listed by agent name.
5. To revoke a key, delete it from the same table -- this disables it immediately.

Each client (Codex, Claude Code, anything else) should get its **own** key with its
own agent name, so you can tell them apart in `action_change_log` and revoke one
without affecting the others.

## Endpoint

```
https://dandelion-daily.org/api/v1/mcp
```

JSON-RPC 2.0 over HTTPS POST. `X-Api-Key` and `Content-Type: application/json` on
every request.

## Codex CLI setup

Codex's MCP configuration is **global per machine** (`~/.codex/config.toml` --
on Windows, `%USERPROFILE%\.codex\config.toml`), not per-project. Configure it once
and it's available in every future Codex session regardless of working directory --
you don't need to launch Codex from inside this repo.

1. Store the API key as a **persistent environment variable** on the machine running
   Codex -- never inline in `config.toml`. On Windows, a User or Machine environment
   variable named `DANDELION_MCP_API_KEY` works (System Properties -> Environment
   Variables, or `[Environment]::SetEnvironmentVariable(...)` in PowerShell). Any
   terminal or Codex session already running when you set it won't see it -- start a
   fresh one.
2. Add this to `config.toml`:

   ```toml
   [mcp_servers.dandelion]
   url = "https://dandelion-daily.org/api/v1/mcp"
   env_http_headers = { "X-Api-Key" = "DANDELION_MCP_API_KEY" }
   ```

   `env_http_headers` maps a header name to the *name* of an environment variable
   Codex reads at runtime -- the key itself never appears in this file.
3. Start a new Codex session and confirm the server connected (Codex should list
   `dandelion` among its available MCP servers/tools, or you can just ask it to use
   one of the tools above, e.g. "check my outlook for this week").

## Rotating or removing a key

Delete the old key under Settings -> Web API Client Keys, mint a new one, and update
the environment variable it's stored in. Nothing in `config.toml` needs to change,
since it only ever references the variable's name.

## Claude Code setup

Like Codex, Claude Code's MCP configuration can be scoped globally per machine
(`user` scope) rather than per-project, so one setup covers every future session
regardless of working directory. User- and local-scope servers both live in
`~/.claude.json` (on Windows, `%USERPROFILE%\.claude.json`); user scope is the one
that isn't tied to a specific project directory.

1. Store the API key as a persistent environment variable first (same as the Codex
   step above) -- e.g. `DANDELION_MCP_API_KEY`. Any Claude Code session already
   running when you set it won't see it; start a fresh one.
2. Add the server with the CLI (don't type the real key -- `${VAR}` stays literal
   and is resolved from the environment at runtime):

   ```bash
   claude mcp add --transport http dandelion https://dandelion-daily.org/api/v1/mcp \
     --header 'X-Api-Key: ${DANDELION_MCP_API_KEY}' --scope user
   ```

   Quote the `--header` value with single quotes (or otherwise prevent shell
   expansion) so the literal string `${DANDELION_MCP_API_KEY}` reaches Claude Code,
   not an already-expanded (or empty) value. You can confirm what actually got
   stored with:

   ```bash
   grep -A 5 '"dandelion"' ~/.claude.json
   ```

   The `X-Api-Key` value shown should be exactly `${DANDELION_MCP_API_KEY}`, never
   the real key.
3. Start a new Claude Code session and confirm the connection -- ask it to use one
   of the tools above, e.g. "check my outlook for this week in Dandelion."

To remove or change it later: `claude mcp remove dandelion --scope user`, then
re-add.
