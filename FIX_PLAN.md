# Audio Tag Editor — Comprehensive Fix Plan

## Overview of Files Changed
1. `TagEngine.kt` — remove double file-descriptor for cover art check
2. `LibraryScreenViewModel.kt` — remove dead search state
3. `Navigation.kt` — remove double insets
4. `Color.kt` — fix saturated primary, fix error color
5. `Theme.kt` — complete missing color roles in both schemes
6. `LibraryScreen.kt` — skeleton animation, AssistChip → badge, card modifier chain, dropdown cleanup, alpha token replacements, FAB padding
7. `EditorScreen.kt` — hoist showRemoveCoverOption, add contentWindowInsets, alpha token replacements
8. `RenameScreen.kt` — add contentWindowInsets, alpha token replacements
9. `SettingsScreen.kt` — ThemeMode.entries, alpha token replacements

All changes are in-place edits to existing files. No new files or dependencies are introduced.

---

## 1. `TagEngine.kt`

### 1-A. Remove `hasCoverArt()` double file-descriptor

**Problem:** `readMetadata()` opens one `ParcelFileDescriptor` for metadata via `kTagLib.getMetadata()`, then immediately calls `hasCoverArt()` which opens a SECOND `ParcelFileDescriptor` and decodes the full artwork byte array via `kTagLib.getArtwork()` just to check `!= null`. This doubles native I/O for every file during list load, saturating `Dispatchers.IO` and causing the perceived load lag.

**Fix:**
- In `readMetadata()`, replace:
  ```kotlin
  val hasCover = hasCoverArt(context, uri, ext)
  ```
  with:
  ```kotlin
  val hasCover = false
  ```
- Delete the entire `private fun hasCoverArt(...)` function — it has no other callers.

**Side effect handled in EditorScreen.kt (see §7-A):** The `showRemoveCoverOption` condition in `EditorScreen` currently reads `files.firstOrNull()?.hasCoverArt == true`. Since `hasCoverArt` is now always `false`, this condition is changed to check `uiState.albumArtBytes != null` instead, which is already loaded asynchronously by `EditorScreenViewModel` for single-file mode and is the correct source of truth.

---

## 2. `LibraryScreenViewModel.kt`

### 2-A. Remove dead search infrastructure

**Problem:** `_searchQuery`, `searchQuery`, `updateSearchQuery()` and the `combine()` call in `filteredFiles` are never used from any screen. The `combine` operator adds an unnecessary reactive layer and an extra coroutine subscription.

**Fix:**

Remove these members entirely:
```kotlin
// DELETE these three
private val _searchQuery = MutableStateFlow("")
val searchQuery = _searchQuery.asStateFlow()
fun updateSearchQuery(query: String) { _searchQuery.value = query }
```

Replace `filteredFiles` declaration — change from the `combine(...)` pattern to a direct `stateIn`:
```kotlin
// BEFORE
val filteredFiles: StateFlow<List<AudioMetadata>> = combine(
    repository.loadedFiles,
    _searchQuery
) { files, query ->
    if (query.isBlank()) files
    else files.filter { ... }
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

// AFTER
val filteredFiles: StateFlow<List<AudioMetadata>> = repository.loadedFiles
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

Remove unused import:
```kotlin
// DELETE
import kotlinx.coroutines.flow.combine
```

`SharingStarted` and `stateIn` imports are still needed — keep them.

---

## 3. `Navigation.kt`

### 3-A. Remove `safeDrawingPadding()` double insets

**Problem:** `enableEdgeToEdge()` is called in `MainActivity`. Every screen composable in `NavHost` is wrapped with `Modifier.safeDrawingPadding()`, which consumes the status bar inset (~44–56 dp) at the outer level. But each screen uses `Scaffold` or `BottomSheetScaffold` which internally apply `TopAppBarDefaults.windowInsets` (including status bars) a second time. Result: ~44–56 dp of dead space below every top bar, visible in the editor and rename screens.

**Fix:** Remove `Modifier.safeDrawingPadding()` from all four `composable` blocks. Change every instance of:
```kotlin
modifier = Modifier.safeDrawingPadding()
```
to:
```kotlin
modifier = Modifier
```

Remove the now-unused import:
```kotlin
// DELETE
import androidx.compose.foundation.layout.safeDrawingPadding
```

The `Scaffold` in `LibraryScreen` and `SettingsScreen` uses `ScaffoldDefaults.contentWindowInsets` by default, which is `WindowInsets.systemBars` — this handles both status bar and navigation bar correctly. The `BottomSheetScaffold` in `EditorScreen` and `RenameScreen` requires an explicit fix (see §7-B and §8-A).

---

## 4. `Color.kt`

### 4-A. Reduce `PrimaryCyan` saturation

**Problem:** `Color(0xFF00E5FF)` is 100% chroma neon cyan. On dark backgrounds it causes chromatic vibration and OLED blooming. Material Design 3 caps dark-scheme primary chroma at ~70%. It also renders as a washed-out pale blue on the FAB in dark mode because the eye compensates for the saturation by perceiving low contrast.

**Fix:**
```kotlin
// BEFORE
val PrimaryCyan = Color(0xFF00E5FF)
// AFTER
val PrimaryCyan = Color(0xFF52D9F0)
```
Same hue (~189°), saturation reduced from 100% to ~68%, luminance adjusted. Reads clearly as cyan, passes WCAG AA contrast against `DarkNavy` (ratio ~8.2:1), eliminates bloom on OLED.

### 4-B. Remove `AccentRed` — error colors are now defined per-scheme in Theme.kt

```kotlin
// DELETE this line
val AccentRed = Color(0xFFFF3D00)
```

`AccentRed` (`#FF3D00`) fails WCAG AA on light backgrounds (contrast ~3.3:1). Both schemes will now define their own correct MD3-standard error values directly in `Theme.kt` (see §5).

