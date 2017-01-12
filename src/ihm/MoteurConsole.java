package ihm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import exception.HomeException;
import fr.doodle.dao.CompteLbcDao;
import scraper.CompteLbc;
import service.ObjectManager;
import service.PrintManager;
import util.Console;

public class MoteurConsole {

	ObjectManager manager;
	PrintManager printManager;

	public static void main(String[] args) {

		MoteurConsole console = new MoteurConsole();
		console.acceuil();


	}


	// Proc�dure qui permet d'afficher le type de sondage choisi par
	// l'utilisateur
	public void acceuil() {
		manager = new ObjectManager();
		printManager = new PrintManager(manager);
		System.out.println("------------------------------------");
		System.out.println("------- BIENVENUE ADDS MANAGER ------");
		System.out.println("------------------------------------");
		System.out.println();
		System.out.println("---------    COMMANDES    ----------");
		System.out.println("ESC : pour quitter l'appli");
		System.out.println("HOME : pour revenir � l'acceuil");
		System.out.println();
		boolean continueBoucle = true;
		while (continueBoucle) {
			System.out.println("----------    ACCUEIL    -----------");
			System.out.println();
			System.out.println("1 : Publier des annonces");
			System.out.println("2 : Ajouter un nouveau compte LBC");
			System.out.println("3 : Controler un compte LBC");
			System.out.println("4 : Afficher un compte LBC");
			System.out.println();
			String saisie = Console.readString("Que voulez vous faire ?");
			// Enregistrement du choix de l'utilisateur dans num�ro
			switch (saisie) {
			// si le num�ro, on va cr�er un doodle
			case "1":
				try {
					publishAdd();
				} catch (HomeException homeException) {
					continueBoucle = true;
				}
				break;
			case "2":
				try {
					addNewCompteLbc();
				} catch (HomeException homeException) {
					continueBoucle = true;
				}
				break;
			case "3":
				try {
					ControlCompteLbc();
				} catch (HomeException homeException) {
					continueBoucle = true;
				}
				break;
			case "4":
				afficherCompteLbc();
				continueBoucle = true;
				break;

			case "ESC":
				System.out.println("Fermeture de l'application ");
				return;
			case "HOME":
				System.out.println("C'est d�j� le menu d'acceuil ! ");
				break;
			default:
				System.out.println("Erreur de saisie");
				break;
			}
		}
	}

	private void afficherCompteLbc() {
		manager.setComptes();
		printManager.printCompte();

	}


	private void ControlCompteLbc() throws HomeException{
		System.out.println("!! Attention !!\n"
				+ "Bien attendre le passage de la mod�ration lbc avant de contr�ler les comptes");
		choixDunCompte();
		manager.createAgentLbc();
		manager.lancerControlCompte();
		ControlAdds();
		if(manager.isSavingOk()){
			System.out.println("Les annonces de lbc ont bien �t� sauvegard�s");
			printManager.printResults();
		}else{
			System.out.println("Probl�me avec la sauvegarde des annonces : plus de 2 correspondances trouv�s pour une annonce du bon coin");
		}
	}


	private void ControlAdds() throws HomeException{
		System.out.println("------    CONTR�LE DES ANNONCES SUR LBC   ------");
		ControlCommunes();
		ControlTitres();
		ControlTextes();
		if(manager.isPreparationOk()){
			System.out.println("Toutes les annonces ont �t� parcourues et enregistr�es");
		}else{
			System.out.println("Le contr�le des annonces a pris fin car certaines annonces ne correspondat � ce qu'il y a en bdd");
			throw new HomeException();
		}
	}


	private void ControlTitres() throws HomeException {
		System.out.println("**    COMPARAISON DES TITRES SUR LBC � LA BDD   **");
		while(manager.hasNextAddReadyTosave()){
			boolean correspondance = printManager.toCompareTitles();
			if(!correspondance){
				String confirmation = readConsoleInput("^OK$", 
						"En attente d'une intervention manuelle ! Mettez � jour la bdd. Tapez OK pour reprendre",
						"Votre r�ponse", 
						"doit �tre OK");
				if(confirmation.equals("OK")){
					manager.previousTitleReadyTosave();
				}
			}
		}
		System.out.println("Tout est ok avec les titres !");
	}

