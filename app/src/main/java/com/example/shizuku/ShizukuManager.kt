package com.example.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

enum class ShizukuState {
    NOT_INSTALLED,
    NOT_RUNNING,
    PERMISSION_REQUIRED,
    READY_SHELL,
    READY_ROOT
}

data class CommandResult(
    val success: Boolean,
    val output: String,
    val exitCode: Int
)

class ShizukuManager(private val context: Context) {

    private val tag = "ShizukuManager"

    private val _shizukuState = MutableStateFlow(ShizukuState.NOT_RUNNING)
    val shizukuState: StateFlow<ShizukuState> = _shizukuState.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkState()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _shizukuState.value = ShizukuState.NOT_RUNNING
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            checkState()
        } else {
            _shizukuState.value = ShizukuState.PERMISSION_REQUIRED
        }
    }

    fun init() {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
            checkState()
        } catch (e: Exception) {
            Log.w(tag, "Failed to register Shizuku listeners: ${e.message}")
            _shizukuState.value = ShizukuState.NOT_RUNNING
        }
    }

    fun destroy() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        } catch (_: Exception) { }
    }

    fun checkState(): ShizukuState {
        val state = try {
            val isInstalled = isShizukuInstalled()
            if (!isInstalled) {
                ShizukuState.NOT_INSTALLED
            } else if (!Shizuku.pingBinder()) {
                ShizukuState.NOT_RUNNING
            } else if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                ShizukuState.PERMISSION_REQUIRED
            } else {
                val uid = Shizuku.getUid()
                if (uid == 0) ShizukuState.READY_ROOT else ShizukuState.READY_SHELL
            }
        } catch (e: Exception) {
            Log.w(tag, "Error checking Shizuku state: ${e.message}")
            ShizukuState.NOT_RUNNING
        }
        _shizukuState.value = state
        return state
    }

    fun requestPermission(requestCode: Int = 1001) {
        try {
            if (Shizuku.pingBinder()) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to request Shizuku permission", e)
        }
    }

    private fun isShizukuInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    suspend fun executePrivileged(command: String): CommandResult = withContext(Dispatchers.IO) {
        if (_shizukuState.value != ShizukuState.READY_SHELL && _shizukuState.value != ShizukuState.READY_ROOT) {
            return@withContext CommandResult(false, "Shizuku not ready (state: ${_shizukuState.value})", -1)
        }

        try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).apply { isAccessible = true }

            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            while (errorReader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            val exitCode = process.waitFor()
            reader.close()
            errorReader.close()
            CommandResult(exitCode == 0, output.toString().trim(), exitCode)
        } catch (e: Exception) {
            Log.e(tag, "Command execution failed: $command", e)
            CommandResult(false, e.message ?: "Execution error", -1)
        }
    }

    // High-level safe privileged actions
    suspend fun trimCaches(): CommandResult {
        return executePrivileged("pm trim-caches 4096M")
    }

    suspend fun forceStopApp(packageName: String): CommandResult {
        // Protect essential system packages
        if (packageName in listOf("com.android.systemui", "com.google.android.gms", context.packageName)) {
            return CommandResult(false, "Protected package cannot be stopped", -1)
        }
        return executePrivileged("am force-stop $packageName")
    }

    suspend fun setGameMode(packageName: String, mode: String): CommandResult {
        val modeArg = when (mode.lowercase()) {
            "performance", "turbo", "turbo++" -> "2"
            "battery", "battery gaming" -> "3"
            "balanced" -> "1"
            else -> "0"
        }
        return executePrivileged("cmd game mode $modeArg $packageName")
    }

    suspend fun restrictBackgroundApp(packageName: String): CommandResult {
        return executePrivileged("cmd appops set $packageName RUN_IN_BACKGROUND ignore")
    }

    suspend fun unrestrictBackgroundApp(packageName: String): CommandResult {
        return executePrivileged("cmd appops set $packageName RUN_IN_BACKGROUND allow")
    }
}
