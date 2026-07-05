package com.tagdeck

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tagdeck.data.DataRepository
import com.tagdeck.data.DefaultDataRepository
import com.tagdeck.ui.library.LibraryScreen
import com.tagdeck.ui.library.LibraryScreenViewModel
import com.tagdeck.ui.library.RenameScreen
import com.tagdeck.ui.editor.EditorScreen
import com.tagdeck.ui.editor.EditorScreenViewModel
import com.tagdeck.ui.settings.SettingsScreen

@Composable
fun MainNavigation() {
    val appViewModel: AppViewModel = viewModel()
    val repository = appViewModel.repository
    val navController = rememberNavController()

    val libraryViewModel: LibraryScreenViewModel = viewModel { LibraryScreenViewModel(repository) }

    NavHost(
        navController = navController,
        startDestination = "library"
    ) {
        composable("library") {
            LibraryScreen(
                onEditSelected = { uris ->
                    repository.setSelectedUris(uris)
                    navController.navigate("editor")
                },
                onNavigateToRename = { uris ->
                    repository.setSelectedUris(uris)
                    navController.navigate("rename")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                viewModel = libraryViewModel,
                modifier = Modifier
            )
        }
        composable(route = "editor") {
            val uris = repository.getSelectedUris()
            val viewModel: EditorScreenViewModel = viewModel { EditorScreenViewModel(repository) }

            EditorScreen(
                selectedUris = uris,
                onNavigateBack = { navController.popBackStack() },
                viewModel = viewModel,
                modifier = Modifier
            )
        }
        composable(route = "rename") {
            val uris = repository.getSelectedUris()
            RenameScreen(
                selectedUris = uris,
                onNavigateBack = { navController.popBackStack() },
                viewModel = libraryViewModel,
                modifier = Modifier
            )
        }
        composable(route = "settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                modifier = Modifier
            )
        }
    }
}
