package ihm;

import java.io.File;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	
/*
		do{
			Scanner in = new Scanner(System.in);
			String newLine = in.nextLine();
			Pattern p = Pattern.compile("^((\\S*)|(\\s)),((\\d*)|(\\s)),((\\d*)|(\\s))$");
			// cr�ation d'un moteur de recherche
			Matcher m = p.matcher(newLine);
			// lancement de la recherche de toutes les occurrences
			boolean b = m.matches();
			// si recherche fructueuse
			if(b) {
				// pour chaque groupe
				for(int i=0; i <= m.groupCount(); i++) {
					// affichage de la sous-cha�ne captur�e
					System.out.println("Groupe " + i + " : " + m.group(i));
				}
				String[] elementsRequete = newLine.split(",");
				System.out.println(elementsRequete[0].toLowerCase()+".");
				System.out.println(elementsRequete[1]+".");
				System.out.println(elementsRequete[2]+".");
			}
		}while(true);
*/
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
			System.out.println("5 : Enregistrer des nouveaux textes dans la bdd");
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
			case "5":
				try {
					addNewTextInBdd();
				} catch (HomeException homeException) {
					continueBoucle = true;
				}
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

	private void addNewTextInBdd() throws HomeException{
		System.out.println("------    AJOUT DE TEXTES � LA BDD   ------");
		File path = printManager.selectFileWithTexte();
		String typeTexte = printManager.chooseTypeTexte();
		manager.addNewTextInBdd(path, typeTexte);
		System.out.println("Textes bien enregistr�s dans la bdd");
	}


	private void afficherCompteLbc() {
		manager.setComptes();
		printManager.printCompte();

	}


	private void ControlCompteLbc() throws HomeException{
		System.out.println("!! Attention !!\n"
				+ "Bien attendre le passage de la mod�ration lbc avant de contr�ler les comptes"
				+ "\nFaire ce contr�le deux jours apr�s le passage de la mod�ration");
		choixDunCompte();
		manager.createAgentLbc();
		manager.scanAddsOnLbc();


		boolean texteAndTitleOnlineReferenced;
		System.out.println("V�rification des correspondances entre les annonces en ligne et la bdd");
		do{
			texteAndTitleOnlineReferenced = manager.isTexteAndTitleOnlineReferenced();
			if(!texteAndTitleOnlineReferenced){
				System.out.println("Il y a des textes et des titres en ligne non r�f�renc�s !");
			}else{
				System.out.println("Tous les textes et les titres sont r�f�renc�s");
			}
			printManager.toLinkTexteAndTitleWhitoutRef();
		}while(!texteAndTitleOnlineReferenced);
		boolean addsOnlineHasMoreThanOneReference;
		System.out.println("V�rification que les annonces en ligne soient r�f�renc�es qu'une seule fois dans la bdd");

		do{
			addsOnlineHasMoreThanOneReference = manager.hasAddsWithMultipleReferenced();// vaudra vrai si chaque annonce a une unique correspondance en bdd
			if(addsOnlineHasMoreThanOneReference){
				System.out.println("Il y a des annonces avec plusieurs correspondances !");
				printManager.toSolveMultipleAddMatch();
			}else{
				System.out.println("Toutes les annonces en lignes sont r�f�renc�s au plus une fois");
			}

		}while(addsOnlineHasMoreThanOneReference);
		boolean readyToSave;

		do{
			// pour les adds non r�f�renc�s, on doit s'assurer que la commune en ligne est pas dans la bdd
			// et mettre cette ref de commune dans les adds non r�f�renc�s pour pouvoir les sauvegarder
			if(manager.hasAddsNotReferencedWithCommuneNotReferenced()){
				System.out.println("Il y a des annonces non r�f�renc�s \n"
						+ "ce sont des annonces normalement ins�r�s sans ce robot (ou bien des erreurs)"
						+ " Il n'y a donc aucune trace de l'annonce soumise en base"
						+ " et il reste donc normalement plus qu'� li� la commune de cette "
						+ " annnonce � la base pour pouvoir la sauvegarder car titre et texte sont r�f�renc�s");
				printManager.toSolveAddsNotReferencedWithCommuneNotReferenced();
			}else{
				System.out.println("Il n'y a pas d'annonces en ligne non r�f�renc�s avec des communes sans ref");
			}
		}while(manager.hasAddsNotReferencedWithCommuneNotReferenced());

		if(manager.getAddsSaver().areAddsReadyToSave()){
			System.out.println("Toutes les �l�ments des annonces lbc ont une correspondance en bdd et une seule");
			System.out.println("De m�me chaque annonce en ligne a une unique ref en base (sauf celles pas publi�s avec le robot)");
			System.out.println("---- Gestion de la correspondance des communes des annonces pas r�f�renc�s entre la Bdd et LeBonCoin ----");
			printManager.gererCorrepondanceCommunes(manager.getAddsSaver().getAddsNotReferencedWithCommuneNotReferenced());
			manager.saveAddsFromScanOfLbc();
			System.out.println("---- Gestion de la correspondance des communes des annonces r�f�renc�s en ligne entre la Bdd et LeBonCoin ----");
			printManager.gererCorrepondanceCommunes(manager.getAddsSaver().getAddsStillOnline());
			System.out.println("Les annonces de lbc ont bien �t� sauvegard�s");
			printManager.printResults();
		}else{
			System.out.println("Les annonces sont pas pr�tes � �tre sauvegard�s");
		}



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
		boolean reglageParDefaut = useReglageByDefault();
		selectionTitres(reglageParDefaut);
		selectionTextes(reglageParDefaut);
		selectionCommunes(reglageParDefaut);
		selectionImages(reglageParDefaut);
		System.out.println("D�marrage de la publication ...");
		manager.genererEtPublier();

		// pour g�rer les correspondances des communes (faire en sorte que la commune soumise soit bien en base)
		printManager.gererCorrepondanceCommunes(manager.getAddsPublieAvtMode());
		// pour afficher le nb d'annones soumises
		printManager.printBilanPublication();
	}


	private boolean useReglageByDefault() throws HomeException{
		String rep = readConsoleInput("^oui|non$", 
				"Voulez vous utiliser les r�glages par d�faut",
				"Votre r�ponse", "�tre oui ou non");
		if(rep.equals("oui")){
			return true;
		}else{
			return false;
		}

	}


	private void selectionImages(boolean selectionImages) throws HomeException{
		String path = "MINE";
		if(!selectionImages)
			path = selectPath(" les images");
		manager.setPathToAdds(path);
	}


	private void selectionCommunes(boolean reglageParDefaut) throws HomeException{
		String renouvellez;
		do{
			String strTypeSource="SQL";
			if(!reglageParDefaut){
				strTypeSource = selectSource("communes");
			}
			manager.setCommuneSourceType(strTypeSource);

			switch (manager.getAddsGenerator().getTypeSourceCommunes()) {
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

	private String selectPath(String elementsAdds)throws HomeException{
		String renouvellez;
		String path;
		do{
			path = readConsoleInput("^MINE$|CLIENT", "Saisir le r�pertoire des annonces � utiliser pour les "+elementsAdds,
					"Votre r�ponse", "doit �tre MINE ou CLIENT");
			renouvellez = readConsoleInput("^oui|non", "Est ce bien ce r�pertoire : "+ path +""
					+ " que vous voulez utiliser ? ",
					"Votre r�ponse", "doit �tre oui ou non");
		}while(renouvellez.equals("non"));
		return path;
	}


	private void selectionTextes(boolean reglageParDefaut) throws HomeException {
		String renouvellez;
		do{
			String strTypeSource="SQL";
			if(!reglageParDefaut)
				strTypeSource = selectSource("textes");
			manager.setTexteSourceType(strTypeSource);

			switch (manager.getAddsGenerator().getTypeSourceTextes()) {
			case SQL:
				selectionTexteSql(reglageParDefaut);
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
		String path = selectPath("les textes");
		manager.setPathToAdds(path);	
	}


	private void selectionTexteSql(boolean reglageParDefaut) throws HomeException{
		String typeTexteChoisie="mes200TextesSoutienParMailScolaireAvecResa10jours";
		if(!reglageParDefaut)
			typeTexteChoisie = printManager.chooseTypeTexte();
		manager.setCritSelectTexte(typeTexteChoisie);

	}


	private void selectionTitres(boolean reglageParDefaut) throws HomeException{
		String renouvellez;
		do{
			String strTypeSource = "SQL";
			if(!reglageParDefaut)
				strTypeSource = selectSource("titres");
			manager.setTitleSourceType(strTypeSource);

			switch (manager.getAddsGenerator().getTypeSourceTitles()) {
			case SQL:
				selectionTitresSql(reglageParDefaut);
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
		String path = selectPath("titres");
		manager.setPathToAdds(path);
	}


	private void selectionTitresSql(boolean reglageParDefaut) throws HomeException{
		String typeTitleChoisie = "sebScolaire122";
		if(!reglageParDefaut)
			typeTitleChoisie = printManager.chooseTypeTitle();
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
