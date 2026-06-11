# Quick Troubleshooting Checklist

## Is Your Scheduler Dead or Alive Right Now?

### Quick Test (2 minutes)

1. **Save any template** via Template Management Servlet → new instances should appear immediately for today + 14 days
   - ✓ Works? = Template generation logic is fine, issue is likely the background scheduler
   - ✗ Fails? = Problem is in the generation logic itself; check logs for errors

2. **Check app startup logs** for:
   ```
   [TemplateScheduler] Scheduled. First hourly run in X min.
   ```
   - ✓ See it? = Listener started. Now check if hourly runs work.
   - ✗ Don't see it? = Listener never loaded; check web.xml registration

3. **Wait until past the top of the hour** (e.g., if it's 14:08, wait until 15:15), then:
   - Check logs for: `[TemplateScheduler] Running template generation.`
   - ✓ See it? = Hourly runs are firing. Check for errors in that section.
   - ✗ Don't see it after waiting? = Scheduler is dead; needs investigation

### What the Logs Should Show (Every Hour)

```
[INFO] [TemplateScheduler] Running template generation.
[INFO] [TemplateScheduler] Processing 5 workspace/contact pairs.
```

If you **never** see this pattern repeating hourly, the scheduler is stuck.

---

## Database Snapshot Queries (Copy-Paste Ready)

### How Many Templates Are Active?
```sql
SELECT COUNT(*) as active_templates
FROM action_next an
WHERE an.template_type IS NOT NULL 
  AND an.next_action_status <> 'X';
```

Expected: `> 0` if you have templates configured.

### What's the Latest Generated Date?
```sql
SELECT 
  MAX(config.last_generated_date) as most_recent_gen,
  MIN(config.last_generated_date) as oldest_config,
  COUNT(*) as configs_with_date
FROM action_next_template_config config;
```

Expected: `most_recent_gen` should be **today or very recent** (not weeks old).

### Future Instances: Do They Exist?
```sql
SELECT 
  DATE(an.next_action_date) as action_date,
  COUNT(*) as instance_count
FROM action_next an
WHERE an.template_action_next_id IS NOT NULL
  AND an.next_action_status <> 'X'
  AND DATE(an.next_action_date) >= CURDATE()
GROUP BY DATE(an.next_action_date)
ORDER BY action_date
LIMIT 14;
```

Expected: **14+ rows** with steadily increasing dates extending 14 days into future.

If last row is **today or yesterday** → scheduler has stopped.

### Stale Past Instances: Cleanup Working?
```sql
SELECT 
  COUNT(*) as stale_ready_instances,
  MIN(DATE(next_action_date)) as oldest_date
FROM action_next
WHERE template_action_next_id IS NOT NULL
  AND next_action_status = 'R'  /* READY */
  AND DATE(next_action_date) < CURDATE();
```

Expected: **0 rows** (all past missed instances should be auto-cancelled or carried forward).

If this returns rows with `oldest_date` from weeks ago → scheduler hasn't run in weeks.

---

## After You Redeploy the Fix

### Immediate Test
1. Restart the application
2. Look for startup log: `[TemplateScheduler] Scheduled. First hourly run in X min.`
3. Manually save a template → instances should appear
4. Wait 1 hour, check logs for `[TemplateScheduler] Running template generation.`

### 24-Hour Monitoring

**Expected hourly pattern:**
```
15:15:00 [INFO] [TemplateScheduler] Running template generation.
15:15:00 [INFO] [TemplateScheduler] Processing 5 workspace/contact pairs.
16:15:00 [INFO] [TemplateScheduler] Running template generation.
16:15:00 [INFO] [TemplateScheduler] Processing 5 workspace/contact pairs.
17:15:00 [INFO] [TemplateScheduler] Running template generation.
...
```

If you see this pattern **every hour**, the fix works.

**If you see errors:**
```
16:15:00 [ERROR] [TemplateScheduler] Error processing workspace=123 contact=456: ...
java.lang.NullPointerException: ...
```

Good news: The error is now **logged and visible** instead of silently killing the scheduler. This is diagnostic data.

---

## Configuration: Advance Days

If templates aren't generating far enough into the future, check:

```sql
SELECT key_value
FROM tracker_keys
WHERE key_name = 'TEMPLATE_ADVANCE_DAYS'
  AND key_type = 0
  AND key_id = 'global';
```

- Returns `14` or nothing? = Default 14 days (should be sufficient)
- Returns `7` or less? = Short window; may look empty
- Returns `NULL`? = Uses hardcoded default of 14

To **increase to 30 days**:
```sql
INSERT INTO tracker_keys (key_name, key_type, key_id, key_value)
VALUES ('TEMPLATE_ADVANCE_DAYS', 0, 'global', '30')
ON DUPLICATE KEY UPDATE key_value = '30';
```

Then restart or wait for next scheduler run.

---

## If Errors Persist After Fix

**Collect and share:**
1. Full `ERROR` log lines with stack traces
2. Output of all three SQL queries above
3. Count of templates and how many have `auto_generate = 'Y'`

The logs will now show exactly where the failure is, so we can fix the root cause (data corruption, connection pool, etc.).
