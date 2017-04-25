package finder.patterns.meso;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import finder.graph.Edge;
import finder.graph.Graph;
import finder.graph.Node;
import finder.patterns.CompositePattern;
import finder.patterns.InventorialPattern;
import finder.patterns.Pattern;
import finder.patterns.micro.Enemy;
import finder.patterns.micro.Room;
import game.Map;

/**
 * The GuardedTreasure class represents the GuardedTreasure pattern.
 * 
 * <p>This is not yet properly implemented and is mostly added as a macro
 * pattern placeholder.
 * 
 * @author Johan Holmberg
 */
public class GuardedTreasure extends CompositePattern {
	
	public static List<CompositePattern> matches(Map map, Graph<Pattern> patternGraph) {
		List<CompositePattern> guardedTreasures = new ArrayList<CompositePattern>();
		
		patternGraph.resetGraph();
		
		Queue<Node<Pattern>> nodeQueue = new LinkedList<Node<Pattern>>();
		nodeQueue.add(patternGraph.getStartingPoint());
		
		while(!nodeQueue.isEmpty()){
			Node<Pattern> current = nodeQueue.remove();
			current.tryVisit();
			if(current.getValue() instanceof Room){
				List<InventorialPattern> containedEnemies = ((Room)current.getValue()).getContainedPatterns().stream().filter(p->{return p instanceof Enemy;}).collect(Collectors.toList());
				if(containedEnemies.size() > 1){
					GuardedTreasure g = new GuardedTreasure();
					g.patterns.add(current.getValue());
					g.patterns.addAll(containedEnemies);
					guardedTreasures.add(g);
					//System.out.println("Got a guard room!");
				}
			}
			nodeQueue.addAll(current.getEdges().stream().map((Edge<Pattern> e)->{
				Node<Pattern> ret = null;
				if(e.getNodeA() == current)
					ret = e.getNodeB();
				else
					ret = e.getNodeA();
				return ret;
				}).filter((Node<Pattern> n)->{return !n.isVisited();}).collect(Collectors.toList()));
		}
		
		return guardedTreasures;
	}
	
}
