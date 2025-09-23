package com.bezkoder.spring.datajpa.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Transacao")
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transacao")
    private Long idTransacao;

    @Column(name = "cod_investimento")
    private Long codInvestimento;

    @ManyToOne
    @JoinColumn(name = "cod_investimento", insertable = false, updatable = false)
    private TipoInvestimento investimento;

    @Column(name = "id_usuario")
    private Long idUsuario;

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

    public Transacao() {

    }

    public Long getIdTransacao(){
        return this.idTransacao;
    }

    public void setIdTransacao(Long idTransacao){
        this.idTransacao = idTransacao;
    }

    public Long getCodInvestimento(){
        return this.codInvestimento;
    }

    public void setCodInvestimento(Long codInvestimento){
        this.codInvestimento = codInvestimento;
    }

    public Long getIdUsuario(){
        return this.idUsuario;
    }

    public void setIdUsuario(Long idUsuario){
        this.idUsuario = idUsuario;
    }

    public LocalDateTime getDataTransacao(){
        return this.dataTransacao;
    }

    public void setDataTransacao(LocalDateTime dataTransacao){
        this.dataTransacao = dataTransacao;
    }

    public LocalDateTime getDataVencimento(){
        return this.dataVencimento;
    }

    public void setDataVencimento(LocalDateTime dataVencimento){
        this.dataVencimento = dataVencimento;
    }

    // Getters e Setters
}