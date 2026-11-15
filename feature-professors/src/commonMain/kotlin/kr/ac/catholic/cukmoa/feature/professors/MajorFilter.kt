package kr.ac.catholic.cukmoa.feature.professors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kr.ac.catholic.cukmoa.core.resources.Res
import kr.ac.catholic.cukmoa.core.resources.all
import kr.ac.catholic.cukmoa.core.theme.Space
import kr.ac.catholic.cukmoa.core.ui.FilterChip
import kr.ac.catholic.cukmoa.core.ui.FastScroller
import kr.ac.catholic.cukmoa.core.ui.contentWidth
import kr.ac.catholic.cukmoa.core.ui.indexLetter
import org.jetbrains.compose.resources.stringResource

/**
 * Professor filter: one chip per major I picked (1st, then 2nd if any), then All.
 * selected is the index into majors, or null for All.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MajorFilter(majors: List<String>, selected: Int?, onSelect: (Int?) -> Unit) {
    FlowRow(
        Modifier.contentWidth().padding(horizontal = Space.l),
        horizontalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        majors.forEachIndexed { i, name -> FilterChip(name, selected == i) { onSelect(i) } }
        FilterChip(stringResource(Res.string.all), selected == null) { onSelect(null) }
    }
}

/** Launcher-style fast scroller over name-sorted rows that start at list item firstIndex. */
@Composable
internal fun <T> BoxScope.NameIndex(
    rows: List<T>,
    name: (T) -> String,
    state: LazyListState,
    firstIndex: Int,
    onReveal: (Int) -> Unit = {},
) {
    val letters = remember(rows) { rows.map { indexLetter(name(it)) } }
    FastScroller(state, letters, firstIndex, onReveal)
}
