package generator.algorithm;

import game.narrative.GrammarGraph;
import game.narrative.GrammarNode;
import game.narrative.GrammarPattern;
import game.narrative.TVTropeType;
import generator.config.GeneratorConfig;
import util.Util;

import java.util.ArrayList;
import java.util.List;

public class GrammarGenotype
{
    private List<GrammarPattern> chromosome;
    private GeneratorConfig config;

    /**
     * Get chromosome
     *
     * @return Chromosome
     */
    public List<GrammarPattern> getChromosome(){
        return chromosome;
    }

    /**
     * Set chromosome
     *
     * @param chromosome Chromosome
     */
    public void setChromosome(List<GrammarPattern> chromosome){
        this.chromosome = chromosome;
    }

    /**
     * Get the length of the chromosome
     *
     * @return Chromosome length
     */
    public int getSizeChromosome(){
        return chromosome.size();
    }

    public GrammarGenotype()
    {
        this.chromosome = new ArrayList<GrammarPattern>();
        produceRndChromosome();
    }

    public GrammarGenotype(GrammarGenotype other)
    {
        this.chromosome = new ArrayList<GrammarPattern>();
        for(GrammarPattern genome : other.chromosome)
        {
            this.chromosome.add(new GrammarPattern(genome));
        }
    }

    public GrammarGenotype(List<GrammarPattern> chromosome)
    {
        this.chromosome = new ArrayList<GrammarPattern>();
        for(GrammarPattern genome : chromosome)
        {
            this.chromosome.add(new GrammarPattern(genome));
        }
    }

    private void produceRndChromosome()
    {
        int rule_count = Util.getNextInt(1, 4);

        for(int j = 0; j < rule_count; j++)
        {
            GrammarPattern rndRule = new GrammarPattern();
            GrammarGraph pattern = new GrammarGraph();

            int node_amount = Util.getNextInt(1, 4);

            //Create the nodes
            for(int i = 0; i < node_amount; i++)
            {
                //I add any but maybe i shouldn't; lets try!
                pattern.addNode(TVTropeType.ANY);
            }

            //Add random connections
            for(int i = 0; i < node_amount; i++)
                addConnection(pattern);

            rndRule.setPattern(pattern);

            //Now create the production
            GrammarGraph production1 = new GrammarGraph();
            node_amount = Util.getNextInt(1, 4);

            for(int i = 0; i < node_amount; i++)
            {
                //I add any but maybe i shouldn't; lets try!
                production1.addNode(TVTropeType.ANY);
            }

            //Add random connections
            for(int i = 0; i < node_amount; i++)
                addConnection(production1);

            rndRule.addProductionRule(production1);

            this.chromosome.add(rndRule);
        }

    }

    public void addNodeRndRule()
    {
        GrammarPattern rndRule = chromosome.get(Util.getNextInt(0, this.chromosome.size()));
        GrammarGraph pat = rndRule.pattern;
        pat.addNode(TVTropeType.ANY);

        rndRule.setPattern(pat);

//        rndRule.
    }

    public void removeNodeRndRule()
    {
        GrammarPattern rndRule = chromosome.get(Util.getNextInt(0, this.chromosome.size()));
        GrammarGraph pat = rndRule.pattern;

        if (pat.nodes.size() <= 1)
            return;

        pat.removeNode(pat.nodes.get(Util.getNextInt(0, pat.nodes.size())));

        rndRule.setPattern(pat);
    }

    //TODO: Not clear the reasoning behind this.
    //It can be to exchange the type of node or to exchange the connections.
    public void exchangeNodeRndRule()
    {
        GrammarPattern rndRule = chromosome.get(Util.getNextInt(0, this.chromosome.size()));
        GrammarGraph pat = rndRule.pattern;

        if(pat.nodes.size() >= 2)
        {

        }
//        pat.removeNode(pat.nodes.get(Util.getNextInt(0, pat.nodes.size())));

        rndRule.setPattern(pat);
    }

    public void changeNodeTypeRndRule()
    {
        GrammarPattern rndRule = chromosome.get(Util.getNextInt(0, this.chromosome.size()));
        GrammarGraph pat = rndRule.pattern;

        pat.nodes.get(Util.getNextInt(0, pat.nodes.size())).setGrammarNodeType(
                TVTropeType.values()[Util.getNextInt(0, TVTropeType.values().length)]);

        rndRule.setPattern(pat); //Perhaps not needed
    }

    public void addConnectionRndRule()
    {
        GrammarPattern rndRule = chromosome.get(Util.getNextInt(0, this.chromosome.size()));
        GrammarGraph pat = rndRule.pattern;

        if(pat.nodes.size() >= 2)
        {
            int first_index = Util.getNextInt(0, pat.nodes.size());
            int second_index = Util.getNextInt(0, pat.nodes.size());
            GrammarNode first = pat.nodes.get(first_index);

            while( second_index == first_index)
                second_index = Util.getNextInt(0, pat.nodes.size());

            GrammarNode second = pat.nodes.get(second_index);

            //Fixme: This still needs more testing!
            if(!first.checkConnectionExists(second))
            {
                first.addConnection(second, 0);
                second.addConnection(first, 0);
            }
        }

        rndRule.setPattern(pat);
    }

    public void removeConnectionRndRule()
    {
        GrammarPattern rndRule = chromosome.get(Util.getNextInt(0, this.chromosome.size()));
        GrammarGraph pat = rndRule.pattern;

        if(pat.nodes.size() >= 2)
        {
            int first_index = Util.getNextInt(0, pat.nodes.size());

            //FIXME: Not really testing if the connection was succesfully removed!
            GrammarNode first = pat.nodes.get(first_index);
            first.removeRndConnection();
        }

        rndRule.setPattern(pat);
    }