	private void ControlTextes() throws HomeException {
		System.out.println("**    COMPARAISON DES TEXTES SUR LBC � LA BDD   **");
		boolean controlReussie = printManager.toControlTexte();
		if(controlReussie){
			System.out.println("Tout est ok avec les textes !");
		}else{
			System.out.println("Aucun texte(s) dans la bdd correspondant � ci dessus");
		}

	}


	private void ControlCommunes() throws HomeException{
		System.out.println("**    COMPARAISON DES COMMUNES SUR LBC � LA BDD   **");
		while(manager.hasNextAddReadyTosave()){
			boolean pasDeCorespBddLbc = printManager.toCompareCommunes(manager.nextCommuneReadyTosave());
			if(pasDeCorespBddLbc){

				String idCommuneCorrespo;
				do{
					String nomCom = readConsoleInput("^.{3,}$", 
							"Saisir le nom de la commune qui correspond � celle du bon coin (pour la trouver dans la bdd) :",
							"Votre r�ponse", 
							"doit faire au moins 3 caract�res");
					printManager.searchResults(nomCom);
					idCommuneCorrespo = readConsoleInput("^\\d+$|^non$", 
							"Saisir l'identifiant de la commune qui correspond � celle du bon coin ou non pour refaire une recherche :",
							"Votre r�ponse", 
							"doit �tre un entier ou \"non\"");
					if(!idCommuneCorrespo.equals("non")){
						String confirmation = readConsoleInput("^oui$|^non$", 
								"Confirmez vous votre choix d'identifiant : "+idCommuneCorrespo+" ?",
								"Votre r�ponse", 
								"doit �tre un entier oui ou non");
						if(confirmation.equals("non")){
							idCommuneCorrespo = "non";
						}
					}
				}while(idCommuneCorrespo.equals("non"));
				manager.saveCodePostalAndNomCommuneNoCorrep(idCommuneCorrespo);
				System.out.println("Correspondance corrig�");
			}
			manager.saveCodePostal();// pour mettre � jour le code postal de la commune si pas d�j� dans la bdd
		}
		System.out.println("Tout est ok avec les communes");
	}


	private void addNewCompteLbc() throws HomeException {
		String mail = readConsoleInput("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$", "Entrez le mail du compte LBC � ajouter",
				"Votre r�ponse", "doit �tre une adresse mail");
		String password = readConsoleInput(".{3,}", "Entrez le password du compte LBC � ajouter",
				"Votre r�ponse", "doit �tre faire plus de 3 caract�res");
		CompteLbc compteToAdd = new CompteLbc(mail, password);
		CompteLbcDao compteLbcDao = new CompteLbcDao();
		compteLbcDao.save(compteToAdd);

	}


	private void publishAdd() throws HomeException{
		System.out.println("------    MENU DE PUBLICATION DES ANNONCES   ------");
		choixDunCompte();
		printManager.doYouWantToSaveAddIndd();
		String nbAnnonces = readConsoleInput("^[1-9]\\d*$", "Entrez le nb d'annonces � publier",
				"Votre r�ponse", "doit �tre un entier positif");
		manager.createAgentLbc(Integer.parseInt(nbAnnonces));
		manager.createAddsGenerator();
		selectionTitres();
		selectionTextes();
		selectionCommunes();

		System.out.println("D�marrage de la publication ...");
		manager.lancerPublication();
		printManager.printBilanPublication();

		//String numDepart = readConsoleInput("^[1-9]\\d*$", "Entrez le num�ro de l'annonce de d�part",
		//		"Votre r�ponse", "doit �tre un mdp LBC");
	}



