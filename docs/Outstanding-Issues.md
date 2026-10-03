# Outstanding Issues

Items noted for later attention. Add new items at the bottom; when one is resolved, remove it
here and record the fix in the commit message (or the relevant doc).

## OI-3: Local sign-in process for testing

*Logged 2026-10-03.* There is no easy way for a developer session (or Claude) to sign in to the
local deploy for testing. In particular, the local MCP endpoint
(http://localhost:8089/dandelion/api/v1/mcp) requires OAuth. Because of that, changes to MCP tools
could only be checked through the matching UI page or after deploying to production (e.g. the
5.10.11 `get_time_allocation_context` change).

Wanted: a dev-only sign-in path for the local instance, for both the web UI and the MCP/API. It
must be disabled unless an explicit local/dev flag is set, so it can never be enabled in
production. InteropHub's dev-mode magic-link bypass (the `interophub-dev-signin` skill) is a model
to follow. Once it exists, document it here or in `docs/MCP-Codex-Setup.md`, and consider a
matching skill.
