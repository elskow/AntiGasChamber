package org.unesa.agc.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.unesa.agc.AppGraph
import org.unesa.agc.data.UserDto
import org.unesa.agc.data.formatIso
import org.unesa.agc.data.friendlyError
import org.unesa.agc.ui.components.AgcGroupedSection
import org.unesa.agc.ui.components.AgcScreenHeader
import org.unesa.agc.ui.components.AgcListRow
import org.unesa.agc.ui.components.AgcRowSeparator
import org.unesa.agc.ui.components.clickableNoRipple
import org.unesa.agc.ui.components.AgcTextField
import org.unesa.agc.ui.theme.AgcSpacing
import org.unesa.agc.ui.theme.AgcThemeColors
import org.unesa.agc.ui.theme.AgcTypographyLight

@Composable
fun SettingsScreen(graph: AppGraph) {
    val colors = AgcThemeColors.current
    val session = graph.session.state
    val scope = rememberCoroutineScope()
    var users by remember { mutableStateOf<List<UserDto>>(emptyList()) }
    var usersError by remember { mutableStateOf<String?>(null) }
    var userToDelete by remember { mutableStateOf<UserDto?>(null) }
    var showAddUser by remember { mutableStateOf(false) }
    var newEmail by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newCode by remember { mutableStateOf("") }
    var addUserError by remember { mutableStateOf<String?>(null) }
    var addingUser by remember { mutableStateOf(false) }

    suspend fun refreshUsers() {
        try {
            users = graph.api.users().users
        } catch (e: Exception) {
            usersError = friendlyError(e)
        }
    }

    LaunchedEffect(Unit) { refreshUsers() }

    Column(Modifier.fillMaxSize()) {
        AgcScreenHeader(title = "Settings", scrollState = null)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = AgcSpacing.xs.dp, bottom = AgcSpacing.tabBarClearance.dp),
            verticalArrangement = Arrangement.spacedBy(AgcSpacing.sectionGap.dp),
        ) {
            item {
                AgcGroupedSection(
                    header = "USERS",
                    footer = if (usersError != null) usersError else "All accounts on this deployment.",
                ) {
                    users.forEachIndexed { index, user ->
                        AgcListRow(
                            title = user.email,
                            subtitle = formatIso(user.createdAt) ?: "",
                            trailing = {
                                Text(
                                    "Delete",
                                    style = AgcTypographyLight.footnote,
                                    color = AgcThemeColors.current.statusAlarm,
                                    modifier = Modifier.clickableNoRipple { userToDelete = user },
                                )
                            },
                        )
                        if (index < users.lastIndex) AgcRowSeparator()
                    }
                }
            }
            item {
                AgcGroupedSection(
                    header = "ADD USER",
                    footer = "New accounts need the deployment's provisioning code.",
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = AgcSpacing.listRowMinHeight.dp)
                            .clickableNoRipple(onClick = {
                                newEmail = ""; newPassword = ""; newCode = ""; addUserError = null
                                showAddUser = true
                            })
                            .padding(horizontal = AgcSpacing.cardInset.dp, vertical = AgcSpacing.md.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Add user…", style = AgcTypographyLight.body, modifier = Modifier.weight(1f))
                    }
                }
            }
            item {
                AgcGroupedSection(
                    header = "ACCOUNT",
                    footer = "Signing out clears this device's session.",
                ) {
                    AgcListRow(
                        title = session.email ?: "Signed in",
                        subtitle = "Session on this device",
                    )
                    AgcRowSeparator()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = AgcSpacing.listRowMinHeight.dp)
                            .clickableNoRipple(onClick = { scope.launch { graph.signOut() } })
                            .padding(horizontal = AgcSpacing.cardInset.dp, vertical = AgcSpacing.md.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Sign out",
                            style = AgcTypographyLight.body,
                            color = colors.statusAlarm,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            item {
                AgcGroupedSection(
                    header = "ABOUT",
                    footer = null,
                ) {
                    AgcListRow(
                        title = "Version",
                        subtitle = "1.0 (1)",
                    )
                }
            }
            item {
                Column(
                    Modifier.fillMaxWidth().padding(top = AgcSpacing.sm.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "AntiGasChamber",
                        style = AgcTypographyLight.footnote,
                        color = colors.secondaryLabel,
                    )
                    Spacer(Modifier.padding(top = AgcSpacing.xs.dp))
                    Text(
                        "LPG leak alerts",
                        style = AgcTypographyLight.caption1,
                        color = colors.secondaryLabel,
                    )
                }
            }
        }
    }
    if (showAddUser) {
        AlertDialog(
            onDismissRequest = { if (!addingUser) showAddUser = false },
            title = { Text("Add user") },
            text = {
                Column {
                    AgcTextField(newEmail, { newEmail = it }, "Email")
                    AgcTextField(newPassword, { newPassword = it }, "Password (min 8 characters)", secure = true)
                    AgcTextField(newCode, { newCode = it }, "Provisioning code", secure = true)
                    addUserError?.let {
                        Text(it, style = AgcTypographyLight.footnote, color = AgcThemeColors.current.statusAlarm)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !addingUser && newEmail.contains("@") && newPassword.length >= 8 && newCode.isNotBlank(),
                    onClick = {
                        addingUser = true
                        addUserError = null
                        scope.launch {
                            try {
                                graph.api.register(newEmail.trim(), newPassword, newCode.trim())
                                showAddUser = false
                                refreshUsers()
                            } catch (e: Exception) {
                                addUserError = friendlyError(e)
                            } finally {
                                addingUser = false
                            }
                        }
                    },
                ) { Text(if (addingUser) "Adding…" else "Add") }
            },
            dismissButton = {
                TextButton(enabled = !addingUser, onClick = { showAddUser = false }) { Text("Cancel") }
            },
        )
    }
    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Delete account?") },
            text = { Text("${user.email} will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    val id = user.id
                    userToDelete = null
                    scope.launch {
                        try {
                            graph.api.deleteUser(id)
                            refreshUsers()
                        } catch (e: Exception) {
                            usersError = friendlyError(e)
                        }
                    }
                }) { Text("Delete", color = colors.statusAlarm) }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) { Text("Cancel") }
            },
        )
    }
}