	private void selectionCommunes() throws HomeException{
		String renouvellez;
		do{
			String strTypeSource = selectSource("communes");
			manager.setCommuneSourceType(strTypeSource);

			switch (manager.getCommuneSourceType()) {
			case SQL:
				selectionCommuneSql();
				break;
			case XLSX:
				selectionCommuneXlsx();	
				break;
			}
			manager.setcommunes();
			// on affiche les titres choisies pour v�rification de la part de l'utilisateur 
			System.out.println(printManager.communeToString());
			renouvellez = readConsoleInput("^oui|non", "Est ce bien les communes ci dessus que vous voulez utiliser ?",
					"Votre r�ponse", "doit �tre oui ou non");
		}while(renouvellez.equals("non"));

	}


	private void selectionCommuneXlsx() {
		// TODO Auto-generated method stub

	}


	private void selectionCommuneSql() throws HomeException{
		String renouvellez;
		int bornInf;
		int bornSup ;
		do{
			bornInf = Integer.parseInt(readConsoleInput("[0-9]\\d*", "Saisir la borne inf�rieur de population des communes � choisir :",
					"Votre r�ponse", "doit �tre un entier positive"));
			bornSup = Integer.parseInt(readConsoleInput("[0-9]\\d*", "Saisir la borne sup�rieure de population des communes � choisir :",
					"Votre r�ponse", "doit �tre un entier positive"));
			if(bornSup>bornInf){
				renouvellez = readConsoleInput("^oui|non", "Vous confirmez votre choix : "
						+ "born inf : "+ bornInf+
						" born sup : "+ bornSup,
						"Votre r�ponse", "doit �tre oui ou non");
			}else{
				System.out.println("Veuillez renouvellez votre saisie afin que la borne sup soit plus"
						+ "grande que la borne inf");
				renouvellez="non";
			}

		}while(renouvellez.equals("non"));
		manager.setCritSelectVille(bornInf, bornSup);
	}

	// pour poser la question : quelle type de source � utiliser ?
	private String selectSource(String objectRelated)throws HomeException{
		String renouvellez;
		String strTypeSource;
		do{
			String[] pourAffichageEtSaisieDesSourceType = this.printManager.typeSourcesToString();
			System.out.println();
			System.out.println("Choisir un type de source � utiliser pour les "+objectRelated+" : ");
			System.out.println(pourAffichageEtSaisieDesSourceType[0]);
			System.out.println("Saisir le type de source � utiliser pour les "+objectRelated+" : ");
			strTypeSource = readConsoleInput(pourAffichageEtSaisieDesSourceType[1], "Entrez le type de source choisi : ",
					"Votre r�ponse", "doit �tre un des types sources");
			renouvellez = readConsoleInput("^oui|non", "Est ce bien ce type de source : "+ strTypeSource +""
					+ " que vous voulez utiliser ? ",
					"Votre r�ponse", "doit �tre oui ou non");
		}while(renouvellez.equals("non"));
		return strTypeSource;
	}

	private String selectPath()throws HomeException{
		String renouvellez;
		String path;
		do{
			path = readConsoleInput("^MINE$|CLIENT", "Saisir le r�pertoire des annonces � utiliser",
					"Votre r�ponse", "doit �tre MINE ou CLIENT");
			renouvellez = readConsoleInput("^oui|non", "Est ce bien ce r�pertoire : "+ path +""
					+ " que vous voulez utiliser ? ",
					"Votre r�ponse", "doit �tre oui ou non");
		}while(renouvellez.equals("non"));
		return path;
	}


	private void selectionTextes() throws HomeException {
		String renouvellez;
		do{
			String strTypeSource = selectSource("textes");
			manager.setTexteSourceType(strTypeSource);

			switch (manager.getTexteSourceType()) {
			case SQL:
				selectionTexteSql();
				break;
			case XLSX:
				selectionTextesXlsx();	
				break;
			}
			manager.setTextes();
			// on affiche les titres choisies pour v�rification de la part de l'utilisateur 
			System.out.println(printManager.texteToString());
			renouvellez = readConsoleInput("^oui|non", "Est ce bien les textes ci dessus que vous voulez utiliser ?",
					"Votre r�ponse", "doit �tre oui ou non");
		}while(renouvellez.equals("non"));

	}


