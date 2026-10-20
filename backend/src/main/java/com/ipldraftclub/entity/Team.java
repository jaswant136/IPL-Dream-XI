package com.ipldraftclub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "teams")
@Getter
@Setter
@NoArgsConstructor
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 8, unique = true)
    private String code;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    public Team(String code, String name, boolean active) {
        this.code = code;
        this.name = name;
        this.active = active;
    }
}
