package com.preappointment1.app.data

import com.preappointment1.app.data.api.ApiClient
import com.preappointment1.app.data.model.UpdateSubscriptionRequest
import com.preappointment1.app.ui.screens.FollowUpUi
import com.preappointment1.app.ui.screens.toFollowUpUi

suspend fun updateFollowUpSchedule(
    followUpId: String,
    newSchedule: Map<String, List<String>>
): FollowUpUi? {
    ApiClient.apiService.patchSubscription(
        followUpId,
        UpdateSubscriptionRequest(
            parameters = mapOf("schedule" to newSchedule)
        )
    )
    val subscriptions = ApiClient.apiService.getSubscriptions()
    val agents = ApiClient.apiService.getAgents().associateBy { it.id }
    return subscriptions.map { it.toFollowUpUi(agents) }.find { it.id == followUpId }
}