	private void selectionTextesXlsx() throws HomeException{
		String path = selectPath();
		manager.setPathToAdds(path);	
	}


	private void selectionTexteSql() throws HomeException{
		String typeTexteChoisie = printManager.chooseTypeTexte();
		manager.setCritSelectTexte(typeTexteChoisie);

	}


	private void selectionTitres() throws HomeException{
		String renouvellez;
		do{
			String strTypeSource = selectSource("titres");
			manager.setTitleSourceType(strTypeSource);

			switch (manager.getTitleSourceType()) {
			case SQL:
				selectionTitresSql();
				break;
			case XLSX:
				selectionTitresXlsx();	
				break;
			}
			manager.setTitres();
			// on affiche les titres choisies pour v�rification de la part de l'utilisateur 
			System.out.println(printManager.titreToString());
			renouvellez = readConsoleInput("^oui|non", "Est ce bien les titres ci dessus que vous voulez utiliser ?",
					"Votre r�ponse", "doit �tre oui ou non");
		}while(renouvellez.equals("non"));


	}




	private void selectionTitresXlsx() throws HomeException{
		String path = selectPath();
		manager.setPathToAdds(path);
	}


	private void selectionTitresSql() throws HomeException{
		String typeTitleChoisie = printManager.chooseTypeTitle();
		manager.setCritSelectTitre(typeTitleChoisie);

	}


	private void choixDunCompte() throws HomeException{

		manager.setComptes();
		String[] pourAffichageEtSaisieDesComptes = this.printManager.comptestoString();
		String renouvellez;
		String idCompte ;
		System.out.println();
		do{


			System.out.println("Choisir un compte � utiliser : ");
			System.out.println(pourAffichageEtSaisieDesComptes[0]);
			System.out.println("Saisir le compte � utiliser : ");
			idCompte = readConsoleInput(pourAffichageEtSaisieDesComptes[1], "Entrez l'identifiant du compte choisi : ",
					"Votre r�ponse", "doit �tre un des identifiants");
			manager.setCompte(Integer.parseInt(idCompte));
			//		System.out.println("Vous avez choisi le compte : "+manager.getCompteInUse().getMail());
			renouvellez = readConsoleInput("^oui|non", "Est ce bien ce compte : "+ manager.getCompteInUse().getMail() +""
					+ " que vous voulez utiliser ? ",
					"Votre r�ponse", "doit �tre oui ou non");
		}while(renouvellez.equals("non"));
		manager.setCompte(Integer.parseInt(idCompte));
	}


	public String readConsoleInput(String regex, String message, String variableASaisir, String format)
			throws HomeException {
		Pattern p = Pattern.compile(regex);
		String phraseCloseApplication = "Voulez fermez l'application ? (si il y a un travail, il ne sera pas enregistr�)";
		boolean continueBoucle = true;
		String input = "";
		while (continueBoucle) {
			input = Console.readString(message);
			if (input.equals(phraseCloseApplication))
				input = "autreChoseQueESCQueHomeQueOUIQueNON";
			switch (input) {
			case "ESC":
				String closeAppli = readConsoleInput("OUI|NON", phraseCloseApplication, "La r�ponse ",
						"�tre OUI ou NON.");
				if (closeAppli.equals("OUI")) {
					System.out.println("Vous venez de fermer l'application ! ");
					System.exit(0);
				} else {
					continueBoucle = true;
				}
				break;
			case "HOME":
				throw new HomeException();
			default:
				Matcher m = p.matcher(input);
				boolean b = m.matches();
				if (b) {
					continueBoucle = false;
				} else {
					System.out.println(variableASaisir + " doit " + format);
				}
				break;
			}
		}
		return (input);
	}


}
