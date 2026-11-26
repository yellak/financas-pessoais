package br.com.financas.backend.usuario;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "text")
    private String email;

    @Column(name = "senha_hash", nullable = false, columnDefinition = "text")
    private String senhaHash;

    @CreationTimestamp(source = SourceType.DB)
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected Usuario() {
    }

    Usuario(String email, String senhaHash) {
        this.email = email;
        this.senhaHash = senhaHash;
    }

    void alterarEmail(String email) {
        this.email = email;
    }

    void alterarSenha(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    String getSenhaHash() {
        return senhaHash;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
