package com.example.manusagentapp.core

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.delay

/**
 * اليد (The Hands) - منفذ الإجراءات
 */
class Hands(private val accessibilityService: AccessibilityService) {
    
    suspend fun click(x: Float, y: Float): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        
        return accessibilityService.dispatchGesture(gesture, null, null)
    }
    
    suspend fun clickElement(element: UIElement): Boolean {
        val centerX = ((element.bounds.left + element.bounds.right) / 2).toFloat()
        val centerY = ((element.bounds.top + element.bounds.bottom) / 2).toFloat()
        return click(centerX, centerY)
    }
    
    suspend fun clickBounds(bounds: SerializableRect): Boolean { // <-- تم التغيير هنا
        val centerX = ((bounds.left + bounds.right) / 2).toFloat()
        val centerY = ((bounds.top + bounds.bottom) / 2).toFloat()
        return click(centerX, centerY)
    }
    
    suspend fun typeText(element: UIElement, text: String): Boolean {
        if (!clickElement(element)) {
            return false
        }
        
        delay(500)
        
        val rootNode = accessibilityService.rootInActiveWindow
        val targetNode = findNodeByBounds(rootNode, Rect(element.bounds.left, element.bounds.top, element.bounds.right, element.bounds.bottom))
        
        return if (targetNode != null) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            val success = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            targetNode.recycle()
            success
        } else {
            false
        }
    }
    
    suspend fun scroll(direction: String, distance: Float = 500f): Boolean {
        val displayMetrics = accessibilityService.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels.toFloat()
        val screenHeight = displayMetrics.heightPixels.toFloat()
        
        val path = Path()
        
        when (direction.lowercase()) {
            "up" -> {
                path.moveTo(screenWidth / 2, screenHeight * 0.7f)
                path.lineTo(screenWidth / 2, screenHeight * 0.3f)
            }
            "down" -> {
                path.moveTo(screenWidth / 2, screenHeight * 0.3f)
                path.lineTo(screenWidth / 2, screenHeight * 0.7f)
            }
            "left" -> {
                path.moveTo(screenWidth * 0.7f, screenHeight / 2)
                path.lineTo(screenWidth * 0.3f, screenHeight / 2)
            }
            "right" -> {
                path.moveTo(screenWidth * 0.3f, screenHeight / 2)
                path.lineTo(screenWidth * 0.7f, screenHeight / 2)
            }
            else -> return false
        }
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()
        
        return accessibilityService.dispatchGesture(gesture, null, null)
    }
    
    suspend fun longPress(element: UIElement): Boolean {
        val centerX = ((element.bounds.left + element.bounds.right) / 2).toFloat()
        val centerY = ((element.bounds.top + element.bounds.bottom) / 2).toFloat()
        
        val path = Path().apply {
            moveTo(centerX, centerY)
        }
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 1000))
            .build()
        
        return accessibilityService.dispatchGesture(gesture, null, null)
    }
    
    suspend fun drag(fromX: Float, fromY: Float, toX: Float, toY: Float): Boolean {
        val path = Path().apply {
            moveTo(fromX, fromY)
            lineTo(toX, toY)
        }
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 500))
            .build()
        
        return accessibilityService.dispatchGesture(gesture, null, null)
    }
    
    suspend fun pressBack(): Boolean {
        return accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }
    
    suspend fun pressHome(): Boolean {
        return accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }
    
    suspend fun openRecents(): Boolean {
        return accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
    }
    
    private fun findNodeByBounds(rootNode: AccessibilityNodeInfo?, targetBounds: Rect): AccessibilityNodeInfo? {
        if (rootNode == null) return null
        
        val nodeBounds = Rect()
        rootNode.getBoundsInScreen(nodeBounds)
        
        if (nodeBounds == targetBounds) {
            return rootNode
        }
        
        for (i in 0 until rootNode.childCount) {
            val childNode = rootNode.getChild(i)
            if (childNode != null) {
                val result = findNodeByBounds(childNode, targetBounds)
                if (result != null) {
                    childNode.recycle()
                    return result
                }
                childNode.recycle()
            }
        }
        
        return null
    }
    
    suspend fun wait(milliseconds: Long) {
        delay(milliseconds)
    }
}
