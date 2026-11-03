# Ekran z Figmy albo Mobbin

Użyj, gdy masz klatkę, link albo chcesz oprzeć układ na prawdziwych aplikacjach. Zostaw jedno źródło, drugie wykreśl.

```text
Zaimplementuj jeden ekran Androida w Jetpack Compose na podstawie referencji. Przeczytaj AGENTS.md i skill compose-screen-from-design. Tokeny bierz z motywu. Jeśli motywu nie ma albo ekran musiałby wpisać kolor na sztywno, najpierw skill design-system-tokens.

Źródło:
- Figma (link do klatki, Copy link to selection): <URL ALBO „BRAK”>
- Mobbin: wyszukaj platformę <Android>, kategorię <NP. BANKOWOŚĆ> i wzorzec <NP. PUSTY STAN LISTY TRANSAKCJI>. Zostaw nazwę produktu i URL każdej referencji, z której naprawdę korzystasz.
- Zrzut lokalny: <ŚCIEŻKA ALBO „BRAK”>

Ekran: <NAZWA>
Trasa: <NP. invoice/{id}>

Zasady:
- Figma jest specyfikacją wizualną, nie kodem do wklejenia. Dopasuj układ do komponentów, które już są w projekcie. Nowy composable tylko wtedy, gdy żaden nie pasuje — napisz dlaczego.
- Z Mobbin bierz hierarchię, kolejność i sposób pokazania stanu. Nie kopiuj marki, tekstów marketingowych ani assetów.
- Rozpisz stany: treść, ładowanie, pusty, błąd — nawet gdy na klatce jest tylko happy path.
- Jasny i ciemny motyw. contentDescription tam, gdzie ikona niesie znaczenie. Cel dotyku co najmniej 48.dp. Tekst ma się mieścić przy większej czcionce.
- ViewModel + StateFlow, hiltViewModel, trasa @Serializable. W nawigacji id, nie cały model.
- Preview light i dark dla treści oraz dla stanu pustego albo błędu.
- Test jednostkowy ViewModelu: sukces i błąd.
- Na końcu skill verify-on-emulator. W podsumowaniu wypisz różnice względem referencji i powód każdej z nich.
```
