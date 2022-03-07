package Agents;

import pacman.controllers.Controller;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Random;


public class Node {

    public Node parent;
    public Game game;
    public int visitCount = 0;
    public MOVE action = MOVE.UP;
    public double reward = -1.0f;
    public ArrayList<Node> children = new ArrayList<>();
    public ArrayList<MOVE> triedActions = new ArrayList<>();
    public ArrayList<MOVE> untriedActions = new ArrayList<>();

    public Node(Node parent, Game game) {
        this.parent = parent;
        this.game = game;
    }

    public void incrementVisitCount() {
        this.visitCount++;
    }

    public static int ghostDistAvg(Game state) {
        int numOfGhosts = GHOST.values().length;
        double[] ghostDistances = new double[numOfGhosts];

        for (int i = 0; i < numOfGhosts; i++) {
            GHOST g = GHOST.values()[i];
            ghostDistances[i] = state.getDistance(
                    state.getPacmanPosition(),
                    state.getGhostCurrentNodeIndex(g),
                    DM.PATH
            );
        }

        return ((int) Arrays.stream(ghostDistances).sum()) / numOfGhosts;
    }

    public Node expend() {

        MOVE nextMove = newMove(game);

        if (nextMove != game.getPacmanLastMoveMade().opposite()) {
            Node expandedChild = getNearestJunction(nextMove);
            expandedChild.action = nextMove;
            MctsAgent.tree_depth++;
            this.children.add(expandedChild);
            expandedChild.parent = this;
            return expandedChild;
        }

        return this;
    }

    public Node getNearestJunction(MOVE dir) {

        Game state = game.copy();
        Controller<EnumMap<GHOST, MOVE>> ghostController = MctsAgent.ghosts;

        int from = state.getPacmanPosition();
        int current = from;
        MOVE currentPacmanDir = dir;

        //Simulation reward variables
        int pillsBefore = state.getAmountOfRemainingPills();
        int capsulesBefore = state.getAmountOfRemainingPowerPills();
        int livesBefore = state.getLivesRemaining();

        // use current == from , so we skip the junction we are currently in
        while (!state.isJunction(current) || current == from) {

            //make pacman follow the path
            ArrayList<MOVE> moves = new ArrayList<>(
                    Arrays.asList(state.getPossibleMoves(state.getPacmanPosition()))
            );

            if (!moves.contains(currentPacmanDir)) {
                moves.remove(state.getPacmanLastMoveMade().opposite());
                assert moves.size() == 1; // along a path there is only one possible way remaining
                currentPacmanDir = moves.get(0);
            }

            //advance game state
            state.advanceGame(currentPacmanDir,
                    ghostController.getMove(state,
                            System.currentTimeMillis()));

            current = state.getPacmanPosition();
        }

        int livesAfter = state.getLivesRemaining();
        int pillsAfter = state.getAmountOfRemainingPills();
        int capsulesAfter = state.getAmountOfRemainingPowerPills();

        Node node = new Node(this, state);
        //dead during transition
        if (livesAfter < livesBefore
                || capsulesAfter < capsulesBefore && ghostDistAvg(state) > 100) {
            node.reward = 0.0f;
        }
        //alive but no pills eaten
        else if (pillsAfter == pillsBefore) {
            node.reward = 0.2f;
        }
        //pills eaten and alive
        else {
            node.reward = 1.0f;
        }

        return node;
    }

    public boolean isGameOver() {
        return game.isPacmanDead() || game.getRemainingPillsIndices().length == 0;
    }

    public MOVE generateRandomMove() {
        int randomIndex = new Random().nextInt(untriedActions.size());
        MOVE untriedMove = untriedActions.get(randomIndex);
        triedActions.add(untriedMove);
        return untriedMove;
    }

    //Pick randomly non-tried action
    public MOVE newMove(Game game) {

        untriedActions.clear();
        int pacman = game.getPacmanPosition();
        List<MOVE> possibleMoves = Arrays.asList(game.getPossibleMoves(pacman));
        List<MOVE> gameMoves = Arrays.asList(MOVE.UP, MOVE.RIGHT, MOVE.DOWN, MOVE.LEFT);

        for (MOVE move : gameMoves) {
            if (possibleMoves.contains(move) && !triedActions.contains(move)) {
                untriedActions.add(move);
            }
        }

        return generateRandomMove();
    }

    public boolean isFullyExpanded() {
        int pacman = game.getPacmanPosition();
        MOVE[] possibleMoves = game.getPossibleMoves(pacman);
        return possibleMoves.length == children.size() || possibleMoves.length == triedActions.size();
    }
}