    public void changeConnectionRndRule()
    {
        //TODO: Change the type of connection; but do we want this?
    }

    /// TODO: THIS ONES AFFECT THE PRODUCTION RULE NOT THE PATTERN

    public void addNodeRndOutput()
    {
        GrammarPattern rndRule = chromosome.get(Util.getNextInt(0, this.chromosome.size()));
        rndRule.productionRules.get(Util.getNextInt(0, rndRule.productionRules.size())).addNode(TVTropeType.ANY);
//        GrammarGraph rndOutput = rndRule.productionRules.remove(Util.getNextInt(0, this.chromosome.size()));
    }

    public void removeNodeRndOutput()
    {
        GrammarPattern rndRule = chromosome.get(Util.getNextInt(0, this.chromosome.size()));
        GrammarGraph rndOutput = rndRule.productionRules.get(Util.getNextInt(0, rndRule.productionRules.size()));

        if (rndOutput.nodes.size() <= 1)
            return;

        rndOutput.removeNode(rndOutput.nodes.get(Util.getNextInt(0, rndOutput.nodes.size())));
    }

    public void exchangeNodeRndOutput() {
    }

    public void changeNodeTypeRndOutput()
    {
        GrammarPattern rndRule = chromosome.get(Util.getNextInt(0, this.chromosome.size()));
        GrammarGraph rndOutput = rndRule.productionRules.get(Util.getNextInt(0, rndRule.productionRules.size()));

        //TODO: Not clear if this actually works!
        rndOutput.nodes.get(Util.getNextInt(0, rndOutput.nodes.size())).setGrammarNodeType(
                TVTropeType.values()[Util.getNextInt(0, TVTropeType.values().length)]);
    }

    public void addConnectionRndOutput()
    {
        GrammarPattern rndRule = chromosome.get(Util.getNextInt(0, this.chromosome.size()));
        GrammarGraph rndOutput = rndRule.productionRules.get(Util.getNextInt(0, rndRule.productionRules.size()));

        if(rndOutput.nodes.size() >= 2)
        {
            int first_index = Util.getNextInt(0, rndOutput.nodes.size());
            int second_index = Util.getNextInt(0, rndOutput.nodes.size());
            GrammarNode first = rndOutput.nodes.get(first_index);

            while( second_index == first_index)
                second_index = Util.getNextInt(0, rndOutput.nodes.size());

            GrammarNode second = rndOutput.nodes.get(second_index);

            //Fixme: This still needs more testing!
            if(!first.checkConnectionExists(second))
            {
                first.addConnection(second, 0);
                second.addConnection(first, 0);
            }
        }
    }

    public void removeConnectionRndOutput()
    {
        GrammarPattern rndRule = chromosome.get(Util.getNextInt(0, this.chromosome.size()));
        GrammarGraph rndOutput = rndRule.productionRules.get(Util.getNextInt(0, rndRule.productionRules.size()));

        if(rndOutput.nodes.size() >= 2)
        {
            int first_index = Util.getNextInt(0, rndOutput.nodes.size());

            //FIXME: Not really testing if the connection was succesfully removed!
            GrammarNode first = rndOutput.nodes.get(first_index);
            first.removeRndConnection();
        }
    }

    public void changeConnectionRndOutput()
    {

    }

    // THESE TWO ARE MORE HARDCORE
    public void createRule()
    {
        GrammarPattern rndRule = new GrammarPattern();
        GrammarGraph pattern = new GrammarGraph();

        int maxNodes = 2;

        //We basically are getting the max number of nodes that a pattern in this individual have.
        for(GrammarPattern current_patterns : this.chromosome)
        {
            if(current_patterns.pattern.nodes.size() > maxNodes)
                maxNodes = current_patterns.pattern.nodes.size();
        }

        maxNodes += 1;
        int node_amount = Util.getNextInt(1, maxNodes);

        //Create the nodes
        for(int i = 0; i < node_amount; i++)
        {
            //I add any but maybe i shouldn't; lets try!
            pattern.addNode(TVTropeType.ANY);
        }

        //Add random connections
        for(int i = 0; i < node_amount; i++)
            addConnection(pattern);

        rndRule.setPattern(pattern);

        //Now create the production
        GrammarGraph production1 = new GrammarGraph();
        node_amount = Util.getNextInt(1, maxNodes);

        for(int i = 0; i < node_amount; i++)
        {
            //I add any but maybe i shouldn't; lets try!
            production1.addNode(TVTropeType.ANY);
        }

        //Add random connections
        for(int i = 0; i < node_amount; i++)
            addConnection(production1);

        rndRule.addProductionRule(production1);
        this.chromosome.add(rndRule);

    }

    private GrammarGraph addConnection(GrammarGraph pat)
    {
        if(pat.nodes.size() >= 2)
        {
            int first_index = Util.getNextInt(0, pat.nodes.size());
            int second_index = Util.getNextInt(0, pat.nodes.size());
            GrammarNode first = pat.nodes.get(first_index);

            while( second_index == first_index)
                second_index = Util.getNextInt(0, pat.nodes.size());

            GrammarNode second = pat.nodes.get(second_index);

            //Fixme: This still needs more testing!
            if(!first.checkConnectionExists(second))
            {
                first.addConnection(second, 0);
                second.addConnection(first, 0);
            }
        }

        return pat;
    }

    public void createConnection() {
    }
}
