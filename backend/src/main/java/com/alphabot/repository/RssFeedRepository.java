package com.alphabot.repository;

import com.alphabot.entity.RssFeed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RssFeedRepository extends JpaRepository<RssFeed, Long> {

    /** Returns all active feeds for the crawler cycle. */
    List<RssFeed> findByIsActiveTrue();
}
