package io.hackle.ygy

import io.hackle.sdk.common.decision.Decision
import org.json.JSONObject

data class HackleAbTest(

    /**
     * iLog abtest_id 컬럼
     */
    val id: Long,

    /**
     * iLog abtest_group 컬럼
     */
    val group: String,

    /**
     * iLog abtest_version 컬럼 (신규)
     */
    val version: Int,

    /**
     * iLog abtest_decision_reason 컬럼 (신규)
     */
    val decisionReason: String,
) {

    companion object {

        fun createIfValid(decision: Decision): HackleAbTest? {
            val experiment = decision.experiment ?: return null
            return HackleAbTest(
                id = experiment.key,
                group = decision.variation.name,
                version = experiment.version,
                decisionReason = decision.reason.name,
            )
        }

        fun createIfValid(decision: JSONObject): HackleAbTest? {
            if (!decision.has("experiment")) {
                return null
            }
            val experiment = decision.getJSONObject("experiment")
            return HackleAbTest(
                id = experiment.getLong("key"),
                group = decision.getString("variation"),
                version = experiment.getInt("version"),
                decisionReason = decision.getString("decisionReason")
            )
        }
    }
}
