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
import com.example.nativeaccent.viewmodel.PracticeViewModel

object Routes {
    const val PRACTICE = "practice"

    const val ARG_ITEM_INDEX = "itemIndex"
    const val ARG_RECORDING_PATH = "recordingPath"
    const val COMPARE = "compare/{$ARG_ITEM_INDEX}/{$ARG_RECORDING_PATH}"

    /** File paths contain slashes, so they have to be escaped into the route. */
    fun compare(itemIndex: Int, recordingPath: String): String =
        "compare/$itemIndex/${Uri.encode(recordingPath)}"
}

/**
 * Hosts both screens against one activity-scoped [PracticeViewModel], so the
 * recorder, player and coach voice survive navigation between them.
 */
@Composable
fun NativeAccentApp(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: PracticeViewModel = viewModel(factory = PracticeViewModel.factory(context))
    val navController = rememberNavController()

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
                onToggleRecording = {
                    // Returns a path only on the tap that finishes a recording.
                    viewModel.toggleRecording()?.let { path ->
                        navController.navigate(Routes.compare(viewModel.currentIndex, path))
                    }
                },
                onPermissionDenied = viewModel::onPermissionDenied,
                onPrevious = viewModel::previousItem,
                onCamera = { /* Camera capture is a later milestone. */ },
            )
        }

        composable(
            route = Routes.COMPARE,
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

            CompareScreen(
                state = state,
                onClose = onExit,
                onPlayCoach = viewModel::playCoach,
                onPlayAttempt = { viewModel.playAttempt(recordingPath) },
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
