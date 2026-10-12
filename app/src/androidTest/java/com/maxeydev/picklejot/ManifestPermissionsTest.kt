package com.maxeydev.picklejot

import android.Manifest
import android.content.pm.FeatureInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManifestPermissionsTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val packageManager = context.packageManager

    private val requested: List<String> =
        packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            ?.toList()
            .orEmpty()

    private fun isRuntimePermission(permission: String): Boolean =
        try {
            val info = packageManager.getPermissionInfo(permission, 0)
            info.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE == PermissionInfo.PROTECTION_DANGEROUS
        } catch (unknown: PackageManager.NameNotFoundException) {
            false
        }

    @Test
    fun read_media_images_is_never_declared() {
        assertFalse(requested.contains("android.permission.READ_MEDIA_IMAGES"))
        assertFalse(requested.contains("android.permission.READ_MEDIA_VISUAL_USER_SELECTED"))
        assertFalse(requested.contains(Manifest.permission.READ_EXTERNAL_STORAGE))
    }

    @Test
    fun the_only_runtime_permissions_are_camera_and_notifications() {
        val allowed = setOf(Manifest.permission.CAMERA, "android.permission.POST_NOTIFICATIONS")

        val runtime = requested.filter(::isRuntimePermission)

        assertTrue("unexpected runtime permissions: ${runtime - allowed}", allowed.containsAll(runtime))
        assertTrue(runtime.contains(Manifest.permission.CAMERA))
    }

    @Test
    fun the_camera_is_optional_hardware() {
        val features =
            packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_CONFIGURATIONS)
                .reqFeatures
                .orEmpty()
        listOf("android.hardware.camera", "android.hardware.camera.any").forEach { name ->
            val camera = features.firstOrNull { it.name == name }
            assertTrue("$name is not declared", camera != null)
            assertFalse("$name is required", camera!!.flags and FeatureInfo.FLAG_REQUIRED != 0)
        }
    }
}
