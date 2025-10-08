package com.bezkoder.spring.datajpa.dto;

public class UsuarioDTO {
    private Long idUsuario;
    private String nome;
    private String token;

    public UsuarioDTO() {
    }

    public UsuarioDTO(Long idUsuario, String nome, String token) {
        this.idUsuario = idUsuario;
        this.nome = nome;
        this.token = token;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
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
