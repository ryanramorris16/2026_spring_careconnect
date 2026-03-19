package com.careconnect.model;

import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import io.micrometer.common.lang.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Template {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String description;

    @Nullable
    private String frequency;

    @Nullable
    @Column(name = "task_interval")
    private int taskInterval;

    @Nullable
    @Column(name = "do_count")
    private int doCount;

    @Nullable
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "days_of_week", columnDefinition = "jsonb")
    private List<Boolean> daysOfWeek;

    @Nullable
    @Column(name = "time_of_day")
    private String timeOfDay;

    @Column(nullable = false)
    private int icon;

    @Nullable
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> notifications;
}
