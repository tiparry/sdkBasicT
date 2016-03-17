Pour publier sous eclipse : 
Selectionner pom.xml
Click droit
run as ... Maven build... 
Mettre deploy dans le champs Goals


------------

Pour utiliser le controle de certificat serveur, il faut commencer par ajouter le .cer fourni au trustore

Perform the following command.

cd d:\workspace\sdk-basic-travaux\etc
keytool -import -file cert_hubic_pp.cer -alias firstCA -keystore myTrustStore
Enter this command two more times, but for the second and third entries, substitute secondCA and thirdCA for firstCA. Each of these command entries has the following purposes:

Mettre comme mot de passe "basictravaux" pour le truststore

The first entry creates a KeyStore file named myTrustStore in the current working directory and imports the firstCA certificate into the TrustStore with an alias of firstCA. The format of myTrustStore is JKS.
For the second entry, substitute secondCA to import the secondCA certificate into the TrustStore, myTrustStore.
For the third entry, substitute thirdCA to import the thirdCA certificate into the TrustStore.
Once completed, myTrustStore is available to be used as the TrustStore for the adapter.

- Puis positionner myTrustStore dans le répertoire src/main/resources du SDK