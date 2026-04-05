# Timezone and Date Handling Guide

## Problem Summary

This application stores **date-only values** (no time component) in the database and must correctly interpret these dates across different user timezones. A recurring bug occurs when timezone mismatches cause dates to shift by one day.

### The Bug
- User in MDT (UTC-6) saves an action for "today" 
- App stores the date correctly as `2026-03-04`
- When querying or filtering, the date is formatted without explicit timezone
- Java's default timezone (often UTC on servers) interprets the date differently
- Result: User is redirected to yesterday's todo list instead of today's

## Root Cause

Java's `java.util.Date` class stores timestamps as milliseconds since epoch (UTC). When you format this to a string without specifying a timezone, it uses the JVM's default timezone.

### Example Timeline:
```
User's local time:  2026-03-04 @ 6:00 PM (MDT, UTC-6)
UTC time:           2026-03-05 @ 12:00 AM (midnight)
Database stores:    2026-03-04 (date only, no time)

When formatting for comparison:
✓ CORRECT:   SimpleDateFormat with TimeZone.UTC → "2026-03-04"
✗ WRONG:     SimpleDateFormat without timezone  → "2026-03-05" (or "2026-03-04" 
                                                   depending on which side of UTC
                                                   you fall on)
```

## The Correct Approach

Date handling in this app follows these principles:

### 1. User Input → Database
When a user enters "03/04/2026", that exact date goes into the database. **No timezone conversion happens.**

```java
// User types "03/04/2026"
String userInput = "03/04/2026";
Date parsedDate = webUser.parseDate(userInput);  // Uses user's timezone to parse
// Database stores: 2026-03-04 (just the date)
```

### 2. "What day is it RIGHT NOW?" - Only Use Case for Timezone
Timezone is **only** used to determine the user's current date/day.

```java
// User in MDT at 2026-03-05 00:30 AM UTC (03-04 6:30 PM local)
Date now = webUser.now();                    // Current time in UTC
Date today = webUser.getToday();             // Today's date in USER'S timezone
// Result: 2026-03-04 (because it's 6:30 PM in MUD, still the 4th)

// Server timestamp doesn't matter - user's local clock matters
```

### 3. Database Operations - Always Use UTC
When formatting dates for database queries, comparisons, or storage, **always use UTC timezone explicitly**.

```java
// CORRECT - Explicit UTC
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
String dateKey = sdf.format(date);

// WRONG - Implicit (JVM) timezone
String dateKey = new SimpleDateFormat("yyyy-MM-dd").format(date);
```

### 4. Display to User - Use User's Timezone
When showing dates to the user, use their timezone.

```java
// CORRECT - User's timezone
String displayDate = webUser.getDateFormatService()
    .formatTransportDate(date, webUser.getTimeZone());

// User in MDT sees: "03/04/2026"
// User in EDT sees: "03/04/2026" (same date)
```

## Implementation Rules

### Rule 1: Date Keys for Database/Comparison
Always use UTC-based keys when comparing or querying dates:

```java
protected String toDatabaseDateKey(Date date) {
    if (date == null) {
        return null;
    }
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));  // ← REQUIRED
    return sdf.format(date);
}
```

### Rule 2: Date Keys for Display/User Interaction
Use the user's timezone for formatting dates shown to users:

```java
protected String toUserDateKey(Date date, WebUser webUser) {
    if (date == null) {
        return null;
    }
    return webUser.getDateFormatService()
        .formatTransportDate(date, webUser.getTimeZone());
}
```

### Rule 3: Query Operations
When passing dates to database queries, use UTC-formatted keys:

```java
// WRONG - mixes timezones
Date selectedDate = getSelectedDate(request, webUser);
String userTzKey = toUserDateKey(selectedDate, webUser);
java.sql.Date sqlDate = java.sql.Date.valueOf(userTzKey);  // ← Interprets as UTC!
List<ProjectActionNext> actions = fetchReadyActions(userTzKey);

// CORRECT - consistent UTC
Date selectedDate = getSelectedDate(request, webUser);
String utcKey = toDatabaseDateKey(selectedDate);  // Uses UTC explicitly
java.sql.Date sqlDate = java.sql.Date.valueOf(utcKey);  // Matches database
List<ProjectActionNext> actions = fetchReadyActions(utcKey);
```

### Rule 4: Parsing Dates from User Input
Use user's timezone context, but store only the date part:

```java
// User input "03/04/2026"
Date userDate = webUser.parseDate(dateString);  // Parses using user's TZ
// Internally: java.util.Date object
// Database:   Stores as 2026-03-04 (time forced to 00:00:00 UTC)
```

## Common Pitfalls

