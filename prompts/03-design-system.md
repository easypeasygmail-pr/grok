# Design system — tokeny przed ekranami

Użyj, zanim powstanie seria nowych ekranów, albo gdy Compose jest upstrzony kolorami i przypadkowymi odstępami.

```text
Uporządkuj design system tej aplikacji Compose, zanim powstaną kolejne ekrany. Przeczytaj AGENTS.md i skill design-system-tokens.

Zakres: <CAŁA APLIKACJA | MODUŁ app | TYLKO NOWE EKRANY>
Wejście: <LINK DO ZMIENNYCH W FIGMIE | BRAK — ZOSTAŃ PRZY MATERIAL 3 I TYM, CO JUŻ JEST W MOTYWIE>

Zrób to w tej kolejności:
1. Inwentarz. Znajdź Color(0x…), .sp i jednorazowe .dp w kodzie ekranów. Wypisz pliki, nic jeszcze nie przerabiaj hurtem.
2. Jeden MaterialTheme. ColorScheme jasny i ciemny (background, surface, primary, onPrimary, error i używane role on*). Dynamic color tylko jako opcja z tymi schematami jako fallbackiem.
3. Typografia Material 3 — nadpisz wyłącznie style, których produkt używa. Kształty w Shapes. Skala odstępów: 4, 8, 16, 24, 32 dp. Ekrany używają nazw semantycznych, nie surowej palety.
4. Dodatkowe znaczenia (sukces, ostrzeżenie) tylko wtedy, gdy Material ich nie niesie — CompositionLocal, obie jasność.
5. Cienki wrapper komponentu (przycisk, pole, pasek) wyłącznie gdy ten sam zestaw nadpisań już się kopiuje. Nie zasłaniaj Material 3 tak, żeby nie dało się podać Modifier.
6. Jeśli motyw już istnieje, rozszerz go. Nie stawiaj obok drugiego systemu z innymi nazwami.
7. Krótki KDoc przy motywie: w którym pliku dodać token i że ekran nie wpisuje heksa.

Nie przerabiaj całych ekranów poza podmianą wartości, które łamią tokeny. Nie zmieniaj targetSdk i nie dodawaj bibliotek „dla design systemu”.

Skończone: oba motywy się budują, jest preview light/dark reprezentatywnego komponentu i wiadomo, gdzie dodać następny kolor.
```
