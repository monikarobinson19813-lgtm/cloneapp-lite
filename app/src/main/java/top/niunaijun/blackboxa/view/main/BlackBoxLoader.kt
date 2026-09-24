package top.niunaijun.blackboxa.view.main

import android.app.Application
import android.content.Context
import android.util.Log
import java.io.File
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.app.BActivityThread
import top.niunaijun.blackbox.app.configuration.AppLifecycleCallback
import top.niunaijun.blackbox.app.configuration.ClientConfiguration
import top.niunaijun.blackboxa.app.App
import top.niunaijun.blackboxa.biz.cache.AppSharedPreferenceDelegate


class BlackBoxLoader {

    private var mDaemonEnable by AppSharedPreferenceDelegate(App.getContext(), false)
    fun daemonEnable(): Boolean {
        return try {
            mDaemonEnable
        } catch (e: Exception) {
            Log.e(TAG, "Error getting daemonEnable: ${e.message}")
            false
        }
    }

    fun invalidDaemonEnable(enable: Boolean) {
        try {
            this.mDaemonEnable = enable
        } catch (e: Exception) {
            Log.e(TAG, "Error setting daemonEnable: ${e.message}")
        }
    }

    fun getBlackBoxCore(): BlackBoxCore {
        return try {
            BlackBoxCore.get()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting BlackBoxCore: ${e.message}")
            throw e
        }
    }

    fun addLifecycleCallback() {
        try {
            BlackBoxCore.get()
                    .addAppLifecycleCallback(
                            object : AppLifecycleCallback() {
                                override fun beforeCreateApplication(
                                        packageName: String?,
                                        processName: String?,
                                        context: Context?,
                                        userId: Int
                                ) {
                                    try {
                                        Log.d(
                                                TAG,
                                                "beforeCreateApplication: pkg $packageName, processName $processName,userID:${BActivityThread.getUserId()}"
                                        )
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error in beforeCreateApplication: ${e.message}")
                                    }
                                }

                                override fun beforeApplicationOnCreate(
                                        packageName: String?,
                                        processName: String?,
                                        application: Application?,
                                        userId: Int
                                ) {
                                    try {
                                        Log.d(
                                                TAG,
                                                "beforeApplicationOnCreate: pkg $packageName, processName $processName"
                                        )
                                    } catch (e: Exception) {
                                        Log.e(
                                                TAG,
                                                "Error in beforeApplicationOnCreate: ${e.message}"
                                        )
                                    }
                                }

                                override fun afterApplicationOnCreate(
                                        packageName: String?,
                                        processName: String?,
                                        application: Application?,
                                        userId: Int
                                ) {
                                    try {
                                        Log.d(
                                                TAG,
                                                "afterApplicationOnCreate: pkg $packageName, processName $processName"
                                        )
                                    } catch (e: Exception) {
                                        Log.e(
                                                TAG,
                                                "Error in afterApplicationOnCreate: ${e.message}"
                                        )
                                    }
                                }

                                override fun onStoragePermissionNeeded(
                                        packageName: String?,
                                        userId: Int
                                ): Boolean {
                                    try {
                                        Log.w(
                                                TAG,
                                                "Storage permission needed for launching: $packageName"
                                        )
                                        
                                        
                                        
                                        val intent =
                                                android.content.Intent(
                                                        "top.niunaijun.blackboxa.REQUEST_STORAGE_PERMISSION"
                                                )
                                        intent.putExtra("package_name", packageName)
                                        intent.putExtra("user_id", userId)
                                        intent.setPackage(App.getContext().packageName)
                                        App.getContext().sendBroadcast(intent)
                                        
                                        
                                        
                                        
                                        
                                        return false
                                    } catch (e: Exception) {
                                        Log.e(
                                                TAG,
                                                "Error in onStoragePermissionNeeded: ${e.message}"
                                        )
                                        return false
                                    }
                                }
                            }
                    )
        } catch (e: Exception) {
            Log.e(TAG, "Error adding lifecycle callback: ${e.message}")
        }
    }

    fun attachBaseContext(context: Context) {
        try {
            BlackBoxCore.get()
                    .doAttachBaseContext(
                            context,
                            object : ClientConfiguration() {
                                override fun getHostPackageName(): String {
                                    return try {
                                        context.packageName
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error getting package name: ${e.message}")
                                        "unknown"
                                    }
                                }

                                override fun isEnableDaemonService(): Boolean {
                                    return try {
                                        mDaemonEnable
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error checking daemonEnable: ${e.message}")
                                        false
                                    }
                                }
                                override fun requestInstallPackage(
                                        file: File?,
                                        userId: Int
                                ): Boolean {
                                    return try {
                                        if (file == null) {
                                            Log.w(TAG, "requestInstallPackage: file is null")
                                            return false
                                        }
                                        val packageInfo =
                                                context.packageManager.getPackageArchiveInfo(
                                                        file.absolutePath,
                                                        0
                                                )
                                        false
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error in requestInstallPackage: ${e.message}")
                                        false
                                    }
                                }
                            }
                    )
        } catch (e: Exception) {
            Log.e(TAG, "Error in attachBaseContext: ${e.message}")
        }
    }

    fun doOnCreate(context: Context) {
        try {
            BlackBoxCore.get().doCreate()

            
            try {
                BlackBoxCore.get().addServiceAvailableCallback {
                    Log.d(TAG, "Services became available, triggering app list refresh")
                    
                    
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error registering service available callback: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in doOnCreate: ${e.message}")
        }
    }

    companion object {
        val TAG: String = BlackBoxLoader::class.java.simpleName
    }
}
