package service;

import java.util.ArrayList;
import java.util.List;

import exception.MultipleCorrespondanceAddException;
import fr.doodle.dao.AddDao;
import fr.doodle.dao.CommuneDao;
import fr.doodle.dao.TexteDao;
import fr.doodle.dao.TitreDao;
import scraper.Add;
import scraper.Commune;
import scraper.CommuneLink;
import scraper.CompteLbc;
import scraper.EtatAdd;
import scraper.ResultsControl;
import scraper.Texte;
import scraper.Title;

public class AddsSaver {
	private List<Add> addsToControle;
	private List<Add> addsReadyTosave;
	private CommuneDao communeDao;
	private TitreDao titreDao;
	private TexteDao texteDao;
	private AddDao addDao;
	private CompteLbc compteLbc;
	private List<Add> addsRefused;
	private List<Add> addsNotOnlineAnymore;
	private ResultsControl results;
	private List<Add> newAddsOnline = new ArrayList<Add>();
	private List<Add> addsStillOnline = new ArrayList<Add>();

	// pour contr�ler les adds online sur lbc
	private List<Add> addsOnLineWithTexteAndTitleInBdd = new ArrayList<Add>();
	//private List<Add> addsOnLineWithoutRefForCommune = new ArrayList<Add>();
	private List<Add> addsOnLineWithoutRefForTitre = new ArrayList<Add>();
	private List<Add> addsOnLineWithoutRefForTexte = new ArrayList<Add>();
	private List<Add> addsOnLineWithMultipleRef = new ArrayList<Add>();
	private List<Add> addsOnLineNotReferenced = new ArrayList<Add>();
	private List<Add> addsOnlineNotReferencedWithCommuneNotReferenced = new ArrayList<Add>();

	// utiliser pour sauvegarder les annonces �tant en ligne sur lbc
	public AddsSaver(List<Add> addsControled, CompteLbc compteLbc) {
		super();
		this.addsToControle = addsControled;
		communeDao = new CommuneDao();
		titreDao = new TitreDao();
		texteDao = new TexteDao();
		this.compteLbc = compteLbc;

	}

	// soit on met � jour l'add si il est en bdd (cas d'un n�me controle avec n>1)
	// soit on l'ins�re (cas d'un premier contr�le)
	public void setAddsWithMultipleReferencedAndNotReferenced(){
		addsOnLineWithMultipleRef = new ArrayList<Add>();
		addsOnLineNotReferenced = new ArrayList<Add>();
		addDao = new AddDao();
		// pour mettre � jour les annonces encore en ligne et ins�rer les nouvelles annonces en ligne
		for(Add addToSave : addsOnLineWithTexteAndTitleInBdd){
			Add addInBdd=null;
			try{
				addInBdd = addDao.findOneAddFromLbc(addToSave);
				if(addInBdd.isAddNotReferenced()){
					addsOnLineNotReferenced.add(addToSave);
					// les adds non r�f�renc�s sont les adds
					// - dont le titre et le texte �tait en bdd mais pas l'annonce avec ce titre et ce texte
					// - dont le titre et le texte n'�tait pas en bdd mais mais pas l'annonce avec ce titre et ce texte
					// Il peut s'agir d'annonces publi�s sans ce robot
					// ou d'erreurs
					// dans tous les cas, il faut sauvegard� cette annonce
					// � ce stade le titre et le texte sont r�f�renc�s
					// il reste donc � r�f�rencer la commune manuellement 
					// pour pouvoir sauvergarder l'annonce par la suite
				}
			}catch(MultipleCorrespondanceAddException exception){
				addsOnLineWithMultipleRef.add(addToSave);
			}
		}
	}
	public void setAddsNotReferencedWithCommuneNotReferenced(){
		addsOnlineNotReferencedWithCommuneNotReferenced = new ArrayList<Add>();
		for(Add add: addsOnlineNotReferencedWithCommuneNotReferenced){
			if(add.getCommuneLink().onLine.getCodePostal().equals("")){
				addsOnlineNotReferencedWithCommuneNotReferenced.add(add);
			}
		}
	}
	public boolean areAddsReadyToSave(){
		setAddsWithMultipleReferencedAndNotReferenced();
		setAddsNotReferencedWithCommuneNotReferenced();
		if(addsOnLineWithMultipleRef.size()==0 & addsOnlineNotReferencedWithCommuneNotReferenced.size()==0){
			addsReadyTosave = addsOnLineWithTexteAndTitleInBdd;
			return true;
		}else{
			return false;
		}
	}
	public boolean hasAddsNotReferencedWithCommuneNotReferenced(){
		setAddsWithMultipleReferencedAndNotReferenced();
		return(addsOnlineNotReferencedWithCommuneNotReferenced.size()!=0);
	}
	public boolean hasAddsWithMultipleReferenced(){
		setAddsWithMultipleReferencedAndNotReferenced();
		return(addsOnLineWithMultipleRef.size()!=0);
	}
	
	

