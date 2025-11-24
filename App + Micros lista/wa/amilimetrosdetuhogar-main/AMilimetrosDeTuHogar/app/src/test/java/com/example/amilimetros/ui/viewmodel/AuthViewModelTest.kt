package com.example.amilimetros.ui.viewmodel

import com.example.amilimetros.data.local.storage.UserPreferences
import com.example.amilimetros.data.remote.dto.LoginRequest
import com.example.amilimetros.data.remote.dto.RegisterRequest
import com.example.amilimetros.data.remote.dto.UsuarioResponse
import com.example.amilimetros.data.repository.AuthApiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

@ExperimentalCoroutinesApi
class AuthViewModelTest {

    @Mock
    private lateinit var repository: AuthApiRepository

    @Mock
    private lateinit var userPreferences: UserPreferences

    private lateinit var viewModel: AuthViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = AuthViewModel(repository, userPreferences)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loginExitosoActualizaEstadoASuccess() = runTest {
        val email = "test@test.com"
        val password = "123456"
        val usuarioResponse = UsuarioResponse(
            id = 1L,
            nombre = "Test User",
            email = email,
            telefono = "123456789",
            isAdmin = false
        )

        `when`(repository.login(any())).thenReturn(Result.success(usuarioResponse))
        `when`(userPreferences.saveUser(anyLong(), anyString(), anyString(), anyString(), anyBoolean()))
            .thenReturn(Unit)

        viewModel.login(email, password)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.loginState.value is LoginState.Success)
        verify(repository).login(any())
        verify(userPreferences).saveUser(
            eq(1L),
            eq("Test User"),
            eq(email),
            eq("123456789"),
            eq(false)
        )
    }

    @Test
    fun loginFallidoActualizaEstadoAError() = runTest {
        val email = "wrong@test.com"
        val password = "wrong"
        val errorMessage = "Credenciales incorrectas"

        `when`(repository.login(any())).thenReturn(Result.failure(Exception(errorMessage)))

        viewModel.login(email, password)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.loginState.value is LoginState.Error)
        assertEquals(errorMessage, (viewModel.loginState.value as LoginState.Error).message)
    }

    @Test
    fun loginMuestraLoadingDuranteProceso() = runTest {
        val email = "test@test.com"
        val password = "123456"

        `when`(repository.login(any())).thenAnswer {
            Thread.sleep(100)
            Result.success(UsuarioResponse(1L, "Test", email, "123", false))
        }

        viewModel.login(email, password)

        assertTrue(viewModel.loginState.value is LoginState.Loading)

        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.loginState.value is LoginState.Success)
    }

    @Test
    fun registerExitosoActualizaEstadoASuccess() = runTest {
        val nombre = "Test User"
        val email = "new@test.com"
        val telefono = "123456789"
        val password = "123456"

        `when`(repository.register(any())).thenReturn(Result.success(Unit))

        viewModel.register(nombre, email, telefono, password)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.registerState.value is RegisterState.Success)
        verify(repository).register(any())
    }

    @Test
    fun registerFallidoActualizaEstadoAError() = runTest {
        val nombre = "Test User"
        val email = "existing@test.com"
        val telefono = "123456789"
        val password = "123456"
        val errorMessage = "El correo ya está registrado"

        `when`(repository.register(any())).thenReturn(Result.failure(Exception(errorMessage)))

        viewModel.register(nombre, email, telefono, password)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.registerState.value is RegisterState.Error)
        assertEquals(errorMessage, (viewModel.registerState.value as RegisterState.Error).message)
    }

    @Test
    fun logoutLimpiaDatosDelUsuario() = runTest {
        `when`(userPreferences.clearUser()).thenReturn(Unit)

        viewModel.logout()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(userPreferences).clearUser()
        assertNull(viewModel.currentUser.value)
        assertTrue(viewModel.loginState.value is LoginState.Idle)
    }

    @Test
    fun resetStatesReiniciaEstadosAIdle() {
        viewModel.resetStates()

        assertTrue(viewModel.loginState.value is LoginState.Idle)
        assertTrue(viewModel.registerState.value is RegisterState.Idle)
    }
}