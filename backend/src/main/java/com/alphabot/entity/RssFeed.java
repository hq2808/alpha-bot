package com.alphabot.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a configurable RSS feed source for the news crawler.
 * Default feeds are pre-populated via Flyway migration V4.
 */
@Entity
@Table(name = "rss_feeds")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RssFeed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /**
     * RSS feed URL — must be unique so duplicates are rejected at DB level.
     */
    @Column(nullable = false, unique = true)
    private String url;

    /**
     * Category: e.g. "VN", "Global", "Macro"
     */
    @Column
    private String category;

    /**
     * Whether this feed is currently active in the crawl cycle.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