	public void saveAddsFromLbcInBdd() {
		addDao = new AddDao();
		// pour mettre � jour les annonces encore en ligne et ins�rer les nouvelles annonces en ligne
		for(Add addToSave : addsReadyTosave){
			CommuneLink communeLink = addToSave.getCommuneLink();
			Add addInBdd=null;
			try{	
				addInBdd = addDao.findOneAddFromLbc(addToSave);//retourne une ADD vide si pas correspondance
			}catch(Exception excep){
				System.out.println("C'est normalement pas possible de venir l� car on a fait en sorte ques"
						+ "qu'il n'y est plus de r�f�rences multiples");
			}
			Add addSaved;
			if(addInBdd.isAddNotReferenced()){

				// comme add pas r�f�rence , on ne connais pas submit commune
				// car aucun enregistrement en base n'a �t� fait lors 
				// de la soumission de l'annonce
				// on va donc ins�rer l'add telle quelle	
				// probl�me : aucune r�f�rence de commune
				addToSave.setEtat(EtatAdd.onLine);
				addSaved = addDao.save(addToSave, true);
				newAddsOnline.add(addSaved);
			}else{// sinon on la met � jour
				addToSave.setNbControle(addInBdd.getNbControle()+1);
				addToSave.setEtat(EtatAdd.onLine);
				addToSave.setRefAdd(addInBdd.getRefAdd());
				addDao.update(addToSave);
				addSaved = addToSave;
				addsStillOnline.add(addSaved);
			}
			// on instancie commune submit pour pouvoir faire la correspondance des communes par la suite
			communeLink.submit = communeDao.findOne(communeLink.onLine.getRefCommune());
		}
		// mettre � jour les annonces qui ont �t� refus�s par la mod�ration
		addsRefused = addDao.findAddsWithHerState(new Add(EtatAdd.enAttenteModeration, compteLbc));
		for(Add addRefused :  addsRefused){
			addRefused.setEtat(EtatAdd.refused);
			addDao.updateStateAndNbControlAndDateControl(addRefused);
		}
		// mettre � jour les annonces qui ont p�rim�s et ont �t� supprim�s apr�s mise en ligne
		addsNotOnlineAnymore = addDao.findAddsNotOnlineAnymore(new Add(compteLbc));
		for(Add addNotOnlineAnymore :  addsNotOnlineAnymore){
			addNotOnlineAnymore.setEtat(EtatAdd.notOnLineAnymore);
			addDao.updateStateAndNbControlAndDateControl(addNotOnlineAnymore);
		}
	}

	public List<Add> getAddsControled() {
		return addsToControle;
	}

	public void setAddsControled(List<Add> addsControled) {
		this.addsToControle = addsControled;
	}

