package com.example.brainclean

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BrainCleanAppUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun addingThoughtInInbox_showsThoughtInInboxList() {
        val thoughtText = uniqueThought("Add")

        addThoughtInInbox(thoughtText)

        composeRule.onNodeWithText(thoughtText).assertExists()
    }

    @Test
    fun movingThoughtToToday_showsThoughtOnTodayTab() {
        val thoughtText = uniqueThought("Move")

        addThoughtInInbox(thoughtText)
        moveNewestInboxThoughtToToday()

        composeRule.onNodeWithTag("tab_today").performClick()
        composeRule.waitForText(thoughtText)
        composeRule.onNodeWithText(thoughtText).assertExists()
    }

    @Test
    fun searchFiltersOnlyWithinCurrentlySelectedTab() {
        val inboxThought = uniqueThought("InboxSearch")
        val todayThought = uniqueThought("TodaySearch")

        addThoughtInInbox(inboxThought)
        addThoughtInInbox(todayThought)
        moveNewestInboxThoughtToToday()

        composeRule.onNodeWithTag("search_input").performTextClearance()
        composeRule.onNodeWithTag("search_input").performTextInput(inboxThought)
        composeRule.waitForText(inboxThought)

        composeRule.onNodeWithText(inboxThought).assertExists()
        composeRule.onNodeWithText(todayThought).assertDoesNotExist()

        composeRule.onNodeWithTag("tab_today").performClick()
        composeRule.waitForText(todayThought)
        composeRule.onNodeWithText(todayThought).assertExists()
    }

    private fun addThoughtInInbox(thoughtText: String) {
        composeRule.onNodeWithTag("tab_inbox").performClick()
        composeRule.onNodeWithTag("inbox_input").performTextClearance()
        composeRule.onNodeWithTag("inbox_input").performTextInput(thoughtText)
        composeRule.onNodeWithTag("add_thought_button").performClick()
        composeRule.waitForText(thoughtText)
    }

    private fun moveNewestInboxThoughtToToday() {
        composeRule.onAllNodes(
            matcher = hasText("Today") and
                hasClickAction() and
                hasAnyAncestor(hasTestTag("inbox_list"))
        )[0].performClick()
    }

    private fun uniqueThought(prefix: String): String {
        return "$prefix thought ${System.currentTimeMillis()}"
    }
}

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.waitForText(text: String) {
    waitUntil(timeoutMillis = 5_000) {
        onAllNodes(hasText(text), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }
}
