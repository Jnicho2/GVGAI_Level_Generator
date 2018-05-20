package levelGenerators.jnicho;

import core.game.GameDescription;
import core.generator.AbstractLevelGenerator;
import tools.ElapsedCpuTimer;
import tools.GameAnalyzer;
import tools.LevelMapping;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

public class LevelGenerator extends AbstractLevelGenerator {

    private LevelMapping bestChromosomeLevelMapping;
    private LevelEvaluator levelEval;

    private ArrayList<Double> bestFitness;
    private double[] scoreBounds;


    public LevelGenerator(GameDescription game, ElapsedCpuTimer elapsedTimer) {
        Constants.random = new Random();
        Constants.gameDescription = game;
        Constants.gameAnalyzer = new GameAnalyzer(game);
        Constants.constructiveGen = new levelGenerators.constructiveLevelGenerator.LevelGenerator(game, null);
        bestChromosomeLevelMapping = null;
        bestFitness = null;
        scoreBounds = new double[2];
        scoreBounds[0] = Double.MAX_VALUE;
        scoreBounds[1] = Double.MIN_VALUE;


    }


    @Override
    public String generateLevel(GameDescription game, ElapsedCpuTimer elapsedTimer) {

        bestFitness = new ArrayList<Double>();
        Constants.gameDescription = game;
        ElapsedCpuTimer timer = new ElapsedCpuTimer();

        int size = 0;
        if (Constants.gameAnalyzer.getSolidSprites().size() > 0) {
            size = 2;
        }

        int width = (int) Math.max(Constants.minSize + size, game.getAllSpriteData().size() * (1 + 0.25 * Constants.random.nextDouble()) + size);
        int height = (int) Math.max(Constants.minSize + size, game.getAllSpriteData().size() * (1 + 0.25 * Constants.random.nextDouble()) + size);
        width = (int) Math.min(width, Constants.maxSize + size);
        height = (int) Math.min(height, Constants.maxSize + size);


        ArrayList<Chromosome> population = new ArrayList<>();

        while (population.size() < Constants.populationSize) {

            Chromosome chromosome = new Chromosome(width, height);
            chromosome.InitialiseConstructive();

            try {

                levelEval = new LevelEvaluator();
                levelEval.calculateFitness(chromosome, scoreBounds);
                scoreBounds = levelEval.getScoreBounds();

                population.add(chromosome);
            }
            catch (Exception e) {
            }
        }

        for (Chromosome c: population) {
            c.updateScoreDiff(scoreBounds);
        }

        Collections.sort(population);
        bestFitness.add(population.get(0).getFitness());


        double worstTime = Constants.evaluationTime * Constants.populationSize * 2;
        double avgTime = timer.elapsedMillis();
        double totalTime = avgTime;
        double numberOfIts = 1;

        String s = "";
        s += population.get(0).getFitness();
        for (int i=0; i<population.get(0).getConstraints().length; i++)
            s += ", "+population.get(0).getConstraints()[i];

//        bestChromosomeLevelMapping = population.get(0).getLevelMapping();
//        System.out.println(population.get(0).getLevelString(bestChromosomeLevelMapping));
//        System.out.println(bestChromosomeLevelMapping.getCharMapping());

        System.out.println("Generation #" + 1 +" Best Fitness:" + s +" Time left: " + elapsedTimer.remainingTimeMillis() + " " + avgTime + " " + worstTime);
        while (elapsedTimer.remainingTimeMillis() > (2 * avgTime)
                && elapsedTimer.remainingTimeMillis() > worstTime) {

            timer = new ElapsedCpuTimer();
            //System.out.println("Generation #" + (numberOfIts + 1) +" Best Fitness:" + population.get(0).getConstraints() + " Time left:" + elapsedTimer.remainingTimeMillis() + " " + avgTime + " " + worstTime);

            ArrayList<Chromosome> nextPopulation = getNextPopulation(population);
            population.clear();
            for (Chromosome c : nextPopulation) {
                population.add(c);
            }

            Collections.sort(population);
            bestFitness.add(population.get(0).getFitness());
            numberOfIts++;
            totalTime += timer.elapsedMillis();
            avgTime = totalTime / numberOfIts;

//            bestChromosomeLevelMapping = population.get(0).getLevelMapping();
//            System.out.println(population.get(0).getLevelString(bestChromosomeLevelMapping));
//            System.out.println(bestChromosomeLevelMapping.getCharMapping());

            s = "";
            s += population.get(0).getFitness();
            for (int i=0; i<population.get(0).getConstraints().length; i++)
                s += ", "+population.get(0).getConstraints()[i];

            System.out.println("Generation #" + numberOfIts +" Best Fitness:" + s + " Time left:" + elapsedTimer.remainingTimeMillis() + " " + avgTime + " " + worstTime);


        }

        Collections.sort(population);
        bestChromosomeLevelMapping = population.get(0).getLevelMapping();

        String result = population.get(0).getLevelString(bestChromosomeLevelMapping);
        result += "\n--------\n";
        result += bestChromosomeLevelMapping.getCharMapping();
        result += "\n--------\n";
        result += "Generations: "+numberOfIts+"\n";
        result += "Average Time: "+avgTime+"\n";
        result += "Best Fitness: "+population.get(0).getFitness()+"\n";
        for (int i=0; i<population.get(0).getConstraints().length; i++)
            result += population.get(0).getConstraints()[i]+"\n";
        result += "-------\n";
        for (int i=0; i<bestFitness.size(); i++) {
            result += bestFitness.get(i)+"\n";
        }

        PrintWriter p = null;
        try {
            p = new PrintWriter(new FileWriter(new File("data_"+game+".txt")));
        } catch (Exception e) {
        }

        p.write(result);
        p.close();


        return population.get(0).getLevelString(bestChromosomeLevelMapping);
    }

