package com.example.dovashiapp.presentation.testcases

/** [resultsByKey] is keyed by [resultKey]. */
fun buildTestCasesUiState(catalog: List<TestCase>, resultsByKey: Map<String, TestCaseResult>): TestCasesUiState {
    val cases = catalog.map { testCase ->
        TestCaseUi(
            ticket = testCase.ticket,
            id = testCase.id,
            title = testCase.title,
            expected = testCase.expected,
            priority = testCase.priority,
            isAuto = testCase.isAuto,
            result = resultsByKey[resultKey(testCase.ticket, testCase.id)] ?: TestCaseResult.UNSET,
        )
    }
    val groups = cases.groupBy { it.ticket }.map { (ticket, items) -> TicketGroupUi(ticket, items) }
    return TestCasesUiState(
        groups = groups,
        passed = cases.count { it.result == TestCaseResult.PASS },
        failed = cases.count { it.result == TestCaseResult.FAIL },
        total = cases.size,
    )
}

fun resultKey(ticket: String, id: String): String = "$ticket/$id"
