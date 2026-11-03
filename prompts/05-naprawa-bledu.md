# Naprawa błędu

Użyj, gdy coś jest zepsute i ma takie zostać w zachowaniu poza tą jedną poprawką. Bez refaktoru przy okazji.

```text
Napraw błąd. Przeczytaj AGENTS.md. Nie refaktoryzuj sąsiedniego kodu i nie dodawaj bibliotek.

Objaw: <CO WIDZI UŻYTKOWNIK>
Kroki: <1. … 2. … 3. …>
Oczekiwane: <CO POWINNO SIĘ STAĆ>
Faktyczne: <CO SIĘ DZIEJE>
Gdzie: <WERSJA, URZĄDZENIE, LOG — JEŚLI MASZ>

Kolejność:
1. Znajdź przyczynę i nazwij ją jednym zdaniem, zanim zmienisz pliki. Wskaż plik i warunek.
2. Dodaj albo uzupełnij test, który pada na tym błędzie. Jednostkowy, gdy da się zamknąć stan ViewModelu. Test Compose, gdy błąd jest w interakcji UI, której jednostkowo nie widać.
3. Najmniejsza poprawka, po której ten test przechodzi. Bez zmiany architektury, chyba że błąd właśnie z niej wynika — wtedy napisz to w planie i ogranicz zmianę do tej przyczyny.
4. Uruchom ten test i podaj wynik (polecenie i czy przeszedł).
5. Jeśli błąd widać na ekranie, jeden zrzut po poprawce według skilla verify-on-emulator.

W podsumowaniu: przyczyna, pliki, nazwa testu, ryzyko regresji. Nie opisuj rzeczy, których nie ruszałeś, jako zrobionych.
```