    private ArrayList<Chromosome> getNextPopulation(ArrayList<Chromosome> pop) {
        ArrayList<Chromosome> newPop = new ArrayList<>();

        while (newPop.size() < Constants.populationSize) {

            try {

                ArrayList<Chromosome> oldPop = pop;
                Chromosome parent1 = oldPop.get(Constants.random.nextInt(oldPop.size()));
                Chromosome parent2 = oldPop.get(Constants.random.nextInt(oldPop.size()));
                if (parent2 == parent1){
                    while (parent2 == parent1) {
                        parent2 = oldPop.get(Constants.random.nextInt(oldPop.size()));
                    }
                }
                Chromosome child1 = parent1.clone();
                Chromosome child2 = parent1.clone();

                if (Constants.random.nextDouble() < Constants.crossOverProb) {
                    ArrayList<Chromosome> children = parent1.crossOver(parent2);
                    child1 = children.get(0);
                    child2 = children.get(1);

                    if (Constants.random.nextDouble() < Constants.mutationProb)
                        child1.mutate();
                    if (Constants.random.nextDouble() < Constants.mutationProb)
                        child2.mutate();
                } else if (Constants.random.nextDouble() < Constants.mutationProb)
                    child1.mutate();
                else if (Constants.random.nextDouble() < Constants.mutationProb)
                    child2.mutate();



                levelEval = new LevelEvaluator();
                levelEval.calculateFitness(child1, scoreBounds);

                levelEval = new LevelEvaluator();
                levelEval.calculateFitness(child2, scoreBounds);

                newPop.add(child1);
                newPop.add(child2);

            } catch (Exception e) {
            }

        }

        for (Chromosome c: newPop) {
            c.updateScoreDiff(scoreBounds);
        }

        Collections.sort(newPop);
        for (int i = Constants.populationSize - Constants.elitism; i < newPop.size(); i++) {
            newPop.remove(i);
        }

        Collections.sort(pop);
        for (int i = 0; i < Constants.elitism; i++) {
            newPop.add(pop.get(i));
        }
        return newPop;
    }

    @Override
    public HashMap<Character, ArrayList<String>> getLevelMapping() {
        return bestChromosomeLevelMapping.getCharMapping();
    }
}
