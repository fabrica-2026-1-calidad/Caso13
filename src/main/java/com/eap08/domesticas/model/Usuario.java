package com.eap08.domesticas.model;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "email", unique = true, nullable = false, length = 150)
    private String email;

    // Nunca se guarda la contraseña en texto plano — solo el hash
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "nombre", length = 70)
    private String nombre;

    @Column(name = "u_created_at", updatable = false)
    private LocalDateTime uCreatedAt;

    @Column(name = "u_updated_at")
    private LocalDateTime uUpdatedAt;

    // Se ejecuta automáticamente antes de insertar el registro
    @PrePersist
    protected void onCreate() {
        uCreatedAt = LocalDateTime.now();
        uUpdatedAt = LocalDateTime.now();
    }

    // Se ejecuta automáticamente antes de actualizar el registro
    @PreUpdate
    protected void onUpdate() {
        uUpdatedAt = LocalDateTime.now();
    }
}