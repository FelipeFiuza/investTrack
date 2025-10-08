package com.bezkoder.spring.datajpa.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.bezkoder.spring.datajpa.dto.UsuarioCreateDTO;
import com.bezkoder.spring.datajpa.dto.UsuarioDTO;
import com.bezkoder.spring.datajpa.dto.UsuarioUpdateDTO;
import com.bezkoder.spring.datajpa.service.UsuarioService;

@CrossOrigin(origins = "http://localhost:8081")
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> getAll(@RequestParam(required = false) String nome) {
        try {
            List<UsuarioDTO> usuarios = usuarioService.getAllUsuarios(nome);

            if (usuarios.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(usuarios, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> getById(@PathVariable("id") Long id) {
        Optional<UsuarioDTO> usuarioData = usuarioService.getUsuarioById(id);

        return usuarioData.map(usuario -> new ResponseEntity<>(usuario, HttpStatus.OK))
                          .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<UsuarioDTO> create(@RequestBody UsuarioCreateDTO usuarioCreateDTO) {
        try {
            UsuarioDTO usuario = usuarioService.createUsuario(usuarioCreateDTO);
            return new ResponseEntity<>(usuario, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> update(@PathVariable("id") Long id, @RequestBody UsuarioUpdateDTO usuarioUpdateDTO) {
        Optional<UsuarioDTO> usuarioData = usuarioService.updateUsuario(id, usuarioUpdateDTO);

        if (usuarioData.isPresent()) {
            return new ResponseEntity<>(usuarioData.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> delete(@PathVariable("id") Long id) {
        try {
            if (usuarioService.deleteUsuario(id)) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping
    public ResponseEntity<HttpStatus> deleteAll() {
        try {
            if (usuarioService.deleteAllUsuarios()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/token/{token}")
    public ResponseEntity<UsuarioDTO> getByToken(@PathVariable("token") String token) {
        Optional<UsuarioDTO> usuarioData = usuarioService.getUsuarioByToken(token);

        return usuarioData.map(usuario -> new ResponseEntity<>(usuario, HttpStatus.OK))
                          .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}