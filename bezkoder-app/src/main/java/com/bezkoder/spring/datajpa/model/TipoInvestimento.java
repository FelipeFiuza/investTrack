package com.bezkoder.spring.datajpa.model;

import javax.persistence.*;

@Entity
@Table(name = "TipoInvestimento")
public class TipoInvestimento {

    @Id
    @Column(name = "codInvestimento")
    private Long codInvestimento;

    @Column(name = "codIndice", length = 20)
    private String codIndice;

    @ManyToOne
    @JoinColumn(name = "codIndice", insertable = false, updatable = false)
    private Indice indice;

    @Column(name = "descricao", length = 200)
    private String descricao;

    @Column(name = "incideIof", length = 1)
    private String incideIof;

    @Column(name = "incideIr", length = 1)
    private String incideIr;

    // Getters e Setters
}