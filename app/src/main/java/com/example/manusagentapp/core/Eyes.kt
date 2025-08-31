package com.example.manusagentapp.core

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.serialization.Serializable

/**
 * العين (The Eyes) - محلل الشاشة
 */
class Eyes(private val accessibilityService: AccessibilityService) {
    
    fun analyzeCurrentScreen(): ScreenContext {
        val rootNode = accessibilityService.rootInActiveWindow
        val elements = mutableListOf<UIElement>()
        
        if (rootNode != null) {
            extractUIElements(rootNode, elements)
            rootNode.recycle()
        }
        
        return ScreenContext(
            elements = elements,
            timestamp = System.currentTimeMillis()
        )
    }
    
    private fun extractUIElements(node: AccessibilityNodeInfo, elements: MutableList<UIElement>) {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        
        val element = UIElement(
            id = node.viewIdResourceName ?: "",
            text = node.text?.toString() ?: "",
            contentDescription = node.contentDescription?.toString() ?: "",
            className = node.className?.toString() ?: "",
            bounds = bounds.toSerializableRect(), // <-- تم التغيير هنا
            isClickable = node.isClickable,
            isEditable = node.isEditable,
            isScrollable = node.isScrollable,
            isVisible = node.isVisibleToUser
        )
        
        if (element.isUseful()) {
            elements.add(element)
        }
        
        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            if (childNode != null) {
                extractUIElements(childNode, elements)
                childNode.recycle()
            }
        }
    }
    
    fun findElementByText(screenContext: ScreenContext, text: String): UIElement? {
        return screenContext.elements.find { 
            it.text.contains(text, ignoreCase = true) ||
            it.contentDescription.contains(text, ignoreCase = true)
        }
    }
    
    fun findElementByType(screenContext: ScreenContext, type: String): UIElement? {
        return when (type.lowercase()) {
            "button" -> screenContext.elements.find { 
                it.className.contains("Button", ignoreCase = true) && it.isClickable 
            }
            "input", "edittext" -> screenContext.elements.find { 
                it.isEditable || it.className.contains("EditText", ignoreCase = true)
            }
            "search" -> screenContext.elements.find {
                it.text.contains("بحث", ignoreCase = true) ||
                it.contentDescription.contains("search", ignoreCase = true) ||
                it.className.contains("SearchView", ignoreCase = true)
            }
            "scroll" -> screenContext.elements.find { it.isScrollable }
            else -> null
        }
    }
    
    fun convertToTextDescription(screenContext: ScreenContext): String {
        val description = StringBuilder()
        description.append("الشاشة الحالية تحتوي على:\n")
        
        screenContext.elements.forEach { element ->
            when {
                element.isClickable -> {
                    description.append("- زر قابل للنقر: ${element.text.ifEmpty { element.contentDescription }}\n")
                }
                element.isEditable -> {
                    description.append("- حقل إدخال: ${element.text.ifEmpty { "فارغ" }}\n")
                }
                element.isScrollable -> {
                    description.append("- منطقة قابلة للتمرير\n")
                }
                element.text.isNotEmpty() -> {
                    description.append("- نص: ${element.text}\n")
                }
            }
        }
        
        return description.toString()
    }
}

@Serializable
data class ScreenContext(
    val elements: List<UIElement>,
    val timestamp: Long
) {
    fun findElementByText(text: String): UIElement? {
        return elements.find { 
            it.text.contains(text, ignoreCase = true) ||
            it.contentDescription.contains(text, ignoreCase = true)
        }
    }
    
    fun findElementByType(type: String): UIElement? {
        return when (type.lowercase()) {
            "button" -> elements.find { 
                it.className.contains("Button", ignoreCase = true) && it.isClickable 
            }
            "input", "edittext" -> elements.find { 
                it.isEditable || it.className.contains("EditText", ignoreCase = true)
            }
            "search" -> elements.find {
                it.text.contains("بحث", ignoreCase = true) ||
                it.contentDescription.contains("search", ignoreCase = true) ||
                it.className.contains("SearchView", ignoreCase = true)
            }
            else -> null
        }
    }
}

@Serializable
data class UIElement(
    val id: String,
    val text: String,
    val contentDescription: String,
    val className: String,
    val bounds: SerializableRect, // <-- تم التغيير هنا
    val isClickable: Boolean,
    val isEditable: Boolean,
    val isScrollable: Boolean,
    val isVisible: Boolean
) {
    fun isUseful(): Boolean {
        return isVisible && (
            isClickable || 
            isEditable || 
            isScrollable || 
            text.isNotEmpty() || 
            contentDescription.isNotEmpty()
        )
    }
}
