package com.diabeat.utils

import android.content.Context
import android.content.ContentResolver
import android.os.Build
import android.provider.Settings
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class DeviceIdUtilTest {

    @Mock
    private lateinit var mockContext: Context
    @Mock
    private lateinit var mockContentResolver: ContentResolver

    @Before
    fun setup() {
        // Mock Settings.Secure.ANDROID_ID
        `when`(mockContext.contentResolver).thenReturn(mockContentResolver)
        `when`(Settings.Secure.getString(mockContentResolver, Settings.Secure.ANDROID_ID))
            .thenReturn("test_android_id")

        // Mock Build properties (using reflection for final fields)
        setStaticField(Build::class.java, "MANUFACTURER", "test_manufacturer")
        setStaticField(Build::class.java, "MODEL", "test_model")

        // Ensure DeviceIdUtil is reset before each test
        setStaticField(DeviceIdUtil::class.java, "deviceId", null)

        DeviceIdUtil.init(mockContext)
    }

    @Test
    fun testGetDeviceId_returnsValidId() {
        val deviceId = DeviceIdUtil.getDeviceId()
        assertNotNull(deviceId)
        assertTrue(deviceId.isNotBlank())
        assertTrue(deviceId.length == 64) // SHA-256 hash is 64 characters long
    }

    @Test
    fun testGetDeviceId_returnsSameIdAfterMultipleCalls() {
        val deviceId1 = DeviceIdUtil.getDeviceId()
        val deviceId2 = DeviceIdUtil.getDeviceId()
        assertNotNull(deviceId1)
        assertTrue(deviceId1.isNotBlank())
        assertTrue(deviceId1 == deviceId2)
    }

    private fun setStaticField(clazz: Class<*>, fieldName: String, value: Any) {
        val field = clazz.getField(fieldName)
        field.isAccessible = true
        val modifiersField = java.lang.reflect.Field::class.java.getDeclaredField("modifiers")
        modifiersField.isAccessible = true
        modifiersField.setInt(field, field.modifiers and java.lang.reflect.Modifier.FINAL.inv())
        field.set(null, value)
    }
}
