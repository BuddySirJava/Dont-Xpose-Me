// SPDX-License-Identifier: Apache-2.0

package me.dontxpose.ui

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import kotlinx.coroutines.launch
import me.dontxpose.R
import me.dontxpose.config.ConfigStore
import me.dontxpose.config.DuressConfig
import me.dontxpose.config.PinHasher
import me.dontxpose.config.TestEvent
import me.dontxpose.config.TestEventLog
import me.dontxpose.status.StatusChecker
import me.dontxpose.status.SystemStatus
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    var status by remember { mutableStateOf<SystemStatus?>(null) }
    var testEvents by remember { mutableStateOf<List<TestEvent>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var pin by remember { mutableStateOf("") }
    var pinConfirm by remember { mutableStateOf("") }
    var showArmConfirm by remember { mutableStateOf(false) }
    var showAutoConfirmWarn by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    var showWarningBody by remember { mutableStateOf(false) }

    fun refresh() {
        scope.launch {
            loading = true
            status = StatusChecker.refresh(context)
            testEvents = if (status?.magiskRoot == true) TestEventLog.read() else emptyList()
            loading = false
        }
    }

    fun flash(text: String) {
        scope.launch { snackbar.showSnackbar(text) }
    }

    val lifecycleState by lifecycleOwner.lifecycle.currentStateAsState()
    LaunchedEffect(lifecycleState) {
        if (lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) {
            refresh()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Dont Xpose Me", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Duress PIN · Magisk / LSPosed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { refresh() }, enabled = !loading) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ModeBanner(status = status, loading = loading && status == null)

                NextStepCard(
                    status = status,
                    matchCount = testEvents.size,
                    loading = loading && status == null,
                )

                ChecklistCard(
                    status = status,
                    hasTestMatch = testEvents.isNotEmpty(),
                    expanded = showDetails,
                    onToggle = { showDetails = !showDetails },
                    loading = loading && status == null,
                )

                PinSection(
                    pin = pin,
                    pinConfirm = pinConfirm,
                    onPinChange = { value -> pin = value.filter { it.isDigit() } },
                    onConfirmChange = { value -> pinConfirm = value.filter { it.isDigit() } },
                    enabled = status?.magiskRoot == true && !loading,
                    pinAlreadySet = status?.pinSet == true,
                    onSave = {
                        scope.launch {
                            val err = validatePin(pin, pinConfirm)
                            if (err != null) {
                                flash(err)
                                return@launch
                            }
                            showAutoConfirmWarn = true
                        }
                    },
                )

                TestAttemptsSection(
                    events = testEvents,
                    enabled = status?.magiskRoot == true,
                    onClear = {
                        scope.launch {
                            val ok = TestEventLog.clear()
                            flash(if (ok) "Cleared test attempts" else "Failed to clear")
                            refresh()
                        }
                    },
                )

                ArmSection(
                    status = status,
                    hasTestMatch = testEvents.isNotEmpty(),
                    onArm = { showArmConfirm = true },
                    onDisarm = {
                        confirmDeviceCredential(context as? FragmentActivity) {
                            scope.launch {
                                val ok = ConfigStore.setArmed(false)
                                flash(if (ok) "Disarmed — back in test mode" else "Failed to disarm")
                                refresh()
                            }
                        }
                    },
                )

                WarningFooter(
                    expanded = showWarningBody,
                    onToggle = { showWarningBody = !showWarningBody },
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showAutoConfirmWarn) {
        AlertDialog(
            onDismissRequest = { showAutoConfirmWarn = false },
            title = { Text("Check auto-confirm") },
            text = {
                Text(
                    "Your duress PIN is ${pin.length} digits. If lock-screen auto-confirm is on and " +
                        "your real PIN is shorter, the lock screen may submit before you finish. " +
                        "Disable auto-confirm or keep the duress PIN no longer than your real PIN.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAutoConfirmWarn = false
                        scope.launch {
                            val ok = savePin(pin)
                            flash(if (ok) "PIN saved — still in test mode" else "Failed to write config")
                            if (ok) {
                                pin = ""
                                pinConfirm = ""
                                refresh()
                            }
                        }
                    },
                ) { Text("Save anyway") }
            },
            dismissButton = {
                TextButton(onClick = { showAutoConfirmWarn = false }) { Text("Cancel") }
            },
        )
    }

    if (showArmConfirm) {
        AlertDialog(
            onDismissRequest = { showArmConfirm = false },
            title = { Text("Arm irreversible wipe?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "The next matching duress PIN at the lock screen will destroy encryption keys, " +
                            "request a userdata wipe, and shut down. There is no recovery.",
                    )
                    if (testEvents.isEmpty()) {
                        Text(
                            "No test matches yet. Enter the duress PIN at the lock screen first.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showArmConfirm = false
                        confirmDeviceCredential(context as? FragmentActivity) {
                            scope.launch {
                                val s = StatusChecker.refresh(context)
                                status = s
                                if (!s.canArm) {
                                    flash("Refuse to arm: ${s.readyMessage}")
                                    return@launch
                                }
                                val cfg = ConfigStore.read()
                                if (cfg == null) {
                                    flash("No PIN configured")
                                    return@launch
                                }
                                val ok = ConfigStore.write(cfg.copy(armed = true, testMode = false))
                                flash(if (ok) "ARMED — duress PIN will wipe" else "Failed to arm")
                                refresh()
                            }
                        }
                    },
                ) { Text("Arm") }
            },
            dismissButton = {
                TextButton(onClick = { showArmConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ModeBanner(status: SystemStatus?, loading: Boolean) {
    val armed = status?.armed == true
    val bg = when {
        loading -> MaterialTheme.colorScheme.surfaceVariant
        armed -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val fg = when {
        loading -> MaterialTheme.colorScheme.onSurfaceVariant
        armed -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    val title = when {
        loading -> "Checking device…"
        armed -> "ARMED"
        else -> "TEST MODE"
    }
    val subtitle = when {
        loading -> "Reading Magisk, LSPosed, and hook status"
        armed -> "A matching PIN wipes keys and data"
        else -> "Matches are logged — wipe is off"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = fg,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = fg.copy(alpha = 0.9f))
    }
}

@Composable
private fun NextStepCard(
    status: SystemStatus?,
    matchCount: Int,
    loading: Boolean,
) {
    if (loading || status == null) return
    val step = nextStep(status, matchCount)
    Panel {
        Text("Next step", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(6.dp))
        Text(step, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun nextStep(status: SystemStatus, matchCount: Int): String = when {
    status.armed -> "Armed. Disarm only when you no longer need the wipe."
    !status.magiskRoot -> "Grant Magisk su to this app, then refresh."
    !status.hookLoaded -> "In LSPosed, enable this module for android + SystemUI, then soft-reboot."
    !(status.hookScopeAndroid || status.hookScopeSystemUi) ->
        "Hook loaded but scope is missing — re-enable the module and reboot."
    !(status.deleteAllKeysPresent || status.hookDeleteAllKeysOk) ->
        "deleteAllKeys is missing on this ROM — cannot arm safely."
    !status.pinSet || status.pinLength < PinHasher.MIN_PIN_LENGTH ->
        "Set a duress PIN of at least ${PinHasher.MIN_PIN_LENGTH} digits (not your real PIN)."
    matchCount == 0 ->
        "Enter the duress PIN at the lock screen, unlock fails, then refresh — a test match should appear."
    status.canArm ->
        "Test match recorded. Arm only when you accept irreversible wipe."
    else -> status.readyMessage
}

@Composable
private fun ChecklistCard(
    status: SystemStatus?,
    hasTestMatch: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    loading: Boolean,
) {
    Panel {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Setup checklist", style = MaterialTheme.typography.titleMedium)
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "Hide details" else "Show details",
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (loading || status == null) {
            Text("Checking…", style = MaterialTheme.typography.bodyMedium)
            return@Panel
        }
        CheckRow("Magisk root", status.magiskRoot)
        CheckRow("Hook heartbeat", status.hookLoaded)
        CheckRow("Scope (android or SystemUI)", status.hookScopeAndroid || status.hookScopeSystemUi)
        CheckRow("deleteAllKeys", status.deleteAllKeysPresent || status.hookDeleteAllKeysOk)
        CheckRow("Duress PIN set", status.pinSet && status.pinLength >= PinHasher.MIN_PIN_LENGTH)
        CheckRow("Test match recorded", hasTestMatch, optional = true)

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                DetailRow("system_server scope", yesNo(status.hookScopeAndroid))
                DetailRow("SystemUI scope", yesNo(status.hookScopeSystemUi))
                DetailRow("Test mode", yesNo(status.testMode))
                DetailRow("Armed", yesNo(status.armed), danger = status.armed)
                DetailRow(
                    "Last event",
                    status.lastEventLabel,
                    danger = status.lastEventIsDegraded,
                )
            }
        }
    }
}

@Composable
private fun CheckRow(label: String, ok: Boolean, optional: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    when {
                        ok -> MaterialTheme.colorScheme.secondary
                        optional -> MaterialTheme.colorScheme.outline
                        else -> MaterialTheme.colorScheme.error
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (ok) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = when {
                ok -> "Done"
                optional -> "Optional"
                else -> "Needed"
            },
            style = MaterialTheme.typography.labelLarge,
            color = when {
                ok -> MaterialTheme.colorScheme.secondary
                optional -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.error
            },
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String, danger: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (danger) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun PinSection(
    pin: String,
    pinConfirm: String,
    onPinChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    enabled: Boolean,
    pinAlreadySet: Boolean,
    onSave: () -> Unit,
) {
    Panel {
        Text("1 · Duress PIN", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            if (pinAlreadySet) {
                "PIN is set. Saving a new one replaces it and stays in test mode."
            } else {
                "Not your real lock PIN. Min ${PinHasher.MIN_PIN_LENGTH} digits. Only a salted hash is stored."
            },
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = onPinChange,
            label = { Text("Duress PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            enabled = enabled,
            supportingText = {
                Text("${pin.length} digits")
            },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = pinConfirm,
            onValueChange = onConfirmChange,
            label = { Text("Confirm") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = onSave,
            enabled = enabled && pin.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (pinAlreadySet) "Replace PIN" else "Save PIN")
        }
    }
}

@Composable
private fun TestAttemptsSection(
    events: List<TestEvent>,
    enabled: Boolean,
    onClear: () -> Unit,
) {
    val timeFormat = remember {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    }
    Panel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("2 · Test attempts", style = MaterialTheme.typography.titleMedium)
            if (events.isNotEmpty()) {
                TextButton(onClick = onClear, enabled = enabled) { Text("Clear") }
            }
        }
        Text(
            "Lock screen → duress PIN → come back and refresh. Matches mean the hook works; wipe stays off.",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (events.isEmpty()) {
            Text(
                if (enabled) "No matches yet" else "Need Magisk root to read attempts",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            events.take(12).forEachIndexed { index, event ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    )
                }
                Text(timeFormat.format(Date(event.atMs)), style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Match · wipe suppressed · ${event.pinLength}-digit PIN",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            if (events.size > 12) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "+ ${events.size - 12} older",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ArmSection(
    status: SystemStatus?,
    hasTestMatch: Boolean,
    onArm: () -> Unit,
    onDisarm: () -> Unit,
) {
    val armed = status?.armed == true
    val canArm = status?.canArm == true && !armed
    Panel {
        Text("3 · Arm", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            if (armed) {
                "Live. Disarm returns you to test mode."
            } else {
                "Only after a test match. Arming turns test mode off."
            },
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (armed) {
            OutlinedButton(
                onClick = onDisarm,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Disarm")
            }
        } else {
            Button(
                onClick = onArm,
                enabled = canArm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (hasTestMatch) "Arm wipe" else "Arm wipe (test first)")
            }
            if (status != null && !status.canArm) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    status.readyMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun WarningFooter(expanded: Boolean, onToggle: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f))
            .clickable(onClick = onToggle)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                stringResource(R.string.warning_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Text(
                text = stringResource(R.string.warning_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun Panel(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        content()
    }
}

private fun yesNo(value: Boolean): String = if (value) "Yes" else "No"

private fun validatePin(pin: String, confirm: String): String? {
    if (pin.length < PinHasher.MIN_PIN_LENGTH) {
        return "PIN must be at least ${PinHasher.MIN_PIN_LENGTH} digits"
    }
    if (!pin.all { it.isDigit() }) return "PIN must be digits only"
    if (PinHasher.isTrivialPin(pin)) return "PIN cannot be all the same digit"
    if (pin != confirm) return "PINs do not match"
    return null
}

private suspend fun savePin(pin: String): Boolean {
    val salt = PinHasher.generateSalt()
    val hash = PinHasher.hashPin(pin, salt)
    val config = DuressConfig(
        salt = salt,
        pinHash = hash,
        armed = false,
        testMode = true,
        pinLength = pin.length,
    )
    return ConfigStore.write(config)
}

private fun confirmDeviceCredential(activity: FragmentActivity?, onSuccess: () -> Unit) {
    if (activity == null) {
        onSuccess()
        return
    }
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL ||
                    errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS
                ) {
                    onSuccess()
                } else {
                    Toast.makeText(activity, errString, Toast.LENGTH_SHORT).show()
                }
            }
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Confirm to change arming")
        .setSubtitle("Device credential required")
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL,
        )
        .build()
    try {
        prompt.authenticate(info)
    } catch (_: Exception) {
        onSuccess()
    }
}
