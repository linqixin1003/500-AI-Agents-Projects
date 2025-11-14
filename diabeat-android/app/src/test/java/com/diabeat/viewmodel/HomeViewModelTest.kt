package com.diabeat.viewmodel

import android.app.Application
import android.content.Context
import com.diabeat.data.model.DeviceAuthRequest
import com.diabeat.data.model.LoginResponse
import com.diabeat.data.model.MealRecordResponse
import com.diabeat.data.model.UserResponse
import com.diabeat.network.ApiService
import com.diabeat.network.RetrofitClient
import com.diabeat.utils.DeviceIdUtil
import com.diabeat.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import retrofit2.Response
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class HomeViewModelTest {

    @Mock
    private lateinit var mockApplication: Application

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockApiService: ApiService

    private lateinit var homeViewModel: HomeViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock Application context
        `when`(mockApplication.applicationContext).thenReturn(mockContext)

        // Mock DeviceIdUtil (static object)
        mockStatic(DeviceIdUtil::class.java).use {
            `when`<String> { DeviceIdUtil.getDeviceId() }.thenReturn("test_device_id")
        }

        // Mock TokenManager (static object)
        mockStatic(TokenManager::class.java).use {
            // Default: no token/userId saved
            `when`<String?> { TokenManager.getToken(mockContext) }.thenReturn(null)
            `when`<String?> { TokenManager.getUserId(mockContext) }.thenReturn(null)
            doNothing().`when`<Any> { TokenManager.saveToken(eq(mockContext), anyString()) }
            doNothing().`when`<Any> { TokenManager.saveUserId(eq(mockContext), anyString()) }
            doNothing().`when`<Any> { TokenManager.clearToken(mockContext) }
        }


        // Inject mock ApiService into RetrofitClient
        RetrofitClient.setTestingApiService(mockApiService)

        homeViewModel = HomeViewModel(mockApplication)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `performDeviceAuth success updates state and saves token`() = runTest {
        val userId = UUID.randomUUID().toString()
        val userResponse = UserResponse(
            id = userId,
            device_id = "test_device_id",
            email = "test@example.com",
            name = "Test User",
            diabetes_type = "type1",
            created_at = "2023-01-01T00:00:00Z"
        )
        val loginResponse = LoginResponse(
            access_token = "new_test_token",
            token_type = "bearer",
            user = userResponse
        )
        `when`(mockApiService.deviceAuth(any(DeviceAuthRequest::class.java)))
            .thenReturn(Response.success(loginResponse))
        `when`(mockApiService.getMealRecords(anyString(), anyInt(), anyInt())).thenReturn(Response.success(emptyList()))
        `when`(mockApiService.getInsulinRecords(anyString(), anyInt(), anyInt())).thenReturn(Response.success(emptyList()))

        homeViewModel.performDeviceAuth()
        testDispatcher.scheduler.advanceUntilIdle() // Advance past the fetchRecordsForDate call

        assertTrue(homeViewModel.isAuthenticated.first())
        assertEquals(userResponse, homeViewModel.user.first())
        assertNull(homeViewModel.errorRecords.first())

        // Verify that token and user ID were saved using mockStatic
        mockStatic(TokenManager::class.java).use { mockedStatic ->
            mockedStatic.verify { TokenManager.saveToken(mockContext, "new_test_token") }
            mockedStatic.verify { TokenManager.saveUserId(mockContext, userId) }
        }
    }

    @Test
    fun `performDeviceAuth failure clears state and sets error`() = runTest {
        `when`(mockApiService.deviceAuth(any(DeviceAuthRequest::class.java)))
            .thenReturn(Response.error(400, "{\"detail\":\"Bad Request\"}".toResponseBody("application/json".toMediaTypeOrNull())))

        homeViewModel.performDeviceAuth()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(homeViewModel.isAuthenticated.first())
        assertNull(homeViewModel.user.first())
        assertNotNull(homeViewModel.errorRecords.first())
        assertTrue(homeViewModel.errorRecords.first()!!.contains("设备认证失败"))

        mockStatic(TokenManager::class.java).use { mockedStatic ->
            mockedStatic.verify { TokenManager.clearToken(mockContext) }
        }
    }

    @Test
    fun `fetchRecordsForDate loads data when authenticated`() = runTest {
        // First, simulate successful authentication
        val userId = UUID.randomUUID().toString()
        val userResponse = UserResponse(
            id = userId,
            device_id = "test_device_id",
            email = "test@example.com",
            name = "Test User",
            diabetes_type = "type1",
            created_at = "2023-01-01T00:00:00Z"
        )
        val loginResponse = LoginResponse(
            access_token = "authenticated_token",
            token_type = "bearer",
            user = userResponse
        )
        `when`(mockApiService.deviceAuth(any(DeviceAuthRequest::class.java)))
            .thenReturn(Response.success(loginResponse))

        // Mock token and user ID saving
        mockStatic(TokenManager::class.java).use { mockedStatic ->
            mockedStatic.`when`<String?> { TokenManager.getToken(mockContext) }.thenReturn("authenticated_token")
            mockedStatic.`when`<String?> { TokenManager.getUserId(mockContext) }.thenReturn(userId)
        }

        // Re-initialize ViewModel to trigger init block with mocked TokenManager
        homeViewModel = HomeViewModel(mockApplication)
        testDispatcher.scheduler.advanceUntilIdle() // Advance past initial performDeviceAuth

        assertTrue(homeViewModel.isAuthenticated.first())

        val testDate = LocalDate.of(2023, 1, 15)
        val mealRecords = listOf(MealRecordResponse(
            id = "meal1", user_id = userId, meal_time = "2023-01-15T12:00:00Z", food_items = emptyList()
        ))
        val insulinRecords = listOf(InsulinRecordResponse(
            id = "insulin1", user_id = userId, injection_time = "2023-01-15T11:45:00Z", actual_dose = 5.0f
        ))

        `when`(mockApiService.getMealRecords(eq(testDate.format(DateTimeFormatter.ISO_LOCAL_DATE)), anyInt(), anyInt()))
            .thenReturn(Response.success(mealRecords))
        `when`(mockApiService.getInsulinRecords(eq(testDate.format(DateTimeFormatter.ISO_LOCAL_DATE)), anyInt(), anyInt()))
            .thenReturn(Response.success(insulinRecords))

        homeViewModel.fetchRecordsForDate(testDate)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(mealRecords, homeViewModel.mealRecords.first())
        assertEquals(insulinRecords, homeViewModel.insulinRecords.first())
        assertFalse(homeViewModel.isLoadingRecords.first())
        assertNull(homeViewModel.errorRecords.first())
    }

    @Test
    fun `fetchRecordsForDate does not load data when unauthenticated`() = runTest {
        // Ensure initial state is unauthenticated
        assertFalse(homeViewModel.isAuthenticated.first())

        val testDate = LocalDate.of(2023, 1, 15)
        homeViewModel.fetchRecordsForDate(testDate)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("用户未认证，无法加载数据。", homeViewModel.errorRecords.first())
        assertFalse(homeViewModel.isLoadingRecords.first())
        verify(mockApiService, never()).getMealRecords(anyString(), anyInt(), anyInt())
        verify(mockApiService, never()).getInsulinRecords(anyString(), anyInt(), anyInt())
    }

    @Test
    fun `fetchRecordsForDate handles API error`() = runTest {
        // First, simulate successful authentication
        val userId = UUID.randomUUID().toString()
        val userResponse = UserResponse(
            id = userId,
            device_id = "test_device_id",
            email = "test@example.com",
            name = "Test User",
            diabetes_type = "type1",
            created_at = "2023-01-01T00:00:00Z"
        )
        val loginResponse = LoginResponse(
            access_token = "authenticated_token",
            token_type = "bearer",
            user = userResponse
        )
        `when`(mockApiService.deviceAuth(any(DeviceAuthRequest::class.java)))
            .thenReturn(Response.success(loginResponse))

        homeViewModel = HomeViewModel(mockApplication)
        testDispatcher.scheduler.advanceUntilIdle() // Advance past initial performDeviceAuth

        assertTrue(homeViewModel.isAuthenticated.first())

        val testDate = LocalDate.of(2023, 1, 15)
        `when`(mockApiService.getMealRecords(eq(testDate.format(DateTimeFormatter.ISO_LOCAL_DATE)), anyInt(), anyInt()))
            .thenReturn(Response.error(500, "{\"detail\":\"Internal Server Error\"}".toResponseBody("application/json".toMediaTypeOrNull())))
        `when`(mockApiService.getInsulinRecords(eq(testDate.format(DateTimeFormatter.ISO_LOCAL_DATE)), anyInt(), anyInt()))
            .thenReturn(Response.success(emptyList())) // One success, one failure

        homeViewModel.fetchRecordsForDate(testDate)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(homeViewModel.errorRecords.first())
        assertTrue(homeViewModel.errorRecords.first()!!.contains("服务器错误"))
        assertFalse(homeViewModel.isLoadingRecords.first())
    }
}

// Helper function for Mockito.any() with nullable types
fun <T> any(type: Class<T>): T = Mockito.any(type)
