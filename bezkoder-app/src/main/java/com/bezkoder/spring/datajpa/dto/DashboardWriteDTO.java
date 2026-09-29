package com.bezkoder.spring.datajpa.dto;

public class DashboardWriteDTO {

    private Long idUsuario;
    private String descricao;
    private Long idPanelEsquerdaSuperior;
    private Long idPanelDireitaSuperior;
    private Long idPanelEsquerdaInferior;
    private Long idPanelDireitaInferior;

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Long getIdPanelEsquerdaSuperior() {
        return idPanelEsquerdaSuperior;
    }

    public void setIdPanelEsquerdaSuperior(Long idPanelEsquerdaSuperior) {
        this.idPanelEsquerdaSuperior = idPanelEsquerdaSuperior;
    }

    public Long getIdPanelDireitaSuperior() {
        return idPanelDireitaSuperior;
    }

    public void setIdPanelDireitaSuperior(Long idPanelDireitaSuperior) {
        this.idPanelDireitaSuperior = idPanelDireitaSuperior;
    }

    public Long getIdPanelEsquerdaInferior() {
        return idPanelEsquerdaInferior;
    }

    public void setIdPanelEsquerdaInferior(Long idPanelEsquerdaInferior) {
        this.idPanelEsquerdaInferior = idPanelEsquerdaInferior;
    }

    public Long getIdPanelDireitaInferior() {
        return idPanelDireitaInferior;
    }

    public void setIdPanelDireitaInferior(Long idPanelDireitaInferior) {
        this.idPanelDireitaInferior = idPanelDireitaInferior;
    }
}
