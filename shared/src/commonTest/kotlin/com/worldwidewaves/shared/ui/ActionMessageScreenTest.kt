package com.worldwidewaves.shared.ui

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.MokoRes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Unit tests for ActionMessageScreen.
 *
 * Tests verify:
 * - Screen properties (name)
 * - String resource accessibility
 * - Screen instantiation
 */
class ActionMessageScreenTest {
    @Test
    fun actionMessageScreen_hasCorrectName() {
        val screen = ActionMessageScreen()
        assertEquals("Action", screen.name, "ActionMessageScreen name should be 'Action'")
    }

    @Test
    fun actionMessageScreen_tabStringResourceExists() {
        assertNotNull(
            MokoRes.strings.action_message_tab,
            "action_message_tab string resource should exist",
        )
    }

    @Test
    fun actionMessageScreen_titleStringResourceExists() {
        assertNotNull(
            MokoRes.strings.action_message_title,
            "action_message_title string resource should exist",
        )
    }

    @Test
    fun actionMessageScreen_allPhraseStringResourcesExist() {
        assertNotNull(
            MokoRes.strings.action_message_phrase_1,
            "action_message_phrase_1 string resource should exist",
        )
        assertNotNull(
            MokoRes.strings.action_message_phrase_2,
            "action_message_phrase_2 string resource should exist",
        )
        assertNotNull(
            MokoRes.strings.action_message_phrase_3,
            "action_message_phrase_3 string resource should exist",
        )
        assertNotNull(
            MokoRes.strings.action_message_phrase_4,
            "action_message_phrase_4 string resource should exist",
        )
        assertNotNull(
            MokoRes.strings.action_message_phrase_5,
            "action_message_phrase_5 string resource should exist",
        )
        assertNotNull(
            MokoRes.strings.action_message_phrase_6,
            "action_message_phrase_6 string resource should exist",
        )
    }

    @Test
    fun actionMessageScreen_allSectionStringResourcesExist() {
        assertNotNull(
            MokoRes.strings.action_message_section_1,
            "action_message_section_1 string resource should exist",
        )
        assertNotNull(
            MokoRes.strings.action_message_section_2,
            "action_message_section_2 string resource should exist",
        )
        assertNotNull(
            MokoRes.strings.action_message_section_3,
            "action_message_section_3 string resource should exist",
        )
    }

    @Test
    fun actionMessageScreen_instantiatesSuccessfully() {
        val screen = ActionMessageScreen()
        assertNotNull(screen, "ActionMessageScreen should instantiate successfully")
        assertEquals("Action", screen.name)
    }
}
