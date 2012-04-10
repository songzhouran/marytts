package marytts.language.sv.phonemiser;

import java.util.LinkedList;
import java.util.ListIterator;

import org.apache.log4j.Logger;

import marytts.modules.phonemiser.Allophone;

import marytts.modules.phonemiser.AllophoneSet;




import marytts.exceptions.MaryConfigurationException;
import marytts.util.MaryUtils;

public class SV_Syllabifier {

    protected Logger logger;
    protected AllophoneSet allophoneSet;
    protected String stress;
    protected String[] stressMarker;

    public SV_Syllabifier(AllophoneSet allophoneSet, String stress) {
	this.allophoneSet = allophoneSet;
	this.stressMarker = stress.split("\\s+");

	this.logger = MaryUtils.getLogger("SV_Syllabifier");
    }

    public String syllabify(String phoneString) {
	LinkedList<String> phoneList = splitIntoAllophones(phoneString);
	syllabify(phoneList);
	StringBuilder sb = new StringBuilder();
	for (String ph : phoneList) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(ph);
        }
        return sb.toString();
    }

    public void syllabify(LinkedList<String> phoneList) {
	correctStressSymbol(phoneList);
	if (phoneList == null) return;
        ListIterator<String> it = phoneList.listIterator(0);
        if (!it.hasNext()) return;
	
	// for saving the position of each vowel in phoneList
	LinkedList<Integer> vowelPos = new LinkedList<Integer>();
	Allophone first=null;

	//How many vowels are there?
	//int vowels=0;
	int sonority=0;
	String s="";
	// For every phone...
	for (int i=0; i<phoneList.size(); i++) {
	    // check if phone is stressed
	    for (int j=0; j<stressMarker.length; j++) {
		if (phoneList.get(i).startsWith(stressMarker[j])) {
		    s=phoneList.get(i).substring(1,phoneList.get(i).length());
		    break;
		} else {
		    s=phoneList.get(i);
		}
	    }

	    if(getAllophone(s) != null) {
		sonority = getAllophone(s).sonority();
	    }
	    if (sonority>=4) {
		vowelPos.add(i);
		//vowels++;
	    }
	    // Look for syllable boundary between two vowels
	    if (vowelPos.size()>1) {
		LinkedList<String> consList = new LinkedList<String>();
		while (vowelPos.size()>1) {
		    // consonants between vowels
		    String cluster="";
		    for (int j=vowelPos.get(0)+1; j<vowelPos.get(1); j++) {
			consList.add(phoneList.get(j));
			cluster = cluster+phoneList.get(j);
		    }
		    // insert boundary before an initial cluster of consonants
		    if (initialCluster(cluster)) {
			phoneList.add(vowelPos.getFirst()+1,"-");
			vowelPos.set(1,vowelPos.get(1)+1);
			i++;
		    } else {
			int counter=0;
			while(consList.size()>0) {
			    cluster="";
			    for (int k=1; k<consList.size(); k++) {
			    	cluster = cluster+consList.get(k);
			    }
			    counter++;
			    if (initialCluster(cluster)) {
				phoneList.add(vowelPos.getFirst()+1+counter,"-");
				vowelPos.set(1,vowelPos.get(1)+1);
				i++;
				break;
			    }
			    consList.removeFirst();
			}
		    }

		    vowelPos.removeFirst();
		}
	    }
	}
    }

     /**
     * Checks if a cluster of consonants is a permitted initial sequence in swedish.
     */
    public boolean initialCluster(String cluster) {
	if (cluster.length()<2) {
	    // if matches non initial allophone return false, else true.
	    if (cluster.matches("N")) {
		    return false;
		} else return true;
	    // return true if a two consonant cluster matches this.
	} else if (cluster.length()<3) {
	    if (cluster.substring(0,2).matches("mj|nj|vr|dv|dr|tv|tr|gr|gl|gn|br|bl|bj|pr|pl|pj|kr|kl|kv|kn|fr|fl|fj|fn|sm|sn|sv|sf|sl|st|sk|sp")) {
		return true;
	    } else return false;
	    // return true if a three consonant cluster matches this.
	} else if (cluster.length()<4) {
	    if (cluster.substring(0,3).matches("str|skr|skv|spr|spl|spj")) {
		return true;
	    } else return false;
	    // return false if cluster is larger than three.   
	} else return false;
    }


    protected void correctStressSymbol(LinkedList<String> phoneList) {
	boolean stressFound = false;
	ListIterator<String> it = phoneList.listIterator(0);
	while(it.hasNext()) {
	    String s = it.next();
	    for(int i=0; i<stressMarker.length; i++) {
		String end="";
		end += (i+3);
		if (s.endsWith(end)) {
		    it.set(s.substring(0, s.length()-1));
		    int steps = 0;
		    it.set(stressMarker[i]+s.substring(0, s.length()-1));
		    while (steps > 0) {
                        it.next();
                        steps--;
                    }
		    stressFound = true;
		}
	    }	
	}
	// No stresses vowel in word?
	if (!stressFound) {
	    // Stress first non-schwa syllable
	    it = phoneList.listIterator(0);
            while (it.hasNext()) {
                String s = it.next();
                Allophone ph = allophoneSet.getAllophone(s);
                if (ph != null && ph.sonority() >= 5) { // non-schwa vowel
                    // Search backwards for syllable boundary or beginning of word:
		    /*
                    int steps = 0;
                    while (it.hasPrevious()) {
                        steps++;
                        String t = it.previous();
                        if (t.equals("-") || t.equals("_")) { // syllable boundary
                            it.next();
                            steps--;
                            break;
                        }
                    }
                    it.add("'");
                    while (steps > 0) {
                        it.next();
                        steps--;
                    }
		    */
		    it.set(stressMarker[0]+s);
                    break; // OK, that's it.
                }
            }
        }
    }
    

    /**
     * Splits a phone string into allophones, returning a linked list.
     */
    protected LinkedList<String> splitIntoAllophones(String phoneString) {
	LinkedList<String> phoneList = new LinkedList<String>();
	for (int i=0; i<phoneString.length(); i++) {
            // Try to cut off individual segments, 
            // starting with the longest prefixes,
            // and allowing for a suffix number marking stress:
            String name = null;
            for (int j=3; j>=1; j--) {
                if (i+j <= phoneString.length()) {
                    String candidate = phoneString.substring(i, i+j);
                    if (getAllophone(candidate) != null) { // found
                        name = candidate;
                        i+=j-1; // so that the next i++ goes beyond current phone
                        break;
                    }
                }
            }
            if (name != null) {
                phoneList.add(name);
            }
        }
	return phoneList;
    }

    protected Allophone getAllophone(String phone) {
        if (phone.endsWith("3")) {
            return allophoneSet.getAllophone(phone.substring(0,phone.length()-1));
        } else if (phone.endsWith("4")) {
	    return allophoneSet.getAllophone(phone.substring(0,phone.length()-1));
	} else if (phone.endsWith("5")) {
	    return allophoneSet.getAllophone(phone.substring(0,phone.length()-1));
	} else
            return allophoneSet.getAllophone(phone);
    }



    public static void main(String[] args) throws Exception {
	
	AllophoneSet allophoneSet = AllophoneSet.getAllophoneSet(args[0]);

	SV_Syllabifier syl = new SV_Syllabifier(allophoneSet, "' , %");
	syl.syllabify(args[1]);
	
    }

}