package com.bezkoder.spring.datajpa.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.datajpa.dto.UsuarioCreateDTO;
import com.bezkoder.spring.datajpa.dto.UsuarioDTO;
import com.bezkoder.spring.datajpa.dto.UsuarioUpdateDTO;
import com.bezkoder.spring.datajpa.model.Usuario;
import com.bezkoder.spring.datajpa.repository.UsuarioRepository;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    public List<UsuarioDTO> getAllUsuarios(String nome) {
        List<Usuario> usuarios;
        
        if (nome == null) {
            usuarios = usuarioRepository.findAll();
        } else {
            usuarios = usuarioRepository.findByNomeContaining(nome);
        }
        
        List<UsuarioDTO> usuarioDTOs = new ArrayList<>();
        for (Usuario usuario : usuarios) {
            usuarioDTOs.add(convertToDTO(usuario));
        }
        
        return usuarioDTOs;
    }

    public Optional<UsuarioDTO> getUsuarioById(Long id) {
        Optional<Usuario> usuario = usuarioRepository.findById(id);
        return usuario.map(this::convertToDTO);
    }

    public UsuarioDTO createUsuario(UsuarioCreateDTO usuarioCreateDTO) {
        Usuario usuario = new Usuario();
        usuario.setNome(usuarioCreateDTO.getNome());
        usuario.setToken(usuarioCreateDTO.getToken());
        
        Usuario savedUsuario = usuarioRepository.save(usuario);
        return convertToDTO(savedUsuario);
    }

    public Optional<UsuarioDTO> updateUsuario(Long id, UsuarioUpdateDTO usuarioUpdateDTO) {
        Optional<Usuario> usuarioData = usuarioRepository.findById(id);
        
        if (usuarioData.isPresent()) {
            Usuario usuario = usuarioData.get();
            usuario.setNome(usuarioUpdateDTO.getNome());
            usuario.setToken(usuarioUpdateDTO.getToken());
            
            Usuario updatedUsuario = usuarioRepository.save(usuario);
            return Optional.of(convertToDTO(updatedUsuario));
        }
        
        return Optional.empty();
    }

    public boolean deleteUsuario(Long id) {
        try {
            usuarioRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteAllUsuarios() {
        try {
            usuarioRepository.deleteAll();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Optional<UsuarioDTO> getUsuarioByToken(String token) {
        Usuario usuario = usuarioRepository.findByToken(token);
        if (usuario != null) {
            return Optional.of(convertToDTO(usuario));
        }
        return Optional.empty();
    }

    private UsuarioDTO convertToDTO(Usuario usuario) {
        return new UsuarioDTO(
            usuario.getIdUsuario(),
            usuario.getNome(),
            usuario.getToken()
        );
    }

}
