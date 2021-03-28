package game.narrative.NarrativeFinder;

import game.narrative.GrammarGraph;
import game.narrative.GrammarNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple Conflict pattern is formally described as:
 * SCp = {S, C, T} where S is the source that have a conflict, C is the conflict itself, and T is the target of the conflict
 * I am thinking, and actually S and T might not be positive if they are not
 */
public class SimpleConflictPattern extends CompositeNarrativePattern
{
    protected NarrativePattern source_pattern;
    protected NarrativePattern target_pattern;

    public SimpleConflictPattern(){super();}

    public void setSource(NarrativePattern sp) {source_pattern = sp;}
    public void setTarget(NarrativePattern tp) {target_pattern = tp;}

    public NarrativePattern getSource() {return source_pattern;}
    public NarrativePattern getTarget() {return target_pattern;}

    public boolean isSelfConflict()
    {
        // If source and target are equals we know this is a bidirectional connection to conflict, which means selfconflict
        return source_pattern.equals(target_pattern);
    }


    public static List<CompositeNarrativePattern> matches(GrammarGraph narrative_graph, List<NarrativePattern> currentPatterns, NarrativeStructPatternFinder finder)
    {
        ArrayList<CompositeNarrativePattern> results = new ArrayList<CompositeNarrativePattern>();

        /** So this one is a bit harder to identify
         *  1- First, lets go through each of there StructureNodePatterns encountered thus far
         *  2- Then we should iterate the connections of those nodes trying to find directions
         *  3- a basic concli
         */

        for(NarrativePattern np : currentPatterns)
        {
            if(np instanceof StructureNodePattern)
            {
                //Now we search for the connections
//                List<GrammarNode> to_me = new ArrayList<GrammarNode>();
//                List<GrammarNode> from_me = new ArrayList<GrammarNode>();
                HashMap<GrammarNode, NarrativePattern> to_me = new HashMap<GrammarNode, NarrativePattern>();
                HashMap<GrammarNode, NarrativePattern> from_me = new HashMap<GrammarNode, NarrativePattern>();
                List<GrammarNode> non_dir = new ArrayList<GrammarNode>();

                //Need to limit the non_dir connections!!!!

                //These are from me  FIXME: Am I checking here for non-directional???? I DONT THINK SO!
                for(Map.Entry<GrammarNode, Integer> keyValue : ((StructureNodePattern) np).connected_node.connections.entrySet()) //Get Target!
                {
                    NarrativePattern target_pat = finder.existNodeAsPattern(keyValue.getKey());
                    //Limited to only add if it is hero or villain
                    if(target_pat!= null && (target_pat instanceof HeroNodePattern || target_pat instanceof VillainNodePattern))
                    {
//                        from_me.add(keyValue.getKey());
                        from_me.put(keyValue.getKey(), target_pat);
                    }
                }

                //These are to me (only directional!)
                for(GrammarNode node : narrative_graph.getAllConnectionsToNode(((StructureNodePattern) np).connected_node, true)) //Get Source
                {
                    NarrativePattern source_pat = finder.existNodeAsPattern(node);
                    //Only add if it is hero or villain
                    if(source_pat!= null && (source_pat instanceof HeroNodePattern || source_pat instanceof VillainNodePattern))
                    {
//                        to_me.add(node);
                        to_me.put(node, source_pat);
                    }
//                    to_me.add(node);
                }

                //If there is no source or no targets we do not have SimpleConflicts!
                if(from_me.isEmpty() || to_me.isEmpty())
                    continue;

                //Start filling the compound conflict!
                CompoundConflictPattern ccp = new CompoundConflictPattern();
                GrammarGraph combined_graph = new GrammarGraph();
                combined_graph.addNode(((StructureNodePattern) np).connected_node, false);

                //Now lets create a subgraph! and create the simple conflicts!
                for(Map.Entry<GrammarNode, NarrativePattern> source : to_me.entrySet()) //Get Target!
                {
//                for(GrammarNode source_node : to_me)
//                {
                    //Add source nodes to the compound graph!
                    combined_graph.addNode(source.getKey(), false);

                    for(Map.Entry<GrammarNode, NarrativePattern> target : from_me.entrySet()) //Get Target!
                    {
//                    for(GrammarNode target_node : from_me)
//                    {
                        GrammarGraph temp = new GrammarGraph();
                        temp.addNode(source.getKey(), false);
                        temp.addNode(((StructureNodePattern) np).connected_node, false);
                        temp.addNode(target.getKey(), false);

                        SimpleConflictPattern scp = new SimpleConflictPattern();
                        scp.addSubgraph(temp);

                        // Now we add the actual patterns not only the subgraph!
                        scp.addNarrativePattern(source.getValue());
                        scp.setSource(source.getValue());
                        scp.addNarrativePattern(np);
                        scp.addNarrativePattern(target.getValue());
                        scp.setTarget(target.getValue());

//                        scp.pattern_subgraphs.add(temp);
//                        scp.relevant_nodes.add(source_node);

                        results.add(scp);
                        ccp.addNarrativePattern(scp); // Fill the compound conflict pattern with the simple ones!
                    }
                }

                for(Map.Entry<GrammarNode, NarrativePattern> target : from_me.entrySet()) //Get Target!
                {
                    //Add target nodes to the compound graph!!
                    combined_graph.addNode(target.getKey(), false);
                }


                ccp.addSubgraph(combined_graph);
//                ccp.pattern_subgraphs.add(combined_graph);
                results.add(ccp);


            }
        }
        return results;
    }

    /**
     * Returns a measure of the quality of this pattern
     *
     * @return A number between 0.0 and 1.0 representing the quality of the pattern (where 1 is best)
     */
    public double getQuality(){
        return quality;
    }
}