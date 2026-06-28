# Audio Tag Editor — Full TODO List FOR CODING AGENT
# Work through these in order. Each is self-contained and safe to implement one at a time.

---

## BUGS (Fix First — These Cause Real Problems)

### BUG-01: Double Toast on Save Success
**File:** `EditorScreenViewModel.kt` + `EditorScreen.kt`
**Problem:**
When save succeeds, TWO toasts appear back-to-back:
1. ViewModel fires: "Changes saved successfully! Music players may take a few seconds to update." (LENGTH_LONG)
2. EditorScreen LaunchedEffect fires: "Tags saved successfully!" (LENGTH_SHORT)
**Fix:**
- Remove the `withContext(Dispatchers.Main) { Toast.makeText(...).show() }` block entirely from `saveChanges()` in `EditorScreenViewModel.kt`
- Keep only the LaunchedEffect toast in `EditorScreen.kt`, it is already the correct place to show UI feedback

---

### BUG-02: isLoading Flicker / Race Condition After Save
**File:** `DataRepository.kt` → `updateTags()`
**Problem:**
After writing tags, `updateTags` calls `loadFiles()` or `loadFolder()`, which BOTH set `_isLoading.value = true` internally. Then `updateTags` sets `_isLoading.value = false` AFTER the load call returns. This creates a brief false-false-true-false flicker.
Sequence:
1. `updateTags` sets `_isLoading = true`
2. Calls `loadFolder` which sets `_isLoading = true` again (redundant)
3. `loadFolder` finishes, sets `_isLoading = false` internally
4. `updateTags` sets `_isLoading = false` again (redundant)
**Fix:**
- Remove the `_isLoading.value = false` line at line 308 (the one inside `updateTags` after the reload call)
- The `finally` block inside `loadFolder` / `loadFiles` already handles it
- Add a comment explaining this

---

### BUG-03: titleToSave Logic Is Redundant and Contradicts Intent
**File:** `DataRepository.kt` → `updateTags()` → the `titleToSave` variable
**Problem:**
```kotlin
val titleToSave = if (uris.size > 1) null else title   // null when batch
val result = TagEngine.writeMetadata(
    title = titleToSave ?: title,   // falls back to title — so null is never passed!
```
The null guard `?: title` completely undoes the batch-mode null assignment. Title is always written.
**Fix:**
- Change the TagEngine call to use `titleToSave` directly (not `titleToSave ?: title`)
- Same logic flaw exists for `trackToSave ?: track` — fix that too
- These should be `title = titleToSave` and `track = trackToSave`

---

### BUG-04: hasCoverArt Always Returns False
**File:** `TagEngine.kt` → `readMetadata()`
**Problem:**
Line 134: `val hasCover = false` — hardcoded, never checked.
This means:
- `AudioItemCard` always shows `MusicNote` icon, never `Album` icon
- `showRemoveCoverOption` in EditorScreen only shows in batch mode since `uiState.albumArtBytes` is checked separately
- The `hasCoverArt` field in `AudioMetadata` is always wrong
**Fix:**
- After reading metadata from `kTagLib`, call `kTagLib.getArtwork(rawFd, ext)` and check if the result is non-null and non-empty
- OR: check if `propertyMap` contains any picture-related keys
- Set `val hasCover = artwork != null && artwork.isNotEmpty()`
- BUT: reading artwork twice is expensive. Instead, check if artwork bytes exist by reading them once and passing `hasCoverArt = artBytes != null` to `AudioMetadata`
- Simplest correct approach: `val hasCover = readAlbumArt(context, uri) != null` — but this doubles IO. Better: check if metadata has a COVERART/PICTURE key in the propertyMap as a lightweight indicator

---

