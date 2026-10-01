# Action Attention Modes

**Status:** Idea, not yet designed
**Raised:** 2026-09-30, in a planning conversation between Nathan and Claude
**Scope:** The underlying Dandelion model, the user interface, and the MCP

This note describes a need and the concept that came out of discussing it. It
deliberately does not propose a data model, UI design, or MCP tool changes. Those are
left for the design work.

## The need

With AI agents doing some of the work, Nathan now has tasks that run **alongside** other
work, not in place of it. There are also tasks that need only his **ears**, such as
listening to a NotebookLM podcast on a topic while walking.

Dandelion has no way to express either. Today both end up as **MIGHT**, which says
"I'm not sure I'll do this" when the real meaning is "this doesn't need my full attention
right now." As a result:

- the planning view can't tell optional work from concurrent work;
- day-load estimates treat concurrent and listening work as overflow, making days look
  more overbooked than they are (this happened in the 2026-09-30 planning session);
- there's nowhere to keep work ready for times like meetings and walks, so those times
  go unused;
- the assistant can't suggest the right task for the moment ("you're in meetings all
  afternoon; here's something to run").

## The concept: mode

Each action needs something like a **mode**: what kind of attention it requires. Three
modes came up:

| Mode | What it needs from Nathan | When it fits |
| --- | --- | --- |
| **Focus** | Full attention (the normal case) | Open working time |
| **Concurrent** | Brief attention: start it, check in, review the result | During meetings; while other work is running |
| **Listen** | Ears only | Walking, driving, chores |

Meetings are already their own action type. For concurrent work, meetings are the time
that gets used.

### Mode is independent of commitment

The existing verbs mostly express **commitment** (will, might, would like to, committed
to). Mode expresses **attention**. The two vary independently:

- a listening task can be committed ("listen to the ISO podcast before the WGM") or
  optional;
- an agent task can have a hard deadline or be something to do if there's bandwidth.

"I will listen to…", "I might listen to…", and "I will run concurrently…" are all
reasonable sentences, so the verb language should still read naturally.

### Showing modes visually

Nathan would like modes to be visible at a glance, for example headphones beside
listening tasks, or a distinct color per mode.

## Related considerations

These came up in the same discussion. They're inputs to the design, not decisions.

### Attention time versus running time

An agent task may run for an hour but need only 5 minutes to start and 15 to review.
For planning, the useful estimate is Nathan's **attention** time, not the task's total
running time. The review step may itself need focus time.

### Readiness

The goal is to always have something ready for the moment:

- a concurrent task is ready when it's specced well enough to start;
- a listening task is ready when the audio exists.

Readiness matters as much as mode. A queue of unready tasks doesn't help during a
meeting or a walk.

### Chains

Some work naturally crosses modes. Example:

1. "Generate a NotebookLM podcast on X" (concurrent, a few minutes to start)
2. "Listen to the podcast on X" (listen), ready only once step 1 is done

Dandelion's WAITING actions already hide follow-on work until something finishes, which
may be related.

### Not every meeting allows multitasking

Nathan can't run an agent while facilitating a call. Some way to know which meetings
leave room for concurrent work would prevent overbooking the ones he runs.

### Listening time as availability

If some listening time is expected on most days (for example, a daily walk), listening
tasks would have a place in the plan without inflating the working day.

### Concurrent work carries over

Concurrent tasks often don't finish in one sitting. They may need to roll forward without
counting as missed, while the review step gets a real slot.

## What the assistant should be able to do

Through the MCP, once modes exist:

- see each action's mode in planning results;
- report a day's load by mode, so concurrent and listening work isn't counted as overflow
  (see `MCP-Feedback.md`, I-3);
- notice when the concurrent or listening queue is running low for upcoming meetings or
  walks, and suggest refilling it (for example, "Tomorrow has 5 hours of meetings but
  only one concurrent task ready");
- propose and set modes on actions, with Nathan's approval.

## Open questions

1. Should mode be separate from the existing verbs, or should new verbs cover it? (The
   discussion favored separate, since mode and commitment vary independently.)
2. Are there modes beyond Focus, Concurrent, and Listen, such as reading on a phone or
   quick tasks between meetings?
3. How should the day's capacity for each mode be expressed: meeting time for
   concurrent, walking time for listen?
4. How is readiness recorded, and who marks a task ready?
5. How should chains across modes be represented?
6. Until this exists, MIGHT continues to stand in for concurrent work.
