package com.hrassistant.model;

import jakarta.persistence.*;

/** A policy knowledge-base article the bot draws its POLICY_QUESTION answers from. */
@Entity
@Table(name = "policies")
public class Policy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150)
    private String title;
    @Column(length = 500)
    private String keywords;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    public Policy() {}
    public Policy(String title, String keywords, String content) {
        this.title = title; this.keywords = keywords; this.content = content;
    }
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String v) { this.keywords = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
}