---

## 5. `Theme.kt`

### 5-A. Complete `DarkColorScheme` with all missing MD3 color roles

The current dark scheme defines only 11 of ~27 MD3 roles. Missing roles fall back to Material3's auto-generated neutrals which are not calibrated to the neon-cyan brand, causing `primaryContainer`, `secondaryContainer`, `outlineVariant`, `errorContainer`, and all surface container tokens to resolve to wrong values in dark mode.

Add the following to `DarkColorScheme`:
```kotlin
primaryContainer = Color(0xFF004F5E),
onPrimaryContainer = Color(0xFFACEEFF),
secondaryContainer = Color(0xFF4A0083),
onSecondaryContainer = Color(0xFFEDD9FF),
tertiaryContainer = Color(0xFF573E5C),
onTertiaryContainer = Color(0xFFEDD9FF),
error = Color(0xFFFFB4AB),
onError = Color(0xFF690005),
errorContainer = Color(0xFF93000A),
onErrorContainer = Color(0xFFFFDAD6),
surfaceContainerLowest = Color(0xFF030508),
surfaceContainerLow = Color(0xFF0F1319),
surfaceContainer = Color(0xFF131820),
surfaceContainerHigh = Color(0xFF1D2330),
surfaceContainerHighest = Color(0xFF272F3E),
outlineVariant = Color(0xFF3A4860),
scrim = Color(0xFF000000),
inverseSurface = Color(0xFFE3E9F5),
inverseOnSurface = Color(0xFF2D3340),
inversePrimary = Color(0xFF006779),
```

Also remove the reference to the now-deleted `AccentRed`:
```kotlin
// BEFORE
error = AccentRed
// AFTER — already included in additions above as error = Color(0xFFFFB4AB)
// Simply remove the standalone `error = AccentRed` line;
// the new error value is included in the block above.
```

### 5-B. Add missing roles and fix error colors in `LightColorScheme`

```kotlin
error = Color(0xFFBA1A1A),
onError = Color.White,
errorContainer = Color(0xFFFFDAD6),
onErrorContainer = Color(0xFF410002),
surfaceContainerLowest = Color(0xFFFFFFFF),
surfaceContainerLow = Color(0xFFF0F4F8),
surfaceContainer = Color(0xFFEAEEF2),
surfaceContainerHigh = Color(0xFFE4E8EC),
surfaceContainerHighest = Color(0xFFDEE2E6),
outlineVariant = Color(0xFFC4C7CF),
scrim = Color(0xFF000000),
inverseSurface = Color(0xFF2E3135),
inverseOnSurface = Color(0xFFF0F0F4),
inversePrimary = Color(0xFF4DD9F0),
```

Also remove:
```kotlin
// REMOVE from LightColorScheme:
error = AccentRed,   // replaced by error = Color(0xFFBA1A1A) above
onError = Color.White  // now included in the additions above
```

---

## 6. `LibraryScreen.kt`

### 6-A. Hoist shimmer animation out of `SkeletonItem`

