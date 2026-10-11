package com.hrassistant.dto;

import java.util.List;

public record ChatResponse(String reply, String intent, boolean requiresConfirmation, List<String> sources, boolean escalated) {
    public static ChatResponse of(String reply, String intent) {
        return new ChatResponse(reply, intent, false, null, false);
    }
}
