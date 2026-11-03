# Refaktoryzacja bez zmiany zachowania

Użyj, gdy kod ma zostać czytelniejszy albo bliższy regułom z AGENTS.md, a funkcja dla użytkownika ma zostać ta sama.

```text
Zrefaktoryzuj wskazany kawałek bez zmiany zachowania. Przeczytaj AGENTS.md.

Zakres: <PAKIET, PLIKI ALBO EKRAN>
Cel: <NP. WYNIEŚĆ STAN Z COMPOSABLE DO VIEWMODELU | ROZBIĆ WIELKI EKRAN | PRZENIEŚĆ WERSJE DO libs.versions.toml>
Poza zakresem: <CZEGO NIE RUSZAMY>

Zasady:
1. Najpierw opisz obecne zachowanie i testy, które je pilnują. Brak testu na ścieżkę, którą ruszasz — dopisz go, zanim zmienisz strukturę, i uruchom go na starym kodzie.
2. Małe kroki. Po każdym kroku testy tego modułu mają przechodzić. Nie mieszaj refaktoru z nową funkcją.
3. Docelowy kształt, tam gdzie i tak przerabiasz ten kod: UiState w StateFlow, zdarzenia do ViewModelu, Hilt, tokeny zamiast heksów, jedna aktywność, Navigation Compose z @Serializable. Jeśli moduł świadomie używa Navigation 3, zostań przy Navigation 3 i skillu navigation-3.
4. Nie zmieniaj kontraktu sieci ani schematu Room. Destrukcyjna migracja bazy nie wchodzi w grę.
5. Nie formatuj i nie „czyść” plików spoza zakresu. Nowe uwagi detekta i ktlint popraw tylko w ruszanym kodzie.
6. Publiczne zachowanie UI zostaje. Gdy refaktor rusza układ, jeden zrzut przed i po (skill verify-on-emulator) albo napisz, że urządzenia nie było.

Na końcu: co się przesunęło (pliki), które testy to pilnują i które zostały uruchomione, co celowo zostało po staremu.
```
