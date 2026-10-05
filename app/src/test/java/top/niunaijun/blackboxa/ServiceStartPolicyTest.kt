package top.niunaijun.blackboxa

import android.content.pm.ServiceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import top.niunaijun.blackbox.core.system.am.ServiceStartPolicy

class ServiceStartPolicyTest {

    @Test
    fun foregroundRequestUsesForegroundStartFromAndroidO() {
        assertTrue(ServiceStartPolicy.shouldUseForegroundService(true, 26))
        assertTrue(ServiceStartPolicy.shouldUseForegroundService(true, 34))
    }

    @Test
    fun ordinaryServiceAndPreOForegroundRequestStayOnOrdinaryStartPath() {
        assertFalse(ServiceStartPolicy.shouldUseForegroundService(false, 34))
        assertFalse(ServiceStartPolicy.shouldUseForegroundService(true, 25))
    }

    @Test
    fun preAndroid11BridgeKeepsMicrophoneButNotCameraType() {
        val mixed = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
        assertEquals(
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            ServiceStartPolicy.supportedForegroundServiceType(mixed, 29)
        )
    }

    @Test
    fun android11PlusBridgePreservesMicrophoneAndCameraTypes() {
        val mixed = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
        assertEquals(
            mixed,
            ServiceStartPolicy.supportedForegroundServiceType(mixed, 30)
        )
        assertEquals(
            mixed,
            ServiceStartPolicy.supportedForegroundServiceType(mixed, 34)
        )
    }
}
