package com.genesys.cloud.messenger.sample.tracking

import com.genesys.cloud.integration.messenger.tracking.DeviceCategory
import com.genesys.cloud.integration.messenger.tracking.MessengerTracking
import com.genesys.cloud.integration.messenger.tracking.TrackingTraits

/**
 * Parses the two free-text inputs of the tracking screen into Mobile Tracking SDK calls.
 *
 * The whole screen is driven by text so it can be injected from an automation harness (e.g. Appium).
 * Format: one `key=value` per line (`;` is also accepted as a separator). Unknown keys are ignored,
 * empty inputs produce minimal/`null` results so "required-fields-only" cases are easy to express.
 *
 * Setter keys: `screenName`, `searchQuery`, `eventName`, `externalId`, the metadata setters
 * (`appName`, `appNamespace`, `appVersion`, `appBuildNumber`, `deviceCategory`, `deviceType`,
 * `osFamily`, `osVersion`, `isMobile`, `screenHeight`, `screenWidth`, `screenDensity`, `fingerprint`,
 * `manufacturer`, `carrier`, `bluetoothEnabled`, `cellularEnabled`, `wifiEnabled`) and `attr.<name>`
 * pairs that become the event attributes map (values inferred as Boolean, Int, Long, Double, or String).
 *
 * Trait keys mirror [TrackingTraits]: `email`, `cellPhone`, `homePhone`, `otherPhone`, `workPhone`,
 * `salutation`, `jobTitle`, `givenName`, `middleName`, `familyName`.
 */
object TrackingInputParser {

    private const val ATTR_PREFIX = "attr."

    /** Placeholder inserted by the setters template button: every supported key with no value, so leaving a line untouched is a no-op. Includes 3 example `attr.<name>` placeholders. */
    val SETTERS_TEMPLATE = """
        screenName=
        searchQuery=
        eventName=
        externalId=
        appName=
        appNamespace=
        appVersion=
        appBuildNumber=
        deviceCategory=
        deviceType=
        osFamily=
        osVersion=
        isMobile=
        screenHeight=
        screenWidth=
        screenDensity=
        fingerprint=
        manufacturer=
        carrier=
        bluetoothEnabled=
        cellularEnabled=
        wifiEnabled=
        attr.attr1=
        attr.attr2=
        attr.attr3=
    """.trimIndent()

    /** Placeholder inserted by the traits template button: every supported key with no value, so leaving a line untouched is a no-op. */
    val TRAITS_TEMPLATE = """
        email=
        cellPhone=
        homePhone=
        otherPhone=
        workPhone=
        salutation=
        jobTitle=
        givenName=
        middleName=
        familyName=
    """.trimIndent()

    fun parsePairs(input: String): Map<String, String> =
        input.split('\n', ';')
            .mapNotNull { line ->
                val trimmed = line.trim()
                if (!trimmed.contains('=')) return@mapNotNull null
                val key = trimmed.substringBefore('=').trim()
                val value = trimmed.substringAfter('=').trim()
                key.takeIf { it.isNotEmpty() }?.let { it to value }
            }
            .toMap()

    fun screenName(pairs: Map<String, String>): String? = pairs["screenName"]?.ifEmpty { null }

    fun searchQuery(pairs: Map<String, String>): String? = pairs["searchQuery"]?.ifEmpty { null }

    fun eventName(pairs: Map<String, String>): String? = pairs["eventName"]?.ifEmpty { null }

    fun attributes(pairs: Map<String, String>): Map<String, Any>? =
        pairs.filterKeys { it.startsWith(ATTR_PREFIX) && it.length > ATTR_PREFIX.length }
            .mapKeys { it.key.removePrefix(ATTR_PREFIX) }
            .filterValues { it.isNotEmpty() }
            .mapValues { (_, value) -> inferAttributeValue(value) }
            .takeIf { it.isNotEmpty() }

    fun traits(input: String): TrackingTraits? {
        val pairs = parsePairs(input)
        return TrackingTraits(
            email = pairs["email"]?.ifEmpty { null },
            cellPhone = pairs["cellPhone"]?.ifEmpty { null },
            homePhone = pairs["homePhone"]?.ifEmpty { null },
            otherPhone = pairs["otherPhone"]?.ifEmpty { null },
            workPhone = pairs["workPhone"]?.ifEmpty { null },
            salutation = pairs["salutation"]?.ifEmpty { null },
            jobTitle = pairs["jobTitle"]?.ifEmpty { null },
            givenName = pairs["givenName"]?.ifEmpty { null },
            middleName = pairs["middleName"]?.ifEmpty { null },
            familyName = pairs["familyName"]?.ifEmpty { null },
        ).takeIf { it.hasAnyValue() }
    }

    /**
     * Applies any session-metadata setters and `externalId` found in [pairs] to [tracking]. Values
     * that fail type conversion (e.g. a non-integer `screenHeight`) are skipped.
     */
    fun applyConfig(tracking: MessengerTracking, pairs: Map<String, String>) = with(tracking) {
        pairs["externalId"]?.ifEmpty { null }?.let(::setExternalId)
        pairs["appName"]?.ifEmpty { null }?.let(::setAppName)
        pairs["appNamespace"]?.ifEmpty { null }?.let(::setAppNamespace)
        pairs["appVersion"]?.ifEmpty { null }?.let(::setAppVersion)
        pairs["appBuildNumber"]?.ifEmpty { null }?.let(::setAppBuildNumber)
        pairs["deviceCategory"]?.let { value -> deviceCategoryOf(value)?.let(::setDeviceCategory) }
        pairs["deviceType"]?.ifEmpty { null }?.let(::setDeviceType)
        pairs["osFamily"]?.ifEmpty { null }?.let(::setOsFamily)
        pairs["osVersion"]?.ifEmpty { null }?.let(::setOsVersion)
        pairs["isMobile"]?.toBooleanStrictOrNull()?.let(::setIsMobile)
        pairs["screenHeight"]?.toIntOrNull()?.let(::setScreenHeight)
        pairs["screenWidth"]?.toIntOrNull()?.let(::setScreenWidth)
        pairs["screenDensity"]?.toIntOrNull()?.let(::setScreenDensity)
        pairs["fingerprint"]?.ifEmpty { null }?.let(::setFingerprint)
        pairs["manufacturer"]?.ifEmpty { null }?.let(::setManufacturer)
        pairs["carrier"]?.ifEmpty { null }?.let(::setCarrier)
        pairs["bluetoothEnabled"]?.toBooleanStrictOrNull()?.let(::setBluetoothEnabled)
        pairs["cellularEnabled"]?.toBooleanStrictOrNull()?.let(::setCellularEnabled)
        pairs["wifiEnabled"]?.toBooleanStrictOrNull()?.let(::setWifiEnabled)
    }

    private fun deviceCategoryOf(value: String): DeviceCategory? =
        DeviceCategory.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }

    /** Infers Boolean → Int → Long → Double, otherwise keeps the raw String. */
    private fun inferAttributeValue(raw: String): Any =
        raw.toBooleanStrictOrNull()
            ?: raw.toIntOrNull()
            ?: raw.toLongOrNull()
            ?: raw.toDoubleOrNull()
            ?: raw

    private fun TrackingTraits.hasAnyValue(): Boolean =
        email != null || cellPhone != null || homePhone != null || otherPhone != null ||
            workPhone != null || salutation != null || jobTitle != null || givenName != null ||
            middleName != null || familyName != null
}
