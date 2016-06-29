package gr.iti.mklab.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.*;
import java.util.stream.Stream;

import gr.iti.mklab.geo.GeoCell;

public class MyHashMap extends HashMap<Object, Object> {

	private static final long serialVersionUID = 8005524467946015401L;
	
	public static <K extends Comparable,V extends Comparable> Map<K,V> sortByValues(Map<K,V> unsortMap){
		
        List<Map.Entry<K,V>> entries = new LinkedList<Map.Entry<K,V>>(unsortMap.entrySet());
      
        Collections.sort(entries, Collections.reverseOrder(new Comparator<Map.Entry<K,V>>() {

            @SuppressWarnings("unchecked")
			public int compare(Entry<K, V> o1, Entry<K, V> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        }));
        
        //LinkedHashMap will keep the keys in the order they are inserted
        //which is currently sorted on natural ordering
        Map<K,V> sortedMap = new LinkedHashMap<K,V>();
        for(Map.Entry<K,V> entry: entries){
            sortedMap.put(entry.getKey(), entry.getValue());
        }
      
        return sortedMap;
    }
	
	public static Map<Long, GeoCell> sortByMLCValues(Map<Long, GeoCell> unsortMap) {
		 
		// Convert Map to List
		List<Map.Entry<Long, GeoCell>> list = 
			new LinkedList<Map.Entry<Long, GeoCell>>(unsortMap.entrySet());
 
		// Sort list with comparator, to compare the Map values
		Collections.sort(list, new Comparator<Map.Entry<Long, GeoCell>>() {
			public int compare(Map.Entry<Long, GeoCell> o1,
                                           Map.Entry<Long, GeoCell> o2) {
				return -(o1.getValue()).getTotalProb().compareTo(o2.getValue().getTotalProb());
			}
		});

		// Convert sorted map back to a Map
		Map<Long, GeoCell> sortedMap = new LinkedHashMap<Long, GeoCell>();
		for (Iterator<Map.Entry<Long, GeoCell>> it = list.iterator(); it.hasNext();) {
			Map.Entry<Long, GeoCell> entry = it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}
}
