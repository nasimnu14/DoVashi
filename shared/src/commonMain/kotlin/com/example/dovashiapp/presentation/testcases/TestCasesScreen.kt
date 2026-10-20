package com.example.dovashiapp.presentation.testcases

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dovashiapp.shared.generated.resources.Res
import dovashiapp.shared.generated.resources.ic_arrow_back
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestCasesScreen(
    state: TestCasesUiState,
    onResultSelected: (ticket: String, id: String, result: TestCaseResult) -> Unit,
    onBackClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test Cases") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(painterResource(Res.drawable.ic_arrow_back), contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            SummaryBar(state)
            HorizontalDivider()
            LazyColumn(Modifier.fillMaxWidth()) {
                state.groups.forEach { group ->
                    item(key = group.ticket) { TicketHeader(group.ticket, group.cases) }
                    items(group.cases, key = { "${it.ticket}/${it.id}" }) { testCase ->
                        TestCaseRow(testCase, onResultSelected)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryBar(state: TestCasesUiState) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("${state.passed} passed", color = PASS_COLOR, style = MaterialTheme.typography.labelLarge)
        Text("${state.failed} failed", color = FAIL_COLOR, style = MaterialTheme.typography.labelLarge)
        Text("${state.total} total", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun TicketHeader(ticket: String, cases: List<TestCaseUi>) {
    val passed = cases.count { it.result == TestCaseResult.PASS }
    val failed = cases.count { it.result == TestCaseResult.FAIL }
    Surface(tonalElevation = 2.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(ticket, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("$passed/${cases.size} passed" + if (failed > 0) ", $failed failed" else "", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun TestCaseRow(testCase: TestCaseUi, onResultSelected: (ticket: String, id: String, result: TestCaseResult) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val suffix = listOfNotNull(testCase.priority, if (testCase.isAuto) "Auto" else null).joinToString(" · ")
            Text("${testCase.id} — ${testCase.title}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(testCase.expected, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(suffix, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ResultToggle(
            symbol = "✓",
            selected = testCase.result == TestCaseResult.PASS,
            selectedColor = PASS_COLOR,
            onClick = { onResultSelected(testCase.ticket, testCase.id, TestCaseResult.PASS) },
        )
        ResultToggle(
            symbol = "✗",
            selected = testCase.result == TestCaseResult.FAIL,
            selectedColor = FAIL_COLOR,
            onClick = { onResultSelected(testCase.ticket, testCase.id, TestCaseResult.FAIL) },
        )
    }
}

@Composable
private fun ResultToggle(symbol: String, selected: Boolean, selectedColor: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onClick),
        color = if (selected) selectedColor else colors.surfaceVariant,
        shape = CircleShape,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                symbol,
                textAlign = TextAlign.Center,
                color = if (selected) colors.surface else colors.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

private val PASS_COLOR = androidx.compose.ui.graphics.Color(0xFF2E7D32)
private val FAIL_COLOR = androidx.compose.ui.graphics.Color(0xFFC62828)
