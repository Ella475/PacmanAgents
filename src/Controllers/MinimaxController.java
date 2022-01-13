package Controllers;

import java.util.*;

import pacman.controllers.Controller;
import pacman.controllers.examples.StarterGhosts;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;


public class MinimaxController extends Controller<MOVE> {

    public static Controller<EnumMap<GHOST, MOVE>> ghosts = new StarterGhosts();
    public int numOfAgents = 5;
    public int treeDepth;

    public MinimaxController(int d) {
        this.treeDepth = d;
    }

    public static class MoveScorePair<M, S> {
        public M move;
        public S score;
        public MoveScorePair(M m, S s) {
            this.move = m;
            this.score = s;
        }
    }

    public boolean compare(int a, int b, boolean isGreater) {
        return isGreater == (a > b);
    }

    public MoveScorePair<MOVE, Integer> minimax(Game game, int agentIndex, int depth) {

        if (agentIndex == numOfAgents) {
            agentIndex = 0;
            depth -= 1;
        }

        if (game.gameOver() || depth == 0) {
            return new MoveScorePair<>(null, game.getScore());
        }

        ArrayList<MoveScorePair<MOVE, Integer>> actionsValues = new ArrayList<>();
        MOVE[] moves;
        GHOST currentGhost = null;

        if (agentIndex == 0) {
            moves = game.getPossibleMoves(game.getPacmanCurrentNodeIndex());
        } else {
            currentGhost = game.getGhosts().get(agentIndex-1);
            moves = game.getPossibleMoves(game.getGhostCurrentNodeIndex(currentGhost));
        }

        for (MOVE m : moves) {
            Game state = game.copy();
            MOVE pacmanMove = MOVE.NEUTRAL;
            EnumMap<GHOST, MOVE> ghostMoves = new EnumMap<GHOST, MOVE>(GHOST.class);
            for (GHOST g : game.getGhosts()) {
                ghostMoves.put(g, MOVE.NEUTRAL);
            }
            if (agentIndex == 0) {
                pacmanMove = m;
            } else {
                ghostMoves.put(currentGhost, m);
            }
            state.advanceGame(pacmanMove, ghostMoves);

            MoveScorePair<MOVE, Integer> pair = minimax(state, agentIndex+1, depth);
            int value = pair.score;

            actionsValues.add(new MoveScorePair<>(m, value));
        }

        boolean isGreater = agentIndex == 0;

        if (actionsValues.size() == 0)
            return new MoveScorePair<>(MOVE.LEFT, 0);

        MoveScorePair<MOVE, Integer> best = actionsValues.get(0);
        for (MoveScorePair<MOVE, Integer> pair : actionsValues) {
            if (compare(pair.score, best.score, isGreater))
                best = pair;
        }

        return best;
    }


    @Override
    public MOVE getMove(Game game, long timeDue) {
        return minimax(game, 0, this.treeDepth).move;
    }
}
