package com.bezkoder.spring.datajpa.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@IdClass(ApuracaoIndiceId.class)
@Table(name = "ApuracaoIndiceFixa")
public class ApuracaoIndiceFixa {

    @Id
    @Column(name = "codIndice", length = 20)
    private String codIndice;

    @Id
    @Column(name = "dataApuracao")
    private LocalDateTime dataApuracao;

    @ManyToOne
    @JoinColumn(name = "codIndice", insertable = false, updatable = false)
    private Indice indice;

    @Column(name = "valorFechamento")
    private BigDecimal valorFechamento;

    // Getters e Setters
}