### BUG-05: onNavigateToSettings Is a Dead Parameter
**File:** `LibraryScreen.kt` + `Navigation.kt`
**Problem:**
`LibraryScreen` accepts `onNavigateToSettings: () -> Unit` as a parameter, and `Navigation.kt` wires it to `navController.navigate("settings")`. However, nothing in `LibraryScreen` ever calls `onNavigateToSettings`. The Settings screen is unreachable from the UI.
**Fix:**
- Add a Settings menu item to the existing `DropdownMenu` in the TopAppBar overflow menu
- Add a `DropdownMenuItem` with `Icons.Default.Settings` label "Settings" that calls `onNavigateToSettings()`
- Place it below the Rename Files item

---

## PERFORMANCE (Scroll Lag — Highest Priority After Bugs)

### PERF-01: @Immutable Missing on AudioMetadata
**File:** `AudioMetadata.kt`
**Problem:**
Without `@Immutable`, Compose cannot skip recomposition for composables that receive `AudioMetadata`. Every scroll frame, every `AudioItemCard` re-evaluates all parameters.
**Fix:**
- Add `import androidx.compose.runtime.Immutable`
- Add `@Immutable` annotation to `AudioMetadata` data class

---

### PERF-02: @Immutable Missing on EditorUiState
**File:** `EditorScreenViewModel.kt`
**Problem:**
Same issue as PERF-01 but for EditorScreen. Every field update triggers full screen recomposition.
**Fix:**
- Add `import androidx.compose.runtime.Immutable`
- Add `@Immutable` to `EditorUiState` data class

---

### PERF-03: Shimmer Animation Duplicated 8 Times
**File:** `LibraryScreen.kt`
**Problem:**
`rememberInfiniteTransition()` and `animateFloat()` are currently called inside each `SkeletonItem`. With 8 skeleton items, 8 parallel animation states run simultaneously, each triggering its own recomposition pass.
**Fix:**
- Move `rememberInfiniteTransition` and `val shimmerAlpha by shimmerTransition.animateFloat(...)` UP to the `LibraryScreen` composable scope, before the `Scaffold`
- Change `SkeletonItem` signature to accept `shimmerAlpha: Float` as a parameter
- Remove `rememberInfiniteTransition` and `animateFloat` from inside `SkeletonItem`
- Pass `shimmerAlpha` from parent to each `SkeletonItem(shimmerAlpha = shimmerAlpha)` call

---

### PERF-04: LazyColumn Items Missing Keys
**File:** `LibraryScreen.kt`
**Problem:**
- Skeleton list: `items(8)` — no key, uses index, causes unnecessary recompositions
- Content list: `items(files)` — no key, when list changes Compose must diff by position
**Fix:**
- Skeleton: `items(8, key = { "skeleton_$it" })`
- Content: `items(files, key = { it.uriString })`

---

### PERF-05: BorderStroke Created on Every Recomposition in AudioItemCard
**File:** `LibraryScreen.kt` → `AudioItemCard`
**Problem:**
```kotlin
border = BorderStroke(
    width = 1.dp,
    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
)
```
A new `BorderStroke` object is allocated every frame for every visible item during scroll.
**Fix:**
- Add two `remember` values inside `AudioItemCard`:
  ```
  val selectedBorder = remember { BorderStroke(1.dp, MaterialTheme.colorScheme.primary) }
  val defaultBorder = remember { BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) }
  ```
- Use `border = if (isSelected) selectedBorder else defaultBorder`

---

### PERF-06: TextField Colors Rebuilt on Every Keystroke
**File:** `EditorScreen.kt` → `EditorTextField`
**Problem:**
`TextFieldDefaults.colors(...)` is called with 8 color lookups inline every time the composable recomposes (every keystroke triggers this).
**Fix:**
- Wrap the entire `TextFieldDefaults.colors(...)` call in a `remember` block:
  ```
  val textFieldColors = remember {
      TextFieldDefaults.colors(
          focusedContainerColor = ...,
          ...
      )
  }
  ```
- Use `colors = textFieldColors` in the `TextField` call

---

