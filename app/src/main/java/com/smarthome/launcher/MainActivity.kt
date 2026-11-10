package com.smarthome.launcher

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smarthome.launcher.calendar.CalendarAgendaWidget
import com.smarthome.launcher.calendar.CalendarViewModel
import com.smarthome.launcher.calendar.EventDetailOrEditDialog
import com.smarthome.launcher.frigate.FrigateCctvWidget
import com.smarthome.launcher.frigate.FrigateLiveModal
import com.smarthome.launcher.frigate.FrigateViewModel
import com.smarthome.launcher.homeassistant.HaDashboardWebViewModal
import com.smarthome.launcher.homeassistant.HomeAssistantViewModel
import com.smarthome.launcher.homeassistant.HomeAssistantWidget
import com.smarthome.launcher.screensaver.ScreensaverOverlay
import com.smarthome.launcher.screensaver.ScreensaverViewModel
import com.smarthome.launcher.settings.SettingsDialog
import com.smarthome.launcher.ui.components.AppDrawerModal
import com.smarthome.launcher.ui.components.TabletClockWeatherHeader
import com.smarthome.launcher.ui.theme.SmartHomeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val calendarViewModel by viewModels<CalendarViewModel>()
    private val haViewModel by viewModels<HomeAssistantViewModel>()
    private val frigateViewModel by viewModels<FrigateViewModel>()
    private val screensaverViewModel by viewModels<ScreensaverViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SmartHomeTheme {
                HomeScreen(
                    calendarViewModel = calendarViewModel,
                    haViewModel = haViewModel,
                    frigateViewModel = frigateViewModel,
                    screensaverViewModel = screensaverViewModel
                )
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        screensaverViewModel.resetIdleTimer()
    }
}

@Composable
fun HomeScreen(
    calendarViewModel: CalendarViewModel,
    haViewModel: HomeAssistantViewModel,
    frigateViewModel: FrigateViewModel,
    screensaverViewModel: ScreensaverViewModel
) {
    val calendarState by calendarViewModel.uiState.collectAsState()
    val haState by haViewModel.uiState.collectAsState()
    val frigateState by frigateViewModel.uiState.collectAsState()
    val screensaverState by screensaverViewModel.uiState.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    var showAppDrawer by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        calendarViewModel.checkPermissionAndLoad()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { screensaverViewModel.resetIdleTimer() }
                )
            }
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // Top App Bar / Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabletClockWeatherHeader(
                        modifier = Modifier.weight(1f)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Discreet app drawer trigger button
                        FilledTonalIconButton(
                            onClick = { showAppDrawer = true },
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = stringResource(R.string.action_all_apps),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Test screensaver button
                        IconButton(
                            onClick = { screensaverViewModel.triggerScreensaverNow() },
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Nightlight,
                                contentDescription = stringResource(R.string.action_screensaver_preview),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { showSettings = true },
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.action_settings),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Landscape Split Layout: Left Side (Calendar & Agenda), Right Side (Home Assistant & Frigate)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // LEFT COLUMN: Calendar & Agenda widget
                    Box(modifier = Modifier.weight(0.42f).fillMaxHeight()) {
                        CalendarAgendaWidget(
                            uiState = calendarState,
                            onAddClick = { calendarViewModel.startCreateEvent() },
                            onEventClick = { event -> calendarViewModel.selectEvent(event) },
                            onRequestPermission = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_CALENDAR,
                                        Manifest.permission.WRITE_CALENDAR
                                    )
                                )
                            }
                        )
                    }

                    // RIGHT COLUMN: Home Assistant Quick Controls + Frigate CCTV
                    Column(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        HomeAssistantWidget(
                            uiState = haState,
                            onToggleEntity = { entity -> haViewModel.toggleEntity(entity) },
                            onOpenDashboard = { haViewModel.setShowWebViewDashboard(true) },
                            onRefresh = { haViewModel.loadData() },
                            onConfigureClick = { showSettings = true }
                        )

                        FrigateCctvWidget(
                            uiState = frigateState,
                            getSnapshotUrl = { cam -> frigateViewModel.getSnapshotUrl(cam) },
                            onCameraClick = { cam -> frigateViewModel.selectCamera(cam) },
                            onRefresh = { frigateViewModel.loadData() },
                            onConfigureClick = { showSettings = true }
                        )
                    }
                }
            }
        }

        // Modals & Dialogs
        if (calendarState.selectedEvent != null || calendarState.isCreating || calendarState.isEditing) {
            EventDetailOrEditDialog(
                event = calendarState.selectedEvent,
                calendars = calendarState.calendars,
                isEditingOrCreating = calendarState.isCreating || calendarState.isEditing,
                onDismiss = { calendarViewModel.cancelDialog() },
                onSave = { id, calId, title, desc, loc, start, end, allDay ->
                    calendarViewModel.saveEvent(id, calId, title, desc, loc, start, end, allDay)
                },
                onDelete = { id -> calendarViewModel.deleteEvent(id) },
                onStartEdit = { ev -> calendarViewModel.startEditEvent(ev) }
            )
        }

        if (haState.showWebViewDashboard) {
            HaDashboardWebViewModal(
                url = haViewModel.getDashboardUrl(),
                onDismiss = { haViewModel.setShowWebViewDashboard(false) }
            )
        }

        frigateState.selectedCameraForLiveView?.let { cam ->
            FrigateLiveModal(
                camera = cam,
                liveUrl = frigateViewModel.getLiveStreamUrl(cam),
                onDismiss = { frigateViewModel.selectCamera(null) }
            )
        }

        if (showAppDrawer) {
            AppDrawerModal(onDismiss = { showAppDrawer = false })
        }

        if (showSettings) {
            SettingsDialog(
                onDismiss = { showSettings = false },
                onSaved = {
                    haViewModel.loadData()
                    frigateViewModel.loadData()
                    screensaverViewModel.resetIdleTimer()
                }
            )
        }

        // Bing Screensaver Overlay
        ScreensaverOverlay(
            isActive = screensaverState.isScreensaverActive,
            bingImage = screensaverState.dailyImage,
            onDismiss = { screensaverViewModel.dismissScreensaver() }
        )
    }
}
