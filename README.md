# AI-Powered HR & Employee Assistance Chatbot

Java 17 · Spring Boot 3 · MySQL · HTML/CSS/JS · LLM API (Anthropic Messages API) · JWT auth

This is the Java/Spring Boot version of the HR chatbot, built to match the stack of
the IT Help Desk chatbot project. Same features and API shape as the original Go version,
reimplemented with Spring Data JPA instead of raw SQL and a lightweight JWT servlet filter
instead of Fiber middleware.

## What it does
Employees log in and chat naturally about HR matters:
- **Leave balance** - "How many leave days do I have?" -> real balance from MySQL.
- **Apply for leave** - "Apply for leave next Monday" -> the bot resolves the date, checks the balance, and always asks **"Please confirm your leave request for ... yes/no"** before writing anything to the database.
- **Attendance** - recent or a specific month's attendance record.
- **Holiday calendar** - company holidays for a given year.
- **Leave status** - status of the employee's most recent request.
- **Policy questions** - answered from a MySQL-backed policy knowledge base (leave, WFH, notice period, probation, working hours, expenses).
- **Escalation to HR** - if the bot can't answer from the policy KB, or the employee asks for a human, it drafts an escalation and (after a yes/no confirmation) raises a ticket HR can see on `/hr.html`.

HR users get a separate dashboard to approve/reject pending leave requests (balances update automatically on approval) and resolve escalations.

## Why numbers are never hallucinated
The LLM is used **only** to understand the employee's message (intent + date/slot extraction) and to phrase policy answers from the KB text it's given. It must return strict JSON:
```json
{"intent":"CHECK_LEAVE_BALANCE","leaveType":"","startDate":"","endDate":"","reason":"","month":0,"year":0,"policyAnswer":"","clarify":""}
```
`ChatService.handleIntent()` then always pulls the actual balance, attendance, holiday dates, or approval status from MySQL and builds the reply itself - the model never states a number on the app's behalf. See `ChatService.SYSTEM_PROMPT_TEMPLATE` for the full prompt.

## Confirm-before-write flow
Every leave application and every escalation goes through a `pending_actions` table (one row per employee, unique on `employee_id`) before it's committed:
1. Employee describes what they want -> bot drafts it and asks to confirm.
2. Employee replies "yes"/"no" (checked with a fast regex first, so a literal confirmation never needs a second LLM round trip) -> bot either inserts the real row and clears the draft, or cancels it.

## Auth
`AuthFilter` (a plain `OncePerRequestFilter`, not the full Spring Security chain) validates the `Authorization: Bearer <jwt>` header on every `/api/**` route except `/api/auth/login`, and additionally requires `role=HR` on anything under `/api/hr/**`. Passwords are hashed with `BCryptPasswordEncoder`.

## Architecture
```
Browser (login.html / index.html / hr.html)
   |  Authorization: Bearer <JWT>
   v
AuthFilter (JWT check) --> Controllers --> ChatService
                                              |-- LlmClient        (Anthropic Messages API)
                                              |-- PolicyService    (keyword-ranked KB search)
                                              |-- LeaveRequestRepository / EmployeeRepository / AttendanceRepository / HolidayRepository
                                              |-- EscalationRepository / PendingActionRepository / ChatMessageRepository
                                                                    MySQL (via Spring Data JPA)
```

## Run it
1. Install JDK 17+, Maven, MySQL 8.
2. Set env vars (the database `hr_chatbot_db` is auto-created, tables via `ddl-auto=update`):
   ```
   export DB_USER=root
   export DB_PASSWORD=yourpassword
   export JWT_SECRET=$(openssl rand -hex 32)
   export LLM_API_KEY=sk-ant-...      # from console.anthropic.com
   ```
3. `mvn spring-boot:run`
4. Open http://localhost:8080/login.html

Demo logins seeded on first run:
- Employee: `prathibha@company.com` / `password123`
- HR: `hr@company.com` / `password123`

Without `LLM_API_KEY` set, chat replies with a safe fallback ("having trouble understanding, want me to loop in HR?") so the rest of the app (leave balance API, HR dashboard, etc.) can still be demoed - set the key to see the full conversational flow.

## REST API
| Method | Endpoint | Auth | Purpose |
|---|---|---|---|
| POST | /api/auth/login | - | `{email, password}` -> `{token, employee}` |
| POST | /api/chat | Bearer | `{sessionId, message}` -> `{reply, intent, requiresConfirmation, sources, escalated}` |
| GET | /api/chat/{sessionId}/history | Bearer | Conversation history |
| GET | /api/leave/balance | Bearer | Current employee's leave balances |
| GET | /api/leave/history | Bearer | Current employee's leave requests |
| GET | /api/attendance?year=&month= | Bearer | Attendance for a month (defaults to current) |
| GET | /api/holidays?year= | Bearer | Holiday calendar |
| GET | /api/hr/leave/pending | Bearer + HR role | All leave requests awaiting a decision |
| PATCH | /api/hr/leave/{id}/decision?status=APPROVED\|REJECTED | Bearer + HR role | Approve/reject; adjusts balance |
| GET | /api/hr/escalations | Bearer + HR role | All escalation tickets |
| PATCH | /api/hr/escalations/{id}/resolve | Bearer + HR role | Mark an escalation resolved |

## Ideas to extend
- Full Spring Security + refresh tokens instead of the lightweight filter
- Manager approval chain (manager -> HR) instead of a flat HR role
- Replace keyword KB search with embeddings + a vector store
- MockMvc/Mockito tests for `ChatService` with a mocked `LlmClient`
- Dockerfile + docker-compose (app + MySQL)
- Push notifications to employees when a leave request is approved/rejected
