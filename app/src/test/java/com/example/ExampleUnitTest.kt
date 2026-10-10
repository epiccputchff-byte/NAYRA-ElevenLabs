package com.example

import com.example.data.GeminiContent
import com.example.data.GeminiPart
import com.example.data.GeminiRepository
import com.example.data.GeminiRequest
import com.example.ui.ChatMessage
import com.example.ui.MessageSender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testCleanTextForSpeech_stripsMarkdownSymbols() {
        val markdownText = "**Hello!** I am *NAYRA*, your #1 assistant. Check this `code`: [link](url)"
        val cleaned = GeminiRepository.cleanTextForSpeech(markdownText)
        assertFalse(cleaned.contains("*"))
        assertFalse(cleaned.contains("#"))
        assertFalse(cleaned.contains("`"))
        assertTrue(cleaned.contains("Hello!"))
        assertTrue(cleaned.contains("NAYRA"))
    }

    @Test
    fun testChatMessage_defaultsAndSender() {
        val userMsg = ChatMessage(sender = MessageSender.USER, text = "नमस्ते NAYRA")
        assertEquals(MessageSender.USER, userMsg.sender)
        assertEquals("नमस्ते NAYRA", userMsg.text)
        assertFalse(userMsg.id.isEmpty())
        assertFalse(userMsg.timestamp.isEmpty())
    }

    @Test
    fun testGeminiRequest_structure() {
        val contents = listOf(
            GeminiContent(role = "user", parts = listOf(GeminiPart("Tell me a fact")))
        )
        val request = GeminiRequest(contents = contents)
        assertEquals(1, request.contents.size)
        assertEquals("user", request.contents[0].role)
        assertEquals("Tell me a fact", request.contents[0].parts[0].text)
    }
}
