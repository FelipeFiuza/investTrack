package com.bezkoder.spring.datajpa.model;

import javax.persistence.*;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "token", length = 1024)
    private String token;

    @Column(name = "nome", length = 60)
    private String nome;

    public Usuario() {

    }

    public Usuario(String token, String nome) {
        this.token = token;
        this.nome = nome;
    }

    public Long getIdUsuario(){
        return this.idUsuario;
    }

    public void setIdUsuario(Long idUsuario){
        this.idUsuario = idUsuario;
    }

    public String getToken(){
        return this.token;
    }

    public void setToken(String token){
        this.token = token;
    }

    public String getNome(){
        return this.nome;
    }

    public void setNome(String nome){
        this.nome = nome;
    }


    // Getters e Setters
}