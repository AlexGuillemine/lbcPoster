package scraper;

public class ResultsControl {
	
	private int nbRefus; // annonces refus�s apr�s tentative de mise en ligne
	private int nbSuppression; // annonces supprim�s � cause de mod�ration lbc ou p�remption apr�s mise en ligne
	private int nbNewAddsOnline; // nb nouvelles adds online
	private int nbAddStillOnline;
	
	public ResultsControl(int nbRefus, int nbSuppression, int nbNewAddsOnline, int nbAddStillOnline) {
		super();
		this.nbRefus = nbRefus;
		this.nbSuppression = nbSuppression;
		this.nbNewAddsOnline = nbNewAddsOnline;
		this.nbAddStillOnline = nbAddStillOnline;
	}
	public int getNbRefus() {
		return nbRefus;
	}
	public void setNbRefus(int nbRefus) {
		this.nbRefus = nbRefus;
	}
	public int getNbSuppression() {
		return nbSuppression;
	}
	public void setNbSuppression(int nbSuppression) {
		this.nbSuppression = nbSuppression;
	}
	public int getNbNewAddsOnline() {
		return nbNewAddsOnline;
	}
	public int getNbAddStillOnline() {
		return nbAddStillOnline;
	}
	
	
}
