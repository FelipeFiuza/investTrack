package com.bezkoder.spring.datajpa.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Transacao")
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idTransacao")
    private Long idTransacao;

    @Column(name = "codInvestimento")
    private Long codInvestimento;

    @ManyToOne
    @JoinColumn(name = "codInvestimento", insertable = false, updatable = false)
    private TipoInvestimento investimento;

    @Column(name = "idUsuario")
    private Long idUsuario;

    @ManyToOne
    @JoinColumn(name = "idUsuario", insertable = false, updatable = false)
    private Usuario usuario;

    @Column(name = "data")
    private LocalDateTime data;

    @Column(name = "dataVencimento")
    private LocalDateTime dataVencimento;

    @Column(name = "instituicao", length = 200)
    private String instituicao;

    @Column(name = "valor")
    private BigDecimal valor;

    @Column(name = "quantidade")
    private BigDecimal quantidade;

    // Getters e Setters
}