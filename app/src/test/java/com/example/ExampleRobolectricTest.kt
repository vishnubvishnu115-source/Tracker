package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.network.ZohoTrackerApi
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TrackPulse", appName)
    }

    @Test
    fun `parseZohoPayload unwraps stringified result json`() {
        val raw = """{"code":3000,"result":"{\"status\":\"success\",\"user_id\":\"USR-42\",\"name\":\"Vishnu\"}"}"""
        val parsed = ZohoTrackerApi.parseZohoPayload(raw)
        assertEquals("success", parsed.optString("status"))
        assertEquals("USR-42", parsed.optString("user_id"))
        assertEquals("Vishnu", parsed.optString("name"))
    }

    @Test
    fun `parseZohoPayload handles direct json payload`() {
        val raw = """{"status":"error","message":"User inactive"}"""
        val parsed = ZohoTrackerApi.parseZohoPayload(raw)
        assertEquals("error", parsed.optString("status"))
        assertEquals("User inactive", parsed.optString("message"))
    }
}
