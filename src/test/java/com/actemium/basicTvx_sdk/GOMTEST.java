package com.actemium.basicTvx_sdk;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.ParseException;
import org.xml.sax.SAXException;

import com.actemium.basicTvx_sdk.restclient.RestException;
import com.rff.basictravaux.model.travaux.demandeCapacite.DemandeCapacite;
import com.rff.basictravaux.model.travaux.demandeCapacite.EtatDemandeCapaciteEnregistree;
import com.rff.basictravaux.model.travaux.demandeTravaux.DemandeTravaux;
import com.rff.basictravaux.model.webservice.reponse.Reponse;
import com.rff.basictravaux.model.webservice.requete.RequeteDemandesCapaciteByDate;

import ariane.modele.structure.Structure;
import giraudsa.marshall.deserialisation.text.json.JsonUnmarshaller;
import giraudsa.marshall.exception.ChampNotFund;
import giraudsa.marshall.exception.JsonHandlerException;
import giraudsa.marshall.exception.NotImplementedSerializeException;
import giraudsa.marshall.serialisation.text.json.JsonMarshaller;

public class GOMTEST {

	public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, IOException, SAXException, NotImplementedSerializeException, JsonHandlerException, java.text.ParseException {
		String json = "{\"__type\":\"com.rff.basictravaux.model.travaux.demandeTravaux.DemandeTravaux\",\"id\":\"4dd4fa6e-0999-4429-b818-d19fe8df5dd0\",\"dateCreation\":\"2015-12-06T23:00:00.000Z\",\"lieuxTravaux\":\"lieuTravaux\",\"natureComlementaire\":\"natureComplementaire\",\"numeroDemande\":\"numerodemande\",\"etatsDemandeTravaux\":{\"__type\":\"list\",\"__valeur\":[{\"__type\":\"com.rff.basictravaux.model.travaux.demandeTravaux.EtatDemandeTravauxSupprimee\",\"id\":\"2dd2faf6-0999-4429-b818-d19fe8df5dd0\",\"dateCreation\":\"2015-12-06T23:00:00.000Z\",\"rang\":1,\"commentaire\":{\"__type\":\"com.rff.basictravaux.model.utilitaire.commentaire.CommentaireSimple\",\"id\":\"4dddda6e-2899-4429-b818-d19fe8df5dd0\",\"dateCreation\":\"2015-12-06T23:00:00.000Z\",\"valeur\":\"valeur\"},\"createurEtat\":{\"id\":\"3d54fa6e-2899-4404-b804-d19fe8df5dd0\"}},{\"__type\":\"com.rff.basictravaux.model.travaux.demandeTravaux.EtatDemandeTravauxDemandee\",\"id\":\"2dd4fa6e-0999-4429-b818-d19fe8df5dd0\",\"causeRefus\":\"causeRefus\",\"causeRenvoi\":\"causeRenvoi\",\"dateCreation\":\"2015-12-06T23:00:00.000Z\",\"rang\":1,\"statut\":\"statut\",\"associationsDTActivite\":{\"__type\":\"list\",\"__valeur\":[{\"__type\":\"com.rff.basictravaux.model.travaux.demandeTravaux.AssociationDemandeTravauxActivite\",\"id\":\"36d4fa6e-2899-4412-b812-d19fe8df5dd0\"}]},\"associationsDTDomainesDeProtection\":{\"__type\":\"list\",\"__valeur\":[{\"__type\":\"com.rff.basictravaux.model.travaux.demandeTravaux.AssociationDemandeTravauxDomaineDeProtection\",\"id\":\"30d4fa6e-2899-4406-b806-d19fe8df5dd0\"}]},\"commentaire\":{\"id\":\"4dddda6e-2899-4429-b818-d19fe8df5dd0\"},\"createurEtat\":{\"id\":\"3d54fa6e-2899-4404-b804-d19fe8df5dd0\"},\"profilDemandeTravaux\":{\"id\":\"35d4fa6e-2899-4411-b811-d19fe8df5dd0\"},\"utilisationPlancheTravaux\":{\"__type\":\"list\",\"__valeur\":[{\"__type\":\"com.rff.basictravaux.model.travaux.planche.UtilisationPlancheTravaux\",\"id\":\"32d4fa6e-2899-4408-b808-d19fe8df5dd0\"}]}}]},\"natureTravaux\":{\"id\":\"3d64fa6e-2899-4405-b805-d19fe8df5dd0\"},\"rolesStructure\":{\"__type\":\"list\",\"__valeur\":[{\"__type\":\"com.rff.basictravaux.model.utilitaire.roleStructureExerce.RoleStructureExerce\",\"id\":\"3d14fa6e-2899-4401-b801-d19fe8df5de0\"}]}}";
		
		String login = "APP_CLIENT";
		String pwd = "APP_PASSWORD";
		String baseUrl = "http://212.83.130.104:8080/BasicTravaux/Maintenance/GisementDeDonneeMaintenance/v1/";
		GlobalObjectManager.init(login, pwd, baseUrl);
		GlobalObjectManager gom = GlobalObjectManager.getInstance();
		gom.setDureeCache(15, TimeUnit.HOURS);

		try {
			DemandeCapacite obj = gom.getObject(DemandeCapacite.class, "4dd4fa6e-2899-4429-b818-d19fe8df5dd0", true);
            EtatDemandeCapaciteEnregistree etat = gom.createObject(EtatDemandeCapaciteEnregistree.class, new java.util.Date());
            etat.setPriorite(1);
            EtatDemandeCapaciteEnregistree etat2 = gom.createObject(EtatDemandeCapaciteEnregistree.class, new java.util.Date());
            etat2.setPriorite(2);
            EtatDemandeCapaciteEnregistree etat3 = gom.createObject(EtatDemandeCapaciteEnregistree.class, new java.util.Date());
            etat3.setPriorite(3);
            EtatDemandeCapaciteEnregistree etat4 = gom.createObject(EtatDemandeCapaciteEnregistree.class, new java.util.Date());
            etat4.setPriorite(4);
            EtatDemandeCapaciteEnregistree etat5 = gom.createObject(EtatDemandeCapaciteEnregistree.class, new java.util.Date());
            etat5.setPriorite(5);
            obj.getEtatsDemandeCapacite().add(etat);
            obj.getEtatsDemandeCapacite().add(etat2);
            obj.getEtatsDemandeCapacite().add(etat3);
            obj.getEtatsDemandeCapacite().add(etat4);
            obj.getEtatsDemandeCapacite().add(etat5);
            gom.saveAll();

			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	3764b119-a48b-495b-a11f-1bb752557da9
	0238a26b-070d-47e0-93b0-4831ea6ffce7
	6374d607-b1fa-4ff1-ab77-7cf0a0d8f0ad
	f9ee45be-e28b-4f1a-a43d-ab03d0ed2858
	4c32933d-b6ce-4276-b7ca-d809b7c4a272
	cef6a38e-10ed-4f6a-88f2-3702229fd79f
	81aa224f-c161-4792-b492-6d37730b363c
	d88d9112-2c97-43f0-82a9-95458c2892ab
	abf403b0-311e-4897-98c3-6a0881787751
	76279f12-34f0-4cb9-9b5f-86c7eee873e2
	*/
}
