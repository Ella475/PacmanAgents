package Agents.MCTS;

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

/**
 * Node class of MCTS agent. node consists of variables that represent current state and relative score
 * of all simulations made through that state.
 */
public class Node {

    /**
     * Public variables of Node of MCTS agent
     */
    public Node parent;
    public Game game;
    public int visitCount = 0;
    public MOVE action = MOVE.UP;
    public double reward = -1.0f;
    public ArrayList<Node> children = new ArrayList<>();
    public ArrayList<MOVE> triedActions = new ArrayList<>();
    public ArrayList<MOVE> untriedActions = new ArrayList<>();

    /**
     * constructor function
     * @param parent parent node
     * @param game a copy of game state
     */
    public Node(Node parent, Game game) {
        this.parent = parent;
        this.game = game;
    }

    /**
     * increment the visit count.
     */
    public void incrementVisitCount() {
        this.visitCount++;
    }

    /**
     * checks average distance from all ghosts in game.
     * @param state a copy of game state
     * @return average distance (int)
     */
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

    /**
     * function that expends this node.
     * @return an expended child, or otherwise the node itself, because we cant expend anymore.
     */
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

    /**
     * walks in 'dir' direction and checks what is the nearest junction pacman can get to.
     * @param dir the direction of pacman movement.
     * @return the node of the closest junction in this direction.
     */
    public Node getNearestJunction(MOVE dir) {

        Game state = game.copy();
        Controller<EnumMap<GHOST, MOVE>> ghostController = MctsAgent.ghosts;

        int from = state.getPacmanPosition();
        int current = from;
        MOVE currentPacmanDir = dir;

        //Simulation reward variables
        int prevPills = state.getAmountOfRemainingPills();
        int prevCapsules = state.getAmountOfRemainingPowerPills();
        int prevLives = state.getLivesRemaining();

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

        int currLives = state.getLivesRemaining();
        int currPills = state.getAmountOfRemainingPills();
        int currCapsules = state.getAmountOfRemainingPowerPills();
        Node node = new Node(this, state);

        //dead during transition
        if (currLives < prevLives || currCapsules < prevCapsules && ghostDistAvg(state) > 100) {
            node.reward = 0.0f;
        }
        //alive but no pills eaten
        else if (currPills == prevPills) {
            node.reward = 0.2f;
        }
        //pills eaten and alive
        else {
            node.reward = 1.0f;
        }

        return node;
    }

    /**
     * terminate function, check if game over.
     * @return boolean.
     */
    public boolean isGameOver() {
        return game.isPacmanDead() || game.getRemainingPillsIndices().length == 0;
    }

    public MOVE generateRandomMove() {
        int randomIndex = new Random().nextInt(untriedActions.size());
        MOVE untriedMove = untriedActions.get(randomIndex);
        triedActions.add(untriedMove);
        return untriedMove;
    }

    /**
     * pick a move from all untried moves yet.
     * @param game a copy of game state
     * @return a move.
     */
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

    /**
     * check if our node is fully expended or not.
     * @return boolean
     */
    public boolean isFullyExpanded() {
        int pacman = game.getPacmanPosition();
        MOVE[] possibleMoves = game.getPossibleMoves(pacman);
        return possibleMoves.length == children.size() || possibleMoves.length == triedActions.size();
    }
}
