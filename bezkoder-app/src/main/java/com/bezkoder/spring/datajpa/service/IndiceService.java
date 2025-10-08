package com.bezkoder.spring.datajpa.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.datajpa.dto.IndiceCreateDTO;
import com.bezkoder.spring.datajpa.dto.IndiceDTO;
import com.bezkoder.spring.datajpa.dto.IndiceUpdateDTO;
import com.bezkoder.spring.datajpa.model.Indice;
import com.bezkoder.spring.datajpa.repository.IndiceRepository;

@Service
public class IndiceService {

    @Autowired
    private IndiceRepository indiceRepository;

    public List<IndiceDTO> getAllIndices(String descricao, String tipoIndice) {
        List<Indice> indices;
        
        if (descricao != null && !descricao.isEmpty()) {
            indices = indiceRepository.findByDescricaoContaining(descricao);
        } else if (tipoIndice != null && !tipoIndice.isEmpty()) {
            indices = indiceRepository.findByTipoIndice(tipoIndice);
        } else {
            indices = indiceRepository.findAll();
        }
        
        List<IndiceDTO> indiceDTOs = new ArrayList<>();
        for (Indice indice : indices) {
            indiceDTOs.add(convertToDTO(indice));
        }
        
        return indiceDTOs;
    }

    public Optional<IndiceDTO> getIndiceById(Long codIndice) {
        Optional<Indice> indice = indiceRepository.findById(codIndice);
        return indice.map(this::convertToDTO);
    }

    public IndiceDTO createIndice(IndiceCreateDTO indiceCreateDTO) {
        Indice indice = new Indice();
        indice.setDescricao(indiceCreateDTO.getDescricao());
        indice.setPeriodicidade(indiceCreateDTO.getPeriodicidade());
        indice.setTipoIndice(indiceCreateDTO.getTipoIndice());
        
        Indice savedIndice = indiceRepository.save(indice);
        return convertToDTO(savedIndice);
    }

    public Optional<IndiceDTO> updateIndice(Long codIndice, IndiceUpdateDTO indiceUpdateDTO) {
        Optional<Indice> indiceData = indiceRepository.findById(codIndice);
        
        if (indiceData.isPresent()) {
            Indice indice = indiceData.get();
            indice.setDescricao(indiceUpdateDTO.getDescricao());
            indice.setPeriodicidade(indiceUpdateDTO.getPeriodicidade());
            indice.setTipoIndice(indiceUpdateDTO.getTipoIndice());
            
            Indice updatedIndice = indiceRepository.save(indice);
            return Optional.of(convertToDTO(updatedIndice));
        }
        
        return Optional.empty();
    }

    public boolean deleteIndice(Long codIndice) {
        try {
            indiceRepository.deleteById(codIndice);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteAllIndices() {
        try {
            indiceRepository.deleteAll();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public List<IndiceDTO> getIndicesByTipoIndice(String tipoIndice) {
        List<Indice> indices = indiceRepository.findByTipoIndice(tipoIndice);
        List<IndiceDTO> indiceDTOs = new ArrayList<>();
        for (Indice indice : indices) {
            indiceDTOs.add(convertToDTO(indice));
        }
        return indiceDTOs;
    }

    public List<IndiceDTO> getIndicesByDescricao(String descricao) {
        List<Indice> indices = indiceRepository.findByDescricaoContaining(descricao);
        List<IndiceDTO> indiceDTOs = new ArrayList<>();
        for (Indice indice : indices) {
            indiceDTOs.add(convertToDTO(indice));
        }
        return indiceDTOs;
    }

    private IndiceDTO convertToDTO(Indice indice) {
        return new IndiceDTO(
            indice.getCodIndice(),
            indice.getDescricao(),
            indice.getPeriodicidade(),
            indice.getTipoIndice()
        );
    }
}
