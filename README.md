# BielmatKSeF

## 📋 O projekcie

Aplikacja stworzona dla firmy w celu przyspieszenia wprowadzania faktur do KSeF przed wdrożeniem programu docelowego.

1. Wczytywanie pliku PDF z fakturą.
2. Wyciąganie danych z pliku PDF.
3. Parsowanie danych z pdf na obiekt faktury za pomocą wyrażeń regularnych.
4. Sprawdzenie poprawności danych faktury, wyświetlenie ewentualnych ostrzeżeń oraz możliwość edycji danych.
5. Generowanie pliku XML zgodnego z wymaganiami KSeF.

Parser został dostosowany do struktury dokumentów wykorzystywanych w procesie fakturowania firmy Bielmat.

Aplikacja może być używana lokalnie. 
Instalator na Windows do pobrania w zakładce [Releases](https://github.com/MattJavaGZ/bielmat-ksef/releases/tag/v1.0.0).
Po instalacji i uruchomieniu aplikacja będzie dostępna pod adresem [http://localhost:8080](http://localhost:8080).

Aplikacja gotowa do uruchomienia na serwerze za pomocą Dockera.

