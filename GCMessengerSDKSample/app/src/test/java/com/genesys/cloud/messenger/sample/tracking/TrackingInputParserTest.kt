package com.genesys.cloud.messenger.sample.tracking

import com.genesys.cloud.integration.messenger.tracking.MessengerTracking
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingInputParserTest {

    // region parsePairs

    @Test
    fun parsePairs_whenEmptyInput_returnsEmptyMap() {
        //given
        val input = ""

        //when
        val result = TrackingInputParser.parsePairs(input)

        //then
        assertTrue(result.isEmpty())
    }

    @Test
    fun parsePairs_whenSemicolonSeparator_parsesAllPairs() {
        //given
        val input = "screenName=Home;searchQuery=shoes"

        //when
        val result = TrackingInputParser.parsePairs(input)

        //then
        assertEquals("Home", result["screenName"])
        assertEquals("shoes", result["searchQuery"])
    }

    @Test
    fun parsePairs_whenDuplicateKeys_keepsLastValue() {
        //given
        val input = "screenName=Home\nscreenName=Cart"

        //when
        val result = TrackingInputParser.parsePairs(input)

        //then
        assertEquals("Cart", result["screenName"])
    }

    // endregion

    // region attributes

    @Test
    fun attributes_whenNoAttrKeys_returnsNull() {
        //given
        val pairs = TrackingInputParser.parsePairs("screenName=Home")

        //when
        val result = TrackingInputParser.attributes(pairs)

        //then
        assertNull(result)
    }

    @Test
    fun attributes_whenMixedTypedValues_infersBooleanIntLongDoubleAndString() {
        //given
        val pairs = TrackingInputParser.parsePairs(
            """
            attr.color=red
            attr.count=3
            attr.big=3000000000
            attr.enabled=true
            attr.disabled=false
            attr.price=9.99
            """.trimIndent()
        )

        //when
        val result = TrackingInputParser.attributes(pairs)

        //then
        requireNotNull(result)
        assertEquals("red", result["color"])
        assertEquals(3, result["count"])
        assertEquals(3_000_000_000L, result["big"])
        assertEquals(true, result["enabled"])
        assertEquals(false, result["disabled"])
        assertEquals(9.99, result["price"])
        assertTrue(result["color"] is String)
        assertTrue(result["count"] is Int)
        assertTrue(result["big"] is Long)
        assertTrue(result["enabled"] is Boolean)
        assertTrue(result["price"] is Double)
    }

    @Test
    fun attributes_whenBooleanLikeButNotStrict_keepsString() {
        //given
        val pairs = TrackingInputParser.parsePairs("attr.flag=True")

        //when
        val result = TrackingInputParser.attributes(pairs)

        //then
        assertEquals("True", result?.get("flag"))
        assertTrue(result?.get("flag") is String)
    }

    @Test
    fun attributes_whenValueEmpty_excludesThatAttribute() {
        //given
        val pairs = TrackingInputParser.parsePairs("attr.color=red\nattr.size=")

        //when
        val result = TrackingInputParser.attributes(pairs)

        //then
        assertEquals(mapOf("color" to "red"), result)
    }

    @Test
    fun attributes_whenAllValuesEmpty_returnsNull() {
        //given
        val pairs = TrackingInputParser.parsePairs("attr.color=\nattr.size=")

        //when
        val result = TrackingInputParser.attributes(pairs)

        //then
        assertNull(result)
    }

    // endregion

    // region applyConfig

    @Test
    fun applyConfig_whenValueEmpty_isNoOpForThatField() {
        //given
        val tracking = mockk<MessengerTracking>(relaxed = true)
        val pairs = TrackingInputParser.parsePairs(
            """
            appName=
            appNamespace=
            appVersion=
            appBuildNumber=
            deviceType=
            osFamily=
            osVersion=
            fingerprint=
            manufacturer=
            carrier=
            externalId=
            """.trimIndent()
        )

        //when
        TrackingInputParser.applyConfig(tracking, pairs)

        //then
        verify(exactly = 0) {
            tracking.setAppName(any())
            tracking.setAppNamespace(any())
            tracking.setAppVersion(any())
            tracking.setAppBuildNumber(any())
            tracking.setDeviceType(any())
            tracking.setOsFamily(any())
            tracking.setOsVersion(any())
            tracking.setFingerprint(any())
            tracking.setManufacturer(any())
            tracking.setCarrier(any())
            tracking.setExternalId(any())
        }
    }

    @Test
    fun applyConfig_whenAppNameHasValue_setsAppName() {
        //given
        val tracking = mockk<MessengerTracking>(relaxed = true)
        val pairs = TrackingInputParser.parsePairs("appName=MyApp")

        //when
        TrackingInputParser.applyConfig(tracking, pairs)

        //then
        verify(exactly = 1) { tracking.setAppName("MyApp") }
    }

    // endregion

    // region templates

    @Test
    fun settersTemplate_whenParsedUnedited_isANoOpForEveryField() {
        //given
        val tracking = mockk<MessengerTracking>(relaxed = true)
        val pairs = TrackingInputParser.parsePairs(TrackingInputParser.SETTERS_TEMPLATE)

        //when
        TrackingInputParser.applyConfig(tracking, pairs)

        //then
        assertNull(TrackingInputParser.screenName(pairs))
        assertNull(TrackingInputParser.searchQuery(pairs))
        assertNull(TrackingInputParser.eventName(pairs))
        assertNull(TrackingInputParser.attributes(pairs))
        verify { tracking wasNot Called }
    }

    @Test
    fun traitsTemplate_whenParsedUnedited_isANoOpForEveryField() {
        //given & when
        val result = TrackingInputParser.traits(TrackingInputParser.TRAITS_TEMPLATE)

        //then
        assertNull(result)
    }

    // endregion

    // region traits

    @Test
    fun traits_whenValidTraitKeys_returnsTrackingTraits() {
        //given
        val input = "email=a@b.com\ngivenName=Ada\nfamilyName=Lovelace"

        //when
        val result = TrackingInputParser.traits(input)

        //then
        requireNotNull(result)
        assertEquals("a@b.com", result.email)
        assertEquals("Ada", result.givenName)
        assertEquals("Lovelace", result.familyName)
    }

    @Test
    fun traits_whenOnlyUnrecognizedKeys_returnsNull() {
        //given
        val input = "screenName=Home"

        //when
        val result = TrackingInputParser.traits(input)

        //then
        assertNull(result)
    }

    @Test
    fun traits_whenEmptyInput_returnsNull() {
        //given
        val input = ""

        //when
        val result = TrackingInputParser.traits(input)

        //then
        assertNull(result)
    }

    // endregion
}
