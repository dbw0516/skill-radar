package com.skillradar.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_postings")
@Data
@NoArgsConstructor
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "company_name", nullable = false, length = 128)
    private String companyName;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(length = 64)
    private String location;

    @Column(name = "salary_text", length = 64)
    private String salaryText;

    @Column(name = "source_url", length = 512)
    private String sourceUrl;

    @Lob
    @Column(name = "raw_text", nullable = false)
    private String rawText;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private Status status = Status.open;

    @Column(name = "posted_at")
    private LocalDate postedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum Status { open, closed, pending_category }
}
