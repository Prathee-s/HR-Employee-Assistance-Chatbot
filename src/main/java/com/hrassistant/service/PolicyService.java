package com.hrassistant.service;

import com.hrassistant.model.Policy;
import com.hrassistant.repository.PolicyRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/** Keyword-overlap retrieval over the policies table. Small KB => in-memory scoring is plenty. */
@Service
public class PolicyService {
    private static final Set<String> STOP = Set.of("the","and","for","with","not","how","can","this","that",
            "are","was","have","you","your","what","about","many","does");
    private final PolicyRepository repo;

    public PolicyService(PolicyRepository repo) { this.repo = repo; }

    public List<Policy> search(String query, int limit) {
        Set<String> tokens = tokenize(query);
        Map<Policy, Integer> scores = new LinkedHashMap<>();
        for (Policy p : repo.findAll()) {
            String haystack = (p.getTitle() + " " + p.getKeywords() + " " + p.getContent()).toLowerCase();
            String keywords = p.getKeywords() == null ? "" : p.getKeywords().toLowerCase();
            int score = 0;
            for (String t : tokens) {
                if (haystack.contains(t)) score++;
                if (keywords.contains(t)) score += 2; // curated keywords are more reliable than body text
            }
            if (score > 0) scores.put(p, score);
        }
        return scores.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(limit).map(Map.Entry::getKey).toList();
    }

    private Set<String> tokenize(String text) {
        Set<String> out = new HashSet<>();
        for (String w : text.toLowerCase().split("[^a-z0-9']+"))
            if (w.length() > 2 && !STOP.contains(w)) out.add(w);
        return out;
    }
}
