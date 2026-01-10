package com.alejoacevedodev.wipedatabeta.utils

/**
 * Copyright 2026 Wireless&Mobile, Inc. All rights reserved
 * File GetAppVersion 1.2.5 9/01/26
 *
 * @author Carlos Alejandro Acevedo Espitia
 * @version 1.2.5 9/01/26
 * @since 1.2.5
 */

import android.content.Context
import android.content.pm.PackageManager

fun getAppVersion(context: Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "1.0.0"
    } catch (e: PackageManager.NameNotFoundException) {
        "N/A"
    }
}