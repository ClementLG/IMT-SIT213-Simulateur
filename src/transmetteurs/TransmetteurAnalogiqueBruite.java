package transmetteurs;

import destinations.DestinationInterface;

import java.lang.annotation.IncompleteAnnotationException;
import java.util.Random;
import information.Information;
import information.InformationNonConforme;

/**
 * Classe TransmetteurAnalogiqueParfait hérité de la classe Transmetteur
 *
 * @author c.legruiec
 * @author e.leduc
 * @author p.maquin
 * @author g.fraignac
 * @author m.lejeune
 */
public class TransmetteurAnalogiqueBruite extends Transmetteur<Float, Float>{
	float snr=0;
	Integer seed=null;
	private Information<Float> informationConverti;
	int nbEchantillon=30;
	
	public TransmetteurAnalogiqueBruite(int seed, float snr, int nbEchantillon) {
		this.seed=seed;
		this.snr=snr;
		this.nbEchantillon=nbEchantillon;
		informationConverti =new Information<>();

	}
	
	public TransmetteurAnalogiqueBruite(float snr, int nbEchantillon) {
		super();
		this.snr=snr;
		this.nbEchantillon=nbEchantillon;
		informationConverti =new Information<>();

	}
	
	public TransmetteurAnalogiqueBruite(float snr ) {
		super();
		this.snr=snr;
		this.nbEchantillon=nbEchantillon;
		informationConverti =new Information<>();
		nbEchantillon=30;

	}
	
	/**
     * canal Rx Information (abstract dans la classe mere)
     * 
     */
    public void recevoir(Information<Float> information) throws InformationNonConforme {
        informationRecue = information;
        ajoutBruit();
        emettre();//envoie l'information

    }

    /**
     * canal Tx Information (abstract dans la classe mere)
     * 
     */
    public void emettre() throws InformationNonConforme {
        for (DestinationInterface<Float> destinationConnectee : destinationsConnectees) {
        	destinationConnectee.recevoir(informationConverti);
        }
        informationEmise = informationConverti;//transmetteur parfait src=dest

    }
    
    private void ajoutBruit() {
    	float sigma=calculSigma();
    	Random rand1;
    	Random rand2;
    	if (seed!=null) {
			rand1=new Random(seed);
			rand2=new Random(seed);
		} else {
			rand1=new Random();
			rand2=new Random();
		}
    	float bruit=0f;
    	for (float info : informationRecue) {
    		//sigma=calculSigma(i);
        	bruit=(float) ((float) sigma*(Math.sqrt(-2*Math.log(1-rand1.nextFloat())))*(Math.cos(2*Math.PI*rand2.nextFloat())));
        	informationConverti.add(info+bruit);
    	}
    	
    }
    
    private float calculSigma() {
    	float Ps=0f;
    	float Sigma=0f;
  
        for (float info : informationRecue) {
        	Ps+=Math.pow(info, 2);
		}
        //on a enlev� le nombre d'echantillon par bit dans les calculs suite � une simplication.
        //il ne reste que le calcul de l'esperance des Ak�.
        Ps=Ps/informationRecue.nbElements(); 
        //calcul de sigmaCarre
        Sigma= (float) ((float) (Ps)/(2*Math.pow(10,snr/10)));
        Sigma=(float) Math.sqrt(Sigma);
    	
    	//calcul de la puissance moyenne
    	System.out.println("sigma:"+Sigma);
    	return Sigma;
    }
    
    

}
