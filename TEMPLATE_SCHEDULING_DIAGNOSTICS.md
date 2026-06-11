# Template Scheduling Diagnostics & Fix

## Problem Summary

Your templates work when you manually edit/save them, but after ~2 weeks the automatic scheduling stops. This indicates a **scheduler thread failure** rather than a data issue.

### Root Cause

The `TemplateSchedulerListener` runs hourly via `ScheduledExecutorService`. If **any uncaught exception** occurs during generation, the executor silently suppresses all future scheduled runs without warning. This perfectly matches your symptom: initial generation works, then a bad data condition or runtime error occurs, and the entire scheduler dies forever until next app restart.

---

## How the System Works

### Architecture Flow

1. **TemplateSchedulerListener** (registered in web.xml as a `ServletContextListener`)
   - Fires on app startup: runs immediately, then schedules hourly generation
   - Runs at `:15` past each hour

2. **TemplateGenerationService.generateForwardWindow()**
   - For each active template with `auto_generate = 'Y'`
   - Generates instances for [today, today + advance_days]
   - Default advance_days = 14 (configurable via `TEMPLATE_ADVANCE_DAYS` in `tracker_keys`)

3. **When You Save a Template**
   - `PlanAheadMutationService.saveTemplateEdit()` calls `syncAfterEdit()`
   - Immediately generates/updates instances for the next 14 days
   - **This works because it's synchronous and in your request thread** (you see errors immediately)

4. **Automatic Hourly Generation**
   - **Runs in background daemon thread** (no user request, no visible error)
   - If anything throws, the thread just dies

---

## How to Verify If It's Stopped

### Check 1: Application Logs

**Look for:**
```
[TemplateScheduler] Scheduled. First hourly run in N min.
[TemplateScheduler] Running template generation.
[TemplateScheduler] Processing X workspace/contact pairs.
```

These appear at app startup and every hour. If you **don't see "Running template generation" every hour**, the scheduler is dead.

**With the fix applied**, any errors will also be logged:
```
ERROR [TemplateScheduler] Error processing workspace=123 contact=456: [error details with full stack]
ERROR [TemplateScheduler] Fatal error during generation run: [error details with full stack]
```

### Check 2: Database State

Run these queries to see if generation is actually happening:

#### A) Check if templates exist and are marked for auto-generation:
```sql
SELECT 
  an.action_next_id, 
  an.next_description,
  an.template_type,
  config.auto_generate,
  config.last_generated_date,
  config.missed_action_behavior
FROM action_next an
LEFT JOIN action_next_template_config config 
  ON an.action_next_id = config.action_next_id
WHERE an.template_type IS NOT NULL 
  AND an.template_type <> ''
  AND an.next_action_status <> 'X'  -- not cancelled
LIMIT 10;
```

What to look for:
- `auto_generate = 'Y'` → should be generating
- `last_generated_date` → if old or NULL, generation may be failing
- `template_type = 'D'` (Daily) is simplest to debug

#### B) Check if instances are being created for future dates:
```sql
SELECT 
  DATE(next_action_date) as action_date,
  COUNT(*) as instance_count,
  COUNT(DISTINCT template_action_next_id) as unique_templates
FROM action_next
WHERE template_action_next_id IS NOT NULL
  AND next_action_status <> 'X'
GROUP BY DATE(next_action_date)
ORDER BY action_date DESC
LIMIT 20;
```

What to look for:
- Instances should exist for dates extending ~14 days into the future
- If most recent is today or yesterday, scheduler likely stopped

#### C) Check for old/stale instances that should have been cleaned:
```sql
SELECT 
  template_action_next_id,
  next_action_date,
  next_action_status,
  DATEDIFF(CURDATE(), DATE(next_action_date)) as days_old
FROM action_next
WHERE template_action_next_id IS NOT NULL
  AND next_action_status = 'R'  -- READY
  AND DATE(next_action_date) < CURDATE()
LIMIT 20;
```

What to look for:
- If any rows exist with `days_old > 1`, the "missed action behavior" handler didn't run
- Indicates scheduler has been dead for multiple hours

---

## The Fix I Applied

### Changes to TemplateSchedulerListener