**Problem:** Each of 8 `SkeletonItem` calls creates its own `rememberInfiniteTransition` + `animateFloat`, resulting in 8 independent Choreographer-driven animations competing for Vsync on the same frame.

**Fix:** Change `SkeletonItem`'s signature to accept `shimmerAlpha: Float` as a parameter. Remove `rememberInfiniteTransition` and `animateFloat` from inside `SkeletonItem`. Compute one shared transition at the call site in `LibraryScreen`:

```kotlin
// In LibraryScreen, BEFORE the if (isLoading) block:
val shimmerTransition = rememberInfiniteTransition(label = "ShimmerTransition")
val shimmerAlpha by shimmerTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = 0.8f,
    animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 800, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse
    ),
    label = "ShimmerAlpha"
)
```

Pass it down:
```kotlin
items(8) {
    SkeletonItem(shimmerAlpha = shimmerAlpha)
}
```

Update the `SkeletonItem` composable signature:
```kotlin
// BEFORE
@Composable
fun SkeletonItem() {
    val infiniteTransition = rememberInfiniteTransition(label = "SkeletonTransition")
    val alpha by infiniteTransition.animateFloat(...)
    ...background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha))

// AFTER
@Composable
fun SkeletonItem(shimmerAlpha: Float) {
    // no local animation — use shimmerAlpha parameter directly
    ...background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = shimmerAlpha))
```

Note: `.copy(alpha = shimmerAlpha)` on `outlineVariant` is kept intentionally here because the animation drives the alpha value dynamically — this is the correct and intended pattern for shimmer, not a static alpha-hack.

### 6-B. Replace `AssistChip` with a lightweight `FormatBadge` composable

**Problem:** `AssistChip` is a full interactive Material3 composable with its own ripple layer, minimum 48 dp touch target, hit-test region, and indication. Placed inside every scrolling list card with `onClick = {}` (doing nothing), it adds measurable per-frame layout and drawing cost during scroll with zero functional value.

**Fix:** Add a new private composable `FormatBadge` directly below `AudioItemCard`:

```kotlin
@Composable
private fun FormatBadge(format: String) {
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                shape = RoundedCornerShape(6.dp)
            )
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = format,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
```

In `AudioItemCard`, replace the entire `AssistChip(...)` block with:
```kotlin
FormatBadge(format = item.cleanFormat)
```

Remove the `height(28.dp)` modifier that was on the AssistChip — the `FormatBadge` sizes to its content naturally.

Note: `primary.copy(alpha = 0.4f)` here is intentional — it is a decorative border tint, not a semantic color token. This is the correct and justified use of alpha on a color.

### 6-C. Fix `AudioItemCard` modifier chain

**Problem:** The card has `.clip(RoundedCornerShape(16.dp))` → `.border(...)` → `.combinedClickable(...)` stacked as modifiers. Placing `.clip()` before `.border()` causes the border to be clipped at the corners. More importantly, stacking clip + border + combinedClickable as separate modifiers forces an offscreen compositing layer per card per scroll frame. The `Card` composable has native `shape` and `border` parameters that handle clipping internally without the extra layer.

**Fix:** Remove `.clip()` and `.border()` from the `Modifier` chain on `Card`. Move them to `Card`'s own parameters. Keep `combinedClickable` on the modifier since `Card` does not have an `onLongClick` parameter.

```kotlin
// BEFORE
Card(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp)
        .clip(RoundedCornerShape(16.dp))
        .border(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            RoundedCornerShape(16.dp)
        )
        .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    colors = CardDefaults.cardColors(
        containerColor = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    )
)

// AFTER
Card(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp)
        .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(
        width = 1.dp,
        color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant
    ),
    colors = CardDefaults.cardColors(
        containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                         else MaterialTheme.colorScheme.surfaceContainerLow
    )
)
```

Notes:
- `outlineVariant` replaces `outlineVariant.copy(alpha = 0.5f)` — with proper `outlineVariant` now defined in both schemes (§5), full opacity is correct.
- `secondaryContainer` replaces `primaryContainer.copy(alpha = 0.15f)` for selected state — this uses a proper semantic token that is fully defined in both schemes and gives clear visual selection without alpha math.
- `surfaceContainerLow` replaces `surfaceVariant.copy(alpha = 0.2f)`.

### 6-D. Fix dropdown menu

**Three sub-issues:** unnecessary "Theme Mode" label; `Column` has `width(260.dp)` which forces the entire `DropdownMenu` to 260 dp and bleeds that width to the "Rename Files" `DropdownMenuItem`; excess horizontal padding.

