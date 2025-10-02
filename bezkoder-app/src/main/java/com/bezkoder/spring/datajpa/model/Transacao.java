package com.bezkoder.spring.datajpa.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "Transacao")
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transacao")
    private Long idTransacao;

    @ManyToOne
    @JoinColumn(name = "cod_investimento", insertable = false, updatable = false)
    private TipoInvestimento investimento;

    @ManyToOne
    @JoinColumn(name = "id_usuario", insertable = false, updatable = false)
    private Usuario usuario;

    @Column(name = "data_transacao")
    private LocalDateTime dataTransacao;

    @Column(name = "data_vencimento")
    private LocalDateTime dataVencimento;

    @Column(name = "instituicao", length = 200)
    private String instituicao;

    @Column(name = "valor")
    private BigDecimal valor;

    @Column(name = "quantidade")
    private BigDecimal quantidade;

    public Transacao(Long codInvestimento, LocalDateTime dataTransacao, LocalDateTime dataVencimento, Long idTransacao, Long idUsuario, String instituicao, TipoInvestimento investimento, BigDecimal quantidade, Usuario usuario, BigDecimal valor) {
        this.dataTransacao = dataTransacao;
        this.dataVencimento = dataVencimento;
        this.idTransacao = idTransacao;
        this.instituicao = instituicao;
        this.investimento = investimento;
        this.quantidade = quantidade;
        this.usuario = usuario;
        this.valor = valor;
    }

    public Transacao() {
    }

    public Long getIdTransacao() {
        return idTransacao;
    }

    public void setIdTransacao(Long idTransacao) {
        this.idTransacao = idTransacao;
    }

    public TipoInvestimento getInvestimento() {
        return investimento;
    }

    public void setInvestimento(TipoInvestimento investimento) {
        this.investimento = investimento;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public LocalDateTime getDataTransacao() {
        return dataTransacao;
    }

    public void setDataTransacao(LocalDateTime dataTransacao) {
        this.dataTransacao = dataTransacao;
    }

    public LocalDateTime getDataVencimento() {
        return dataVencimento;
    }

    public void setDataVencimento(LocalDateTime dataVencimento) {
        this.dataVencimento = dataVencimento;
    }

    public String getInstituicao() {
        return instituicao;
    }

    public void setInstituicao(String instituicao) {
        this.instituicao = instituicao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }

}