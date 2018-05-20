package levelGenerators.jnicho;

import core.game.GameDescription;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import levelGenerators.constraints.*;
import ontology.Types;
import tools.ElapsedCpuTimer;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class LevelEvaluator {

    private AbstractPlayer goodAgent;
    private AbstractPlayer badAgent;
    private AbstractPlayer doNothingAgent;
    private StateObservation stateOb;
    private ArrayList<String>[][] level;
    private double[] scoreBounds;

    public LevelEvaluator() {
    }

    public void calculateFitness(Chromosome chromosome, double[] sB) {


        if (!chromosome.isCalculated()) {
            chromosome.setCalculated(true);

            this.scoreBounds = sB;

            double fitness = 0;
            double constraints[] = new double[6];
            Arrays.fill(constraints, 0);

            stateOb = chromosome.getStateObservation().copy();
            constructAgent(stateOb);

            level = chromosome.getLevel();

            long time = Constants.evaluationTime;
            ElapsedCpuTimer elapsedTimer = new ElapsedCpuTimer();

            //Play the game using the "good" agent
            ArrayList<Object> bestGame = playLevel(goodAgent, stateOb.copy(), time);
            StateObservation bestState = (StateObservation) bestGame.get(0);
            ArrayList<Types.ACTIONS> bestSol = (ArrayList) bestGame.get(1);


            //Play the game using the "bad" agent
            ArrayList<Object> worstGame = playLevel(badAgent, stateOb.copy(), time);
            StateObservation worstState = (StateObservation) worstGame.get(0);
            ArrayList<Types.ACTIONS> worstSol = (ArrayList) worstGame.get(1);


//                StepController stepAgent2 = new StepController(badAgent, Constants.evaluationStepTime);
//                elapsedTimer.setMaxTimeMillis(time);
//                stepAgent2.playGame(stateOb.copy(), elapsedTimer);
//                StateObservation worstState = stepAgent2.getFinalState();
//                ArrayList<Types.ACTIONS> worstSol = stepAgent2.getSolution();

            //Play the game doing nothing
            StateObservation doNothingState = null;
            int doNothingLength = Integer.MAX_VALUE;
            int bestSolGameLength = bestSol.size() * (int) Constants.evaluationStepTime;
            for (int i = 0; i < Constants.repetitionAmount; i++) {
                StateObservation tempState = stateOb.copy();
                int temp = getbadPlayerResult(tempState, bestSolGameLength, doNothingAgent);
                if (temp < doNothingLength) {
                    doNothingLength = temp;
                    doNothingState = tempState;
                }
            }


            double bestScore = bestState.getGameScore();
            double worstScore = worstState.getGameScore();

            if (bestScore > scoreBounds[1])
                scoreBounds[1] = bestScore;
            if (worstScore < scoreBounds[0])
                scoreBounds[0] = worstScore;


            //Avatar Constraint - Check only one avatar sprite
            //Goal Constraint - Check all termination conditions are not satisfied
            //Num Sprites - Checks there is at least one object for each non spawned sprite
            //Solution Length - Check that the best players play length in longer than a certain amount
            //Do Nothing - Check the do nothing agent doesn't win and doesn't die for a set number of steps

            double coverPerent = 0;
            double objects = 0;

            for (int y = 0; y < level.length; y++) {
                for (int x = 0; x < level[y].length; x++) {
                    objects += level[y][x].size();
                }
            }

            if(Constants.gameAnalyzer.getSolidSprites().size() > 0){
                objects -= ((level[0].length-1)*2) + ((level.length-1)*2);
            }

            coverPerent = objects / (level.length * level[0].length);

            constraints[0] = coverPercentConstraint(coverPerent);  //avatarConstraint();
            constraints[1] = goalConstraint();
            constraints[2] = numSpritesConstraint();
            constraints[3] = solutionLenConstraint(bestSol.size());
            constraints[4] = doNothingConstraint(doNothingState, doNothingLength);
            constraints[5] = 0.0;

            double scoreDiff = getScoreDiff(bestScore, worstScore);

//
//
            this.goodAgent = null;
            this.badAgent = null;
            this.stateOb = null;

//                fitness = constraints[5];

            for (int i = 0; i< constraints.length; i++)
                fitness += constraints[i];

            fitness /= constraints.length;

            chromosome.setFitness(fitness, constraints, scoreDiff);
        }
    }

    private void constructAgent(StateObservation so){
        try{
            Class agentClass = Class.forName(Constants.goodAgent);
            Constructor agentConst = agentClass.getConstructor(new Class[]{StateObservation.class, ElapsedCpuTimer.class});
            goodAgent = (AbstractPlayer)agentConst.newInstance(so.copy(), null);
        }
        catch(Exception e){
            e.printStackTrace();
        }

        try{
            Class agentClass = Class.forName(Constants.badAgent);
            Constructor agentConst = agentClass.getConstructor(new Class[]{StateObservation.class, ElapsedCpuTimer.class});
            badAgent = (AbstractPlayer)agentConst.newInstance(so.copy(), null);
        }
        catch(Exception e){
            e.printStackTrace();
        }

        try{
            Class agentClass = Class.forName(Constants.doNothingAgent);
            Constructor agentConst = agentClass.getConstructor(new Class[]{StateObservation.class, ElapsedCpuTimer.class});
            doNothingAgent = (AbstractPlayer)agentConst.newInstance(so.copy(), null);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    private ArrayList<Object> playLevel(AbstractPlayer agent, StateObservation so, long time) {
        ElapsedCpuTimer elapsedTimer = new ElapsedCpuTimer();
        ArrayList<Object> result = new ArrayList<>();
        constructAgent(so.copy());

        StepController stepAgent = new StepController(agent, Constants.evaluationStepTime);
        elapsedTimer.setMaxTimeMillis(time);
        stepAgent.playGame(so.copy(), elapsedTimer);

        result.add(stepAgent.getFinalState());
        result.add(stepAgent.getSolution());

        return result;
    }

    private int getbadPlayerResult (StateObservation stateObs, int steps, AbstractPlayer agent){
        int i =0;
        for(i=0;i<steps;i++){
            if(stateObs.isGameOver()){
                break;
            }
            Types.ACTIONS bestAction = agent.act(stateObs, null);
            stateObs.advance(bestAction);
        }
        return i;
    }

    private double avatarConstraint() {
        AvatarNumberConstraint con = new AvatarNumberConstraint();
        HashMap<String, Object> param = new HashMap<>();
        param.put("gameAnalyzer", Constants.gameAnalyzer);
        param.put("gameDescription", Constants.gameDescription);
        param.put("numOfObjects", calculateNumberOfObjects());
        con.setParameters(param);

        return con.checkConstraint();

//        if (con.checkConstraint() >= 0.5)
//            return con.checkConstraint();
//        else
//            return 0.0;

//        if (con.checkConstraint() == 1.0)
//            return true;
//        else
//            return false;
    }

    private double goalConstraint() {
        GoalConstraint con = new GoalConstraint();
        HashMap<String, Object> param = new HashMap<>();
        param.put("gameAnalyzer", Constants.gameAnalyzer);
        param.put("gameDescription", Constants.gameDescription);
        param.put("numOfObjects", calculateNumberOfObjects());
        con.setParameters(param);

        return con.checkConstraint();

//        if (con.checkConstraint() >= 0.5)
//            return con.checkConstraint();
//        else
//            return 0.0;
    }

    private double numSpritesConstraint() {
        SpriteNumberConstraint con = new SpriteNumberConstraint();
        HashMap<String, Object> param = new HashMap<>();
        param.put("gameAnalyzer", Constants.gameAnalyzer);
        param.put("gameDescription", Constants.gameDescription);
        param.put("numOfObjects", calculateNumberOfObjects());
        con.setParameters(param);

        return con.checkConstraint();

//        if (con.checkConstraint() >= 0.5)
//            return con.checkConstraint();
//        else
//            return 0.0;
    }

    private double solutionLenConstraint(int length) {
        SolutionLengthConstraint con = new SolutionLengthConstraint();
        HashMap<String, Object> param = new HashMap<>();
        param.put("gameAnalyzer", Constants.gameAnalyzer);
        param.put("gameDescription", Constants.gameDescription);
        param.put("solutionLength", length * Constants.evaluationStepTime);
        param.put("minSolutionLength", Constants.minGameLength);
        con.setParameters(param);

        return con.checkConstraint();

//        if (con.checkConstraint() >= 0.5)
//            return con.checkConstraint();
//        else
//            return 0.0;
    }

    private double doNothingConstraint(StateObservation doNothingState, double doNothingLength) {
        DeathConstraint con = new DeathConstraint();
        HashMap<String, Object> param = new HashMap<>();
        param.put("gameAnalyzer", Constants.gameAnalyzer);
        param.put("gameDescription", Constants.gameDescription);
        param.put("doNothingSteps", doNothingLength);
        param.put("doNothingState", doNothingState.getGameWinner());
        param.put("minDoNothingSteps", Constants.minNothingSteps);
        con.setParameters(param);

        return con.checkConstraint();

//        if (con.checkConstraint() >= 0.5)
//            return con.checkConstraint();
//        else
//            return 0.0;
    }

    private double coverPercentConstraint (double coverPercent) {

        CoverPercentageConstraint con = new CoverPercentageConstraint();
        HashMap<String, Object> param = new HashMap<>();
        param.put("gameAnalyzer", Constants.gameAnalyzer);
        param.put("gameDescription", Constants.gameDescription);
        param.put("coverPercentage", coverPercent);
        param.put("minCoverPercentage", Constants.minCoverPercent);
        param.put("maxCoverPercentage", Constants.maxCoverPercent);
        con.setParameters(param);

        return con.checkConstraint();

    }

    private double getScoreDiff(double bestScore, double worstScore) {

//        if (bestScore <= 0)
//            return 0.0;
//
//        double scoreDiff = bestScore - worstScore;
//
//        double diffPercent = scoreDiff/bestScore;
//
//        if (diffPercent > 1 )
//            return 1;
//        else
//            return diffPercent;


        double scoreDiff = bestScore - worstScore;
        return scoreDiff;
//

//
//        double range = scoreBounds[1] - scoreBounds[0];
//
//        System.out.println(worstScore + " " +scoreBounds[0] +" " + " " + bestScore + " " + scoreBounds[1] + " " + range);
//
//
//        if (range == 0)
//            return 0.0;
//
//        double result = scoreDiff/range;
////        System.out.println(worstScore + " " + bestScore + " " + result);
//
//
//        if (range == 0)
//            return 0.0;
//
//
//        if (result < 0)
//            return 0.0;
//        else if (result > 1)
//            return 1.0;
//        else
//            return result;


    }

    public double[] getScoreBounds() {
        return scoreBounds;
    }

    private HashMap<String, Integer> calculateNumberOfObjects(){
        HashMap<String, Integer> objects = new HashMap<String, Integer>();
        ArrayList<GameDescription.SpriteData> allSprites = Constants.gameDescription.getAllSpriteData();


        //initialize the hashmap with all the sprite names
        for(GameDescription.SpriteData sprite:allSprites){
            objects.put(sprite.name, 0);
        }


        //modify the hashmap to reflect the number of objects found in this level
        for(int y = 0; y < level.length; y++){
            for(int x = 0; x < level[y].length; x++){
                ArrayList<String> sprites = level[y][x];
                for(String stype:sprites){
                    if(objects.containsKey(stype)){
                        objects.put(stype, objects.get(stype) + 1);
                    }
                    else{
                        objects.put(stype, 1);
                    }
                }
            }
        }

        return objects;
    }

}
