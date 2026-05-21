package com.eap08.domesticas.service;

import com.eap08.domesticas.dto.HogarRequest.CreateHogarRequest;
import com.eap08.domesticas.dto.HogarRequest.InvitarMiembroRequest;
import com.eap08.domesticas.dto.HogarRequest.ResponderInvitacionRequest;
import com.eap08.domesticas.dto.HogarResponse.HogarData;
import com.eap08.domesticas.dto.HogarResponse.InvitacionResponse;
import com.eap08.domesticas.dto.HogarResponse.MiembroResponse;
import com.eap08.domesticas.model.*;
import com.eap08.domesticas.repository.HogarRepository;
import com.eap08.domesticas.repository.InvitacionHogarRepository;
import com.eap08.domesticas.repository.UsuarioHogarRepository;
import com.eap08.domesticas.repository.UsuarioRepository;
import com.eap08.domesticas.service.impl.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HogarService - Test Suite")
public class HogarServiceTest {

    @Mock
    private HogarRepository hogarRepo;

    @Mock
    private UsuarioHogarRepository usuarioHogarRepo;

    @Mock
    private InvitacionHogarRepository invitacionRepo;

    @Mock
    private UsuarioRepository usuarioRepo;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private HogarService hogarService;

    private Usuario usuarioCreador;
    private Usuario usuarioInvitado;
    private Hogar hogarTest;
    private InvitacionHogar invitacionTest;

    @BeforeEach
    void setUp() {
        // Inicializar datos de prueba
        usuarioCreador = Usuario.builder()
            .usuarioId(1L)
            .nombre("Juan Pérez")
            .email("juan@example.com")
            .build();

        usuarioInvitado = Usuario.builder()
            .usuarioId(2L)
            .nombre("María García")
            .email("maria@example.com")
            .build();

        hogarTest = Hogar.builder()
            .hogarId(1L)
            .nombre("Casa Principal")
            .descripcion("Hogar familiar")
            .build();

        invitacionTest = InvitacionHogar.builder()
            .invitacionId(1L)
            .token(UUID.randomUUID().toString())
            .emailInvitado("maria@example.com")
            .hogar(hogarTest)
            .invitadoPor(usuarioCreador)
            .estado("Pendiente")
            .fechaExpiracion(LocalDateTime.now().plusHours(48))
            .build();
    }

    // ==================== Tests para crearHogar ====================

