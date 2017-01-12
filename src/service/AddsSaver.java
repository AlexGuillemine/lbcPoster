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
import scraper.CompteLbc;
import scraper.EtatAdd;
import scraper.ResultsControl;
import scraper.Texte;
import scraper.Title;

public class AddsSaver {
	private List<Add> addsToControle;
	private List<Add> addsReadyTosave;
	private List<Texte> textesPasDansLaBdd;
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


	// utiliser pour sauvegarder les annonces �tant en ligne sur lbc
	public AddsSaver(List<Add> addsControled, CompteLbc compteLbc) {
		super();
		this.addsToControle = addsControled;
		communeDao = new CommuneDao();
		titreDao = new TitreDao();
		texteDao = new TexteDao();
		textesPasDansLaBdd = new ArrayList<Texte>();
		this.compteLbc = compteLbc;
		
	}
	
	// soit on met � jour l'add si il est en bdd (cas d'un n�me controle avec n>1)
	// soit on l'ins�re (cas d'un premier contr�le)
	public boolean saveAnnonceFromLbcInBdd() {
		addDao = new AddDao();
		// pour mettre � jour les annonces encore en ligne et ins�rer les nouvelles annonces en ligne
		for(Add addToSave : addsReadyTosave){
			Add addInBdd;
			try{
				addInBdd = addDao.findOneAddFromLbc(addToSave);
			}catch(MultipleCorrespondanceAddException exception){
				return false;
			}
			Add addSaved;
			if(addInBdd.getRefAdd()==-1){// si premier contr�le, on ins�re cette nouvelle annonce en ligne
				addToSave.setEtat(EtatAdd.onLine);
				addSaved = addDao.save(addToSave);
				newAddsOnline.add(addSaved);
			}else{// sinon on la met � jour
				addToSave.setNbControle(addInBdd.getNbControle()+1);
				addToSave.setEtat(EtatAdd.onLine);
				addToSave.setRefAdd(addInBdd.getRefAdd());
				addDao.update(addToSave);
				addSaved = addToSave;
				addsStillOnline.add(addSaved);
			}
		}
		// mettre � jour les annonces qui ont �t� supprim�s
		addsRefused = addDao.findAddsWithHerState(new Add(EtatAdd.enAttenteModeration, compteLbc));
		for(Add addRefused :  addsRefused){
			addRefused.setEtat(EtatAdd.refused);
			addDao.updateState(addRefused);
		}
		// mettre � jour les annonces qui ont p�rim�s et ont �t� supprim�s apr�s mise en ligne
		addsNotOnlineAnymore = addDao.findAddsNotOnlineAnymore(new Add(compteLbc));
		for(Add addNotOnlineAnymore :  addsNotOnlineAnymore){
			addNotOnlineAnymore.setEtat(EtatAdd.notOnLineAnymore);
			addDao.updateState(addNotOnlineAnymore);
		}
		return true;
	}

	public List<Add> getAddsControled() {
		return addsToControle;
	}

	public void setAddsControled(List<Add> addsControled) {
		this.addsToControle = addsControled;
	}

	public boolean prepareAddsToSaving(CompteLbc compteLbc){
			boolean preparationOk = true; // la pr�paration est ok si chaque commune, texte, et titre du bon coin � une ref dans la bdd 
		for(Add add : addsToControle){
			add.setCompteLbc(compteLbc);
			// pr�parer les communes
			Commune communeComplete = communeDao.findOneWithNomCommune(add.getCommune()); 
			add.setCommune(communeComplete);

			// pr�parer les titres
			Title titreComplet = titreDao.findOneWithTitre(add.getTitle()); 
			add.setTitle(titreComplet);

			// pr�parer les textes
			List<Texte> textesCorrespondant;
			Texte texteRetenu=null;
			int nbRecherche = 0;
			boolean continueRecherche = true;
			do{
				Texte texteLbc = add.getTexte();
				textesCorrespondant = texteDao.findOneWithTexte(texteLbc);
				int levelCorresp = texteLbc.getLevelCorrespondance();
				int nbCorrespon = textesCorrespondant.size();
				if(nbCorrespon==0){
					texteLbc.setLevelCorrespondance(levelCorresp+1);
					System.out.println("Pas de correspondance � la recherche n�"+nbRecherche);
				}else if(nbCorrespon>=2){
					texteRetenu = textesCorrespondant.get(0);
					// on parcourt les textes de la bbd pour prendre celui avec la plus petite distance de Levenshtein
					for(Texte texteCorresp : textesCorrespondant){
						if(texteCorresp.getLevenshteinDistanceBetweenLbcAndBdd()<=texteRetenu.getLevenshteinDistanceBetweenLbcAndBdd()){
							texteRetenu=texteCorresp;
						}
					}
					texteLbc.setLevelCorrespondance(levelCorresp-1);
					System.out.println("Plus de 2 correspondance � la recherche n�"+nbRecherche);
					continueRecherche = false;
				}else{
					texteRetenu = textesCorrespondant.get(0);
					continueRecherche = false;
				}
				nbRecherche++;
			}while(continueRecherche);
			if(texteRetenu.getLevenshteinDistanceBetweenLbcAndBdd()>=30){
				textesPasDansLaBdd.add(texteRetenu);
				preparationOk=false;
			}
			add.setTexte(texteRetenu);
			
			addsReadyTosave = addsToControle;
		}
		return preparationOk;
	}

	public List<Texte> getTextesPasDansLaBdd() {
		return textesPasDansLaBdd;
	}

	public void setTextesPasDansLaBdd(List<Texte> textesPasDansLaBdd) {
		this.textesPasDansLaBdd = textesPasDansLaBdd;
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


	

	
	
}
