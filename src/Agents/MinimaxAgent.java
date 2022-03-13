package Agents;

import pacman.controllers.Controller;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;

/**
 * Minimax agent class. Has a depth variable, that determines the depth of the
 * minimax tree, and an evaluation function.
 */
public class MinimaxAgent extends Controller<MOVE> {

    /**
     * tree depth
     */
    public int treeDepth;

    /**
     * constructor
     * @param d: depth
     */
    public MinimaxAgent(int d) {
        this.treeDepth = d;
    }

    /**
     * helper function for determining quality of certain layer's solution.
     * @param a: first
     * @param b: second
     * @param isGreater: do we check is larger or is smaller.
     * @return true if condition apply, false otherwise
     */
    public boolean compare(int a, int b, boolean isGreater) {
        return isGreater == (a > b);
    }

    /**
     * Evaluation function for states.
     * Takes into consideration: score, pills, powerPills, scaredGhosts, activeGhosts.
     * Weights for each variable is described in the bottom of the function.
     * @param state: the game state of which we determine the quality.
     * @return the evaluation score of that state.
     */
    public static Integer evaluationFunction(Game state) {
        if (state.gameOver()) {
            if (state.isPacmanDead())
                return Integer.MIN_VALUE;
            else
                return Integer.MAX_VALUE;
        }

        int currentScore = state.getScore();
        int powerPillsLeft = state.getAmountOfRemainingPowerPills();
        int pillsLeft = state.getAmountOfRemainingPills();

        int pacmanIndex = state.getPacmanPosition();


        ArrayList<Integer> distanceToFood = new ArrayList<>();
        for (int i : state.getRemainingPillsIndices()) {
            distanceToFood.add(state.getShortestPathDistance(pacmanIndex, i));
        }
        int closestFood = Collections.min(distanceToFood);


        ArrayList<Integer> distancesToScaredGhosts = new ArrayList<>();
        ArrayList<Integer> distancesToActiveGhosts = new ArrayList<>();
        for (GHOST g : state.getGhosts()) {
            int ghostIndex = state.getGhostCurrentNodeIndex(g);
            int d = state.getShortestPathDistance(pacmanIndex, ghostIndex);
            if (state.getGhostEdibleTime(g) > 0) {
                distancesToScaredGhosts.add(d);
            } else {
                distancesToActiveGhosts.add(d);
            }
        }

        int closestActiveGhost = Integer.MAX_VALUE, closestScaredGhost = Integer.MAX_VALUE;
        if (distancesToActiveGhosts.size() > 0) {
            closestActiveGhost = Collections.min(distancesToActiveGhosts);
        }

        if (distancesToScaredGhosts.size() > 0) {
            closestScaredGhost = Collections.min(distancesToScaredGhosts);
        }

        return (int) (currentScore +
                -1.5 * closestFood +
                -2 * (1/closestActiveGhost) +
                -2 * closestScaredGhost +
                -20 * powerPillsLeft +
                -4 * pillsLeft);
    }

    /**
     * Minimax evaluation function. Checks the best option to take from
     * a given state.
     * @param game: current game state.
     * @param agentIndex: the index of the agent for which we search the best course of action.
     *                  0 - pacman, 1< - ghost
     * @param depth: how far down the minimax tree we went.
     * @return the best pair of move and score from current position for current agent.
     */
    public MoveScorePair<MOVE, Integer> minimax(Game game, int agentIndex, int depth) {
        int numOfAgents = game.getGhosts().size() + 1;

        if (agentIndex == numOfAgents) {
            agentIndex = 0;
            depth--;
        }

        if (game.gameOver() || depth == 0) {
            return new MoveScorePair<>(null, evaluationFunction(game));
        }

        ArrayList<MoveScorePair<MOVE, Integer>> actionsValues = new ArrayList<>();
        MOVE[] moves;
        GHOST currentGhost = null;

        if (agentIndex == 0) {
            moves = game.getPossibleMoves(game.getPacmanPosition());
        } else {
            currentGhost = game.getGhosts().get(agentIndex - 1);
            moves = game.getPossibleMoves(game.getGhostCurrentNodeIndex(currentGhost));
        }

        for (MOVE m : moves) {
            Game state = game.copy();
            MOVE pacmanMove = MOVE.NEUTRAL;
            EnumMap<GHOST, MOVE> ghostMoves = new EnumMap<>(GHOST.class);
            for (GHOST g : game.getGhosts()) {
                ghostMoves.put(g, MOVE.NEUTRAL);
            }
            if (agentIndex == 0) {
                pacmanMove = m;
            } else {
                ghostMoves.put(currentGhost, m);
            }
            state.advanceGame(pacmanMove, ghostMoves);
            MoveScorePair<MOVE, Integer> pair = minimax(state, agentIndex + 1, depth);
            int value = pair.score;
            actionsValues.add(new MoveScorePair<>(m, value));
        }

        boolean isGreater = agentIndex == 0;

        if (actionsValues.size() == 0) {
            return new MoveScorePair<>(MOVE.LEFT, 0);
        }

        MoveScorePair<MOVE, Integer> best = actionsValues.get(0);
        for (MoveScorePair<MOVE, Integer> pair : actionsValues) {
            if (compare(pair.score, best.score, isGreater)) {
                best = pair;
            }
        }

        return best;
    }

    /**
     * get the move pacman needs to make from this position.
     * @param game A copy of the current game
     * @param timeDue The time the next move is due
     * @return the move to make.
     */
    @Override
    public MOVE getMove(Game game, long timeDue) {
        return minimax(game, 0, this.treeDepth).move;
    }

    public static class MoveScorePair<M, S> {
        public M move;
        public S score;

        public MoveScorePair(M m, S s) {
            this.move = m;
            this.score = s;
        }
    }
}