### PERF-07: LocalTextStyle.current.copy() Called on Every Keystroke
**File:** `EditorScreen.kt` → `EditorTextField`
**Problem:**
`textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Content)` creates a new `TextStyle` object every recomposition (every keystroke).
**Fix:**
- Replace with:
  ```
  val textStyle = remember { TextStyle(textDirection = TextDirection.Content) }
  ```
- Use `textStyle = textStyle` in `TextField`
- Add `import androidx.compose.ui.text.TextStyle`

---

### PERF-08: InputChipDefaults.inputChipColors() Called Inline in Batch Mode
**File:** `EditorScreen.kt` → `EditorTextField` → chip row
**Problem:**
Three `InputChipDefaults.inputChipColors(...)` calls execute every recomposition in batch edit mode.
**Fix:**
- Hoist each chip color block into a `remember` block:
  ```
  val keepChipColors = remember {
      InputChipDefaults.inputChipColors(
          selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
          selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
      )
  }
  val blankChipColors = remember { InputChipDefaults.inputChipColors(...) }
  val overwriteChipColors = remember { InputChipDefaults.inputChipColors(...) }
  ```
- Pass each to the corresponding `InputChip`

---

### PERF-09: EditorScreen Destructures All 13 uiState Fields at Root
**File:** `EditorScreen.kt`
**Problem:**
Lines 46–65 pull every single field from `uiState` at the composable root:
```kotlin
val files = uiState.selectedFiles
val isBatch = uiState.isBatchEdit
val title = uiState.title
val artist = uiState.artist
...
```
This means the entire `EditorScreen` recomposes when any field changes, even unrelated ones (e.g., typing in `artist` forces `discNumber` rows to re-evaluate).
**Fix:**
- Pass `uiState` directly down to each `EditorTextField` call instead of destructuring at the top
- OR: only destructure fields that are used in the scaffold/top-level, and pass `uiState` as a whole into the LazyColumn item composables
- Minimum viable fix: keep only `isLoading`, `files`, `isBatch`, `saveSuccess` at root scope — pass `uiState` into a separate `EditorFormContent(uiState, viewModel)` composable

---

### PERF-10: onValueChange Lambda Recreated on Every Keystroke
**File:** `EditorScreen.kt` → `EditorTextField` → `TextField`
**Problem:**
```kotlin
onValueChange = {
    onValueChange(it)
    if (isMixed && currentAction != "OVERWRITE") {
        onActionChange("OVERWRITE")
    }
}
```
New lambda object on every recomposition. With `isMixed` and `currentAction` as captured vars, the lambda is never stable.
**Fix:**
- Wrap in `remember` with the right keys:
  ```
  val handleChange = remember(onValueChange, onActionChange, isMixed, currentAction) {
      { newValue: String ->
          onValueChange(newValue)
          if (isMixed && currentAction != "OVERWRITE") {
              onActionChange("OVERWRITE")
          }
      }
  }
  ```
- Use `onValueChange = handleChange`

---

### PERF-11: RenameScreen File Preview Calculation Not Memoized
**File:** `RenameScreen.kt` → `items()` block
**Problem:**
The `fileNewName` computation via `remember(renameTemplateInput, file)` is inside `items()`, which is fine, but the entire block with RTL detection and `CompositionLocalProvider` is not extracted into its own composable — Compose cannot skip it.
**Fix:**
- Extract the file card content into a private composable: `private fun RenamePreviewCard(file: AudioMetadata, newFileName: String)`
- Extract the `computeNewFileName` logic into a top-level private function (not a composable)
- In `items()`, call `remember(renameTemplateInput, file) { computeNewFileName(...) }` and pass the string to `RenamePreviewCard`

---

## CODE QUALITY / MINOR IMPROVEMENTS

