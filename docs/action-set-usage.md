# ActionSet Usage Contract

## Purpose

`action_set` is the cross-record grouping primitive for related action rows.

It links:
- `action_next` records (`action_next.action_set_id`)
- `action_taken` records (`action_taken.action_set_id`)

This allows the application to track related rows as one logical thread while still keeping each row independently queryable.

## Current Contract

### `STANDARD` (`S`)

Use for normal single-project action entry.

Rules:
- Default for existing behavior.
- No sibling semantics are implied.
- Mutations operate on the selected row only.

### `SHARED` (`H`)

Use for mirrored same-intent actions recorded across linked projects/workspaces.

Rules:
- All mirrored `action_next` siblings share the same `action_set_id`.
- Group identity is `action_set_id` (not project links at mutation time).
- Intended for parity across linked recording targets.

### `ASK` (`A`) (Reserved)

Reserved for future ask/respond workflow.

Rules:
- Will represent a request/accept lifecycle group.
- Must be handled by explicit workflow transitions (requested -> accepted -> completed/closed).
- Must not be treated as generic SHARED mirror synchronization.

## Implementation Notes

1. `action_set_type` determines semantic behavior.
2. `action_set_id` is nullable in schema, but new action creation paths should assign a set for consistent lineage.
3. `action_taken` rows may share the same set as related `action_next` rows for audit traceability.
4. `action_taken` rows are not mutation siblings for UI edit/delete propagation.

## Current Phase Scope

In the current linked-patch implementation:
- Alias-based mirrored creation uses `SHARED` sets.
- Standard creation continues to use `STANDARD` sets.
- SHARED sibling propagation is enabled for edit/delete/complete/cancel flows.
- ASK lifecycle logic is intentionally deferred.

## Future ASK/Respond Planning Guardrails

When ASK implementation begins:
- Keep `ASK` set handling separate from `SHARED`.
- Define role-aware transitions (requestor, responder, shared workspace bridge).
- Ensure completion/closure rules are deterministic and auditable by set.
