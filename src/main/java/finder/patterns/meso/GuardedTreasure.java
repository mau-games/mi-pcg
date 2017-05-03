package finder.patterns.meso;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils.Collections;

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
	
	public double getQuality(){
		return 1.0;	
	}
	
	public static List<CompositePattern> matches(Map map, Graph<Pattern> patternGraph) {
		List<CompositePattern> guardedTreasures = new ArrayList<CompositePattern>();
		
		List<TreasureRoom> treasureRooms = new ArrayList<TreasureRoom>();
		List<GuardRoom> guardRooms = new ArrayList<GuardRoom>();
		List<DeadEnd> deadEnds = new ArrayList<DeadEnd>();
		for(CompositePattern p : map.getPatternFinder().getMesoPatterns()){
			if (p instanceof GuardRoom)
				guardRooms.add((GuardRoom)p);
			else if (p instanceof TreasureRoom)
				treasureRooms.add((TreasureRoom)p);
			else if (p instanceof DeadEnd)
				deadEnds.add((DeadEnd)p);
		}
		
		List<Room> treasureRoomRooms = treasureRooms.stream().map(tr->{return (Room)tr.getPatterns().get(0);}).collect(Collectors.toList());
		List<Room> guardRoomRooms = guardRooms.stream().map(gr->{return (Room)gr.getPatterns().get(0);}).collect(Collectors.toList());
		
		//For each dead end, see if it contains both treasure rooms and guard rooms
		for(DeadEnd de : deadEnds){
			List<Room> deTreasure = new ArrayList<Room>();
			List<Room> deGuard = new ArrayList<Room>();
			for(Pattern p : de.getPatterns()){
				if(p instanceof Room && treasureRoomRooms.contains(p)){
					deTreasure.add((Room)p);
				}
				else if(p instanceof Room && guardRoomRooms.contains(p)){
					deGuard.add((Room)p);
				}
			}
			
			if(deTreasure.size() > 0 && deGuard.size() > 0){
				//If it does contain both, find the "exit" from the dead end (that is, a node with a neighbour not in the dead end).
				Node<Pattern> exit = findDeadEndExit(de, patternGraph);
				
				//If there is a path from each treasure room to the exit that does not pass through a guard room, that treasure room is a not guarded treasure
				//Otherwise, it is.
				for(Room r : deTreasure){
					patternGraph.resetGraph();
					
					boolean foundPath = false;
					Queue<Node<Pattern>> queue = new LinkedList<Node<Pattern>>();
					queue.add(patternGraph.getNode(r));
					
					while(!queue.isEmpty()){
						Node<Pattern> current = queue.remove();
						current.tryVisit();
						if(current == exit){
							foundPath = true;
							break;
						}
						
						for(Edge<Pattern> e : current.getEdges()){
							Node<Pattern> n = getOtherNode(e,current);
							if(!n.isVisited() && !deGuard.contains(n.getValue())){
								queue.add(n);
								n.tryVisit();
							}
						}	
					}
					
					if(!foundPath){
						//We have a guarded treasure! Do something!
						//...
						GuardedTreasure gt = new GuardedTreasure();
						gt.getPatterns().add(r);
						gt.getPatterns().add(de);
						guardedTreasures.add(gt);
					}
					
				}
				
				
			}
			
		}
		
		if(guardedTreasures.size() > 0) System.out.println("Found " + guardedTreasures.size() + " guardedTreasure.");
		return guardedTreasures;
	}
	
	private static Node<Pattern> findDeadEndExit(DeadEnd de, Graph<Pattern> patternGraph){
		Node<Pattern> start = patternGraph.getNode(de.getPatterns().get(0));
		
		patternGraph.resetGraph();
		Queue<Node<Pattern>> queue = new LinkedList<Node<Pattern>>();
		queue.add(start);
		
		while(!queue.isEmpty()){
			Node<Pattern> current = queue.remove();
			current.tryVisit();
			if(hasUndeadNeighbour(current,de,patternGraph)){
				return current;
			}
			
			for(Edge<Pattern> e : current.getEdges()){
				Node<Pattern> n = getOtherNode(e,current);
				if(!n.isVisited()){
					queue.add(n);
					n.tryVisit();
				}
			}	
		}
		return null;
	}
	
	private static boolean hasUndeadNeighbour(Node<Pattern> node, DeadEnd de, Graph<Pattern> patternGraph){
		for(Edge<Pattern> e : node.getEdges()){
			if(!de.getPatterns().contains(getOtherNode(e,node).getValue()))
				return true;
		}
		return false;
	}
	
	private static Node<Pattern> getOtherNode(Edge<Pattern> e, Node<Pattern> node){
		if (e.getNodeA() == node)
			return e.getNodeB();
		return e.getNodeA();
	}
	
}