### CQ-01: Remove Unused Imports in LibraryScreen
**File:** `LibraryScreen.kt`
**Problem:**
The following imports exist but nothing uses them:
- `android.widget.Toast` — Toast is never called in LibraryScreen
- `android.os.Build` — no SDK version check in LibraryScreen
- `androidx.compose.foundation.Image` — no Image composable used
- `androidx.compose.foundation.shape.CircleShape` — never used
- `androidx.compose.ui.graphics.Brush` — never used
- `androidx.compose.ui.layout.ContentScale` — never used
- `androidx.compose.ui.res.painterResource` — never used
- `com.audiotageditor.data.StorageHelper` — never used directly in LibraryScreen
- `com.audiotageditor.data.SettingsManager` — never used directly in LibraryScreen
- `com.audiotageditor.theme.*` wildcard — only `ThemeManager` and `ThemeMode` are used
**Fix:**
- Remove all listed unused imports
- Replace `import com.audiotageditor.theme.*` with:
  - `import com.audiotageditor.theme.ThemeManager`
  - `import com.audiotageditor.theme.ThemeMode`

---

### CQ-02: Replace All Wildcard Imports
**Files:** `LibraryScreen.kt`, `EditorScreen.kt`, `RenameScreen.kt`, `SettingsScreen.kt`
**Problem:**
All four UI files use wildcard imports (`import androidx.compose.material.icons.filled.*`, `import androidx.compose.material3.*`, etc.). This makes it impossible to know what's actually used without reading the full file.
**Fix:**
For each file, run through the code and identify the exact icons/classes used, then replace wildcards with explicit imports. Specific icons in use:
- `LibraryScreen`: Album, Deselect, LibraryMusic, MoreVert, MusicNote, SelectAll, DarkMode, LightMode, SettingsBrightness, DriveFileRenameOutline, Edit, AutoAwesome
- `EditorScreen`: ArrowBack (automirrored), HideImage, Info, Save, Warning
- `RenameScreen`: ArrowBack (automirrored), DriveFileRenameOutline
- `SettingsScreen`: ArrowBack (automirrored), DarkMode, DriveFileRenameOutline, LightMode, Palette, Settings

---

### CQ-03: @Stable Annotation on ViewModels
**Files:** `LibraryScreenViewModel.kt`, `EditorScreenViewModel.kt`
**Problem:**
Without `@Stable`, Compose treats ViewModel instances as unstable and may not skip recomposition.
**Fix:**
- Add `import androidx.compose.runtime.Stable`
- Add `@Stable` before each ViewModel class declaration

---

### CQ-04: selectedCount Should Use derivedStateOf
**File:** `LibraryScreen.kt`
**Problem:**
`val selectedCount = selectedUris.size` is a raw computation in composable scope.
When `selectedUris` emits a new Set, `selectedCount` is recalculated and triggers recomposition even if the count didn't change (e.g., selected item swapped).
**Fix:**
```kotlin
val selectedCount by remember { derivedStateOf { selectedUris.size } }
```

---

### CQ-05: SettingsScreen Depends on LibraryScreenViewModel — Wrong Coupling
**File:** `SettingsScreen.kt` + `Navigation.kt`
**Problem:**
`SettingsScreen` takes `viewModel: LibraryScreenViewModel` as a parameter, and uses it to call `viewModel.renameSelectedFiles()` and `viewModel.selectedUris`. Settings should not depend on the library ViewModel. This also means selecting files in the library, going to settings, and trying to rename them from settings — it works but it's architecturally wrong.
**Fix:**
- Create a dedicated `SettingsScreenViewModel` that only exposes settings-related state from `SettingsManager` and `ThemeManager`
- Remove the rename/select functionality from the Settings screen entirely (it belongs only in RenameScreen)
- Remove the `viewModel: LibraryScreenViewModel` parameter from `SettingsScreen`
- Update `Navigation.kt` to not pass `libraryViewModel` to `SettingsScreen`

---

