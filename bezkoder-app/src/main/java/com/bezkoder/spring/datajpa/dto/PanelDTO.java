package com.bezkoder.spring.datajpa.dto;

import java.util.ArrayList;
import java.util.List;

import com.bezkoder.spring.datajpa.model.Base;
import com.bezkoder.spring.datajpa.model.Metrica;

public class PanelDTO {

    private Long idPanel;
    private Long idUsuario;
    private String descricao;
    private Base base;
    private Metrica metrica;
    private boolean filtroTransacoes;
    private List<Long> idsTransacao = new ArrayList<Long>();

    public Long getIdPanel() {
        return idPanel;
    }

    public void setIdPanel(Long idPanel) {
        this.idPanel = idPanel;
    }

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

    public Base getBase() {
        return base;
    }

    public void setBase(Base base) {
        this.base = base;
    }

    public Metrica getMetrica() {
        return metrica;
    }

    public void setMetrica(Metrica metrica) {
        this.metrica = metrica;
    }

    public boolean isFiltroTransacoes() {
        return filtroTransacoes;
    }

    public void setFiltroTransacoes(boolean filtroTransacoes) {
        this.filtroTransacoes = filtroTransacoes;
    }

    public List<Long> getIdsTransacao() {
        return idsTransacao;
    }

    public void setIdsTransacao(List<Long> idsTransacao) {
        this.idsTransacao = idsTransacao;
    }
}
