package kr.ac.catholic.cukmoa.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kr.ac.catholic.cukmoa.app.ModuleRegistry
import kr.ac.catholic.cukmoa.core.FeatureModule
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.ui.DataNotice
import kr.ac.catholic.cukmoa.core.ui.Group
import kr.ac.catholic.cukmoa.core.ui.ListRow
import kr.ac.catholic.cukmoa.core.ui.ScreenList
import kr.ac.catholic.cukmoa.core.ui.Section
import kr.ac.catholic.cukmoa.core.ui.Tone
import kr.ac.catholic.cukmoa.core.ui.Txt

/** Module list + on/off switches. Shared by onboarding and the module manager. */
@Composable
fun ModuleList(enabled: List<String>, onChange: (List<String>) -> Unit) {
    val c = AppTheme.colors
    val full = enabled.size >= ModuleRegistry.MAX_TABS
    Group {
        ModuleRegistry.all.forEachIndexed { i, m ->
            val on = m.id in enabled
            ListRow(
                m.title,
                m.summary + if (m.needsNetwork) " · 인터넷 필요" else "",
                divider = i < ModuleRegistry.all.lastIndex,
                leading = { ModuleIcon(m, on) },
                trailing = {
                    Switch(
                        checked = on,
                        enabled = on || !full,
                        onCheckedChange = { checked ->
                            // Order follows the registry order.
                            val next = if (checked) enabled + m.id else enabled - m.id
                            onChange(ModuleRegistry.all.map { it.id }.filter { it in next })
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = c.accent),
                    )
                },
            )
        }
    }
}

@Composable
private fun ModuleIcon(m: FeatureModule, on: Boolean) {
    val c = AppTheme.colors
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(if (on) c.accentSoft else c.fill),
        contentAlignment = Alignment.Center,
    ) { Icon(m.icon, null, tint = if (on) c.accent else c.ink3, modifier = Modifier.size(20.dp)) }
}

/** Module manager screen (dashboard → settings). */
@Composable
fun ModulesScreen(enabled: List<String>, onChange: (List<String>) -> Unit, onBack: () -> Unit) {
    ScreenList(
        title = "기능",
        eyebrow = "필요한 기능만 골라 쓰기",
        trailing = {
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(AppTheme.colors.surface).clickable(role = Role.Button, onClickLabel = "뒤로", onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로", tint = AppTheme.colors.ink, modifier = Modifier.size(18.dp)) }
        },
    ) {
        item {
            Section("기능", "켠 기능은 아래 탭과 홈 카드에 나타나요. 탭은 최대 ${ModuleRegistry.MAX_TABS}개예요.") {
                ModuleList(enabled, onChange)
            }
        }
        item {
            Section("앞으로 추가될 기능") {
                Group {
                    listOf(
                        "현직자 답장 게시판" to "선배·현직자에게 직접 묻고 답을 모아 보기",
                        "AI 수강 정리" to "고른 정보를 AI가 간단히 정리 (서버 함수 경유, 키 노출 없음)",
                        "시간표" to "담은 과목으로 시간표 겹침 확인",
                    ).forEachIndexed { i, (t, d) ->
                        ListRow(t, d, divider = i < 2, trailing = { Txt("준비 중", AppTheme.type.caption, tone = Tone.Ink3) })
                    }
                }
            }
        }
        item {
            DataNotice(
                "Android는 기능을 켤 때 내려받는 방식(Play Feature Delivery)으로 앱 크기를 줄일 예정이에요." +
                    "iOS는 앱 스토어 규칙상 모든 기능을 앱에 포함하고 켜고 끄는 방식이에요.",
            )
        }
    }
}