	public boolean isTexteAndTitleOnlineReferenced(){
		addsOnLineWithTexteAndTitleInBdd = new ArrayList<Add>();
		// pas de contr�le sur les communes car on est pas s�r qu'elle soit r�f�renc� en base
		// et car un contr�le sur les textes et les titres suffit pour faire
		// correspondre les annonnces en ligne aux annonces en bdd
		//addsOnLineWithoutRefForCommune = new ArrayList<Add>();
		addsOnLineWithoutRefForTitre = new ArrayList<Add>();
		addsOnLineWithoutRefForTexte = new ArrayList<Add>();
		// rep�re et isole les annonces dont le titre ou le texte ne trouve pas de correspondance en bdd
		// et lie les titres et les textes des adds en ligne � la bdd 
		for(Add add : addsToControle){
			boolean titleReferenced = false;
			//boolean communeReferenced = false;
			boolean texteReferenced = false;
			add.setCompteLbc(this.compteLbc);
			
			/*
			// lier les communes du bon coin en ligne � la bdd
			Commune communeFrBdd = communeDao.findOneWithNomCommuneOnLbc(add.getCommuneLink().onLine);
			if(communeFrBdd!=null){
				communeReferenced = true;
			}*/

			// lier les titres du bon coin en ligne � la bdd
			Title titreFrBdd = titreDao.findOneWithTitre(add.getTitle());
			if(titreFrBdd!=null){
				titleReferenced=true;
			}

			// lier les textes du bon coin en ligne � la bdd
			List<Texte> textesCorrespondant;
			Texte texteFrBdd=null;
			int nbRecherche = 0;
			boolean continueRecherche = true;
			do{
				Texte texteLbc = add.getTexte();
				textesCorrespondant = texteDao.findWithCorpsTexte(texteLbc);
				int levelCorresp = texteLbc.getLevelCorrespondance();
				int nbCorrespon = textesCorrespondant.size();
				if(nbCorrespon==0){
					texteLbc.setLevelCorrespondance(levelCorresp+1);
					System.out.println("Pas de correspondance de texte � la recherche n�"+nbRecherche);
				}else if(nbCorrespon>=2){
					texteFrBdd = textesCorrespondant.get(0);
					// on parcourt les textes de la bbd pour prendre celui avec la plus petite distance de Levenshtein
					for(Texte texteCorresp : textesCorrespondant){
						if(texteCorresp.getLevenshteinDistanceBetweenLbcAndBdd()<=texteFrBdd.getLevenshteinDistanceBetweenLbcAndBdd()){
							texteFrBdd=texteCorresp;
						}
					}
					System.out.println("Plus de 2 correspondance de texte � la recherche n�"+nbRecherche);
					continueRecherche = false;
				}else{
					texteFrBdd = textesCorrespondant.get(0);
					continueRecherche = false;
				}
				nbRecherche++;
			}while(continueRecherche);
			if(texteFrBdd.getLevenshteinDistanceBetweenLbcAndBdd()<=30){
				texteReferenced=true;
			}
			if(texteReferenced & titleReferenced){
				//add.getCommuneLink().onLine.setRefCommune(communeFrBdd.getRefCommune());
				add.getTitle().setRefTitre(titreFrBdd.getRefTitre());
				add.getTexte().setRefTexte(texteFrBdd.getRefTexte());
				addsOnLineWithTexteAndTitleInBdd.add(add);
			}
			if(!texteReferenced){
				addsOnLineWithoutRefForTexte.add(add);
			}
			if(!titleReferenced){
				addsOnLineWithoutRefForTitre.add(add);
			}
			/*if(!communeReferenced){
				addsOnLineWithoutRefForCommune.add(add);
			}*/
		}
		if(addsOnLineWithTexteAndTitleInBdd.size()==addsToControle.size()){
			return true;// on est s�r que chaque �l�ment (sauf les communes 
			// car pour g�rer le probl�me de correspondance des communes
			// il faut que les adds en ligne soit li� � la base
		}else{
			return false;
		}
	}

	public List<Add> getAddsRefused() {
		return addsRefused;
	}

	public void setAddsRefused(List<Add> addsRefused) {
		this.addsRefused = addsRefused;
	}

	public List<Add> getAddsNotOnlineAnymore() {
		return addsNotOnlineAnymore;
	}

	public void setAddsNotOnlineAnymore(List<Add> addsNotOnlineAnymore) {
		this.addsNotOnlineAnymore = addsNotOnlineAnymore;
	}

	public ResultsControl getResults() {
		results = new ResultsControl(addsRefused.size(), addsNotOnlineAnymore.size(), newAddsOnline.size(), addsStillOnline.size());
		return results;
	}

	public List<Add> getAddsReadyTosave() {
		return addsReadyTosave;
	}

	public void setAddsReadyTosave(List<Add> addsReadyTosave) {
		this.addsReadyTosave = addsReadyTosave;
	}

	/*
	public List<Add> getAddsOnLineWithoutRefForCommune() {
		return addsOnLineWithoutRefForCommune;
	}*/

	public List<Add> getAddsOnLineWithoutRefForTitre() {
		return addsOnLineWithoutRefForTitre;
	}

	public List<Add> getAddsOnLineWithoutRefForTexte() {
		return addsOnLineWithoutRefForTexte;
	}

	public List<Add> getAddsOnLineWithMultipleRef() {
		return addsOnLineWithMultipleRef;
	}

	public List<Add> getAddsNotReferencedWithCommuneNotReferenced() {
		return addsOnlineNotReferencedWithCommuneNotReferenced;
	}

	public List<Add> getAddsStillOnline() {
		return addsStillOnline;
	}
	
	








}
