package com.hrassistant.dto;

/**
 * The strict JSON shape the LLM must return for every employee message. The LLM only
 * does language understanding here - it never supplies the actual leave balance,
 * attendance record or holiday dates; ChatService always fills those in from the
 * database, so the bot can't hallucinate numbers.
 */
public class IntentResult {
    // CHECK_LEAVE_BALANCE, APPLY_LEAVE, CHECK_ATTENDANCE, GET_HOLIDAYS,
    // CHECK_LEAVE_STATUS, POLICY_QUESTION, ESCALATE, SMALL_TALK, UNKNOWN
    public String intent = "UNKNOWN";
    public String leaveType = "";
    public String startDate = ""; // YYYY-MM-DD
    public String endDate = "";   // YYYY-MM-DD
    public String reason = "";
    public int month = 0;
    public int year = 0;
    public String policyAnswer = "";
    public String clarify = "";
}
