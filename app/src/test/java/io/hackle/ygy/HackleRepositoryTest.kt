package io.hackle.ygy

import io.hackle.android.HackleApp
import io.hackle.sdk.common.ParameterConfig
import io.hackle.sdk.common.User
import io.hackle.sdk.common.Variation.A
import io.hackle.sdk.common.Variation.B
import io.hackle.sdk.common.decision.Decision
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.common.decision.DecisionReason.EXPERIMENT_NOT_FOUND
import io.hackle.sdk.common.decision.DecisionReason.TRAFFIC_ALLOCATED
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class HackleRepositoryTest {

    @Test
    fun `getAbTestGroupWithVariationDetail`() {
        val hackleApp = mockk<HackleApp>()
        val dataSource = DataSourceStub()
        val sut = HackleRepository(hackleApp, dataSource)

        every { hackleApp.variationDetail(42, any<User>()) } returns Decision.of(A, EXPERIMENT_NOT_FOUND)

        val variation1 = sut.getAbTestGroupWithVariationDetail(42)
        assertEquals("A", variation1)
        assertEquals(emptyMap<Long, HackleAbTest>(), dataSource.getHackleAbTests())

        every { hackleApp.variationDetail(43, any<User>()) } returns Decision.of(
            B,
            TRAFFIC_ALLOCATED,
            ParameterConfig.empty(),
            HackleExperimentStub(43, 1)
        )
        val variation2 = sut.getAbTestGroupWithVariationDetail(43)
        assertEquals("B", variation2)
        assertEquals(
            mapOf(43L to HackleAbTest(43, "B", 1, DecisionReason.TRAFFIC_ALLOCATED.name)),
            dataSource.getHackleAbTests()
        )
    }

    @Test
    fun `진행중인 AB 테스트는 업데이트한다`() {
        // given
        val dataSource = DataSourceStub()
        val sut = HackleRepository(mockk(relaxed = true), dataSource)
        sut.setAbTests(emptyMap())

        // when
        sut.updateAbTest(HackleAbTest(42, "B", 1, DecisionReason.TRAFFIC_ALLOCATED.name))
        sut.updateAbTest(HackleAbTest(43, "A", 1, DecisionReason.OVERRIDDEN.name))

        // then
        assertEquals(
            mapOf(
                42L to HackleAbTest(42, "B", 1, DecisionReason.TRAFFIC_ALLOCATED.name),
                43L to HackleAbTest(43, "A", 1, DecisionReason.OVERRIDDEN.name),
            ),
            dataSource.getHackleAbTests()
        )
    }

    @Test
    fun `완료된 AB 테스트는 제거한다`() {
        // given
        val dataSource = DataSourceStub()
        val sut = HackleRepository(mockk(relaxed = true), dataSource)
        sut.setAbTests(
            mapOf(
                42L to HackleAbTest(42, "B", 1, DecisionReason.TRAFFIC_ALLOCATED.name),
                43L to HackleAbTest(43, "A", 1, DecisionReason.OVERRIDDEN.name),
            )
        )

        // when
        sut.updateAbTest(HackleAbTest(42, "B", 1, DecisionReason.EXPERIMENT_COMPLETED.name))
        sut.updateAbTest(HackleAbTest(43, "A", 1, DecisionReason.EXPERIMENT_DRAFT.name))

        // then
        assertEquals(
            emptyMap<Long, HackleAbTest>(),
            dataSource.getHackleAbTests()
        )
    }

    @Test
    fun `현재 AB 테스트 버전과 이전 AB 테스트 버전이 다른경우 제거한다`() {
        // given
        val dataSource = DataSourceStub()
        val sut = HackleRepository(mockk(relaxed = true), dataSource)
        sut.setAbTests(
            mapOf(
                42L to HackleAbTest(42, "B", 1, DecisionReason.TRAFFIC_ALLOCATED.name),
                43L to HackleAbTest(43, "A", 1, DecisionReason.OVERRIDDEN.name),
            )
        )

        // when
        sut.updateAbTest(HackleAbTest(42, "B", 2, DecisionReason.NOT_IN_EXPERIMENT_TARGET.name))
        sut.updateAbTest(HackleAbTest(43, "A", 2, DecisionReason.NOT_IN_EXPERIMENT_TARGET.name))

        // then
        assertEquals(
            emptyMap<Long, HackleAbTest>(),
            dataSource.getHackleAbTests()
        )
    }

    @Test
    fun `버전이 유지되는 채로 특정 사유들은 이전 분배 결과를 유지한다`() {
        // given
        val dataSource = DataSourceStub()
        val sut = HackleRepository(mockk(relaxed = true), dataSource)
        sut.setAbTests(
            mapOf(
                42L to HackleAbTest(42, "B", 1, DecisionReason.TRAFFIC_ALLOCATED.name),
                43L to HackleAbTest(43, "A", 1, DecisionReason.OVERRIDDEN.name),
            )
        )

        // when
        sut.updateAbTest(HackleAbTest(42, "A", 1, DecisionReason.NOT_IN_EXPERIMENT_TARGET.name))
        sut.updateAbTest(HackleAbTest(43, "A", 1, DecisionReason.NOT_IN_EXPERIMENT_TARGET.name))

        // then
        assertEquals(
            mapOf(
                42L to HackleAbTest(42, "B", 1, DecisionReason.TRAFFIC_ALLOCATED.name),
                43L to HackleAbTest(43, "A", 1, DecisionReason.OVERRIDDEN.name),
            ),
            dataSource.getHackleAbTests()
        )
    }
}