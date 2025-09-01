package com.example.manusagentapp.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class ManusAccessibilityService : AccessibilityService() {

    private val TAG = "ManusAccessibilityService"

    /**
     * This function is called by the system when the service is first connected (i.e., when the user enables it).
     * It's the perfect place to do one-time setup.
     */
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "==============================================")
        Log.d(TAG, "Manus Agent Service CONNECTED and ready!")
        Log.d(TAG, "Next step: Initialize the AI model here.")
        Log.d(TAG, "==============================================")
    }

    /**
     * This function is called every time an event happens on the screen
     * (e.g., a button is clicked, a window changes, text is typed, etc.).
     * This is where we will eventually listen for user commands.
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We will add logic here later. For now, we just log the event type.
        // Log.d(TAG, "Accessibility Event Received: ${event?.eventType}")
    }

    /**
     * This function is called when the system wants to interrupt the feedback
     * your service is providing, usually in response to a user action like moving focus.
     */
    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted.")
    }

    /**
     * This function is called by the system when the service is disconnected (i.e., when the user disables it).
     */
    override fun onUnbind(intent: android.content.Intent?): Boolean {
        Log.d(TAG, "==============================================")
        Log.d(TAG, "Manus Agent Service DISCONNECTED.")
        Log.d(TAG, "==============================================")
        return super.onUnbind(intent)
    }
}
