package com.diabeat.utils

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.Mockito
import org.mockito.Mockito.clearInvocations

@RunWith(MockitoJUnitRunner::class)
class TokenManagerTest {

    @Mock
    private lateinit var mockContext: Context
    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    private val TEST_TOKEN = "test_access_token"
    private val TEST_USER_ID = "test_user_id"

    @Before
    fun setup() {
        `when`(mockContext.applicationContext).thenReturn(mockContext)
        `when`(mockContext.getSharedPreferences("diabeat_prefs", Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.remove(anyString())).thenReturn(mockEditor)
        TokenManager.clearToken(mockContext)
        clearInvocations(mockSharedPreferences, mockEditor)
    }

    @Test
    fun testSaveToken() {
        TokenManager.saveToken(mockContext, TEST_TOKEN)
        verify(mockEditor).putString("access_token", TEST_TOKEN)
        verify(mockEditor).apply()
    }

    @Test
    fun testGetToken() {
        `when`(mockSharedPreferences.getString("access_token", null)).thenReturn(TEST_TOKEN)
        val retrievedToken = TokenManager.getToken(mockContext)
        assertEquals(TEST_TOKEN, retrievedToken)
    }

    @Test
    fun testClearToken() {
        TokenManager.clearToken(mockContext)
        verify(mockEditor).remove("access_token")
        verify(mockEditor).remove("user_id")
        verify(mockEditor).apply()
    }

    @Test
    fun testSaveUserId() {
        TokenManager.saveUserId(mockContext, TEST_USER_ID)
        verify(mockEditor).putString("user_id", TEST_USER_ID)
        verify(mockEditor).apply()
    }

    @Test
    fun testGetUserId() {
        `when`(mockSharedPreferences.getString("user_id", null)).thenReturn(TEST_USER_ID)
        val retrievedUserId = TokenManager.getUserId(mockContext)
        assertEquals(TEST_USER_ID, retrievedUserId)
    }

    @Test
    fun testGetToken_returnsNullWhenNotSaved() {
        `when`(mockSharedPreferences.getString("access_token", null)).thenReturn(null)
        val retrievedToken = TokenManager.getToken(mockContext)
        assertNull(retrievedToken)
    }

    @Test
    fun testGetUserId_returnsNullWhenNotSaved() {
        `when`(mockSharedPreferences.getString("user_id", null)).thenReturn(null)
        val retrievedUserId = TokenManager.getUserId(mockContext)
        assertNull(retrievedUserId)
    }

    // Helper function for Mockito's anyString()
    private fun <T> anyString(): T = Mockito.anyString() as T
}
