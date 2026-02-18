# Project Tracker

A web-based project and time tracking application.

## Development Guidelines

### Date/Time Handling - IMPORTANT

**All date and time operations MUST use the WebUser timezone-aware helper methods.**

The application supports per-user timezone configuration. To ensure dates and times are interpreted correctly for each user:

- **DO NOT** use `Calendar.getInstance()` directly in servlet code
- **DO NOT** use `LocalDate.now()` or `LocalDateTime.now()` with `ZoneId.systemDefault()`
- **DO** use `WebUser` helper methods for all date/time operations

#### Common WebUser Date/Time Methods

```java
WebUser webUser = appReq.getWebUser();

// Current time
Date now = webUser.now();                    // Current Date in user timezone
long nowMillis = webUser.nowMillis();        // Current time as milliseconds
LocalDate today = webUser.getLocalDateToday();  // Today's date
LocalDateTime nowDateTime = webUser.getLocalDateTimeNow();  // Now as LocalDateTime

// Calendar operations
Calendar cal = webUser.getCalendar();        // Calendar at current time
Calendar cal = webUser.getCalendar(date);    // Calendar for specific date

// Date conversions
Date date = webUser.toDate(localDate);       // LocalDate → Date
LocalDate localDate = webUser.toLocalDate(date);  // Date → LocalDate
LocalDateTime localDateTime = webUser.toLocalDateTime(date);  // Date → LocalDateTime

// Day boundaries
Date startOfDay = webUser.startOfDay(date);  // 00:00:00.000
Date endOfDay = webUser.endOfDay(date);      // 23:59:59.999
Date today = webUser.getToday();             // Start of today
Date tomorrow = webUser.getTomorrow();       // Start of tomorrow

// Date arithmetic
Date futureDate = webUser.addDays(date, 7);  // Add 7 days

// Date comparison
boolean same = webUser.isSameDay(date1, date2);  // Same calendar day?
```

#### Rationale

The `Date` class stores timestamps in UTC (milliseconds since epoch). Timezone interpretation only matters when:
- Converting to/from calendar dates (year/month/day)
- Determining "today", "yesterday", day boundaries
- Formatting dates for display

Using user-specific timezone ensures:
- "Today" matches the user's wall clock, not the server's
- Date comparisons work correctly across timezones
- Reports and summaries align with user expectations

#### Where to Use WebUser Helpers

- **Servlet layer**: Always use WebUser helpers (WebUser is available via `appReq.getWebUser()`)
- **Report generation**: Pass WebUser to report methods
- **Date formatting**: Use WebUser.getDateFormat(), getTimeFormat(), getMonthFormat()

#### Exceptions

Utility classes without WebUser access (e.g., MonthUtil) may continue using Calendar.getInstance() if:
- They operate on already-normalized dates
- Refactoring would require breaking API changes
- The code doesn't interpret "today" or day boundaries

### Building

```bash
mvn clean package
```

Produces `target/tracker.war` for Tomcat deployment.

### Database

Schema files are in `src/db/`. Migration scripts follow the pattern `v{major}_{minor}.sql`.

### Configuration

- Hibernate config: `src/main/resources/hibernate.cfg.xml` (dev/prod variants available)
- Log4j: `src/main/resources/log4j.properties`