    @Test
    @DisplayName("Debería crear un hogar exitosamente")
    void testCrearHogarExito() {
        // Arrange
        CreateHogarRequest request = new CreateHogarRequest("Casa Nueva", "Descripción del hogar");
        when(usuarioRepo.findByEmail("juan@example.com")).thenReturn(Optional.of(usuarioCreador));
        when(hogarRepo.save(any(Hogar.class))).thenReturn(hogarTest);
        when(usuarioHogarRepo.save(any(UsuarioHogar.class))).thenReturn(null);

        // Act
        HogarData resultado = hogarService.crearHogar(request, "juan@example.com");

        // Assert
        assertNotNull(resultado);
        assertEquals(hogarTest.getHogarId(), resultado.hogarId());
        assertEquals(hogarTest.getNombre(), resultado.nombre());
        assertEquals(hogarTest.getDescripcion(), resultado.descripcion());

        verify(usuarioRepo, times(1)).findByEmail("juan@example.com");
        verify(hogarRepo, times(1)).save(any(Hogar.class));
        verify(usuarioHogarRepo, times(1)).save(any(UsuarioHogar.class));
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando el usuario no existe")
    void testCrearHogarUsuarioNoExiste() {
        // Arrange
        CreateHogarRequest request = new CreateHogarRequest("Casa Nueva", "Descripción");
        when(usuarioRepo.findByEmail("noexiste@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> hogarService.crearHogar(request, "noexiste@example.com"));

        verify(usuarioRepo, times(1)).findByEmail("noexiste@example.com");
        verify(hogarRepo, never()).save(any());
    }

    // ==================== Tests para invitarMiembro ====================

    @Test
    @DisplayName("Debería invitar un miembro exitosamente")
    void testInvitarMiembroExito() {
        // Arrange
        InvitarMiembroRequest request = new InvitarMiembroRequest("maria@example.com");
        Long hogarId = 1L;

        UsuarioHogar adminMembership = UsuarioHogar.builder()
            .rol(UsuarioHogar.ROL_ADMINISTRADOR)
            .build();

        when(usuarioRepo.findByEmail("juan@example.com")).thenReturn(Optional.of(usuarioCreador));
        when(usuarioHogarRepo.findByIdUsuarioIdAndIdHogarId(1L, hogarId))
            .thenReturn(Optional.of(adminMembership));
        when(hogarRepo.findById(hogarId)).thenReturn(Optional.of(hogarTest));
        when(usuarioRepo.findByEmail("maria@example.com")).thenReturn(Optional.empty());
        when(invitacionRepo.existsByEmailInvitadoAndHogarHogarIdAndEstado(
                "maria@example.com", hogarId, "Pendiente")).thenReturn(false);
        when(invitacionRepo.save(any(InvitacionHogar.class))).thenReturn(invitacionTest);

        // Act
        InvitacionResponse resultado = hogarService.invitarMiembro(hogarId, request, "juan@example.com");

        // Assert
        assertNotNull(resultado);
        assertEquals("maria@example.com", resultado.emailInvitado());
        assertEquals("Pendiente", resultado.estado());

        verify(hogarRepo, times(1)).findById(hogarId);
        verify(invitacionRepo, times(1)).save(any(InvitacionHogar.class));
        verify(emailService, times(1)).enviarEmailInvitacion(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando no es administrador")
    void testInvitarMiembroNoEsAdmin() {
        // Arrange
        InvitarMiembroRequest request = new InvitarMiembroRequest("maria@example.com");
        Long hogarId = 1L;

        UsuarioHogar miembroBiografia = UsuarioHogar.builder()
            .rol(UsuarioHogar.ROL_MIEMBRO)
            .build();

        when(usuarioRepo.findByEmail("juan@example.com")).thenReturn(Optional.of(usuarioCreador));
        when(usuarioHogarRepo.findByIdUsuarioIdAndIdHogarId(1L, hogarId))
            .thenReturn(Optional.of(miembroBiografia));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> hogarService.invitarMiembro(hogarId, request, "juan@example.com"));

        verify(invitacionRepo, never()).save(any());
        verify(emailService, never()).enviarEmailInvitacion(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando el hogar no existe")
    void testInvitarMiembroHogarNoExiste() {
        // Arrange
        InvitarMiembroRequest request = new InvitarMiembroRequest("maria@example.com");
        Long hogarId = 999L;

        UsuarioHogar adminMembership = UsuarioHogar.builder()
            .rol(UsuarioHogar.ROL_ADMINISTRADOR)
            .build();

        when(usuarioRepo.findByEmail("juan@example.com")).thenReturn(Optional.of(usuarioCreador));
        when(usuarioHogarRepo.findByIdUsuarioIdAndIdHogarId(1L, hogarId))
            .thenReturn(Optional.of(adminMembership));
        when(hogarRepo.findById(hogarId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> hogarService.invitarMiembro(hogarId, request, "juan@example.com"));

        verify(invitacionRepo, never()).save(any());
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando el usuario ya pertenece al hogar")
    void testInvitarMiembroUsuarioYaPertenece() {
        // Arrange
        InvitarMiembroRequest request = new InvitarMiembroRequest("maria@example.com");
        Long hogarId = 1L;

        UsuarioHogar adminMembership = UsuarioHogar.builder()
            .rol(UsuarioHogar.ROL_ADMINISTRADOR)
            .build();

        when(usuarioRepo.findByEmail("juan@example.com")).thenReturn(Optional.of(usuarioCreador));
        when(usuarioHogarRepo.findByIdUsuarioIdAndIdHogarId(1L, hogarId))
            .thenReturn(Optional.of(adminMembership));
        when(hogarRepo.findById(hogarId)).thenReturn(Optional.of(hogarTest));
        when(usuarioRepo.findByEmail("maria@example.com")).thenReturn(Optional.of(usuarioInvitado));
        when(usuarioHogarRepo.existsByIdUsuarioIdAndIdHogarId(2L, hogarId)).thenReturn(true);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> hogarService.invitarMiembro(hogarId, request, "juan@example.com"));

        verify(invitacionRepo, never()).save(any());
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando ya existe una invitación pendiente")
    void testInvitarMiembroInvitacionPendiente() {
        // Arrange
        InvitarMiembroRequest request = new InvitarMiembroRequest("maria@example.com");
        Long hogarId = 1L;

        UsuarioHogar adminMembership = UsuarioHogar.builder()
            .rol(UsuarioHogar.ROL_ADMINISTRADOR)
            .build();

        when(usuarioRepo.findByEmail("juan@example.com")).thenReturn(Optional.of(usuarioCreador));
        when(usuarioHogarRepo.findByIdUsuarioIdAndIdHogarId(1L, hogarId))
            .thenReturn(Optional.of(adminMembership));
        when(hogarRepo.findById(hogarId)).thenReturn(Optional.of(hogarTest));
        when(usuarioRepo.findByEmail("maria@example.com")).thenReturn(Optional.empty());
        when(invitacionRepo.existsByEmailInvitadoAndHogarHogarIdAndEstado(
                "maria@example.com", hogarId, "Pendiente")).thenReturn(true);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> hogarService.invitarMiembro(hogarId, request, "juan@example.com"));

        verify(invitacionRepo, never()).save(any());
    }

    // ==================== Tests para responderInvitacion ====================

    @Test
    @DisplayName("Debería aceptar una invitación exitosamente")
    void testResponderInvitacionAceptar() {
        // Arrange
        ResponderInvitacionRequest request = new ResponderInvitacionRequest("ACEPTAR");
        String token = invitacionTest.getToken();

        when(invitacionRepo.findByToken(token)).thenReturn(Optional.of(invitacionTest));
        when(usuarioRepo.findByEmail("maria@example.com")).thenReturn(Optional.of(usuarioInvitado));
        when(usuarioHogarRepo.save(any(UsuarioHogar.class))).thenReturn(null);
        when(invitacionRepo.save(any(InvitacionHogar.class))).thenReturn(invitacionTest);

        // Act
        InvitacionResponse resultado = hogarService.responderInvitacion(token, request, "maria@example.com");

        // Assert
        assertNotNull(resultado);
        assertEquals("Aceptada", resultado.estado());

        ArgumentCaptor<UsuarioHogar> captor = ArgumentCaptor.forClass(UsuarioHogar.class);
        verify(usuarioHogarRepo).save(captor.capture());
        UsuarioHogar saved = captor.getValue();
        assertEquals(UsuarioHogar.ROL_MIEMBRO, saved.getRol());
    }


    @Test
    @DisplayName("Debería lanzar excepción cuando el token es inválido")
    void testResponderInvitacionTokenInvalido() {
        // Arrange
        ResponderInvitacionRequest request = new ResponderInvitacionRequest("ACEPTAR");
        when(invitacionRepo.findByToken("token_invalido")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            hogarService.responderInvitacion("token_invalido", request, "maria@example.com"));

        verify(invitacionRepo, never()).save(any());
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando la invitación ya fue procesada")
    void testResponderInvitacionYaProcesada() {
        // Arrange
        ResponderInvitacionRequest request = new ResponderInvitacionRequest("ACEPTAR");
        String token = invitacionTest.getToken();

        invitacionTest.setEstado("Aceptada");

        when(invitacionRepo.findByToken(token)).thenReturn(Optional.of(invitacionTest));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            hogarService.responderInvitacion(token, request, "maria@example.com"));

        verify(invitacionRepo, never()).save(any());
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando la invitación ha expirado")
    void testResponderInvitacionExpirada() {
        // Arrange
        ResponderInvitacionRequest request = new ResponderInvitacionRequest("ACEPTAR");
        String token = invitacionTest.getToken();

        invitacionTest.setFechaExpiracion(LocalDateTime.now().minusHours(1));

        when(invitacionRepo.findByToken(token)).thenReturn(Optional.of(invitacionTest));
        when(invitacionRepo.save(any(InvitacionHogar.class))).thenReturn(invitacionTest);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            hogarService.responderInvitacion(token, request, "maria@example.com"));
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando el respondedor no es el invitado")
    void testResponderInvitacionEmailNoCoincide() {
        // Arrange
        ResponderInvitacionRequest request = new ResponderInvitacionRequest("ACEPTAR");
        String token = invitacionTest.getToken();

        when(invitacionRepo.findByToken(token)).thenReturn(Optional.of(invitacionTest));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            hogarService.responderInvitacion(token, request, "otro@example.com"));

        verify(invitacionRepo, never()).save(any());
    }

    @Test
    @DisplayName("Debería lanzar excepción para acción no válida")
    void testResponderInvitacionAccionInvalida() {
        // Arrange
        ResponderInvitacionRequest request = new ResponderInvitacionRequest("INVALIDA");
        String token = invitacionTest.getToken();

        when(invitacionRepo.findByToken(token)).thenReturn(Optional.of(invitacionTest));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            hogarService.responderInvitacion(token, request, "maria@example.com"));

        verify(invitacionRepo, never()).save(any());
    }

    // ==================== Tests para listarMiembros ====================

    @Test
    @DisplayName("Debería listar miembros exitosamente")
    void testListarMiembrosExito() {
        // Arrange
        Long hogarId = 1L;

        UsuarioHogar miembro1 = UsuarioHogar.builder()
            .usuario(usuarioCreador)
            .rol(UsuarioHogar.ROL_ADMINISTRADOR)
            .fechaUnion(LocalDateTime.now())
            .build();

        UsuarioHogar miembro2 = UsuarioHogar.builder()
            .usuario(usuarioInvitado)
            .rol(UsuarioHogar.ROL_MIEMBRO)
            .fechaUnion(LocalDateTime.now())
            .build();

        when(usuarioRepo.findByEmail("juan@example.com")).thenReturn(Optional.of(usuarioCreador));
        when(usuarioHogarRepo.existsByIdUsuarioIdAndIdHogarId(1L, hogarId)).thenReturn(true);
        when(usuarioHogarRepo.findByIdHogarId(hogarId)).thenReturn(List.of(miembro1, miembro2));

        // Act
        List<MiembroResponse> resultado = hogarService.listarMiembros(hogarId, "juan@example.com");

        // Assert
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals("juan@example.com", resultado.get(0).email());
        assertEquals("maria@example.com", resultado.get(1).email());

        verify(usuarioRepo, times(1)).findByEmail("juan@example.com");
        verify(usuarioHogarRepo, times(1)).findByIdHogarId(hogarId);
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando el usuario no tiene acceso al hogar")
    void testListarMiembrosNoTieneAcceso() {
        // Arrange
        Long hogarId = 1L;

        when(usuarioRepo.findByEmail("juan@example.com")).thenReturn(Optional.of(usuarioCreador));
        when(usuarioHogarRepo.existsByIdUsuarioIdAndIdHogarId(1L, hogarId)).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            hogarService.listarMiembros(hogarId, "juan@example.com"));

        verify(usuarioHogarRepo, never()).findByIdHogarId(hogarId);
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando el usuario no existe")
    void testListarMiembrosUsuarioNoExiste() {
        // Arrange
        Long hogarId = 1L;

        when(usuarioRepo.findByEmail("noexiste@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            hogarService.listarMiembros(hogarId, "noexiste@example.com"));

        verify(usuarioHogarRepo, never()).findByIdHogarId(hogarId);
    }

    @Test
    @DisplayName("Debería retornar lista vacía cuando no hay miembros")
    void testListarMiembrosListaVacia() {
        // Arrange
        Long hogarId = 1L;

        when(usuarioRepo.findByEmail("juan@example.com")).thenReturn(Optional.of(usuarioCreador));
        when(usuarioHogarRepo.existsByIdUsuarioIdAndIdHogarId(1L, hogarId)).thenReturn(true);
        when(usuarioHogarRepo.findByIdHogarId(hogarId)).thenReturn(List.of());

        // Act
        List<MiembroResponse> resultado = hogarService.listarMiembros(hogarId, "juan@example.com");

        // Assert
        assertNotNull(resultado);
        assertEquals(0, resultado.size());
    }

    // ==================== Tests para validaciones privadas ====================

    @Test
    @DisplayName("Debería validar administrador correctamente")
    void testValidarAdminCorrectamente() {
        // Este test se ejecuta implícitamente en otros tests que usan validarEsAdministrador
        Long hogarId = 1L;

        UsuarioHogar adminMembership = UsuarioHogar.builder()
            .rol(UsuarioHogar.ROL_ADMINISTRADOR)
            .build();

        when(usuarioRepo.findByEmail("juan@example.com")).thenReturn(Optional.of(usuarioCreador));
        when(usuarioHogarRepo.findByIdUsuarioIdAndIdHogarId(1L, hogarId))
            .thenReturn(Optional.of(adminMembership));
        when(hogarRepo.findById(hogarId)).thenReturn(Optional.of(hogarTest));
        when(invitacionRepo.existsByEmailInvitadoAndHogarHogarIdAndEstado(anyString(), anyLong(), anyString()))
            .thenReturn(false);
        when(invitacionRepo.save(any(InvitacionHogar.class))).thenReturn(invitacionTest);

        InvitarMiembroRequest request = new InvitarMiembroRequest("test@example.com");

        // Esta llamada debería pasar sin lanzar excepciones en las validaciones de admin
        InvitacionResponse resultado = hogarService.invitarMiembro(hogarId, request, "juan@example.com");

        assertNotNull(resultado);
        verify(usuarioHogarRepo).findByIdUsuarioIdAndIdHogarId(1L, hogarId);
    }
}
