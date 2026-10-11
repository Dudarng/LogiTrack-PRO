package com.logitrack.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class JpaUserDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private JpaUserDetailsService userDetailsService;

    @Test
    @DisplayName("Deve carregar usuário existente com suas autoridades e role ADMIN")
    void deveCarregarUsuarioExistenteComRoleAdmin() {
        Usuario usuario = Usuario.builder()
                .id(1)
                .username("admin")
                .passwordHash("$2a$10$92ruw.aMmtGE/y0Gh1Zwru5wQ7.kPbuQckx9EZzsp2qM/IpEBvPE.")
                .role("ADMIN")
                .build();

        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));

        UserDetails userDetails = userDetailsService.loadUserByUsername("admin");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("admin");
        assertThat(userDetails.getPassword()).isEqualTo("$2a$10$92ruw.aMmtGE/y0Gh1Zwru5wQ7.kPbuQckx9EZzsp2qM/IpEBvPE.");
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Deve lançar UsernameNotFoundException quando usuário não for encontrado no banco")
    void deveLancarUsernameNotFoundExceptionQuandoUsuarioNaoExiste() {
        when(usuarioRepository.findByUsername("usuario_fantasma")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("usuario_fantasma"));
    }
}
