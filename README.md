#Kontaktregister Statistikk

Modul for å hente statistikk fra kontaktregister og legge inn i [statistikkløsningen}(https://github.com/difi/poc-statistics).
Bruker rest-apiet i kontaktregisteret for å trekke ut data, og legger det inn v.h.a. rest-apiet til statistikkløsningen.

##Forutsetninger
Du må ha følgende tilgjengelig:
* JDK 1.8 eller nyere
* Maven 3.3 eller nyere
* Docker 1.12 eller nyere
* Tilgang til IDPortens kontaktregister-API
* Startet, evt. tilgang til statistikkapplikasjonen

##Første gangs oppsett
Etter at prosjektet er klonet, kjør

`mvn package`

Dette vil både lage jar-filen for prosjektet, Dockerfile og docker image.
Applikasjonen kan nå startes enten på localhost eller i en docker-container.

##Bygging og utrulling
Prosjektet leverer Java jar applikasjon pakket inn i Docker-bildet.

Applikasjonen benytter to påkrevde parametre for å angi URL til kontaktregisteret og statistikkaplikasjonen:
`url.base.kontaktregister`
`url.base.url.base.statistikk`

Etter bygg kan applikasjonen startes med

**localhost**
`java -jar ./target/ krr-statistikk-klient*.jar \
  --url.base.kontaktregister=http://admin-test1.difi.eon.no \
  --url.base.url.base.statistikk=http://test-statistikk-inndata.difi.no \
  --file.base.difi-statistikk=/run/secrets/krr-stat-pumba`

**Docker**
`mvn docker:run \
  --url.base.kontaktregister=http://admin-test1.difi.eon.no \
  --url.base.url.base.statistikk=http://test-statistikk-inndata.difi.no \
  --file.base.difi-statistikk=/run/secrets/krr-stat-pumba`
