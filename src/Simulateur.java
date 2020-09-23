import sources.*;
import destinations.*;
import transmetteurs.*;

import information.*;

import visualisations.*;

import java.util.regex.*;
import java.util.*;
import java.lang.Math;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;


/**
 * La classe Simulateur permet de construire et simuler une chaine de
 * transmission composee d'une Source, d'un nombre variable de
 * Transmetteur(s) et d'une Destination.
 *
 * @author cousin
 * @author prou
 * @author c.legruiec
 * @author e.leduc
 * @author p.maquin
 * @author g.fraignac
 * @author m.lejeune
 */
public class Simulateur {

    /**
     * indique si le Simulateur utilise des sondes d'affichage
     */
    private boolean affichage = true;
    /**
     * indique si le Simulateur utilise un message genere de maniere aleatoire
     */
    private boolean messageAleatoire = true;
    /**
     * indique si le Simulateur utilise un germe pour initialiser les generateurs aleatoires
     */
    private boolean aleatoireAvecGerme = false;
    /**
     * la valeur de la semence utilisee pour les generateurs aleatoires
     */
    private Integer seed = null;
    /**
     * la longueur du message aleatoire a transmettre si un message n'est pas impose
     */
    private int nbBitsMess = 100;
    /**
     * la chaine de caracteres correspondant a m dans l'argument -mess m
     */
    private String messageString = "100";

    /**
     * la forme correspondant a f dans l'argument -form f. 3 choix possible NRZ, NRZT, RZ.
     */
    private String form = "NRZT";

    /**
     * la forme correspondant a f dans l'argument -form f. 3 choix possible NRZ, NRZT, RZ.
     */
    private float snr = 10f;

    /**
     * le  composant Source de la chaine de transmission
     */
    private Source<Boolean> source = null;

    /**
     * le  composant Transmetteur parfait logique de la chaine de transmission
     */
    private Transmetteur<Boolean, Boolean> transmetteurLogique = null;

    /**
     * le  composant Destination de la chaine de transmission
     */
    private Destination<Boolean> destination = null;

    /**
     *  'ne' precise le nombre d’échantillons par bit
     */
    private int ne = 30;

    /**
     *  'min' precise l'amplitude minimale du signale analogique
     */
    private float min = 0;

    /**
     *  'max' precise l'amplitude maximale du signale analogique
     */
    private float max = 1;

    /**
     *  'export' precise la destination de l'export du TEB
     */
    private String export = null;

    /**
     * Le constructeur de Simulateur construit une chaine de
     * transmission composee d'une Source <Boolean>, d'une Destination
     * <Boolean> et de Transmetteur(s) [voir la methode
     * analyseArguments]...  <br> Les differents composants de la
     * chaine de transmission (Source, Transmetteur(s), Destination,
     * Sonde(s) de visualisation) sont crees et connectes.
     *
     * @param args le tableau des differents arguments.
     * @throws ArgumentsException si un des arguments est incorrect
     */
    public Simulateur(String[] args) throws ArgumentsException {

    	//Analyse des arguments
        analyseArguments(args);

        //Instanciations des differents blocs de traitement
        if (messageAleatoire) {
        	source=new SourceAleatoire(nbBitsMess, seed);
        } else {
        	source=new SourceFixe(messageString);
        }


        Transmetteur<Boolean, Float> emetteur = new Emetteur(max, min, ne, form);
        Transmetteur<Float, Float> transmetteurAnalogiqueParfait=new TransmetteurAnalogiqueParfait();
        Transmetteur<Float, Float> transmetteurAnalogiqueBruitReel;
        if (aleatoireAvecGerme) {
            transmetteurAnalogiqueBruitReel=new TransmetteurAnalogiqueBruitReel(seed,snr, ne);
		} else {
            transmetteurAnalogiqueBruitReel=new TransmetteurAnalogiqueBruitReel(snr, ne);
		}

        Transmetteur<Float, Boolean> recepteur=new Recepteur(max, min, ne, form);
        destination=new DestinationFinale();

        //Instanciations des differentes sondes
        SondeLogique viewSrc = new SondeLogique("ViewSrc", 720);
        SondeAnalogique viewEmet = new SondeAnalogique("ViewEmet");
        SondeAnalogique viewTransmitAna = new SondeAnalogique("ViewTransmitAna");
        SondeLogique viewDest = new SondeLogique("ViewDest", 720);



        //connexion des blocs ensembles
        source.connecter(emetteur);
        emetteur.connecter(transmetteurAnalogiqueBruitReel);
        //transmetteurAnalogiqueParfait.connecter(recepteur);
        transmetteurAnalogiqueBruitReel.connecter(recepteur);
        recepteur.connecter(destination);

        if(affichage) {
        	source.connecter(viewSrc);
        	emetteur.connecter(viewEmet);
        	//transmetteurAnalogiqueParfait.connecter(viewTransmitAna);
        	transmetteurAnalogiqueBruitReel.connecter(viewTransmitAna);
        	recepteur.connecter(viewDest);
        }

        //transmetteurLogique.connecter(destination);
        //if(affichage) transmetteurLogique.connecter(viewTransmit);




    }


