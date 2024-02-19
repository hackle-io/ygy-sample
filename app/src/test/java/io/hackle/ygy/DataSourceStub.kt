package io.hackle.ygy

class DataSourceStub : DataSource {

    private var abTests: Map<Long, HackleAbTest> = emptyMap()

    override fun getHackleAbTests(): Map<Long, HackleAbTest> {
        return HashMap(abTests)
    }

    override fun setHackleAbTests(abTests: Map<Long, HackleAbTest>) {
        this.abTests = abTests
    }
}
