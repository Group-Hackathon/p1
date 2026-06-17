package com.preappointment1.app.shared.agent

import com.preappointment1.app.shared.core.DummyCore

class DummyAgent {
    fun getMessage(): String {
        val core = DummyCore()
        return "Hello from Agent Module! Derived from core: ${core.message}"
    }
}
