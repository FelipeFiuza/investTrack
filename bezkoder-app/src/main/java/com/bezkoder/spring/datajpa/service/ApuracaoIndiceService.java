package com.bezkoder.spring.datajpa.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.datajpa.dto.ApuracaoIndiceCreateDTO;
import com.bezkoder.spring.datajpa.dto.ApuracaoIndiceDTO;
import com.bezkoder.spring.datajpa.dto.ApuracaoIndiceUpdateDTO;
import com.bezkoder.spring.datajpa.model.ApuracaoIndice;
import com.bezkoder.spring.datajpa.model.ApuracaoIndiceId;
import com.bezkoder.spring.datajpa.model.Indice;
import com.bezkoder.spring.datajpa.repository.ApuracaoIndiceRepository;
import com.bezkoder.spring.datajpa.repository.IndiceRepository;

@Service
public class ApuracaoIndiceService {

    @Autowired
    private ApuracaoIndiceRepository apuracaoIndiceRepository;

    @Autowired
    private IndiceRepository indiceRepository;

    public List<ApuracaoIndiceDTO> getAll(LocalDateTime inicio, LocalDateTime fim) {
        List<ApuracaoIndice> apuracoes;
        if (inicio != null && fim != null) {
            apuracoes = apuracaoIndiceRepository.findByDataApuracaoBetween(inicio, fim);
        } else {
            apuracoes = apuracaoIndiceRepository.findAll();
        }

        List<ApuracaoIndiceDTO> dtos = new ArrayList<>();
        for (ApuracaoIndice apuracao : apuracoes) {
            dtos.add(convertToDTO(apuracao));
        }
        return dtos;
    }

    public List<ApuracaoIndiceDTO> getByIndice(Long codIndice) {
        Optional<Indice> indiceOpt = indiceRepository.findById(codIndice);
        if (!indiceOpt.isPresent()) {
            return new ArrayList<>();
        }
        List<ApuracaoIndice> apuracoes = apuracaoIndiceRepository.findByIndice(indiceOpt.get());
        List<ApuracaoIndiceDTO> dtos = new ArrayList<>();
        for (ApuracaoIndice apuracao : apuracoes) {
            dtos.add(convertToDTO(apuracao));
        }
        return dtos;
    }

    public List<ApuracaoIndiceDTO> getByIndiceAndPeriodo(Long codIndice, LocalDateTime inicio, LocalDateTime fim) {
        Optional<Indice> indiceOpt = indiceRepository.findById(codIndice);
        if (!indiceOpt.isPresent()) {
            return new ArrayList<>();
        }
        List<ApuracaoIndice> apuracoes = apuracaoIndiceRepository.findByIndiceAndDataApuracaoBetween(indiceOpt.get(), inicio, fim);
        List<ApuracaoIndiceDTO> dtos = new ArrayList<>();
        for (ApuracaoIndice apuracao : apuracoes) {
            dtos.add(convertToDTO(apuracao));
        }
        return dtos;
    }

    public Optional<ApuracaoIndiceDTO> getById(Long codIndice, LocalDateTime dataApuracao) {
        Optional<Indice> indiceOpt = indiceRepository.findById(codIndice);
        if (!indiceOpt.isPresent()) {
            return Optional.empty();
        }
        ApuracaoIndiceId id = new ApuracaoIndiceId(indiceOpt.get().getCodIndice(), dataApuracao);
        Optional<ApuracaoIndice> apuracao = apuracaoIndiceRepository.findById(id);
        return apuracao.map(this::convertToDTO);
    }

    public Optional<ApuracaoIndiceDTO> create(ApuracaoIndiceCreateDTO createDTO) {
        Optional<Indice> indiceOpt = indiceRepository.findById(createDTO.getCodIndice());
        if (!indiceOpt.isPresent()) {
            return Optional.empty();
        }
        ApuracaoIndice apuracao = new ApuracaoIndice();
        apuracao.setIndice(indiceOpt.get());
        apuracao.setDataApuracao(LocalDateTime.parse(createDTO.getDataApuracao()));
        apuracao.setValorAbertura(createDTO.getValorAbertura());
        apuracao.setValorMaximo(createDTO.getValorMaximo());
        apuracao.setValorMinimo(createDTO.getValorMinimo());
        apuracao.setValorFechamento(createDTO.getValorFechamento());

        ApuracaoIndice saved = apuracaoIndiceRepository.save(apuracao);
        return Optional.of(convertToDTO(saved));
    }

    public Optional<ApuracaoIndiceDTO> update(Long codIndice, LocalDateTime dataApuracao, ApuracaoIndiceUpdateDTO updateDTO) {
        Optional<Indice> indiceOpt = indiceRepository.findById(codIndice);
        if (!indiceOpt.isPresent()) {
            return Optional.empty();
        }
        ApuracaoIndiceId id = new ApuracaoIndiceId(indiceOpt.get().getCodIndice(), dataApuracao);
        Optional<ApuracaoIndice> apuracaoOpt = apuracaoIndiceRepository.findById(id);
        if (!apuracaoOpt.isPresent()) {
            return Optional.empty();
        }
        ApuracaoIndice apuracao = apuracaoOpt.get();
        apuracao.setValorAbertura(updateDTO.getValorAbertura());
        apuracao.setValorMaximo(updateDTO.getValorMaximo());
        apuracao.setValorMinimo(updateDTO.getValorMinimo());
        apuracao.setValorFechamento(updateDTO.getValorFechamento());

        ApuracaoIndice updated = apuracaoIndiceRepository.save(apuracao);
        return Optional.of(convertToDTO(updated));
    }

    public boolean delete(Long codIndice, LocalDateTime dataApuracao) {
        Optional<Indice> indiceOpt = indiceRepository.findById(codIndice);
        if (!indiceOpt.isPresent()) {
            return false;
        }
        ApuracaoIndiceId id = new ApuracaoIndiceId(indiceOpt.get().getCodIndice(), dataApuracao);
        try {
            apuracaoIndiceRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteAll() {
        try {
            apuracaoIndiceRepository.deleteAll();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private ApuracaoIndiceDTO convertToDTO(ApuracaoIndice apuracao) {
        return new ApuracaoIndiceDTO(
            apuracao.getIndice() != null ? apuracao.getIndice().getCodIndice() : null,
            apuracao.getDataApuracao(),
            apuracao.getValorAbertura(),
            apuracao.getValorMaximo(),
            apuracao.getValorMinimo(),
            apuracao.getValorFechamento()
        );
    }
}