    /**
     * La methode analyseArguments extrait d'un tableau de chaines de
     * caracteres les differentes options de la simulation.  Elle met
     * a jour les attributs du Simulateur.
     *
     * @param args le tableau des differents arguments.
     *             <br>
     *             <br>Les arguments autorises sont :
     *             <br>
     *             <dl>
     *             <dt> -mess m  </dt><dd> m (String) constitue de 7 ou plus digits a 0 | 1, le message a transmettre</dd>
     *             <dt> -mess m  </dt><dd> m (int) constitue de 1 a 6 digits, le nombre de bits du message "aleatoire" a  transmettre</dd>
     *             <dt> -s </dt><dd> utilisation des sondes d'affichage</dd>
     *             <dt> -seed v </dt><dd> v (int) d'initialisation pour les generateurs aleatoires</dd>
     *             </dl>
     * @throws ArgumentsException si un des arguments est incorrect.
     */
    public void analyseArguments(String[] args) throws ArgumentsException {

        for (int i = 0; i < args.length; i++) {

            if (args[i].matches("-s")) {
                affichage = true;
            } else if (args[i].matches("-seed")) {
                aleatoireAvecGerme = true;
                i++;
                // traiter la valeur associee
                try {
                    seed = Integer.valueOf(args[i]);
                } catch (Exception e) {
                    throw new ArgumentsException("Valeur du parametre -seed  invalide :" + args[i]);
                }
            } else if (args[i].matches("-mess")) {
                i++;
                // traiter la valeur associee
                messageString = args[i];
                if (args[i].matches("[0,1]{7,}")) {
                    messageAleatoire = false;
                    nbBitsMess = args[i].length();
                } else if (args[i].matches("[0-9]{1,6}")) {
                    messageAleatoire = true;
                    nbBitsMess = Integer.valueOf(args[i]);
                    if (nbBitsMess < 3)
                        throw new ArgumentsException("Valeur du parametre -mess invalide : " + nbBitsMess);
                } else
                    throw new ArgumentsException("Valeur du parametre -mess invalide : " + args[i]);
            } else if (args[i].matches("-form")) {
            	i++;
            	// traiter la valeur associee
            	if(args[i].matches("\\bRZ\\b|\\bNRZ\\b|\\bNRZT\\b")) form=args[i];
            	else throw new ArgumentsException("Forme invalide :" + args[i]);
            } else if (args[i].matches("-nbEch")) {
            	i++;
            	// traiter la valeur associee
            	if(Integer.parseInt(args[i])>0) {
            		ne=Integer.parseInt(args[i]);
            		ne -= ne%3;
            	}
            	else throw new ArgumentsException("Nombre d'echantillon invalide :" + args[i]);
            } else if (args[i].matches("-ampl")) {
            	i++;
            	// traiter la valeur associee
            	if(args[i].matches("^-?\\d*(\\.\\d+)?$")) min=Float.parseFloat(args[i]);
            	else throw new ArgumentsException("Amplitude min incorecte :" + args[i]);
            	i++;
            	if(args[i].matches("^-?\\d*(\\.\\d+)?$")) max=Float.parseFloat(args[i]);
            	else throw new ArgumentsException("Amplitude max incorecte :" + args[i]);
            	if(min>max) throw new ArgumentsException("Amplitudes incorectes (min>max) : " + min + ">"+max);

            } else if (args[i].matches("-snrpb")) {
            	i++;
            	snr=Float.parseFloat(args[i]);
            }else if (args[i].matches("-export")) {
            	i++;
            	export=args[i];
            }else throw new ArgumentsException("Option invalide :" + args[i]);

        }

    }


    /**
     * La methode execute effectue un envoi de message par la source
     * de la chaine de transmission du Simulateur.
     *
     * @throws Exception si un probleme survient lors de l'execution
     */
    public void execute() throws Exception {
    	source.emettre();

    }


    /**
     * La methode qui calcule le taux d'erreur binaire en comparant
     * les bits du message emis avec ceux du message recu.
     *
     * @return La valeur du Taux dErreur Binaire.
     */
    public float calculTauxErreurBinaire() {

    	//Attention si tailles des tableaux differentes ?? --> demander si possible

    	int nbErr=0;
    	float TEB=0.0f;
    	for (int i = 0; i < destination.getInformationRecue().nbElements(); i++) {
			if(destination.getInformationRecue().iemeElement(i)!=source.getInformationEmise().iemeElement(i)) nbErr++;
		}
    	//si taille differente on compte les bits manquant comme erreurs
    	if(destination.getInformationRecue().nbElements()!=source.getInformationEmise().nbElements()) {
    		nbErr+=Math.abs(source.getInformationEmise().nbElements()-destination.getInformationRecue().nbElements());
    	}
    	TEB=(nbErr*1.0f)/(source.getInformationEmise().nbElements());


        return TEB;
    }

    public void exportDuTEB(float TEB) {
    	if(export!=null) {
    		try
    		{
    		    String filename= "C:\\Users\\clegruiec\\OneDrive - RETIS\\IMT\\IMT-SIT213\\src\\test.txt";
    			//String filename= export;
    		    FileWriter fw = new FileWriter(filename,true); //the true will append the new data
    		    fw.write(TEB+"\n");//appends the string to the file
    		    fw.close();
    		}
    		catch(IOException ioe)
    		{
    		    System.err.println("IOException: " + ioe.getMessage());
    		}
    	}

    }


    /**
     * La fonction main instancie un Simulateur a l'aide des
     * arguments parametres et affiche le resultat de l'execution
     * d'une transmission.
     *
     * @param args les differents arguments qui serviront a l'instanciation du Simulateur.
     */
    public static void main(String[] args) {

        Simulateur simulateur = null;

        try {
            simulateur = new Simulateur(args);
        } catch (Exception e) {
            System.out.println(e);
            System.exit(-1);
        }

        try {
            simulateur.execute();
            float tauxErreurBinaire = simulateur.calculTauxErreurBinaire();
            //simulateur.exportDuTEB(tauxErreurBinaire);
            String s = "java  Simulateur  ";
            for (String arg : args) {
                s += arg + "  ";
            }
            System.out.println(s + "  =>   TEB : " + tauxErreurBinaire);
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
            System.exit(-2);
        }
    }
}

