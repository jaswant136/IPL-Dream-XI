package com.ipldraftclub.repository;

import com.ipldraftclub.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlayerRepository extends JpaRepository<Player, UUID> {

    Optional<Player> findByFullNameIgnoreCaseAndCountryIgnoreCase(String fullName, String country);
}
