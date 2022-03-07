package Agents;

import pacman.controllers.Controller;
import pacman.controllers.examples.RandomPacMan;
import pacman.controllers.examples.StarterGhosts;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;


public class MctsAgent extends Controller<MOVE> {

    //CONSTANTS
    public static final double C = 1.0f / Math.sqrt(2.0f);
    public static final int ghost_dist = 9;
    public static final int hunt_dist = 25;
    public static final int TREE_LIMIT = 35;
    public static final int SEARCH_TIME_LIMIT = 50;
    public static final int SIMULATION_STEPS = 30;
    //PROPERTIES
    public static Controller<EnumMap<GHOST, MOVE>> ghosts = new StarterGhosts();
    public static int tree_depth = 0;

    @Override
    public MOVE getMove(Game game, long timeDue) {

        //Hunt edible ghosts if not far away
        for (GHOST ghost : GHOST.values()) {
            if (game.getGhostEdibleTime(ghost) > 0) {
                if (game.getShortestPathDistance(game.getPacmanPosition(), game.getGhostCurrentNodeIndex(ghost)) < hunt_dist) {
                    return game.getNextMoveTowardsTarget(game.getPacmanPosition(), game.getGhostCurrentNodeIndex(ghost), DM.PATH);
                }
            }
        }

        // run Mcts when in a junction to get next move (next move is based on next junction)
        if (pacmanInJunction(game)) {
            tree_depth = 0;
            return MctsSearch(game);
        }

        // follow path until chosen junction is met.
        return FollowPath(game.getPacmanLastMoveMade(), game);
    }


    public MOVE FollowPath(MOVE dir, Game state) {
        MOVE[] possibleMoves = state.getPossibleMoves(state.getPacmanPosition());
        ArrayList<MOVE> moves = new ArrayList<>(Arrays.asList(possibleMoves));

        int current = state.getPacmanPosition();
        //EVADE GHOSTS DURING PATH
        ArrayList<MOVE> getAwayMove = new ArrayList<>();
        ArrayList<Integer> closeGhostDists = new ArrayList<>();
        for (GHOST ghost : GHOST.values()) {
            if (state.getGhostEdibleTime(ghost) == 0 && state.getGhostLairTime(ghost) == 0) {
                int ghostDist = state.getShortestPathDistance(current, state.getGhostCurrentNodeIndex(ghost));
                if (ghostDist < ghost_dist) {
                    getAwayMove.add(state.getNextMoveAwayFromTarget(current, state.getGhostCurrentNodeIndex(ghost),
                            DM.PATH));
                    closeGhostDists.add(ghostDist);
                }
            }
        }

        if (getAwayMove.size() > 0) {
            return getAwayMove.get(closeGhostDists.indexOf(Collections.min(closeGhostDists)));
        }

        if (moves.contains(dir)) {
            return dir;
        }
        moves.remove(state.getPacmanLastMoveMade().opposite());
        assert moves.size() == 1; // along a path there is only one possible way remaining
        return moves.get(0);
    }

    private boolean pacmanInJunction(Game game) {
        return game.isJunction(game.getPacmanPosition());
    }

    public MOVE MctsSearch(Game game) {

        //create root node with state0
        Node root = new Node(null, game);

        long start = new Date().getTime();

        while (new Date().getTime() < start + SEARCH_TIME_LIMIT && tree_depth <= TREE_LIMIT) {
            Node nd = SelectionPolicy(root);
            if (nd == null) {
                return MOVE.DOWN;
            }
            float reward = SimulationPolicy(nd);
            Backpropagation(nd, reward);
        }

        Node bestChild = BestChild(root, 0);

        if (bestChild == null) {
            return new RandomPacMan().getMove(game, -1);
        }

        return bestChild.action;
    }


    public Node SelectionPolicy(Node nd) {
        if (nd == null) {
            return null;
        }
        while (!nd.isGameOver()) {
            if (!nd.isFullyExpanded()) {
                return nd.expend();
            } else {
                nd = SelectionPolicy(BestChild(nd, C));
                if (nd == null) {
                    break;
                }
            }
        }
        return nd;
    }


    public float SimulationPolicy(Node nd) {
        // Check null
        if (nd == null)
            return 0;

        // If died on the way to the junction
        if (nd.reward == 0.0f)
            return 0;

        int steps = 0;
        Controller<MOVE> pacManController = new RandomPacMan();
        Controller<EnumMap<GHOST, MOVE>> ghostController = ghosts;

        Game state = nd.game.copy();
        int pillsBefore = state.getAmountOfRemainingPills();
        int livesBefore = state.getLivesRemaining();

        // simulate
        while (!state.gameOver()) {
            //advance game
            MOVE pacmanMove = pacManController.getMove(state, System.currentTimeMillis());
            EnumMap<GHOST, MOVE> ghostsMoves = ghostController.getMove(state, System.currentTimeMillis());
            state.advanceGame(pacmanMove, ghostsMoves);
            steps++;

            if (steps >= SIMULATION_STEPS) {
                break;
            }
        }

        // DEATH CONDITION
        int livesAfter = state.getLivesRemaining();
        if (livesAfter < livesBefore) {
            return 0.0f;
        }

        // Maze level completed
        if (state.getAmountOfRemainingPills() == 0) {
            return 1.0f;
        }

        //reward based on pills eaten
        return 1.0f - ((float) state.getAmountOfRemainingPills() / ((float) pillsBefore));
    }

    public Node BestChild(Node nd, double C) {
        Node bestChild = null;
        double bestValue = -1.0f;

        for (int i = 0; i < nd.children.size(); i++) {

            Node node = nd.children.get(i);
            double uctValue = UCTvalue(node, C);

            if (uctValue >= bestValue) {
                bestValue = uctValue;
                bestChild = node;
            }
        }
        return bestChild;
    }

    private double UCTvalue(Node nd, double C) {
        return (float) ((nd.reward / nd.visitCount) + C * Math.sqrt(2 * Math.log(nd.parent.visitCount) / nd.visitCount));
    }

    private void Backpropagation(Node node, double reward) {
        while (node != null) {
            node.incrementVisitCount();
            node.reward += reward;
            node = node.parent;
        }
    }
}