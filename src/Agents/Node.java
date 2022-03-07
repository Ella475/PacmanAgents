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

    public int junction;
    public int visitCount = 0;
    public Node parent;
    public ArrayList<Node> children = new ArrayList<>();
    public MOVE actionMove = MOVE.UP;
    public double deltaReward = -1.0f;
    public ArrayList<MOVE> triedMoves = new ArrayList<>();
    public ArrayList<MOVE> untriedMoves = new ArrayList<>();
    public Game game;

    public Node(Node parent, Game game, int junction) {
        this.parent = parent;
        this.junction = junction;
        this.game = game;
    }

    public static int ghostDistAvg(Game state) {
        int numOfGhosts = GHOST.values().length;
        double[] ghostDistances = new double[numOfGhosts];

        for (int i = 0; i < numOfGhosts; i++) {
            GHOST g = GHOST.values()[i];
            ghostDistances[i] = state.getDistance(
                    state.getPacmanCurrentNodeIndex(),
                    state.getGhostCurrentNodeIndex(g),
                    DM.PATH
            );
        }

        return ((int) Arrays.stream(ghostDistances).sum()) / numOfGhosts;
    }

    public Node Expand() {

        MOVE nextMove = untriedMove(game);

        if (nextMove != game.getPacmanLastMoveMade().opposite()) {
            Node expandedChild = GetClosestJunctionInDir(nextMove);
            expandedChild.actionMove = nextMove;
            MctsAgent.tree_depth++;
            this.children.add(expandedChild);
            expandedChild.parent = this;
            return expandedChild;
        }

        return this;
    }

    public Node GetClosestJunctionInDir(MOVE dir) {

        Game state = game.copy();
        Controller<EnumMap<GHOST, MOVE>> ghostController = MctsAgent.ghosts;

        int from = state.getPacmanCurrentNodeIndex();
        int current = from;
        MOVE currentPacmanDir = dir;

        //Simulation reward variables
        int pillsBefore = state.getNumberOfActivePills();
        int capsulesBefore = state.getNumberOfActivePowerPills();
        int livesBefore = state.getPacmanNumberOfLivesRemaining();
        float transition_reward;

        // use current == from , so we skip the junction we are currently in
        while (!state.isJunction(current) || current == from) {

            //make pacman follow the path
            currentPacmanDir = GetMoveToFollowPath(state, currentPacmanDir);

            //advance game state
            state.advanceGame(currentPacmanDir,
                    ghostController.getMove(state,
                            System.currentTimeMillis()));

            current = state.getPacmanCurrentNodeIndex();
        }

        int livesAfter = state.getPacmanNumberOfLivesRemaining();
        int pillsAfter = state.getNumberOfActivePills();
        int capsulesAfter = state.getNumberOfActivePowerPills();

        //dead during transition
        if (livesAfter < livesBefore) {
            transition_reward = 0.0f;
        } else if (capsulesAfter < capsulesBefore && ghostDistAvg(state) > 100) {
            transition_reward = 0.0f;
        }
        //alive but no pills eaten
        else if (pillsAfter == pillsBefore) {
            transition_reward = 0.2f;
        }
        //pills eaten and alive
        else {
            transition_reward = 1.0f;
        }

        //return the child node with updated state and junction number
        Node child = new Node(this, state, current);
        child.deltaReward = transition_reward;
        return child;
    }

    // Make pacman follow a path where only one move is possible (excluding reverse)
    public MOVE GetMoveToFollowPath(Game state, MOVE direction) {
        MOVE[] possibleMoves = state.getPossibleMoves(state.getPacmanCurrentNodeIndex());
        ArrayList<MOVE> moves = new ArrayList<>(Arrays.asList(possibleMoves));

        if (moves.contains(direction)) {
            return direction;
        }
        moves.remove(state.getPacmanLastMoveMade().opposite());
        assert moves.size() == 1; // along a path there is only one possible way remaining
        return moves.get(0);
	}

    public boolean isTerminalGameState() {
        return game.wasPacManEaten() || game.getActivePillsIndices().length == 0;
    }

    public void updateUntriedMoves(Game game) {
        untriedMoves.clear();
        int current_node = game.getPacmanCurrentNodeIndex();
        List<MOVE> possibleMoves = Arrays.asList(game.getPossibleMoves(current_node));
        List<MOVE> gameMoves = Arrays.asList(MOVE.UP, MOVE.RIGHT, MOVE.DOWN, MOVE.LEFT);

        for (MOVE move : gameMoves) {
            if (possibleMoves.contains(move) && !triedMoves.contains(move)) {
                untriedMoves.add(move);
            }
        }
    }

    //Pick randomly non-tried action
    public MOVE untriedMove(Game game) {
        updateUntriedMoves(game);
        MOVE untriedMove = untriedMoves.get(new Random().nextInt(untriedMoves.size()));
        triedMoves.add(untriedMove);
        return untriedMove;
    }

    public boolean isFullyExpanded() {
        if (children.size() == 0) {
            return false;
        }

        int current_node = game.getPacmanCurrentNodeIndex();
        MOVE[] possibleMoves = game.getPossibleMoves(current_node);
        return possibleMoves.length == children.size() || possibleMoves.length == triedMoves.size();
    }


}
			


