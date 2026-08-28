package org.civicfix.app.service;

import org.civicfix.app.dto.RegisterRequest;
import org.civicfix.app.mapper.UserMapper;
import org.civicfix.app.model.Role;
import org.civicfix.app.model.User;
import org.civicfix.app.repository.TeamRepository;
import org.civicfix.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Regole della registrazione pubblica. Finché stavano dentro AuthController
 * servivano MockMvc e un contesto web per verificarle; spostate nel service
 * bastano dei mock dei repository.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @Spy private UserMapper userMapper = new UserMapper();

    @InjectMocks
    private UserService userService;

    private RegisterRequest richiesta;

    @BeforeEach
    void setUp() {
        richiesta = new RegisterRequest("mrossi", "mrossi@example.com", "password123", "Mario Rossi");
    }

    @Test
    void rifiutaUnUsernameGiaInUso() {
        when(userRepository.existsByUsername("mrossi")).thenReturn(true);

        assertThatThrownBy(() -> userService.creaCittadino(richiesta))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username");

        verify(userRepository, never()).save(any());
    }

    @Test
    void rifiutaUnEmailGiaInUso() {
        when(userRepository.existsByUsername("mrossi")).thenReturn(false);
        when(userRepository.existsByEmail("mrossi@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.creaCittadino(richiesta))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email");

        verify(userRepository, never()).save(any());
    }

    /** Il ruolo non è nella richiesta: se lo fosse, chiunque potrebbe registrarsi come admin. */
    @Test
    void chiSiRegistraEsempreUnCittadino() {
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User creato = userService.creaCittadino(richiesta);

        assertThat(creato.getRole()).isEqualTo(Role.CITIZEN);
        assertThat(creato.getUsername()).isEqualTo("mrossi");
        assertThat(creato.getFullName()).isEqualTo("Mario Rossi");
    }

    @Test
    void laPasswordVieneSalvataCifrataENonInChiaro() {
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hash-finto");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User creato = userService.creaCittadino(richiesta);

        assertThat(creato.getPasswordHash()).isEqualTo("$2a$10$hash-finto");
        assertThat(creato.getPasswordHash()).isNotEqualTo("password123");
    }
}
