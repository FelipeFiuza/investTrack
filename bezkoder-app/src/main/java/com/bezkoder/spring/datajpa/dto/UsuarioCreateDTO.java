package com.bezkoder.spring.datajpa.dto;

public class UsuarioCreateDTO {
    private String nome;
    private String token;

    public UsuarioCreateDTO() {
    }

    public UsuarioCreateDTO(String nome, String token) {
        this.nome = nome;
        this.token = token;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
