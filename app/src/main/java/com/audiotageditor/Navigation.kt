package com.audiotageditor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.audiotageditor.data.DefaultDataRepository
import com.audiotageditor.ui.library.LibraryScreen
import com.audiotageditor.ui.library.LibraryScreenViewModel
import com.audiotageditor.ui.library.RenameScreen
import com.audiotageditor.ui.editor.EditorScreen
import com.audiotageditor.ui.editor.EditorScreenViewModel
import com.audiotageditor.ui.settings.SettingsScreen

@Composable
fun MainNavigation() {
    val repository = remember { DefaultDataRepository() }
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
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = libraryViewModel,
                modifier = Modifier
            )
        }
    }
}
