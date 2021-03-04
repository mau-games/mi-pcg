package generator.algorithm;

import com.sun.org.apache.xerces.internal.xni.grammars.Grammar;
import game.Room;
import game.narrative.GrammarGraph;
import game.narrative.GrammarPattern;
import generator.algorithm.MAPElites.grammarDimensions.GADimensionGrammar;
import generator.config.GeneratorConfig;
import util.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

public class GrammarIndividual
{
    private int[] w_mutate = {5, 50, 50};
    private int[] w_mutate_options = {5, 5, 10, 20, 60};
    private int[] w_mutate_optionsRule = {5, 10, 15, 20, 20};
//    private float w_rule = 0.3f;
//    private float w_production = 0.3f;
//    private float

    private double fitness;
    protected HashMap<GADimensionGrammar.GrammarDimensionTypes, Double> dimensionValues; //This will change to specific dimensions!

    private GrammarGenotype genotype;
    private GrammarPhenotype phenotype;
    private boolean evaluate;
    private float mutationProbability;
    private GeneratorConfig config;

    private boolean childOfInfeasibles = false;

    public boolean isChildOfInfeasibles(){
        return childOfInfeasibles;
    }

    public void setChildOfInfeasibles(boolean cOI){
        childOfInfeasibles = cOI;
    }

    public GrammarIndividual()
    {
        this.genotype = new GrammarGenotype(); //Create its own.
    }

    public GrammarIndividual(float mutation_probability)
    {
        this.genotype = new GrammarGenotype(); //Create its own.
        this.mutationProbability = mutation_probability;
        this.config = null;
        this.phenotype = null;
        this.fitness = 0.0;
        this.evaluate = false;
    }

    public GrammarIndividual(GeneratorConfig config, GrammarGenotype genotype, float mutationProbability)
    {
        this.config = config;
        this.genotype = new GrammarGenotype(genotype);
        this.phenotype = null;
        this.fitness = 0.0;
        this.evaluate = false;
        this.mutationProbability = mutationProbability;
    }

    public GrammarIndividual(GeneratorConfig config, List<GrammarPattern> chromosome, float mutationProbability)
    {
        this.config = config;
        this.genotype = new GrammarGenotype(chromosome);
        this.phenotype = null;
        this.fitness = 0.0;
        this.evaluate = false;
        this.mutationProbability = mutationProbability;
    }

    /***
     * For future implementations, to use the designers creation as a seed.
     * @param initial_seed
     * @param mutationProbability
     */
//    public GrammarIndividual(GrammarGraph initial_seed, float mutationProbability)
//    {
////        config = room.getConfig();
//        genotype = new GrammarGenotype(config,initial_seed, room.getColCount() * room.getRowCount());
//        phenotype = null;
//        fitness = 0.0;
//        evaluate = false;
//        this.mutationProbability = mutationProbability;
//
//        genotype.ProduceGenotype(room);
//    }

    public void SetDimensionValues(ArrayList<GADimensionGrammar> dimensions, GrammarGraph axiom)
    {
        dimensionValues = new HashMap<GADimensionGrammar.GrammarDimensionTypes, Double>();

        //FIXME: target graph
        for(GADimensionGrammar dimension : dimensions)
        {
            dimensionValues.put(dimension.GetType(), dimension.CalculateValue(this, axiom));
        }
//
//        this.getPhenotype().getMap(-1, 1, null, null, null).SetDimensionValues(dimensionValues);
    }

    public double getDimensionValue(GADimensionGrammar.GrammarDimensionTypes currentDimension)
    {
        return dimensionValues.get(currentDimension);
    }

    private int get_mutation_by_prob(int ... probability_table)
    {
        int index=Util.getNextInt(0, IntStream.of(probability_table).sum());
        int sum = 0;
        int i = 0;

        while(sum < index)
        {
            sum += probability_table[i++];
        }

        return i-1;
    }

