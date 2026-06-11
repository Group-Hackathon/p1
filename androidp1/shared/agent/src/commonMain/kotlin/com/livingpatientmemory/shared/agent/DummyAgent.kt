package com.livingpatientmemory.shared.agent

import com.livingpatientmemory.shared.core.DummyCore

class DummyAgent {
    fun getMessage(): String {
        val core = DummyCore()
        return "Hello from Agent Module! Derived from core: ${core.message}"
    }
}
