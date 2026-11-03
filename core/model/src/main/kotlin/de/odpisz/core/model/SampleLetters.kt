package de.odpisz.core.model

object SampleLetters {
    val finanzamt: String =
        """
        Finanzamt Berlin Mitte
        Nossener Straße 10
        10115 Berlin

        Einkommensteuerbescheid 2024
        Aktenzeichen: 12/345/67890

        Sehr geehrte Damen und Herren,

        für das Steuerjahr 2024 setzen wir die Einkommensteuer fest.
        Bitte zahlen Sie den Betrag von 186,40 EUR bis zum 20.11.2026
        auf das Konto DE89 3704 0044 0532 0130 00.

        Bei Rückfragen: poststelle@finanzamt-berlin.example oder 030 12345678.
        Steuerliche Identifikationsnummer: 86095742719.

        Mit freundlichen Grüßen
        Finanzamt Berlin Mitte
        """.trimIndent()

    val court: String =
        """
        Amtsgericht Berlin
        Geschäftszeichen: 12 C 345/26

        In der Sache wird Termin zur mündlichen Verhandlung bestimmt
        auf den 15.12.2026, 10:00 Uhr, Saal 214.

        Die Klage ist dem Beklagten zuzustellen.
        Aktenzeichen: 12 C 345/26
        """.trimIndent()

    val enforcement: String =
        """
        Gerichtsvollzieher
        Verteilungsstelle für Gerichtsvollzieheraufträge

        Die Zwangsvollstreckung aus dem Titel wird betrieben.
        Bitte zahlen Sie 420,00 EUR bis zum 02.12.2026.
        Geschäftszeichen: DR II 88/26
        """.trimIndent()
}
