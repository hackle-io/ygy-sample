package io.hackle.ygy

import io.hackle.sdk.common.HackleExperiment

data class HackleExperimentStub(
    override val key: Long,
    override val version: Int
) : HackleExperiment
