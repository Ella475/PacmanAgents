package Agents.MCTS;

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

import static Agents.MCTS.MctsConstants.*;


public class MctsAgent extends Controller<MOVE> {

    public static Controller<EnumMap<GHOST, MOVE>> ghosts = new StarterGhosts();
    public static int tree_depth = 0;

    @Override
    public MOVE getMove(Game game, long timeDue) {

        //Hunt edible ghosts if not far away
        for (int i = 0; i < GHOST.values().length; i++) {
            int pacman = game.getPacmanPosition();
            int ghost = game.getGhostCurrentNodeIndex(GHOST.values()[i]);
            if (game.getShortestPathDistance(pacman, ghost) < hunt_dist
                && game.getGhostEdibleTime(GHOST.values()[i]) > 0) {
                return game.getNextMoveTowardsTarget(pacman, ghost, DM.PATH);
            }
        }

        // run Mcts when in a junction to get next move (next move is based on next junction)
        if (pacmanInJunction(game)) {
            tree_depth = 0;
            return SearchForMove(game);
        }

        // follow path until chosen junction is met.
        return keepFollowingPath(game.getPacmanLastMoveMade(), game);
    }


    public MOVE keepFollowingPath(MOVE dir, Game state) {
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

    public MOVE SearchForMove(Game game) {

        //create root node with state0
        Node root = new Node(null, game);
        long start = new Date().getTime();

        while (new Date().getTime() < start + SEARCH_TIME_LIMIT && tree_depth <= TREE_LIMIT) {
            Node node = selection(root);
            if (node == null) return MOVE.DOWN;
            backpropagation(node, simulation(node));
        }

        Node bestChild = BestChild(root, 0);
        return bestChild==null ? new RandomPacMan().getMove(game, -1) : bestChild.action;
    }


    public Node selection(Node node) {

        if (node == null) return null;

        while (!node.isGameOver()) {
            if (!node.isFullyExpanded()) {
                return node.expend();
            }
            else {
                node = selection(BestChild(node, C));
                if (node == null) break;
            }
        }
        return node;
    }


    public float simulation(Node node) {
        // Check null
        if (node == null)
            return 0;

        // If died on the way to the junction
        if (node.reward == 0.0f)
            return 0;

        int steps = 0;
        Controller<MOVE> pacManController = new RandomPacMan();
        Controller<EnumMap<GHOST, MOVE>> ghostController = ghosts;

        Game state = node.game.copy();
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
            double uctValue = UCT(node, C);

            if (uctValue >= bestValue) {
                bestValue = uctValue;
                bestChild = node;
            }
        }
        return bestChild;
    }

    private double UCT(Node node, double C) {
        double uct = (node.reward / node.visitCount) +
                C * Math.sqrt(2 * Math.log(node.parent.visitCount) / node.visitCount);
        return (float) uct;
    }

    private void backpropagation(Node node, double reward) {
        while (node != null) {
            node.incrementVisitCount();
            node.reward += reward;
            node = node.parent;
        }
    }
}
