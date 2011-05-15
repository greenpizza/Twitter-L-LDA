package uk.ac.cam.ha293.tweetlabel.topics;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uk.ac.cam.ha293.tweetlabel.classify.FullCalaisClassification;
import uk.ac.cam.ha293.tweetlabel.util.Tools;

public class FullLDAClassification {

	private long userID;
	private Map<String,Double> classifications;
	
	public FullLDAClassification(long userID, int numTopics, int burn, int sample, double alpha) {
		this.userID = userID;
		//Construct the path to the classification file
		String path = "classifications/lda/"+numTopics+"-"+burn+"-"+sample+"-"+alpha;
		path += "/"+userID+".csv";
		
		classifications = new HashMap<String,Double>();
		
		try {
			FileInputStream fstream = new FileInputStream(path);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			br.readLine(); //Skp the CSV descriptor
			while ((strLine = br.readLine()) != null)   {
				String[] split = strLine.split(",");
				String topic = split[0];
				double score = Double.parseDouble(split[1]);
				classifications.put(topic,score);
			}
			in.close();
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
		classifications = Tools.sortMapByValueDesc(classifications);
	}
	
	public void print() {
		for(String cat : classifications.keySet()) {
			System.out.println(cat+": "+classifications.get(cat));
		}
	}
	
	public Set<String> getCategorySet() {
		return classifications.keySet();
	}
	
	public boolean hasCategory(String cat) {
		if(classifications.keySet().contains(cat)) return true;
		else return false;
	}
	
	public double getScore(String cat) {
		return classifications.get(cat);
	}
	
	public double magnitude() {
		double sum = 0.0;
		for(String cat : classifications.keySet()) {
			sum += (classifications.get(cat)*classifications.get(cat));
		}
		sum = Math.sqrt(sum);
		return sum;
	}

	public double cosineSimilarity(FullLDAClassification c) {
		Set<String> catSet = new HashSet<String>();
		catSet.addAll(classifications.keySet());
		catSet.addAll(c.getCategorySet());
		
		double score = 0.0;
		for(String cat : catSet) {
			if(this.hasCategory(cat) && c.hasCategory(cat)) {
				score += (this.getScore(cat) * c.getScore(cat));
			}
		}
		
		//normalise by magnitudes
		Double magnitudes = (this.magnitude() * c.magnitude());
		score /= magnitudes;
		if(Double.isNaN(score)) {
			return 0.0; //NaN caused by zero vectors ie no classifications!
		}
		else return score;
	}
	
	public double jsDivergence(FullLDAClassification c) {
		Set<String> catSet = new HashSet<String>();
		catSet.addAll(classifications.keySet());
		catSet.addAll(c.getCategorySet());
		Map<String,Double> M = new HashMap<String,Double>();
		for(String cat : catSet) {
			if(this.hasCategory(cat) && c.hasCategory(cat)) {
				M.put(cat, (this.getScore(cat)+c.getScore(cat))/2.0);
			}
		}
		double d1 = 0.0;
		for(String cat : M.keySet()) {
			if(this.getCategorySet().contains(cat) ) {
				d1 += this.getScore(cat) * Math.log(this.getScore(cat)/M.get(cat));
			}
		}	
		double d2 = 0.0;
		for(String cat : M.keySet()) {
			if(c.getCategorySet().contains(cat)) {
				d1 += c.getScore(cat) * Math.log(c.getScore(cat)/M.get(cat));
			}
		}		
		double score = d1/2.0 + d2/2.0;
		return score;
	}
	
}
