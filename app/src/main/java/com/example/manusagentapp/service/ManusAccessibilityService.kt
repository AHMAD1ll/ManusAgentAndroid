package com.example.manusagentapp.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast // <<< IMPORT ADDED

class ManusAccessibilityService : AccessibilityService() {

    private val TAG = "ManusAccessibilityService"

    /**
     * This function is called by the system when the service is first connected.
     * We will show a Toast message to confirm it's running.
     */
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Manus Agent Service CONNECTED and ready!")

        // Show a confirmation message on the screen
        Toast.makeText(this, "Manus Agent Service: CONNECTED", Toast.LENGTH_SHORT).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We will add logic here later.
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted.")
        Toast.makeText(this, "Manus Agent Service: Interrupted", Toast.LENGTH_SHORT).show()
    }

    /**
     * This function is called by the system when the service is disconnected.
     * We will show a Toast message to confirm it has stopped.
     */
    override fun onUnbind(intent: android.content.Intent?): Boolean {
        Log.d(TAG, "Manus Agent Service DISCONNECTED.")
        
        // Show a confirmation message on the screen
        Toast.makeText(this, "Manus Agent Service: DISCONNECTED", Toast.LENGTH_SHORT).show()
        
        return super.onUnbind(intent)
    }
}