**Fix:**

```kotlin
// BEFORE
Column(
    modifier = Modifier
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .width(260.dp)
) {
    Text(
        text = "Theme Mode",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
    ) { ... }
}

// AFTER
Column(
    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
    // width(260.dp) removed — menu width is now driven by DropdownMenuItem content
) {
    // "Theme Mode" Text composable removed entirely
    Row(
        modifier = Modifier.padding(top = 4.dp),
        // fillMaxWidth() removed — Row sizes to its icon children
        horizontalArrangement = Arrangement.spacedBy(8.dp)
        // CenterHorizontally alignment removed with spacedBy(8.dp)
    ) { ... }
}
```

Also replace deprecated enum iteration:
```kotlin
// BEFORE
ThemeMode.values().forEach { mode ->
// AFTER
ThemeMode.entries.forEach { mode ->
```

### 6-E. Replace remaining `.copy(alpha)` color tokens

In `AudioItemCard` — artwork thumbnail Box backgrounds:
```kotlin
// Unselected (BEFORE)
MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
// Unselected (AFTER)
MaterialTheme.colorScheme.surfaceContainerHigh

// Selected (BEFORE)
MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
// Selected (AFTER)
MaterialTheme.colorScheme.primaryContainer
```

In `EmptyStateComponent` — icon container:
```kotlin
// BEFORE
MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
// AFTER
MaterialTheme.colorScheme.surfaceContainerHigh
```

In `SkeletonItem` — card container:
```kotlin
// BEFORE
MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
// AFTER
MaterialTheme.colorScheme.surfaceContainerLow
```

In dropdown — unselected theme icon button background:
```kotlin
// BEFORE
MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
// AFTER
MaterialTheme.colorScheme.surfaceContainerHigh
```

In dropdown — `HorizontalDivider` color:
```kotlin
// BEFORE
MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
// AFTER
MaterialTheme.colorScheme.outlineVariant
```

### 6-F. Fix LazyColumn bottom padding

After removing `safeDrawingPadding()`, Scaffold handles navigation bar insets via `ScaffoldDefaults.contentWindowInsets`. The `Box` inside the Scaffold already receives navigation bar inset as part of `paddingValues`. The `LazyColumn` only needs to clear the FAB (72 dp height + 16 dp gap = 88 dp).

Both LazyColumn instances in LibraryScreen (loading skeleton and file list):
```kotlin
// BEFORE
contentPadding = PaddingValues(bottom = 120.dp)
// AFTER
contentPadding = PaddingValues(bottom = 88.dp)
```

---

## 7. `EditorScreen.kt`

### 7-A. Hoist `showRemoveCoverOption` above `LazyColumn` + fix condition

**Problem 1:** The val is computed inside the `LazyColumn` content lambda, which re-executes on every recomposition during scroll.

**Problem 2:** After §1-A, `hasCoverArt` is always `false`, so the condition `files.firstOrNull()?.hasCoverArt == true` will never be true in single-file mode. The correct check is whether album art was actually found, which is tracked in `uiState.albumArtBytes`.

**Fix:** Move the val to just before the `LazyColumn` call, and update the condition:
```kotlin
// Place this BEFORE the LazyColumn(...)  call, INSIDE the `else` branch:
val showRemoveCoverOption = isBatch || uiState.albumArtBytes != null

LazyColumn(...) {
    if (showRemoveCoverOption) {
        item { ... }
    }
    ...
}
```

### 7-B. Add `contentWindowInsets` to `BottomSheetScaffold`

After removing `safeDrawingPadding()` from `Navigation.kt`, the `BottomSheetScaffold` does not automatically handle system bar insets the same way `Scaffold` does. Add the parameter explicitly:

```kotlin
BottomSheetScaffold(
    ...
    contentWindowInsets = WindowInsets.systemBars,
    ...
)
```

Add import:
```kotlin
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
```

### 7-C. Replace `.copy(alpha)` color tokens

In `EditorTextField` — `TextFieldDefaults.colors(...)`:
```kotlin
// focusedContainerColor (BEFORE)
MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
// (AFTER)
MaterialTheme.colorScheme.surfaceContainerHigh

// unfocusedContainerColor (BEFORE)
MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
// (AFTER)
MaterialTheme.colorScheme.surfaceContainerHigh

// disabledContainerColor (BEFORE)
MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
// (AFTER)
MaterialTheme.colorScheme.surfaceContainerLow

// disabledTextColor (BEFORE)
MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
// (AFTER)
MaterialTheme.colorScheme.onSurfaceVariant
```

