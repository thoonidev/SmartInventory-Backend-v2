package com.thoonidev.InventoryMgtSystem.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "quality_records")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QualityRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;
    private String processArea;
    private String referenceDocument;
    private String findingType;
    private String severity;
    private String status;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String correctiveAction;

    private String responsibleName;
    private String sourceType;
    private Long sourceId;
    private String productName;
    private Boolean active;
    private LocalDate dueDate;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
