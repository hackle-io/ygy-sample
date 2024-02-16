package io.hackle.ygy

interface DataSource {

    fun getHackleAbTests(): Map<Long, HackleAbTest>

    fun setHackleAbTests(abTests: Map<Long, HackleAbTest>)
}