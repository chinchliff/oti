package org.opentree.oti;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import opentree.tnrs.queries.AbstractBaseQuery;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.FuzzyQuery;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.opentree.graphdb.GraphDatabaseAgent;
import org.opentree.oti.indexproperties.OTINodeProperty;
import org.opentree.properties.OTPropertyPredicate;
import org.opentree.properties.OTVocabularyPredicate;

public class QueryRunner extends OTIDatabase {
	
	public final Index<Node> studyMetaNodesByPropertyExact = getNodeIndex(OTINodeIndex.STUDY_METADATA_NODES_BY_PROPERTY_EXACT);
	public final Index<Node> studyMetaNodesByPropertyFulltext = getNodeIndex(OTINodeIndex.STUDY_METADATA_NODES_BY_PROPERTY_FULLTEXT);
	public final Index<Node> treeRootNodesByPropertyExact = getNodeIndex(OTINodeIndex.TREE_ROOT_NODES_BY_PROPERTY_EXACT);
	public final Index<Node> treeRootNodesByPropertyFulltext = getNodeIndex(OTINodeIndex.TREE_ROOT_NODES_BY_PROPERTY_FULLTEXT);
	public final Index<Node> treeNodesByPropertyExact = getNodeIndex(OTINodeIndex.TREE_NODES_BY_PROPERTY_EXACT);
	public final Index<Node> treeNodesByPropertyFulltext = getNodeIndex(OTINodeIndex.TREE_NODES_BY_PROPERTY_FULLTEXT);

	public QueryRunner(EmbeddedGraphDatabase embeddedGraph) {
		super(embeddedGraph);
	}

	public QueryRunner(GraphDatabaseService gdbs) {
		super(gdbs);
	}

	public QueryRunner(GraphDatabaseAgent gdba) {
		super(gdba);
	}

	/**
	 * Search the indexes for study matching the search parameters
	 * @param property
	 * 		A SearchableProperty to specify the search domain
	 * @param searchValue
	 * 		The value to be searched for
	 * @return
	 * 		A list of strings containing the node ids of the source meta nodes for sources found during search
	 */
	public Object doBasicSearchForStudies(OTPropertyPredicate property, String searchValue, boolean isExactProperty, boolean isFulltextProperty) {

		HashMap<String, String> studiesFound = new HashMap<String, String>();

   		// using fuzzy queries ... may want to use different queries for exact vs. fulltext indexes
		FuzzyQuery fuzzyQuery = new FuzzyQuery(new Term(property.propertyName(), QueryParser.escape(searchValue.toLowerCase())),
    			AbstractBaseQuery.getMinIdentity(searchValue));
		IndexHits<Node> hits = studyMetaNodesByPropertyExact.query(fuzzyQuery);

		try {
        	if (isExactProperty) {
				hits = studyMetaNodesByPropertyExact.query(fuzzyQuery);
				for (Node hit : hits) {
					studiesFound.put(
							OTVocabularyPredicate.OT_STUDY_ID.propertyName(),
							(String) hit.getProperty(OTVocabularyPredicate.OT_STUDY_ID.propertyName()));
				}
        	}
        	if (isFulltextProperty) {
        		hits = studyMetaNodesByPropertyFulltext.query(fuzzyQuery);
        		for (Node hit : hits) {
        			studiesFound.put(
							OTVocabularyPredicate.OT_STUDY_ID.propertyName(),
							(String) hit.getProperty(OTVocabularyPredicate.OT_STUDY_ID.propertyName()));
        		}
        	}
		} finally {
			hits.close();
		}
        
		return studiesFound;
	}
	
