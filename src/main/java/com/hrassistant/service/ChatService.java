package com.hrassistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrassistant.dto.*;
import com.hrassistant.model.*;
import com.hrassistant.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatService {
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern JSON_BLOCK = Pattern.compile("(?s)\\{.*}");
    private static final Pattern AFFIRMATIVE = Pattern.compile(
            "(?i)^\\s*(yes|yeah|yep|yup|sure|confirm|confirmed|go ahead|ok|okay|do it|please do|correct)\\b");
    private static final Pattern NEGATIVE = Pattern.compile(
            "(?i)^\\s*(no|nope|nah|cancel|never ?mind|don'?t|stop|wait)\\b");

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            You are the NLU layer for "HRBot", an internal HR assistant chatbot.
            Today's date is %s. The employee's name is %s.

            Read the employee's latest message (with recent chat history for context) and reply with ONLY a
            single-line JSON object, no markdown fences, no commentary, matching exactly this shape:
            {"intent":"","leaveType":"","startDate":"","endDate":"","reason":"","month":0,"year":0,"policyAnswer":"","clarify":""}

            Rules:
            - intent must be exactly one of: CHECK_LEAVE_BALANCE, APPLY_LEAVE, CHECK_ATTENDANCE, GET_HOLIDAYS, CHECK_LEAVE_STATUS, POLICY_QUESTION, ESCALATE, SMALL_TALK, UNKNOWN.
            - For APPLY_LEAVE: resolve relative dates ("next Monday", "tomorrow", "from the 5th to the 7th") into real startDate/endDate (YYYY-MM-DD) using today's date above. Default leaveType to "Annual" if not stated. If a required date truly cannot be resolved, leave startDate empty and put a short question in clarify instead.
            - For POLICY_QUESTION: answer ONLY using the POLICY CONTEXT below; put your answer in policyAnswer. If the context doesn't cover it, leave policyAnswer empty and set intent to ESCALATE instead.
            - For CHECK_ATTENDANCE or GET_HOLIDAYS: fill month/year only if the employee mentioned a specific one; otherwise leave them 0 (meaning "recent"/"this year").
            - For ESCALATE: the employee explicitly wants a human, is frustrated, or asked something outside HR/policy scope entirely.
            - Never invent leave balances, attendance records, holiday dates or approval statuses - leave those fields out, the backend fills them in from the database.
            - Output raw JSON only.

            POLICY CONTEXT:
            %s""";

    private final LlmClient llm;
    private final EmployeeRepository employees;
    private final LeaveRequestRepository leaves;
    private final AttendanceRepository attendance;
    private final HolidayRepository holidays;
    private final PolicyService policyService;
    private final EscalationRepository escalations;
    private final ChatMessageRepository chats;
    private final PendingActionRepository pending;

    public ChatService(LlmClient llm, EmployeeRepository employees, LeaveRequestRepository leaves,
                        AttendanceRepository attendance, HolidayRepository holidays, PolicyService policyService,
                        EscalationRepository escalations, ChatMessageRepository chats, PendingActionRepository pending) {
        this.llm = llm; this.employees = employees; this.leaves = leaves; this.attendance = attendance;
        this.holidays = holidays; this.policyService = policyService; this.escalations = escalations;
        this.chats = chats; this.pending = pending;
    }

    public ChatResponse chat(String sessionId, Long employeeId, String message) {
        chats.save(new ChatMessage(sessionId, employeeId, "user", message));
        Employee emp = employees.findById(employeeId).orElseThrow(() -> new RuntimeException("employee not found"));

        // A pending draft (leave request or escalation) always takes priority: resolve it
        // with a clear yes/no before anything else, so the bot never drops a half-confirmed action.
        Optional<PendingAction> draftOpt = pending.findByEmployeeId(employeeId);
        if (draftOpt.isPresent()) {
            ChatResponse resolved = resolvePending(sessionId, emp, draftOpt.get(), message);
            if (resolved != null) return finish(sessionId, employeeId, resolved);
        }

        IntentResult intent;
        List<String> sources = new ArrayList<>();
        try {
            intent = classify(sessionId, emp, message, sources);
        } catch (Exception e) {
            log.error("LLM classification failed", e);
            return finish(sessionId, employeeId, ChatResponse.of(
                    "I'm having trouble understanding that right now. I can raise this with HR if you'd like - just say \"talk to HR\".",
                    "UNKNOWN"));
        }

        ChatResponse resp = handleIntent(emp, intent, sources);
        return finish(sessionId, employeeId, resp);
    }

    private ChatResponse finish(String sessionId, Long employeeId, ChatResponse resp) {
        chats.save(new ChatMessage(sessionId, employeeId, "assistant", resp.reply()));
        return resp;
    }

    private IntentResult classify(String sessionId, Employee emp, String message, List<String> sourcesOut) throws Exception {
        if (!llm.isConfigured()) throw new IllegalStateException("llm not configured");

        List<Policy> matches = policyService.search(message, 2);
        StringBuilder ctx = new StringBuilder();
        for (Policy p : matches) {
            ctx.append("### ").append(p.getTitle()).append("\n").append(p.getContent()).append("\n\n");
            sourcesOut.add(p.getTitle());
        }
        if (ctx.isEmpty()) ctx.append("(no matching policy articles)");

        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + " ("
                + LocalDate.now().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + ")";
        String system = String.format(SYSTEM_PROMPT_TEMPLATE, today, emp.getName(), ctx);

        List<ChatMessage> recent = new ArrayList<>(chats.findTop8BySessionIdOrderByIdDesc(sessionId));
        Collections.reverse(recent);
        while (!recent.isEmpty() && !"user".equals(recent.get(0).getRole())) recent.remove(0);

        List<Map<String, String>> turns = new ArrayList<>();
        for (ChatMessage m : recent) turns.add(Map.of("role", m.getRole(), "content", m.getContent()));
        if (turns.isEmpty()) turns.add(Map.of("role", "user", "content", message));

        String raw = llm.complete(system, turns);
        Matcher m = JSON_BLOCK.matcher(raw);
        String json = m.find() ? m.group() : raw;
        IntentResult result = MAPPER.readValue(json, IntentResult.class);
        result.intent = result.intent == null ? "UNKNOWN" : result.intent.trim().toUpperCase();
        return result;
    }

    private ChatResponse handleIntent(Employee emp, IntentResult intent, List<String> sources) {
        switch (intent.intent) {

            case "CHECK_LEAVE_BALANCE" -> {
                return ChatResponse.of(String.format("You currently have %s days of annual leave and %s days of sick leave remaining.",
                        trimNum(emp.getAnnualLeaveBalance()), trimNum(emp.getSickLeaveBalance())), intent.intent);
            }

            case "APPLY_LEAVE" -> {
                if (intent.startDate == null || intent.startDate.isBlank()) {
                    String q = (intent.clarify != null && !intent.clarify.isBlank()) ? intent.clarify : "What date would you like the leave to start?";
                    return ChatResponse.of(q, intent.intent);
                }
                String end = (intent.endDate == null || intent.endDate.isBlank()) ? intent.startDate : intent.endDate;
                double days = daysBetween(intent.startDate, end);
                String leaveType = (intent.leaveType == null || intent.leaveType.isBlank()) ? "Annual" : intent.leaveType;
                double available = "Sick".equalsIgnoreCase(leaveType) ? emp.getSickLeaveBalance() : emp.getAnnualLeaveBalance();
                if (days > available) {
                    return ChatResponse.of(String.format(
                            "That's %s day(s) of %s leave, but you only have %s day(s) available. Want to pick a shorter range instead?",
                            trimNum(days), leaveType, trimNum(available)), intent.intent);
                }
                LeaveDraftPayload draft = new LeaveDraftPayload();
                draft.leaveType = leaveType; draft.startDate = intent.startDate; draft.endDate = end;
                draft.days = days; draft.reason = intent.reason;
                saveDraft(emp.getId(), "LEAVE_DRAFT", draft);
                String dateLabel = end.equals(intent.startDate) ? intent.startDate : (intent.startDate + " to " + end);
                return new ChatResponse(String.format(
                        "Please confirm your %s leave request for %s (%s day(s)). Reply \"yes\" to submit or \"no\" to cancel.",
                        leaveType, dateLabel, trimNum(days)), intent.intent, true, null, false);
            }

            case "CHECK_ATTENDANCE" -> {
                List<Attendance> records = (intent.year > 0 && intent.month > 0)
                        ? attendance.findByEmployeeAndMonth(emp.getId(), intent.year, intent.month)
                        : attendance.findTop5ByEmployeeIdOrderByDateDesc(emp.getId());
                if (records.isEmpty()) return ChatResponse.of("I couldn't find any attendance records for that period.", intent.intent);
                StringBuilder reply = new StringBuilder("Here's your recent attendance:\n");
                for (Attendance a : records) {
                    reply.append("- ").append(a.getDate()).append(": ").append(a.getStatus());
                    if (a.getCheckIn() != null) reply.append(" (in ").append(a.getCheckIn()).append(", out ").append(a.getCheckOut()).append(")");
                    reply.append("\n");
                }
                return ChatResponse.of(reply.toString(), intent.intent);
            }

            case "GET_HOLIDAYS" -> {
                int year = intent.year > 0 ? intent.year : LocalDate.now().getYear();
                List<Holiday> hs = holidays.findByYear(year);
                if (hs.isEmpty()) return ChatResponse.of("I don't have a holiday calendar loaded for " + year + ".", intent.intent);
                StringBuilder reply = new StringBuilder("Holidays in " + year + ":\n");
                for (Holiday h : hs) reply.append("- ").append(h.getDate()).append(": ").append(h.getName()).append("\n");
                return ChatResponse.of(reply.toString(), intent.intent);
            }

            case "CHECK_LEAVE_STATUS" -> {
                return leaves.findFirstByEmployeeIdOrderByCreatedAtDesc(emp.getId())
                        .map(l -> ChatResponse.of(String.format("Your most recent request (%s leave, %s to %s) is currently %s.",
                                l.getLeaveType(), l.getStartDate(), l.getEndDate(), humanStatus(l.getStatus())), intent.intent))
                        .orElse(ChatResponse.of("You don't have any leave requests on record yet.", intent.intent));
            }

            case "POLICY_QUESTION" -> {
                if (intent.policyAnswer == null || intent.policyAnswer.isBlank()) {
                    return startEscalation(emp, "Policy question the bot couldn't answer from the knowledge base");
                }
                return new ChatResponse(intent.policyAnswer, intent.intent, false, sources, false);
            }

            case "ESCALATE" -> {
                String q = (intent.reason == null || intent.reason.isBlank()) ? "Employee asked to speak with HR" : intent.reason;
                return startEscalation(emp, q);
            }

            case "SMALL_TALK" -> {
                return ChatResponse.of("Hi! I'm HRBot - ask me about your leave balance, attendance, company holidays, or HR policies.", intent.intent);
            }

            default -> {
                return ChatResponse.of("I'm not sure I follow. I can help with leave balances, applying for leave, attendance, holidays, or policy questions - or I can loop in HR for you.", "UNKNOWN");
            }
        }
    }

    private ChatResponse startEscalation(Employee emp, String query) {
        EscalationDraftPayload draft = new EscalationDraftPayload();
        draft.query = query;
        saveDraft(emp.getId(), "ESCALATION_DRAFT", draft);
        return new ChatResponse("I'd like to loop in an HR representative for this. Shall I raise it with the HR team? (yes/no)",
                "ESCALATE", true, null, false);
    }

    private ChatResponse resolvePending(String sessionId, Employee emp, PendingAction draft, String message) {
        if (AFFIRMATIVE.matcher(message.trim()).find()) {
            try {
                if ("LEAVE_DRAFT".equals(draft.getActionType())) {
                    LeaveDraftPayload p = MAPPER.readValue(draft.getPayload(), LeaveDraftPayload.class);
                    LeaveRequest lr = new LeaveRequest();
                    lr.setEmployeeId(emp.getId()); lr.setLeaveType(p.leaveType);
                    lr.setStartDate(LocalDate.parse(p.startDate)); lr.setEndDate(LocalDate.parse(p.endDate));
                    lr.setDays(p.days); lr.setReason(p.reason);
                    LeaveRequest saved = leaves.save(lr);
                    pending.deleteByEmployeeId(emp.getId());
                    return ChatResponse.of(String.format(
                            "Done! Your %s leave request (#%d) for %s to %s has been submitted and is pending manager approval.",
                            p.leaveType, saved.getId(), p.startDate, p.endDate), "APPLY_LEAVE");
                } else if ("ESCALATION_DRAFT".equals(draft.getActionType())) {
                    EscalationDraftPayload p = MAPPER.readValue(draft.getPayload(), EscalationDraftPayload.class);
                    Escalation esc = new Escalation();
                    esc.setEmployeeId(emp.getId()); esc.setSessionId(sessionId); esc.setQuery(p.query);
                    escalations.save(esc);
                    pending.deleteByEmployeeId(emp.getId());
                    return new ChatResponse("I've raised this with the HR team - someone will reach out to you shortly.",
                            "ESCALATE", false, null, true);
                }
            } catch (Exception e) {
                log.error("Failed to resolve pending action", e);
                return ChatResponse.of("Something went wrong submitting that - please try again.", "UNKNOWN");
            }
        }
        if (NEGATIVE.matcher(message.trim()).find()) {
            pending.deleteByEmployeeId(emp.getId());
            String label = "ESCALATION_DRAFT".equals(draft.getActionType()) ? "escalation" : "request";
            return ChatResponse.of("No problem, I've cancelled that " + label + ". Anything else I can help with?", "UNKNOWN");
        }
        // Not a clear yes/no (e.g. employee changed the date instead of confirming) - fall
        // through to normal intent classification, leaving the draft in place.
        return null;
    }

    private void saveDraft(Long employeeId, String type, Object payload) {
        try {
            PendingAction pa = pending.findByEmployeeId(employeeId).orElse(new PendingAction());
            pa.setEmployeeId(employeeId);
            pa.setActionType(type);
            pa.setPayload(MAPPER.writeValueAsString(payload));
            pending.save(pa);
        } catch (Exception e) {
            log.error("Failed to save pending action", e);
        }
    }

    private static String trimNum(double d) {
        return (d == Math.floor(d)) ? String.valueOf((long) d) : String.format("%.1f", d);
    }

    private static double daysBetween(String start, String end) {
        try {
            LocalDate s = LocalDate.parse(start), e = LocalDate.parse(end);
            if (e.isBefore(s)) return 1;
            return java.time.temporal.ChronoUnit.DAYS.between(s, e) + 1;
        } catch (Exception e) {
            return 1;
        }
    }

    private static String humanStatus(LeaveRequest.Status status) {
        return switch (status) {
            case PENDING -> "awaiting approval";
            case APPROVED -> "approved";
            case REJECTED -> "rejected";
            case CANCELLED -> "cancelled";
        };
    }
}
