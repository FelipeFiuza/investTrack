package com.bezkoder.spring.datajpa.model;

import javax.persistence.*;

@Entity
@Table(name = "Usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idUsuario")
    private Long idUsuario;

    @Column(name = "token", length = 1024)
    private String token;

    @Column(name = "nome", length = 60)
    private String nome;

    // Getters e Setters
}