	/**
	 * Search the indexes for trees matching the search parameters
	 * @param property
	 * 		A SearchableProperty to specify the search domain
	 * @param searchValue
	 * 		The value to be searched for
	 * @return
	 * 		A Map object containing information about hits to the search
	 */
	public Object doBasicSearchForTrees(OTPropertyPredicate property, String searchValue, boolean isExactProperty, boolean isFulltextProperty) {

		HashSet<Long> treeRootNodeIds = new HashSet<Long>();
	
   		// using fuzzy queries ... may want to use different queries for exact vs. fulltext indexes
		FuzzyQuery fuzzyQuery = new FuzzyQuery(new Term(property.propertyName(), QueryParser.escape(searchValue.toLowerCase())),
    			AbstractBaseQuery.getMinIdentity(searchValue));
		IndexHits<Node> hits = null;

        try {
        	if (isExactProperty) {
				hits = treeRootNodesByPropertyExact.query(fuzzyQuery);
				for (Node hit : hits) {
					treeRootNodeIds.add(hit.getId());
				}
        	}
        	if (isFulltextProperty) {
				hits = treeRootNodesByPropertyFulltext.query(fuzzyQuery);
				for (Node hit : hits) {
					treeRootNodeIds.add(hit.getId());
				}
        	}
        } finally {
			hits.close();
		}
        
		LinkedList<HashMap<String, String>> treesFound = new LinkedList<HashMap<String,String>>();

		// record identifying information about the trees found
		for (Long nid : treeRootNodeIds) {
			Node hit = graphDb.getNodeById(nid);
			HashMap<String, String> treeResult = new HashMap<String, String>();
			treeResult.put(OTINodeProperty.TREE_ID.propertyName(), (String) hit.getProperty(OTINodeProperty.TREE_ID.propertyName()));
			treeResult.put(OTINodeProperty.NEXSON_ID.propertyName(), (String) hit.getProperty(OTINodeProperty.NEXSON_ID.propertyName())); 
			treeResult.put(OTVocabularyPredicate.OT_STUDY_ID.propertyName(), (String) hit.getProperty(OTVocabularyPredicate.OT_STUDY_ID.propertyName()));
			treesFound.add(treeResult);
		}

		return treesFound;
	}
	
	/**
	 * Search the indexes for tree nodes matching the search parameters
	 * @param property
	 * 		A SearchableProperty to specify the search domain
	 * @param searchValue
	 * 		The value to be searched for
	 * @return
	 * 		A Map object containing information about hits to the search
	 */
	public Object doBasicSearchForTreeNodes(OTPropertyPredicate property, String searchValue, boolean isExactProperty, boolean isFulltextProperty) {

		HashMap<Long, HashSet<Long>> treeRootNodeIdToMatchedTipNodeIdMap = new HashMap<Long, HashSet<Long>>();
	
   		// using fuzzy queries ... may want to use different queries for exact vs. fulltext indexes
		FuzzyQuery fuzzyQuery = new FuzzyQuery(new Term(property.propertyName(), QueryParser.escape(searchValue.toLowerCase())),
				AbstractBaseQuery.getMinIdentity(searchValue));
		IndexHits<Node> hits = null;
        try {
        	if (isExactProperty) {
        		hits = treeNodesByPropertyExact.query(fuzzyQuery);
				for (Node hit : hits) {
			
					Long rootId = OTIDatabaseUtils.getRootOfTreeContaining(hit).getId();
					
					if (!treeRootNodeIdToMatchedTipNodeIdMap.containsKey(rootId)) {
						treeRootNodeIdToMatchedTipNodeIdMap.put(rootId, new HashSet<Long>());
					}
					
					treeRootNodeIdToMatchedTipNodeIdMap.get(rootId).add(hit.getId());
				}
        	}
        	if (isFulltextProperty) {
        		hits = treeNodesByPropertyFulltext.query(fuzzyQuery);
				for (Node hit : hits) {
					
					Long rootId = OTIDatabaseUtils.getRootOfTreeContaining(hit).getId();
					
					if (!treeRootNodeIdToMatchedTipNodeIdMap.containsKey(rootId)) {
						treeRootNodeIdToMatchedTipNodeIdMap.put(rootId, new HashSet<Long>());
					}
					
					treeRootNodeIdToMatchedTipNodeIdMap.get(rootId).add(hit.getId());
				}
        	}
        } finally {
			hits.close();
		}
        
		LinkedList<HashMap<String,Object>> treesFound = new LinkedList<HashMap<String,Object>>();

		// record identifying information about the trees found
		for (Long treeRootId : treeRootNodeIdToMatchedTipNodeIdMap.keySet()) {
			
			HashMap<String, Object> treeResult = new HashMap<String, Object>();
			Node treeRootNode = graphDb.getNodeById(treeRootId);
			treeResult.put(OTINodeProperty.NEXSON_ID.propertyName(), (String) treeRootNode.getProperty(OTINodeProperty.NEXSON_ID.propertyName()));
			treeResult.put(OTVocabularyPredicate.OT_STUDY_ID.propertyName(), (String) treeRootNode.getProperty(OTVocabularyPredicate.OT_STUDY_ID.propertyName()));

			HashMap<String, String> nodes = new HashMap<String, String>();
			for (Long nodeId : treeRootNodeIdToMatchedTipNodeIdMap.get(treeRootId)) {
				nodes.put(OTINodeProperty.NEXSON_ID.propertyName(), (String) graphDb.getNodeById(nodeId).getProperty(OTINodeProperty.NEXSON_ID.propertyName()));
			}

			treeResult.put("matched_nodes", nodes);
			treesFound.add(treeResult);
		}

		return treesFound;
	}
}