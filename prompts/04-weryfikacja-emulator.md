# Weryfikacja na emulatorze — pętla ze zrzutem

Użyj po zmianie UI albo gdy chcesz, żeby agent sam zobaczył ekran. Kompilacja nie zamyka zadania.

```text
Zweryfikuj ekran na emulatorze albo podłączonym urządzeniu. Przeczytaj skill verify-on-emulator. Nie kończ na assemble.

Ekran: <NAZWA>
Jak do niego dojść: <NP. URUCHOM APLIKACJĘ → OTWÓRZ LISTĘ FAKTUR. BEZ PRAWDZIWYCH HASEŁ>
Motyw: <jasny | ciemny | oba>
Moduł aplikacji, jeśli nie nazywa się app: <NAZWA ALBO „ZNAJDŹ SAM”>

Pętla:
1. Sprawdź urządzenie. Jeśli działa mobile-mcp, użyj mobile_list_available_devices. Inaczej adb devices. Nie czyść danych i nie resetuj emulatora.
2. Złóż wariant debug właściwego modułu i zainstaluj APK. Uruchom aplikację (mobile_launch_app albo adb). Dojdź do ekranu tapami albo inputem. Gdy stoi za logowaniem, którego nie masz prawa przejść — zrób zrzut bramki i napisz, że docelowy ekran jest nieosiągnięty.
3. Zrzut zapisz w screenshots/ (katalog jest w .gitignore). Nazwa: <ekran>-<motyw>-<n>.png. Przy ciemnym motywie: adb shell cmd uimode night yes, drugi zrzut, potem przywróć tryb nocny.
4. Oceń zrzut: czy to ten ekran, czy pasek statusu i nawigacja nie zasłaniają tytułu ani przycisku, czy tekst nie jest ucięty, czy cel dotyku ma ok. 48.dp, czy stany ładowania, pusty i błąd są osiągalne, czy w ciemnym motywie da się czytać tekst i ikony pasków.
5. Popraw kod, zainstaluj ponownie, zrób nowy zrzut. Maksymalnie trzy podejścia wizualne. Potem opisz, co nadal odstaje, zamiast kręcić paddingiem w kółko.
6. Przy crashu najpierw przeczytaj log (mobile_get_device_logs albo adb logcat -d -t 200), dopiero potem zmieniaj kod.

Na końcu napisz wprost: nazwa urządzenia, czym instalowałeś, ścieżki zrzutów, co sprawdziłeś, co zostało niesprawdzone. Bez zrzutu nie pisz, że „wygląda dobrze”.
```
