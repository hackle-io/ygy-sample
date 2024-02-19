package io.hackle.ygy

import android.webkit.JavascriptInterface
import org.json.JSONObject

class HackleAbTestJavascriptInterface(
    private val repository: HackleRepository
) {

    /**
     * ts
     * const decision = useVariationDetail(key)
     * JavascriptInterface.updateAbTest(JSON.stringify(decision))
     */
    @JavascriptInterface
    fun updateAbTest(json: String) {
        val decision = JSONObject(json)
        // 분배 결과가 유효하지 않은경우 저장소에 반영하지 않음
        val abTest = HackleAbTest.createIfValid(decision) ?: return
        repository.updateAbTest(abTest)
    }
}
