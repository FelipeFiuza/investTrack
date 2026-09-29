package com.bezkoder.spring.datajpa.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "dashboard")
public class Dashboard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dashboard")
    private Long idDashboard;

    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "descricao", length = 120)
    private String descricao;

    @ManyToOne
    @JoinColumn(name = "id_panel_esquerda_superior")
    private Panel panelEsquerdaSuperior;

    @ManyToOne
    @JoinColumn(name = "id_panel_direita_superior")
    private Panel panelDireitaSuperior;

    @ManyToOne
    @JoinColumn(name = "id_panel_esquerda_inferior")
    private Panel panelEsquerdaInferior;

    @ManyToOne
    @JoinColumn(name = "id_panel_direita_inferior")
    private Panel panelDireitaInferior;

    public Dashboard() {
    }

    public Long getIdDashboard() {
        return idDashboard;
    }

    public void setIdDashboard(Long idDashboard) {
        this.idDashboard = idDashboard;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Panel getPanelEsquerdaSuperior() {
        return panelEsquerdaSuperior;
    }

    public void setPanelEsquerdaSuperior(Panel panelEsquerdaSuperior) {
        this.panelEsquerdaSuperior = panelEsquerdaSuperior;
    }

    public Panel getPanelDireitaSuperior() {
        return panelDireitaSuperior;
    }

    public void setPanelDireitaSuperior(Panel panelDireitaSuperior) {
        this.panelDireitaSuperior = panelDireitaSuperior;
    }

    public Panel getPanelEsquerdaInferior() {
        return panelEsquerdaInferior;
    }

    public void setPanelEsquerdaInferior(Panel panelEsquerdaInferior) {
        this.panelEsquerdaInferior = panelEsquerdaInferior;
    }

    public Panel getPanelDireitaInferior() {
        return panelDireitaInferior;
    }

    public void setPanelDireitaInferior(Panel panelDireitaInferior) {
        this.panelDireitaInferior = panelDireitaInferior;
    }
}
