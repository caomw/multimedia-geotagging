package gr.iti.mklab.methods;

import gr.iti.mklab.tools.DataManager;
import gr.iti.mklab.util.EasyBufferedReader;
import gr.iti.mklab.util.MyHashMap;
import gr.iti.mklab.util.Progress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.log4j.Logger;

/**
 * This class is the core of the algorithm. It is the implementation of the language model.
 * The Most Likely Cell of the given image is calculated.
 * @author gkordo
 *
 */
public class LanguageModel {

	private static NormalDistribution gd;

	protected Map<String,Double> entropyTags;
	protected String file, dir;

	static Logger logger = Logger.getLogger("gr.iti.mklab.method.LanguageModel");

	// Constructor initializes the needed maps
	public LanguageModel(String dir, String file){
		this.entropyTags = new HashMap<String,Double>();
		this.file = dir+file;
	}

	/**
	 * Calculate the probability of every cell based on the given tags of the query image
	 * @param imageTags : the tags and title of an query image
	 * @return the most probable cell
	 */
	public String calculateLanguageModel(List<String> imageTags, Map<String,Map<String,Double>> tagCellProbsMap) {

		Map<String, Double[]> cellMap = calculateCellsProbForImageTags(imageTags, tagCellProbsMap);

		return findMostLikelyCell(cellMap);
	}

	/**
	 * Calculate the probability of every cell based on the given tags of the query image
	 * @param cellMap : map with the cell probabilities
	 * @return the most probable cell
	 */
	public String findMostLikelyCell(Map<String, Double[]> cellMap) {

		cellMap = MyHashMap.sortByValuesTable(cellMap); // descending sort of cell probabilities

		String mostLikelyCell = null;
		if (!cellMap.isEmpty())
			mostLikelyCell = cellMap.keySet().toArray()[0].toString(); // pick the first cell as the Most Likely Cell

		return mostLikelyCell;
	}

	/**
	 * The function that apply the language model on the given tag set
	 * @param imageTags : the tags and title of an query image
	 * @return 
	 */
	public Map<String, Double[]> calculateCellsProbForImageTags (List<String> imageTags,Map<String,Map<String,Double>> tagCellProbsMap) {

		Map<String,Double[]> cellList = new HashMap<String,Double[]>();

		String tag;
		String cell;
		for(int i=0;i<imageTags.size();i++){
			tag = imageTags.get(i);
			
			if(tagCellProbsMap.containsKey(tag)){ // the probability summation for the specific cell has been initialized
				for(Entry<String, Double> entry: tagCellProbsMap.get(tag).entrySet()){
					
					cell = entry.getKey();
					if(cellList.containsKey(cell)){
						Double[] tmp = cellList.get(cell);
						tmp[0] += entry.getValue()*(gd.density(entropyTags.get(tag))); // sum of the weighted tag-cell probabilities
						tmp[1] += 1.0;
						cellList.put(cell,tmp);			
					}else{ // initialization of the probability summation for the particular cell
						Double[] tmp = new Double[2];
						tmp[0] = entry.getValue()*(gd.density(entropyTags.get(tag))); // Initialization of the summation of  weighted tag-cell probabilities
						tmp[1] = 1.0;
						cellList.put(cell,tmp);
					}
				}
			}
		}
		return cellList;
	}

	/**
	 *  initialize Language Model
	 * @param testFile : file that contains test image metadata
	 * @param tagAccFile : the file that contains the accuracies of the tags
	 * @param featureSelection : argument that indicates if the feature selection is used or not 
	 * @param thetaG : feature selection accuracy threshold
	 * @param thetaT : feature selection frequency threshold
	 * @return
	 */
	public Map<String,Map<String,Double>> organizeMapOfCellsTags(String testFile, String tagAccFile, boolean featureSelection, double thetaG, int thetaT){

		EasyBufferedReader reader = new EasyBufferedReader(file);

		Map<String,Map<String,Double>> tagCellProbsMap = new HashMap<String,Map<String,Double>>();

		String input;
		String tag;

		List<Double> p = new ArrayList<Double>();

		Set<String> tagsInTestSet = DataManager.getSetOfTags(testFile);

		Set<String> selectedTags = new HashSet<String>();

		if(featureSelection){
			selectedTags = selectTagAccuracies(tagAccFile, thetaG, thetaT); // feature selection
		}
		logger.info("loading cells' probabilities for all tags from " + file);

		long startTime = System.currentTimeMillis();

		Progress prog = new Progress(startTime,10,1,"loading",logger);

		// load tag-cell probabilities from the given file
		while ((input = reader.readLine())!=null){

			prog.showMessege(System.currentTimeMillis());

			tag = input.split("\t")[0];

			if(input.split("\t").length>1 && 
					tagsInTestSet.contains(tag) && (selectedTags.contains(tag)||!featureSelection)){

				entropyTags.put(tag, Double.parseDouble(input.split("\t")[1])); // load spatial entropy value of the tag 

				p.add(Double.parseDouble(input.split("\t")[1])); // load spatial entropy value of the tag for the Gaussian weight function

				String[] inputCells = input.split("\t")[2].split(" ");
				HashMap<String, Double> tmpCellMap = new HashMap<String,Double>();

				for(int i=0;i<inputCells.length;i++){
					String cellCode = inputCells[i].split(">")[0];
					String cellProb = inputCells[i].split(">")[1];
					tmpCellMap.put(cellCode, Double.parseDouble(cellProb));
				}
				tagCellProbsMap.put(tag, tmpCellMap);
			}
		}

		gd = new NormalDistribution(
				new Mean().evaluate(ArrayUtils.toPrimitive(p.toArray(new Double[p.size()]))),
				new StandardDeviation().evaluate(ArrayUtils.toPrimitive(p.toArray(new Double[p.size()])))); // create the Gaussian weight function
		logger.info(tagCellProbsMap.size()+" tags loaded in "+(System.currentTimeMillis()-startTime)/1000.0+"s");
		reader.close();

		return tagCellProbsMap;
	}

	/**
	 * Select tags based on their accuracies.
	 * @param file : tag accuracies file
	 * @param thetaG : accuracy threshold
	 * @param thetaU : times found threshold
	 * @return
	 */
	private static Set<String> selectTagAccuracies(String file, double thetaG, int thetaT){
		EasyBufferedReader reader = new EasyBufferedReader(file);
		Set<String> tagSet = new HashSet<String>();

		String line;
		int total = 0;

		while((line= reader.readLine()) != null){
			if(Double.parseDouble(line.split(" ")[1])>thetaG && Double.parseDouble(line.split(" ")[2])>thetaT){
				tagSet.add(line.split(" ")[0]);
			}
			total++;
		}

		logger.info(tagSet.size() + " tags selected from the total of " + total + " tags");
		logger.info("Ratio of the selected tags : " + (double)tagSet.size()/total);
		reader.close();

		return tagSet;
	}
}