**Problem:** When `scheduleAtFixedRate()` encounters an uncaught exception, it stops scheduling without notification.

**Solution:** Wrap the task in `runGenerationSafely()` that catches **all** exceptions (including `Throwable`), logs them with full stack trace, and allows the next scheduled run to proceed.

```java
// BEFORE (dies silently on any exception):
scheduler.scheduleAtFixedRate(this::runGeneration, initialDelaySec, 3600L, TimeUnit.SECONDS);

// AFTER (survives exceptions, logs them):
scheduler.scheduleAtFixedRate(() -> runGenerationSafely("hourly"), initialDelaySec, 3600L, TimeUnit.SECONDS);

private void runGenerationSafely(String trigger) {
    try {
        runGeneration();
    } catch (Throwable t) {
        LOGGER.error("[TemplateScheduler] Unhandled error in " + trigger + " run", t);
        // Exception is logged but doesn't kill the scheduler
    }
}
```

**Additional Improvements:**
- Replaced `System.out.println()` with Log4j `LOGGER` so logs appear in your log file, not just console
- Added null-checks for session closing to prevent `NullPointerException` during cleanup
- Log messages now include full exception stack traces, not just `.getMessage()`

---

## How to Debug Right Now

### Step 1: Check logs for scheduler errors

Look at your application log file (usually in Tomcat/servlet container logs). Search for:
- `[TemplateScheduler]` — any of these lines
- `ERROR` — highest priority

If you see `ERROR` logs, that's the failure point. Share the stack trace.

### Step 2: Run the SQL queries above

Get a snapshot of:
1. How many templates have `auto_generate = 'Y'`
2. What's the latest `last_generated_date`?
3. Do future instances exist for the next 14 days?
4. Are there old stale instances from before today?

### Step 3: Check TrackerKeys config

```sql
SELECT key_value 
FROM tracker_keys 
WHERE key_name = 'TEMPLATE_ADVANCE_DAYS' 
  AND key_type = 0 
  AND key_id = 'global';
```

Should return `14` (or whatever you configured). If NULL or missing, defaults to 14.

---

## Deployment Steps

1. **Rebuild:** `mvn clean package` (or just `mvn compile -DskipTests`)
2. **Redeploy:** Replace the WAR or restart the container
3. **Wait for startup logs:** Look for `[TemplateScheduler] Scheduled. First hourly run in N min.`
4. **Wait ~1 hour:** Next scheduled run happens at `:15` past the hour
5. **Check logs:** Look for `[TemplateScheduler] Running template generation.` and `Processing N workspace/contact pairs.`
6. **Run SQL check:** Verify `last_generated_date` was updated and new instances appear

---

## Possible Failure Modes (To Investigate If Errors Appear)

### 1. NullPointerException on resolveUsername / resolveUserTimezone
- **Cause:** A template has no associated user/contact
- **Check:** `SELECT * FROM action_next WHERE template_type IS NOT NULL AND contact_id IS NULL;`
- **Fix:** Ensure all templates have a valid `contact_id`

### 2. Hibernate session/connection exhausted
- **Cause:** Connection pool too small, or sessions not closing properly
- **Check:** Look for "connection pool" or "connection timeout" errors in logs
- **Fix:** Increase `hibernate.c3p0.max_size` in hibernate.cfg.xml (currently 30)

### 3. OutOfMemoryError or StackOverflowError
- **Cause:** Large template or recursive generation logic
- **Check:** Search logs for "OutOfMemory" or "StackOverflow"
- **Fix:** Usually indicates a bug; check for circular references in template data

### 4. Database constraint violation
- **Cause:** Duplicate instance creation or invalid foreign key
- **Check:** Look for "ConstraintViolationException" or "DuplicateKeyException" in logs
- **Fix:** Review template configuration; may need to adjust schedule patterns

---

## Next Steps

1. **Apply the fix** → rebuild & redeploy the WAR
2. **Monitor logs** for the next 24 hours, looking for any `[TemplateScheduler]` messages
3. **Run SQL queries** to verify instances are being generated
4. **Share any error logs** if you see `ERROR` lines — that's the diagnostic data we need

The fix prevents the scheduler from dying, and logs any errors so we can track down the root cause.
