package de.odpisz.core.model

/**
 * Content of a DIN 5008 Form B letter. Layout math lives in `core:pdf`.
 * The body is a filled template plus the user's facts. It is not a legal opinion.
 */
data class Din5008Letter(
    val senderName: String,
    val senderStreet: String,
    val senderPostcodeCity: String,
    val recipientLines: List<String>,
    val placeAndDate: String,
    val aktenzeichen: String?,
    val subject: String,
    val body: String,
)
