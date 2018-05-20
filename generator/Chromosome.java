package levelGenerators.jnicho;

import core.game.GameDescription.SpriteData;

import core.game.StateObservation;

import tools.LevelMapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Chromosome implements  Comparable<Chromosome> {

    private ArrayList<String>[][] level;
    private double fitness;
    private double scoreDiff;
    private double[] constraints;

    private boolean calculated;

    private StateObservation stateObservation;

    public Chromosome (int width, int height) {
        this.level = new ArrayList[height][width];
        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++){
                this.level[y][x] = new ArrayList<String>();
            }
        }
        this.fitness = Double.MIN_VALUE;
        this.scoreDiff = 0;
        this.calculated = false;
        this.stateObservation = null;
        this.constraints = new double[6];
        Arrays.fill(constraints, 0.0);

    }

    public Chromosome clone(){
        Chromosome c = new Chromosome(level[0].length, level.length);
        for(int y = 0; y < level.length; y++){
            for(int x = 0; x < level[y].length; x++){
                c.level[y][x].addAll(level[y][x]);
            }
        }
        return c;
    }

    public void InitialiseConstructive() {

//        for(int i = 0; i < 50; i++){
//            this.mutate();
//        }
//
        String[] levelString = Constants.constructiveGen.generateLevel(Constants.gameDescription, null, level[0].length, level.length).split("\n");
        HashMap<Character, ArrayList<String>> charMap = Constants.constructiveGen.getLevelMapping();

        for (int y=0; y<levelString.length; y++) {
            for (int x=0; x<levelString[y].length(); x++) {
                if (levelString[y].charAt(x) != ' ') {
                    this.level[y][x].addAll(charMap.get(levelString[y].charAt(x)));
                }
            }
        }
        FixPlayer();
    }

    public ArrayList<Chromosome> crossOver(Chromosome c) {
        ArrayList<Chromosome> children = new ArrayList<>();
        children.add(new Chromosome(level[0].length, level.length));
        children.add(new Chromosome(level[0].length, level.length));

        //crossover point
        int pointY = Constants.random.nextInt(level.length);
        int pointX = Constants.random.nextInt(level[0].length);

        //swap the two chromosomes around this point
        for(int y = 0; y < level.length; y++){
            for(int x = 0; x < level[y].length; x++){
                if(y < pointY){
                    children.get(0).level[y][x].addAll(this.level[y][x]);
                    children.get(1).level[y][x].addAll(c.level[y][x]);
                }
                else if(y == pointY){
                    if(x <= pointX){
                        children.get(0).level[y][x].addAll(this.level[y][x]);
                        children.get(1).level[y][x].addAll(c.level[y][x]);
                    }
                    else{
                        children.get(0).level[y][x].addAll(c.level[y][x]);
                        children.get(1).level[y][x].addAll(this.level[y][x]);
                    }
                }
                else{
                    children.get(0).level[y][x].addAll(c.level[y][x]);
                    children.get(1).level[y][x].addAll(this.level[y][x]);
                }
            }
        }

        children.get(0).FixPlayer();
        children.get(1).FixPlayer();

        return children;
    }

    public void mutate() {

        ArrayList<SpriteData> allSprites = Constants.gameDescription.getAllSpriteData();

        for (int i=0; i<Constants.mutationAmount; i++) {
            int solidFrame = 0;
            if (Constants.gameAnalyzer.getSolidSprites().size() > 0) {
                solidFrame = 2;
            }

            int pointX = Constants.random.nextInt(level[0].length - solidFrame)+ solidFrame / 2;
            int pointY = Constants.random.nextInt(level.length - solidFrame) + solidFrame / 2;

            // Add a random new sprite
            if (Constants.random.nextDouble() < Constants.addSpriteProb) {
                String spriteName = allSprites.get(Constants.random.nextInt(allSprites.size())).name;
                level[pointY][pointX].clear();
                level[pointY][pointX].add(spriteName);
//                ArrayList<SpritePointData> freePositions = getFreePositions(new ArrayList<String>(Arrays.asList(new String[]{spriteName})));
//                int index = Constants.random.nextInt(freePositions.size());
//                level[freePositions.get(index).y][freePositions.get(index).x].add(spriteName);
            }

            //Clear a random position
            else {
                level[pointY][pointX].clear();
            }
        }
        calculated = false;
        stateObservation = null;
        FixPlayer();
    }

    public ArrayList<SpritePointData> getPositions(ArrayList<String> sprites){
        ArrayList<SpritePointData> positions = new ArrayList<>();

        for(int y = 0; y < level.length; y++){
            for(int x = 0; x < level[y].length; x++){
                ArrayList<String> tileSprites = level[y][x];
                for(String stype:tileSprites){
                    for(String s:sprites){
                        if(s.equals(stype)){
                            positions.add(new SpritePointData(stype, x, y));
                        }
                    }
                }
            }
        }

        return positions;
    }

    private ArrayList<SpritePointData> getFreePositions(ArrayList<String> sprites){
        ArrayList<SpritePointData> positions = new ArrayList<>();

        for(int y = 0; y < level.length; y++){
            for(int x = 0; x < level[y].length; x++){
                ArrayList<String> tileSprites = level[y][x];
                boolean found = false;
                for(String stype:tileSprites){
                    found = found || sprites.contains(stype);
                    found = found || Constants.gameAnalyzer.getSolidSprites().contains(stype);
                }

                if(!found){
                    positions.add(new SpritePointData("", x, y));
                }
            }
        }

        return positions;
    }

    private void FixPlayer() {
        ArrayList<SpriteData> avatar = Constants.gameDescription.getAvatar();
        ArrayList<String> avatarNames = new ArrayList<>();
        for (SpriteData a:avatar) {
            avatarNames.add(a.name);
        }

        ArrayList<SpritePointData> avatarPositions = getPositions(avatarNames);

        // if no avatar insert a new one
        if(avatarPositions.size() == 0){

            int solidFrame = 0;
            if (Constants.gameAnalyzer.getSolidSprites().size() > 0) {
                solidFrame = 2;
            }

            int pointX = Constants.random.nextInt(level[0].length - solidFrame)+ solidFrame / 2;
            int pointY = Constants.random.nextInt(level.length - solidFrame) + solidFrame / 2;

            level[pointY][pointX].clear();
            level[pointY][pointX].add(avatarNames.get(Constants.random.nextInt(avatarNames.size())));

//            ArrayList<SpritePointData> freePositions = getFreePositions(avatarNames);
//            int index = Constants.random.nextInt(freePositions.size());
//            level[freePositions.get(index).y][freePositions.get(index).x].
//                    add(avatarNames.get(Constants.random.nextInt(avatarNames.size())));
        }

        //if there is more than one avatar remove all of them except one
        else if(avatarPositions.size() > 1){
            int notDelete = Constants.random.nextInt(avatarPositions.size());
            int index = 0;
            for(SpritePointData point:avatarPositions){
                if(index != notDelete){
                    level[point.y][point.x].clear();
                }
                index += 1;
            }
        }
    }

    public String getLevelString(LevelMapping levelMapping) {
        String levelString = "";
        for (int y=0; y<level.length; y++) {
            for (int x=0; x<level[y].length; x++) {
                levelString += levelMapping.getCharacter(level[y][x]);
            }
            levelString += "\n";
        }
        levelString = levelString.substring(0, levelString.length() - 1);
        return levelString;
    }

    public LevelMapping getLevelMapping(){
        LevelMapping levelMapping = new LevelMapping(Constants.gameDescription);
        levelMapping.clearLevelMapping();
        char c = 'a';
        for(int y = 0; y < level.length; y++){
            for(int x = 0; x < level[y].length; x++){
                if(levelMapping.getCharacter(level[y][x]) == null){
                    levelMapping.addCharacterMapping(c, level[y][x]);
                    c += 1;
                }
            }
        }
        return levelMapping;
    }

    public StateObservation getStateObservation(){
        if(stateObservation != null){
            return stateObservation;
        }

        LevelMapping levelMapping = getLevelMapping();
        String levelString = getLevelString(levelMapping);

//        try {
        stateObservation = Constants.gameDescription.testLevel(levelString, levelMapping.getCharMapping());
//        } catch (Exception e) {
//            System.out.println(levelMapping.getCharMapping().toString());
//            System.out.println(levelString);

//            e.printStackTrace();
//            System.exit(0);
//            System.out.println("--------------------DEAD----------------------------");
//            stateObservation = null;
//        }

        return stateObservation;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    public void setFitness(double fitness, double[] constraints, double scoreDiff) {
        this.fitness = fitness;
        this.constraints = constraints;
        this.scoreDiff = scoreDiff;
    }

    public void updateScoreDiff(double[] scoreBounds) {

        double scoreDiffConstraint = 0.0;
        double range = scoreBounds[1] - scoreBounds[0];

        if (range != 0) {
            double result = scoreDiff / range;

            if (result < 0)
                scoreDiffConstraint = 0.0;
            else if (result > 1)
                scoreDiffConstraint = 1.0;
            else
                scoreDiffConstraint = result;
        }

        constraints[5] = scoreDiffConstraint;

        double temp = 0.0;

        for (int i = 0; i< constraints.length; i++)
            temp += constraints[i];

        temp /= constraints.length;

        fitness = temp;

    }

    public boolean isCalculated() {
        return calculated;
    }

    public void setCalculated(Boolean cal) {
        calculated = cal;
    }

    public ArrayList<String>[][] getLevel() {
        return level;
    }

    public double getFitness() {
        return fitness;
    }

    public double[] getConstraints() {
        return  constraints;
    }

    @Override
    public int compareTo(Chromosome o) {

//        double thisPass = 0;
//        double otherPass = 0;
//
//        for (int i=0; i<constraints.length-1; i++) {
//            //if (constraints[i] >= 0.5)
//                thisPass += constraints[i];
//        }
//
//        for (int i=0; i<o.constraints.length-1; i++) {
//            //if (o.constraints[i] >= 0.5)
//                otherPass += o.constraints[i];
//        }
//
//        if (thisPass >= otherPass) {
//            return -1;
//        }
//        else if (thisPass == otherPass) {
//            if (this.fitness > o.fitness)
//                return -1;
//            else if (this.fitness < o.fitness)
//                return 1;
//            else
//                return 0;
//        }
//        else
//            return 1;

        if (this.fitness >= o.fitness)
            return -1;
        else if (this.fitness < o.fitness)
            return 1;
        else
            return 0;
    }

    public class SpritePointData{
        public String name;
        public int x;
        public int y;

        public SpritePointData(String name, int x, int y){
            this.name = name;
            this.x = x;
            this.y = y;
        }
    }

}