In `AdvancedTechnicalInfoCard`:
```kotlin
// Card border (BEFORE)
MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
// (AFTER)
MaterialTheme.colorScheme.outlineVariant

// Card containerColor (BEFORE)
MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
// (AFTER)
MaterialTheme.colorScheme.surfaceContainerLow
```

In `TechnicalInfoItem`:
```kotlin
// label text color (BEFORE)
MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
// (AFTER)
MaterialTheme.colorScheme.onSurfaceVariant
```

In the remove-cover `OutlinedCard`:
```kotlin
// containerColor (BEFORE)
MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
// (AFTER)
MaterialTheme.colorScheme.surfaceContainerLow
```

---

## 8. `RenameScreen.kt`

### 8-A. Add `contentWindowInsets` to `BottomSheetScaffold`

Same as §7-B — add:
```kotlin
contentWindowInsets = WindowInsets.systemBars,
```
with the same imports.

### 8-B. Replace `.copy(alpha)` color tokens

File preview cards:
```kotlin
// containerColor (BEFORE)
MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
// (AFTER)
MaterialTheme.colorScheme.surfaceContainerLow
```

Rename template card:
```kotlin
// containerColor (BEFORE)
MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
// (AFTER)
MaterialTheme.colorScheme.surfaceContainerLow
```

---

## 9. `SettingsScreen.kt`

### 9-A. Replace deprecated `ThemeMode.values()`

```kotlin
// BEFORE
ThemeMode.values().forEach { mode ->
// AFTER
ThemeMode.entries.forEach { mode ->
```

### 9-B. Replace `.copy(alpha)` color tokens

Section cards:
```kotlin
// containerColor (BEFORE)
MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
// (AFTER)
MaterialTheme.colorScheme.surfaceContainerLow

// border color (BEFORE)
MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
// (AFTER)
MaterialTheme.colorScheme.outlineVariant
```

Preview card inside tag-to-filename section:
```kotlin
// containerColor (BEFORE)
MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
// (AFTER)
MaterialTheme.colorScheme.primaryContainer
// Now safe to use at full opacity because primaryContainer
// is properly defined in both schemes after §5.
```

---

## Summary Table

| # | File | Type | Issue |
|---|------|------|-------|
| 1-A | TagEngine.kt | Performance | Double file-descriptor per file during load |
| 2-A | LibraryScreenViewModel.kt | Dead code | searchQuery, filteredFiles combine, updateSearchQuery |
| 3-A | Navigation.kt | Layout bug | safeDrawingPadding + Scaffold double insets |
| 4-A | Color.kt | Color token | PrimaryCyan 100% saturation neon |
| 4-B | Color.kt | Color token | AccentRed fails WCAG AA on light |
| 5-A | Theme.kt | Color token | 16 missing roles in DarkColorScheme |
| 5-B | Theme.kt | Color token | Missing roles + wrong error in LightColorScheme |
| 6-A | LibraryScreen.kt | Performance | 8 independent InfiniteTransitions in SkeletonItem |
| 6-B | LibraryScreen.kt | Performance | AssistChip (interactive) as a decorative badge |
| 6-C | LibraryScreen.kt | Performance | clip+border+combinedClickable modifier chain |
| 6-D | LibraryScreen.kt | Design/UX | Dropdown label, width bleed, excess padding |
| 6-E | LibraryScreen.kt | Color token | Multiple .copy(alpha) → surfaceContainer* tokens |
| 6-F | LibraryScreen.kt | Layout | Hardcoded 120dp bottom padding |
| 7-A | EditorScreen.kt | Performance + Bug | showRemoveCoverOption inside LazyColumn + wrong condition |
| 7-B | EditorScreen.kt | Layout bug | Missing contentWindowInsets after safeDrawingPadding removal |
| 7-C | EditorScreen.kt | Color token | Multiple .copy(alpha) → proper tokens |
| 8-A | RenameScreen.kt | Layout bug | Missing contentWindowInsets after safeDrawingPadding removal |
| 8-B | RenameScreen.kt | Color token | .copy(alpha) → surfaceContainerLow |
| 9-A | SettingsScreen.kt | Deprecation | ThemeMode.values() → ThemeMode.entries |
| 9-B | SettingsScreen.kt | Color token | .copy(alpha) → proper tokens |
