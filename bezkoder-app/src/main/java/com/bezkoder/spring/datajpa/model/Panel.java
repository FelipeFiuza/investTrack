package com.bezkoder.spring.datajpa.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "panel")
public class Panel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_panel")
    private Long idPanel;

    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "descricao", columnDefinition = "varchar(120) default 'Novo Painel'")
    private String descricao = "Novo Painel";

    @Enumerated(EnumType.STRING)
    @Column(name = "base", nullable = false, columnDefinition = "varchar(20) not null default 'BRUTO'")
    private Base base = Base.BRUTO;

    @Enumerated(EnumType.STRING)
    @Column(name = "metrica", nullable = false, columnDefinition = "varchar(20) not null default 'VALOR'")
    private Metrica metrica = Metrica.VALOR;

    @Enumerated(EnumType.STRING)
    @Column(name = "exibicao_transacoes", nullable = false, columnDefinition = "varchar(20) not null default 'INDIVIDUAIS'")
    private ExibicaoTransacoes exibicaoTransacoes = ExibicaoTransacoes.INDIVIDUAIS;

    @Column(name = "filtro_transacoes", nullable = false, columnDefinition = "bit(1) not null default 0")
    private boolean filtroTransacoes = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "panel_transacao",
            joinColumns = @JoinColumn(name = "id_panel"),
            inverseJoinColumns = @JoinColumn(name = "id_transacao"))
    private Set<Transacao> transacoes = new HashSet<Transacao>();

    public Panel() {
    }

    public Long getIdPanel() {
        return idPanel;
    }

    public void setIdPanel(Long idPanel) {
        this.idPanel = idPanel;
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

    public ExibicaoTransacoes getExibicaoTransacoes() {
        return exibicaoTransacoes;
    }

    public void setExibicaoTransacoes(ExibicaoTransacoes exibicaoTransacoes) {
        this.exibicaoTransacoes = exibicaoTransacoes;
    }

    public boolean isFiltroTransacoes() {
        return filtroTransacoes;
    }

    public void setFiltroTransacoes(boolean filtroTransacoes) {
        this.filtroTransacoes = filtroTransacoes;
    }

    public Set<Transacao> getTransacoes() {
        return transacoes;
    }

    public void setTransacoes(Set<Transacao> transacoes) {
        this.transacoes = transacoes;
    }
}
