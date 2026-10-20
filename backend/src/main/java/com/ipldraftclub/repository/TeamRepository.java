package com.ipldraftclub.repository;

import com.ipldraftclub.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {

    Optional<Team> findByCodeIgnoreCase(String code);

    List<Team> findAllByOrderByNameAsc();
}
