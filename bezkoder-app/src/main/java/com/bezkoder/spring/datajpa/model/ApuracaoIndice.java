package com.bezkoder.spring.datajpa.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@IdClass(ApuracaoIndiceId.class)
@Table(name = "ApuracaoIndice")
public class ApuracaoIndice {

    @Id
    @Column(name = "codIndice", length = 20)
    private String codIndice;

    @Id
    @Column(name = "dataApuracao")
    private LocalDateTime dataApuracao;

    @ManyToOne
    @JoinColumn(name = "codIndice", insertable = false, updatable = false)
    private Indice indice;

    @Column(name = "valorAbertura")
    private BigDecimal valorAbertura;

    @Column(name = "valorMaximo")
    private BigDecimal valorMaximo;

    @Column(name = "valorMinimo")
    private BigDecimal valorMinimo;

    @Column(name = "valorFechamento")
    private BigDecimal valorFechamento;

    // Getters e Setters
}