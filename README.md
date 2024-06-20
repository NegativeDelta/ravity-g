# ravity-g
obliczator przyspieszenia ziemskiego przy użyciu barometru smartfona
by Wojciech Dwojga Nazwisk & Szymon W.
<h2>Struktura i działanie projektu</h2>
Projekt był robiony pod Samsunga S24 (Android 14) i na nim testowany. Nie gwarantuję działania na innych urządzeniach.
Projekt składa się z trzech głównych modułów:
<ul>
<li>Aplikacji w Android Studio</li>
    Odpowiada ona za pomiary ciśnienia i czasu spadania urządzenia. Docelowo, podaje wyliczone przyspieszenie ziemskie. Dla najlepszych rezultatów, aplikację należy skalibrować przed użyciem (patrz: instrukcja kalibracji).
<li>Skryptu PHP postawionego na XAMPPie</li>
    Odbiera requesty z odczytami ciśnienia przesyłane przez aplikację i tworzy pliki .csv, każdy plik odpowiada jednemu upuszczeniu urządzenia
<li>Skryptu w Pythonie</li>
    Skanuje directory xamppa i przy wykryciu nowego pliku, tworzy wykres ciśnienia w czasie. Outputy CLI skryptu umożliwiają kalibrację.
</ul>

<h2>Ważne informacje dla madmanów adaptujących projekt</h2>
W kodzie aplikacji jest predefiniowany lokalny adres IP serwera HTTP. W obecnym jej stanie, aplikacja się wykrzacza, jeśli serwer jest unreachable, więc zmień u siebie IP w kodzie na prawidłowe. <br>
W skrypcie Pythonowskim predefiniowana jest ścieżka do folderu XAMPPa (tam, gdzie .php tworzy pliki .csv). Jeśli chcesz korzystać ze skryptu, musisz zmienić ścieżkę na prawidłową.

<h2>Instrukcja kalibracji</h2>
<ol>
<li>Odpowiednio konfigurujesz aplikację, włączasz serwer i skrypt pythonowski</li>
<li>Upuszczasz telefon z wysokości</li>
<li>Python podaje w outpucie CLI wyliczony współczynnik </li>
<li>Zmieniasz podkreślony współczynnik w linijce 74(?) pliku MainActivity.kt:</li>
<tt>val h = (endPressure - startingPressure) / <u>0.107f</u></tt><br>
Nie zapomnij o f na końcu bo to float
<li>Buildujesz aplikację i odpalasz ją na telefonie</li>
</ol>

<h2>Instrukcja obsługi aplikacji</h2>
Primo. Nie odpowiadam za potłuczone telefony. <br>
Secundo. Nie używaj aplikacji do wyliczania przyspieszenia ziemskiego na potrzeby konstrukcji maszyn latających, ani nawet nielatających. Bezpieczniej o przyspieszenie ziemskie jest spytać się CHATGPT.
<ol>
<li>Rozłóż poduszki na podłodze</li>
<li>Upewnij się, że serwer HTTP jest włączony i widoczny z telefonu oraz że w kodzie aplikacji wpisane jest odpowiednie IP</li>
<li>Otwierasz aplikację</li>
<li>Przyznajesz uprawnienia do mikrofonu, plików oraz do keyloggera na kompie Twojej cioci</li>
<li>Wejdź na drabinę czy coś</li>
<li>Upewnij się, że w pomieszczeniu jest cicho</li>
<li>Trzymaj palcem, najlepiej przez sekundę/dwie, przycisk MEASURE PRSSURE, a potem upuść telefon nie nadając mu v<sub>0</sub>. Palec musi puścić przycisk MEASURE PRESSURE w momencie początku spadania.</li>
<li>Módl się, żeby telefon spadł na poduszki i się nie rozwalił</li>
<li>Po upadku, pozwól telefonowi odpocząć przez 5 sekund</li>
<li>Możesz podnieść telefon. Wciśnij przycisk OBLICZ, aby zobaczyć wyliczone przyspieszenie ziemskie.</li>
</ol>