    public GrammarIndividual mutate(boolean test_mutation_prob)
    {
        //Check if mutate at all
        if(!test_mutation_prob || Util.getNextFloat(0.0f, 1.0f) > mutationProbability)
        {
            //For now just a random
            int select=Util.getNextInt(0, 6);
//            GrammarIndividual mutated_version = new GrammarIndividual(config, new ZoneGenotype(config, genotype.getChromosome().clone(), genotype.GetRootChromosome()), mutationProbability);
            GrammarIndividual mutated_version = new GrammarIndividual(null, this.getGenotype().getChromosome(), this.mutationProbability);

            switch(get_mutation_by_prob(w_mutate))
            {
                case 0:
                    mutated_version.genotype.createRule();
                    break;
                case 1:
                    switch(get_mutation_by_prob(w_mutate_optionsRule))
                    {
                        case 0: mutated_version.genotype.removeNodeRndRule(); break;
                        case 1: mutated_version.genotype.removeConnectionRndRule(); break;
                        case 2: mutated_version.genotype.addNodeRndRule(); break;
                        case 3: mutated_version.genotype.addConnectionRndRule(); break;
                        case 4: mutated_version.genotype.changeNodeTypeRndRule(); break;
                    }
                    break;
                case 2:
                    switch(get_mutation_by_prob(w_mutate_options))
                    {
                        case 0: mutated_version.genotype.addNodeRndOutput(); break;
                        case 1: mutated_version.genotype.removeNodeRndOutput(); break;
                        case 2: mutated_version.genotype.removeConnectionRndOutput(); break;
                        case 3: mutated_version.genotype.addConnectionRndOutput(); break;
                        case 4: mutated_version.genotype.changeNodeTypeRndOutput(); break;
                    }
                    break;
            }

//
//
//            //These are all for changing input patterns
//            switch(select)
//            {
//                case 0: mutated_version.genotype.addNodeRndRule(); break;
//                case 1: mutated_version.genotype.removeNodeRndRule(); break;
//                case 2: mutated_version.genotype.exchangeNodeRndRule(); break;
//                case 3: mutated_version.genotype.changeNodeTypeRndRule(); break;
//                case 4: mutated_version.genotype.addConnectionRndRule(); break;
//                case 5: mutated_version.genotype.removeConnectionRndRule(); break;
//                case 6: mutated_version.genotype.changeConnectionRndRule(); break;
//            }
//
//            //These are all for changing output patterns probably I can do something different!
//            switch(select)
//            {
//                case 0: mutated_version.genotype.addNodeRndOutput(); break;
//                case 1: mutated_version.genotype.removeNodeRndOutput(); break;
//                case 2: mutated_version.genotype.exchangeNodeRndOutput(); break;
//                case 3: mutated_version.genotype.changeNodeTypeRndOutput(); break;
//                case 4: mutated_version.genotype.addConnectionRndOutput(); break;
//                case 5: mutated_version.genotype.removeConnectionRndOutput(); break;
//                case 6: mutated_version.genotype.changeConnectionRndOutput(); break;
//            }
//
//            mutated_version.genotype.createRule();
////            mutated_version.genotype.createConnection();

            return mutated_version;

        }
        else
            return null;
    }

    public GrammarIndividual[] crossover(GrammarIndividual other)
    {
        return null;
    }

    /**
     * Get this individual's calculated fitness
     *
     * @return Fitness
     */
    public double getFitness(){
        return fitness;
    }

    /**
     * Set this individual's fitness
     *
     * @param fitness Fitness
     */
    public void setFitness(double fitness){
        this.fitness = fitness;
    }

    /**
     * Has the fitness of this Individual been evaluated yet?
     *
     * @return true if the fitness of this individual has already been evaluated
     */
    public boolean isEvaluated() {
        return evaluate;
    }

    /**
     * Set that this Individual has been evaluated.
     *
     * @param evaluate true if the fitness of this Individual has been evaluated
     */
    public void setEvaluate(boolean evaluate){
        this.evaluate = evaluate;
    }

    /**
     * Get genotype
     *
     * @return Genotype
     */
    public GrammarGenotype getGenotype(){
        return genotype;
    }

    /**
     * Get phenotype
     *
     * @return Phenotype
     */
    public GrammarPhenotype getPhenotype(){
        if(phenotype == null){
            phenotype = new GrammarPhenotype(genotype);
        }
        return phenotype;
    }

    /*
     * Update the config file and create once again the phenotype
     * This operation can be very costly!
     */
    public void ResetPhenotype(GeneratorConfig config)
    {
        this.config = config;
        phenotype = null;
    }

}