package io.hackle.ygy

import io.hackle.android.HackleApp
import io.hackle.sdk.common.User
import io.hackle.sdk.common.decision.Decision
import io.hackle.sdk.common.decision.DecisionReason
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap


class HackleRepository(
    private val hackleApp: HackleApp,
    private val dataSource: DataSource
) {

    private val abTests = ConcurrentHashMap<Long, HackleAbTest>()

    /**
     * AB 테스트 분배 결과를 가져옴.
     * 분배된 결과를 저장소에 반영함.
     */
    fun getAbTestGroupWithVariationDetail(experimentKey: Long): String {
        val user = getUser()
        val decision = hackleApp.variationDetail(experimentKey, user)

        val abTest = HackleAbTest.createIfValid(decision)

        // 분배 결과가 유효하지 않은경우 저장소에 반영하지 않음
        if (abTest != null) {
            updateAbTest(abTest)
        }

        return decision.variation.name
    }

    /**
     * 분배 결과를 저장소에 반영함.
     * 이 메소드는 App, WebView 두곳에서 모두 호출 될 수 있음
     *
     * @see HackleRepository.getAbTestGroupWithVariationDetail App
     * @see HackleAbTestJavascriptInterface.updateAbTest WebView
     */
    fun updateAbTest(abTest: HackleAbTest) {
        abTests.compute(abTest.id) { _, previousAbTest -> resolve(previousAbTest, abTest) }
        dataSource.setHackleAbTests(abTests.toMap())
    }

    /**
     * 이전 분배결과 [previousAbTest]와 현재 분배결과 [currentAbTest]를 기준으로 저장소에 저장할 분배 결과를 리턴한다.
     * null 을 리턴하면 저장되어 있는 분배 결과를 제거한다.
     */
    private fun resolve(previousAbTest: HackleAbTest?, currentAbTest: HackleAbTest): HackleAbTest? {

        /**
         * 현재 AB 테스트가 진행중인 경우 현재 분배 결과로 업데이트
         */
        if (currentAbTest.decisionReason in RUNNING_DECISION_REASONS) {
            return currentAbTest
        }

        /**
         * 현재 AB 테스트가 완료된 경우 null 을 리턴하여 분배결과를 제거함
         */
        if (currentAbTest.decisionReason in COMPLETED_DECISION_REASONS) {
            return null
        }

        /**
         * 현재 AB테스트의 버전과 이전 AB테스트의 버전이 다른경우 이전 AB테스트가 종료되었기 때문에 null 을 리턴하여 이전 분배 결과 정보를 제거함
         */
        if (currentAbTest.version != previousAbTest?.version) {
            return null
        }

        /**
         * 위 경우가 아니면 이전 AB테스트 분배 결과를 유지함
         */
        return previousAbTest
    }

    /**
     * 앱 초기화시 호출
     *
     * Hackle SDK 의 allVariationDetails 메소드를 호출하여 응답받은 분배 결과중 완료된 AB 테스트인 경우 저장소에서 제거한다.
     */
    fun init() {
        val savedAbTests = dataSource.getHackleAbTests()
        val decisions = hackleApp.allVariationDetails()

        // 완료된 AB테스트는 제거함
        val abTestsWithoutCompleted = savedAbTests
            .filterNot { (id, abTest) -> isAbTestCompleted(abTest, decisions[id]) }


        this.setAbTests(abTestsWithoutCompleted)
    }

    internal fun setAbTests(abTests: Map<Long, HackleAbTest>) {
        dataSource.setHackleAbTests(abTests)
        this.abTests.putAll(abTests)
    }

    /**
     * 저장되어있는 분배 정보와 allVariationDetails 를 호출하여 응답받은 결과로 완료된 AB테스트인지 판단한다.
     */
    private fun isAbTestCompleted(previousAbTest: HackleAbTest, currentDecision: Decision?): Boolean {

        /**
         * 현재 분배 결과가 없는 경우는 AB 테스트가 보관 처리되어서 종료된 경우
         */
        if (currentDecision == null) {
            return true
        }

        val currentAbTest = HackleAbTest.createIfValid(currentDecision)

        /**
         * 분배 결과가 유효하지 않은경우 완료 되었는지 판단 할 수 없음
         */
        if (currentAbTest == null) {
            return false
        }

        /**
         * AB 테스트가 완료된 경우 제거 대상
         */
        if (currentAbTest.decisionReason in COMPLETED_DECISION_REASONS) {
            return true
        }

        /**
         * 현재 AB 테스트 버전과 이전 AB 테스트의 버전이 다른경우 이전 AB테스트 완료후 버전이 올라간 경우 (= 이전 AB 테스트 완료됨)
         */
        if (currentAbTest.version != previousAbTest.version) {
            return true
        }

        /**
         * 위 사유들이 아닌경우 분배 결과를 유지
         */
        return false
    }


    private fun getUser(): User {
        return User.builder()
            .id(UUID.randomUUID().toString())
            .build()
    }

    companion object {

        /**
         * 진행중으로 판단하는 분배 사유
         */
        private val RUNNING_DECISION_REASONS = hashSetOf(

            /**
             * AB 테스트 참여한 경우
             */
            DecisionReason.TRAFFIC_ALLOCATED.name,

            /**
             * 테스트 기기인 경우
             */
            DecisionReason.OVERRIDDEN.name,
        )

        /**
         * 완료로 판단하는 분배 사유
         */
        private val COMPLETED_DECISION_REASONS = hashSetOf(

            /**
             * AB 테스트 완료 상태
             */
            DecisionReason.EXPERIMENT_COMPLETED.name,

            /**
             * AB 테스트가 시작 전인 경우 (완료후 버전업 되었을때)
             */
            DecisionReason.EXPERIMENT_DRAFT.name,
        )
    }
}
