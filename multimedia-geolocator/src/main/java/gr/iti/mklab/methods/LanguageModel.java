package gr.iti.mklab.methods;

import gr.iti.mklab.data.Cell;
import gr.iti.mklab.util.CellCoder;
import gr.iti.mklab.util.EasyBufferedReader;
import gr.iti.mklab.util.MyHashMap;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

/**
 * It is the implementation of the language model. Here the word-cell probabilities are loaded and all calculation for the estimated location take place.
 * The model calculate the cell probabilities summing up the word-cell probabilities for every different cell based on the words that are contained in the query sentence.
 * 		  S
 * p(c) = Σ p(w|c)*N(e)
 * 		 w=1
 * The cell with that maximizes this summation considering as the Most Likely Cell for the query sentence.
 * @author gkordo
 *
 */
public class LanguageModel {

	protected Map<String,Map<Long,Double>> wordCellProbsMap;

	protected Map<String,Double> entropyWords;
	protected NormalDistribution gdWeight;

	private static Logger logger = Logger.getLogger("gr.iti.mklab.methods.LanguageModel");

	// The function that compose the other functions to calculate and return the Most Likely Cell for a query tweet.
	public Cell calculateLanguageModel(List<String> sentenceWords) {

		Map<Long, Cell> cellMap = calculateCellsProbForImageTags(sentenceWords);

		Cell mlc = findMLC(cellMap);

		return mlc;
	}

	// find the Most Likely Cell.
	private Cell findMLC(Map<Long, Cell> cellMap) {

		cellMap = MyHashMap.sortByMLCValues(cellMap);

		Cell mlc = null;

		if (!cellMap.isEmpty()){
			Long mlcId = Long.parseLong(cellMap.keySet().toArray()[0].toString());
			double confidence = calculateConfidence(cellMap,mlcId,0.3);
			
			mlc = cellMap.get(mlcId);
			mlc.setConfidence((float) confidence);
		}

		return mlc;
	}

	// Calculate confidence for the estimated location
	private static double calculateConfidence(Map<Long, Cell> cellMap, Long mlc, double l) {
		
		Double sum = 0.0, total = 0.0;

		for(Entry<Long, Cell> entry:cellMap.entrySet()){
			double[] mCell = CellCoder.cellDecoding(mlc);
			double[] cell = CellCoder.cellDecoding(entry.getKey());
			if((cell[0]>=(mCell[0]-l))&&(cell[0]<=(mCell[0]+l))
					&&(cell[1]>=(mCell[1]-l))&&(cell[1]<=(mCell[1]+l))){
				sum += entry.getValue().getTotalProb();
			}
			total += entry.getValue().getTotalProb();
		}

		return sum/total;
	}

	/**
	 * This is the function that calculate the cell probabilities.
	 * @param sentenceWords : list of words contained in tweet text
	 * @return a map of cell
	 */
	public Map<Long, Cell> calculateCellsProbForImageTags (List<String> sentenceWords) {

		Map<Long,Cell> cellMap = new HashMap<Long,Cell>();

		Long cell;
		for(String word:sentenceWords){
			if(wordCellProbsMap.containsKey(word)){
				double entropyValue= entropyWords.get(word);
				for(Entry<Long, Double> entry: wordCellProbsMap.get(word).entrySet()){
					cell = entry.getKey();
					if(cellMap.containsKey(cell)){
						cellMap.get(cell).addProb(entry.getValue()*gdWeight.density(entropyValue), word);
					}else{
						Cell tmp = new Cell(cell);
						tmp.addProb(entry.getValue()*gdWeight.density(entropyValue), word);
						cellMap.put(cell,tmp);
					}
				}
			}
		}
		return cellMap;
	}

	/**
	 * This is the constructor function load the word-cell probabilities file and create
	 * the respective map. The generated map allocate a significant amount of memory.
	 * @param wordCellProbsFile : file that contains the word-cell probabilities
	 */
	public LanguageModel(String wordCellProbsFile){

		EasyBufferedReader reader = new EasyBufferedReader(wordCellProbsFile);

		wordCellProbsMap = new HashMap<String,Map<Long,Double>>();;

		entropyWords = new HashMap<String,Double>();

		String input = reader.readLine();
		String word;

		List<Double> p = new ArrayList<Double>();

		logger.info("opening file" + wordCellProbsFile);
		logger.info("loading cells' probabilities for all tags");

		long t0 = System.currentTimeMillis();

		while ((input = reader.readLine())!=null){

			word = input.split("\t")[0];

			entropyWords.put(word, Double.parseDouble(input.split("\t")[1])); // load spatial entropy value of the tag 

			p.add(Double.parseDouble(input.split("\t")[1])); // load spatial entropy value of the tag for the Gaussian weight function

			String[] inputCells = input.split("\t")[2].split(" ");
			HashMap<Long, Double> tmpCellMap = new HashMap<Long,Double>();

			for(int i=0;i<inputCells.length;i++){
				long cellCode = CellCoder.cellEncoding(inputCells[i].split(">")[0]);
				String cellProb = inputCells[i].split(">")[1];
				tmpCellMap.put(cellCode, Double.parseDouble(cellProb));
			}
			wordCellProbsMap.put(word, tmpCellMap);
		}

		gdWeight = new NormalDistribution(
				new Mean().evaluate(ArrayUtils.toPrimitive(p.toArray(new Double[p.size()]))),
				new StandardDeviation().evaluate(ArrayUtils.toPrimitive(p.toArray(new Double[p.size()])))); // create the Gaussian weight function
		logger.info(wordCellProbsMap.size() + " words loaded in " + (System.currentTimeMillis()-t0)/1000.0 + "s");
		logger.info("closing file" + wordCellProbsFile);

		reader.close();
	}
}