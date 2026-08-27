package org.civicfix.app.service;

import org.civicfix.app.model.User;
import org.civicfix.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private MailService mailService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User utente;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetService, "frontendUrl", "http://localhost:4200");

        utente = new User();
        utente.setId(1L);
        utente.setUsername("citizen1");
        utente.setEmail("citizen1@test.com");
        utente.setFullName("Cittadino Uno");
    }

    @Test
    void unEmailSconosciutaNonRivelaNienteENonInviaMail() {
        when(userRepository.findByEmail("assente@example.com")).thenReturn(Optional.empty());

        // Non deve lanciare eccezioni: chi chiama l'endpoint non può
        // distinguere "email inesistente" da "email esistente", altrimenti
        // l'endpoint diventerebbe un modo per scoprire chi è registrato.
        passwordResetService.richiediReset("assente@example.com");

        verify(mailService, never()).inviaLinkReset(any(), any(), any(), anyInt());
        verify(userRepository, never()).save(any());
    }

    @Test
    void unEmailValidaGeneraUnTokenEInvitaLaMail() {
        when(userRepository.findByEmail(utente.getEmail())).thenReturn(Optional.of(utente));

        passwordResetService.richiediReset(utente.getEmail());

        ArgumentCaptor<User> salvato = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(salvato.capture());

        // Il database deve contenere l'hash del token, mai il token in chiaro.
        assertThat(salvato.getValue().getResetTokenHash()).isNotBlank();
        assertThat(salvato.getValue().getResetTokenExpiresAt()).isAfter(LocalDateTime.now());

        verify(mailService).inviaLinkReset(eq(utente.getEmail()), eq(utente.getFullName()), any(), eq(30));
    }

    @Test
    void unTokenSconosciutoVieneRifiutato() {
        when(userRepository.findByResetTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.reimpostaPassword("token-inventato", "NuovaPassword1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non valido");
    }

    @Test
    void unTokenScadutoVieneRifiutatoERimosso() {
        utente.setResetTokenHash("hash-qualsiasi");
        utente.setResetTokenExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByResetTokenHash(any())).thenReturn(Optional.of(utente));

        assertThatThrownBy(() -> passwordResetService.reimpostaPassword("token-scaduto", "NuovaPassword1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scaduto");

        // Un token scaduto non deve restare riutilizzabile: va ripulito subito.
        assertThat(utente.getResetTokenHash()).isNull();
        assertThat(utente.getResetTokenExpiresAt()).isNull();
        verify(userRepository).save(utente);
    }

    @Test
    void unTokenValidoReimpostaLaPasswordEDiventaInutilizzabile() {
        utente.setResetTokenHash("hash-valido");
        utente.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByResetTokenHash(any())).thenReturn(Optional.of(utente));
        when(passwordEncoder.encode("NuovaPassword1")).thenReturn("hash-bcrypt-finto");

        passwordResetService.reimpostaPassword("token-valido", "NuovaPassword1");

        assertThat(utente.getPasswordHash()).isEqualTo("hash-bcrypt-finto");
        // Monouso: dopo il reset il token non deve più poter essere riutilizzato.
        assertThat(utente.getResetTokenHash()).isNull();
        assertThat(utente.getResetTokenExpiresAt()).isNull();
    }
}
