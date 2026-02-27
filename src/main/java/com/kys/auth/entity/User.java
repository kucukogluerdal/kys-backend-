package com.kys.auth.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String fullName;

    private String email;

    @Builder.Default
    private boolean isActive = true;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    public enum Role {
        ADMIN, USER
    }
}
