package com.example.nativeaccent.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.nativeaccent.viewmodel.PracticeEvent
import com.example.nativeaccent.viewmodel.PracticeViewModel

object Routes {
    const val PRACTICE = "practice"

    const val ARG_ITEM_INDEX = "itemIndex"
    const val ARG_RECORDING_PATH = "recordingPath"
    const val ANALYSIS = "analysis/{$ARG_ITEM_INDEX}/{$ARG_RECORDING_PATH}"

    /** File paths contain slashes, so they have to be escaped into the route. */
    fun analysis(itemIndex: Int, recordingPath: String): String =
        "analysis/$itemIndex/${Uri.encode(recordingPath)}"
}

/**
 * Hosts both screens against one activity-scoped [PracticeViewModel], so the
 * recorder, player, coach voice and cached score survive navigation.
 */
@Composable
fun NativeAccentApp(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: PracticeViewModel = viewModel(factory = PracticeViewModel.factory(context))
    val navController = rememberNavController()

    // Scoring finishes asynchronously, so Screen 1 -> Screen 2 is driven by an
    // event rather than by the tap that stopped the recording.
    LaunchedEffect(viewModel, navController) {
        viewModel.events.collect { event ->
            when (event) {
                is PracticeEvent.OpenAnalysis ->
                    navController.navigate(Routes.analysis(event.itemIndex, event.recordingPath)) {
                        launchSingleTop = true
                    }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.PRACTICE,
        modifier = modifier,
    ) {
        composable(Routes.PRACTICE) {
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            PracticeScreen(
                state = state,
                onClose = onExit,
                onPlayCoach = viewModel::playCoach,
                onToggleRecording = viewModel::toggleRecording,
                onPermissionDenied = viewModel::onPermissionDenied,
                onPrevious = viewModel::previousItem,
                onCamera = { /* Camera capture is a later milestone. */ },
                onRetryAnalysis = { viewModel.retryAnalysis(openAnalysisWhenScored = true) },
                onCompareWithoutScore = viewModel::compareWithoutScore,
            )
        }

        composable(
            route = Routes.ANALYSIS,
            arguments = listOf(
                navArgument(Routes.ARG_ITEM_INDEX) { type = NavType.IntType },
                navArgument(Routes.ARG_RECORDING_PATH) { type = NavType.StringType },
            ),
        ) { entry ->
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val itemIndex = entry.arguments?.getInt(Routes.ARG_ITEM_INDEX) ?: 0
            val recordingPath = entry.arguments?.getString(Routes.ARG_RECORDING_PATH).orEmpty()

            // Re-seeds the ViewModel when the back stack is restored after
            // process death, where the arguments outlive in-memory state.
            LaunchedEffect(itemIndex, recordingPath) {
                if (recordingPath.isNotEmpty()) viewModel.restoreAttempt(itemIndex, recordingPath)
            }

            AnalysisScreen(
                state = state,
                onClose = onExit,
                onWordClick = viewModel::focusWord,
                onCoachTip = viewModel::requestCoachTip,
                onGetScore = { viewModel.retryAnalysis(openAnalysisWhenScored = false) },
                onPlayCoach = viewModel::playCoach,
                onPlayAttempt = { viewModel.playAttempt(state.lastRecordingPath ?: recordingPath) },
                onToggleSpeed = viewModel::toggleSpeed,
                onReRecord = {
                    // Same item, clean slate, back to Screen 1 ready to record.
                    viewModel.startOver()
                    navController.popBackStack(Routes.PRACTICE, inclusive = false)
                },
                onNext = {
                    viewModel.nextItem()
                    navController.popBackStack(Routes.PRACTICE, inclusive = false)
                },
            )
        }
    }
}
