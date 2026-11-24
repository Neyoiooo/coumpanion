package com.coucou.android.core

/**
 * Hide desktop chats, approval requests and questions from screenshots, recordings and recents.
 * Pure, so the list is a unit test; MainActivity sets or clears the flag from it as the screen changes.
 */
object SecureScreens {
    fun needed(
        screen: Screen,
        approvalSheetShown: Boolean,
        questionSheetShown: Boolean,
    ): Boolean = screen == Screen.CHAT || approvalSheetShown || questionSheetShown
}
