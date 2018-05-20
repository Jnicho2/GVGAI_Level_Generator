package levelGenerators.jnicho;

import core.game.GameDescription;
import tools.GameAnalyzer;
import java.util.Random;

public class Constants {

    public static final int populationSize = 30;
    public static final int elitism = 1;

    public static final double crossOverProb = 0.7;
    public static final double mutationProb = 0.2;

    public static final double minSize = 8;
    public static final double maxSize = 25;

    public static final int mutationAmount = 1;

    public static final double clearProb = 0.5;
    public static final double addSpriteProb = 0.5;

    public static final long evaluationTime = 500;
    public static final long evaluationStepTime = 40;
    public static final int repetitionAmount = 50;

    public static final double minGameLength = 300;
    public static final double minNothingSteps = 100;

    public static final double minCoverPercent = 0.1;
    public static final double maxCoverPercent = 0.3;

    public static Random random;
    public static GameDescription gameDescription;
    public static GameAnalyzer gameAnalyzer;
    public static levelGenerators.constructiveLevelGenerator.LevelGenerator constructiveGen;

    public static final String goodAgent = "controllers.singlePlayer.sampleOLMCTS.Agent";//
    public static final String badAgent = "controllers.singlePlayer.sampleMCTS.Agent";//"controllers.singlePlayer.sampleonesteplookahead.Agent"; ////"controllers.singlePlayer.sampleRandom.Agent";//
    public static final String doNothingAgent = "controllers.singlePlayer.doNothing.Agent";

}