### ❌ Pitfall 1: Mixing Timezone Contexts
```java
// WRONG - formatting with user TZ but comparing as UTC
String userTzKey = webUser.getDateFormatService()
    .formatTransportDate(actionDate, webUser.getTimeZone());  // MDT context
java.sql.Date sqlDate = java.sql.Date.valueOf(userTzKey);     // Parsed as UTC!
if (sqlDate.equals(today)) { ... }  // Off by one day
```

### ❌ Pitfall 2: Forgetting Timezone in SimpleDateFormat
```java
// WRONG - SimpleDateFormat uses JVM default timezone
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
String key = sdf.format(date);  // Server's timezone!
```

### ❌ Pitfall 3: Using Calendar.getInstance() Directly
```java
// WRONG - system default timezone
Calendar cal = Calendar.getInstance();
cal.setTime(date);
String key = formatKey(cal);  // Wrong timezone

// CORRECT
Calendar cal = webUser.getCalendar();  // User's timezone
cal.setTime(date);
String key = formatKey(cal);
```

### ❌ Pitfall 4: java.sql.Date without UTC
```java
// WRONG - interprets string in JVM default timezone
String dateStr = "2026-03-04";  // Could be parsed as different UTC moment
java.sql.Date sqlDate = java.sql.Date.valueOf(dateStr);

// CORRECT
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
Date date = sdf.parse(dateStr);
java.sql.Date sqlDate = new java.sql.Date(date.getTime());
```

## WebUser Helper Methods (Use These!)

The `WebUser` class provides timezone-aware convenience methods. **Always prefer these over manual date operations:**

```java
// Current time and today
Date now = webUser.now();                      // Current time (UTC based)
Date today = webUser.getToday();               // Today's date in user's timezone
Date tomorrow = webUser.getTomorrow();

// Calendar operations (uses user's timezone)
Calendar cal = webUser.getCalendar();          // Calendar for now
Calendar cal = webUser.getCalendar(someDate);  // Calendar for specific date

// Date boundaries
Date startOfDay = webUser.startOfDay(date);    // Date at 00:00:00.000
Date endOfDay = webUser.endOfDay(date);        // Date at 23:59:59.999

// Day arithmetic  
Date nextDay = webUser.addDays(date, 1);

// Date formatting (uses user's timezone)
String display = webUser.getDateFormat().format(date);
SimpleDateFormat sdf = webUser.getDateFormat();  // User's TZ format

// Date parsing (uses user's timezone)
Date parsed = webUser.parseDate("03/04/2026");   // User's TZ context

// LocalDate conversions (recommended for new code)
LocalDate localDate = webUser.getLocalDateToday();
LocalDate fromDate = webUser.toLocalDate(date);
Date fromLocalDate = webUser.toDate(localDate);
```

## When NOT to Use Timezone

**Timezone is NOT needed for:**
- Time arithmetic (adding hours/minutes)
- Comparing two existing dates
- Storing exact timestamps (those already encode the moment)
- Server-side operations that don't involve user-facing "today"

**Exception:** Utility classes without WebUser access may use `Calendar.getInstance()` if they operate on already-normalized dates and don't interpret "today" or day boundaries. Document these clearly as timezone-naive.

## Testing Date Handling

When testing timezone-dependent code:

1. Test with user in different timezones (MDT, EDT, UTC, etc.)
2. Test at day boundaries (11:59 PM, 12:00 AM)
3. Test during/after daylight saving time transitions
4. Verify database stores dates as expected across timezones

Example test:
```java
@Test
public void testTodoDateNavigationAcrossTimezones() {
    WebUser mdtUser = ... // Set timezone to MDT
    WebUser utcUser = ... // Set timezone to UTC
    
    Date sameTestDate = new Date();
    
    // Both should see the same database date
    String mdtKey = mdtUser.toLocalDate(sameTestDate);
    String utcKey = utcUser.toLocalDate(sameTestDate);
    
    // But might interpret "now" differently
    Date mdtToday = mdtUser.getToday();
    Date utcToday = utcUser.getToday();
    // These may differ by a day at 12:00-06:00 UTC boundary
}
```

## Repository Conventions

- Use `WebUser` timezone helpers for all user-facing date operations
- Use explicit UTC timezone for all database date formatting
- Use `LocalDate` (Java 8+) for new date-only code
- Document any timezone-naive code with a clear comment
- Include timezone context in code reviews for date-handling changes

## Summary Checklist

When handling dates in this codebase:

- [ ] Does this operation determine "what day is it RIGHT NOW?" → Use user's timezone
- [ ] Does this operation query/compare database dates? → Use UTC timezone explicitly
- [ ] Does this format dates for storage/comparison? → Use UTC timezone explicitly
- [ ] Does this format dates for user display? → Use user's timezone
- [ ] Did I use `WebUser` helpers instead of manual `Calendar`/`SimpleDateFormat`? 
- [ ] Did I specify `.setTimeZone()` for any SimpleDateFormat creation?
- [ ] Did I test with a user in a different timezone than the server?
