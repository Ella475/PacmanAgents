package Agents.MCTS;

/**
 * MCTS agent constants.
 */
public class MctsConstants {
    //CONSTANTS
    public static final double C = 1.0f / Math.sqrt(2.0f);
    public static final int ghost_dist = 9;
    public static final int hunt_dist = 25;
    public static final int TREE_LIMIT = 35;
    public static final int SEARCH_TIME_LIMIT = 50;
    public static final int SIMULATION_STEPS = 30;
}