### CQ-06: EditorScreen Has windowInsetsPadding Double-Applied
**File:** `EditorScreen.kt`
**Problem:**
`BottomSheetScaffold` already handles insets via the `paddingValues` lambda param. Adding `.windowInsetsPadding(WindowInsets.systemBars)` inside the content creates double top padding on some devices.
**Fix:**
- Remove `.windowInsetsPadding(WindowInsets.systemBars)` from the Box modifier inside the Scaffold lambda
- Keep only `.padding(paddingValues)`

---

### CQ-07: RenameScreen Has Same windowInsetsPadding Issue
**File:** `RenameScreen.kt`
**Problem:** Same double-inset issue as CQ-06 — `BottomSheetScaffold` + manual `windowInsetsPadding(systemBars)`.
**Fix:** Same fix — remove the manual `windowInsetsPadding` call from inside the scaffold lambda content.

---

### CQ-08: Cover Art Never Displayed in EditorScreen
**File:** `EditorScreen.kt`
**Problem:**
`EditorUiState` has `coverImageBitmap: ImageBitmap?` and it's loaded in the ViewModel, but `EditorScreen` never renders it. The user cannot see what the current cover art looks like before deciding to remove it.
**Fix:**
- In the `EditorScreen` LazyColumn, add an item before the "Remove Cover Art" card:
  - If `uiState.coverImageBitmap != null`, show it in an Image composable
  - If null and not in batch mode, show a placeholder box with a MusicNote icon
  - Keep it as a card with a fixed height (e.g., 200dp), `ContentScale.Crop`
- Add `import androidx.compose.ui.layout.ContentScale`
- Add `import androidx.compose.ui.graphics.asImageBitmap` if not already present

---

## ORDER OF IMPLEMENTATION

**Round 1 — Bugs (Do First)**
1. BUG-01: Remove double Toast from ViewModel
2. BUG-02: Remove redundant isLoading=false in updateTags
3. BUG-03: Fix titleToSave and trackToSave logic
4. BUG-05: Wire onNavigateToSettings to dropdown menu item

**Round 2 — Scroll Performance (Do Second)**
5. PERF-01: @Immutable on AudioMetadata
6. PERF-02: @Immutable on EditorUiState
7. PERF-03: Move shimmer animation to parent
8. PERF-04: Add keys to LazyColumn items
9. PERF-05: Memoize BorderStroke in AudioItemCard

**Round 3 — Editor Performance (Do Third)**
10. PERF-06: Memoize TextFieldDefaults.colors()
11. PERF-07: Memoize LocalTextStyle.current.copy()
12. PERF-08: Memoize InputChipDefaults.inputChipColors()
13. PERF-10: Stabilize onValueChange lambda

**Round 4 — Architecture & Quality (Do Last)**
14. CQ-01: Remove unused imports from LibraryScreen
15. CQ-02: Replace wildcard imports in all UI files
16. CQ-03: @Stable on ViewModels
17. CQ-04: derivedStateOf for selectedCount
18. CQ-06 + CQ-07: Remove double windowInsetsPadding
19. CQ-08: Show cover art in EditorScreen
20. PERF-09: Extract EditorFormContent composable
21. PERF-11: Extract RenamePreviewCard composable
22. CQ-05: Decouple SettingsScreen from LibraryScreenViewModel (biggest refactor — do last)

**Skip For Now (Needs Profiling First)**
- BUG-04: hasCoverArt detection (requires understanding KTagLib API surface)
- PERF-CACHE: TagEngine metadata cache (only needed if 100+ files are loaded repeatedly)

---

## NOTES FOR CODING AGENT

- Fix one TODO at a time, never combine multiple todos in one prompt
- After BUG-02, test that the loading spinner appears and disappears correctly with no double-flash
- After PERF-03 through PERF-05, test scroll on the library screen — should be noticeably smoother
- After PERF-06 through PERF-10, test typing in EditorScreen TextFields — no lag between keystrokes
- CQ-05 is the biggest refactor and should be broken into sub-steps.
