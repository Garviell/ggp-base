package org.ggp.base.player.gamer.statemachine.unity.MCTS;
import java.util.ArrayList;
import java.lang.Math;
import java.util.Collections;
import java.util.List;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.MachineState;


/**
 * A basic Monte Carlo move wrapper with UCT
 */
public class MCMove {
    final public List<Move> move; //The move that lead to this state
    public MachineState state; //The move that lead to this state
    private final static double C = 20; //Exploration constant
    public boolean terminal;
    public List<Integer> goals = null;
    private static long N = 0;
    private long[] wins;
    private long n; //how often this node has been selected
    public ArrayList<MCMove> children; //Its children because statemachines are slow

    /*
     * This is basically a state node.  I might actually add the state itself to it
     * to sacrifice memory for performance if i think its worth it.
     * @param move The move itself.
     */
    public MCMove(List<Move> move){
        terminal = false;
        this.move = move;
        wins = new long[] {0, 0};
        n = 0;
        children = new ArrayList<MCMove>();
    }

    public static void reset(){
        N = 0;
    }

    public long naiveValue(int who){
        return wins[who]/n;
        
    }


    /*
     * @return the number of wins this move has
     */
    public long w(int who){
        return wins[who];
    }

    /*
     * @return the number of simulations this move has
     */
    public long n(){
        return n;
    }


    /*
     * Takes in the result and updates, wins and simulation counts before calling
     * calcValue() to calculate the new value of the move.
     *
     * @param result The value we will use to update this move
     */
    public void update(List<Integer> result){
        wins[0] += result.get(0);
        wins[1] += result.get(1);
        n += 1;
        MCMove.N += 1;
    }

    /*
     * @return The new UCT value of the move
     */
    public double calcValue(int win){
        if(n == 0){
            return 1000;
        }
         return (wins[win] / n) + (C * Math.sqrt(Math.log(N)/n));
    }

    /*
     * Expands this node, adding the given legal moves to its children
     *
     * @param moves A list of the legal moves in this node
     */
    public void expand(List<List<Move>> moves){
        for (List<Move> move : moves){
            children.add(new MCMove(move));
        }
    }

    /*
     * Sorts the moves and returns the first one
     */
    //TODO:  Make this less god fucking awful
    public MCMove select(){
        double best = 0;
        double best2 = 0;
        MCMove bestMove = null;
        MCMove bestMove2 = null;
        for (MCMove move : children){
            double value = move.calcValue(0);
            double value2 = move.calcValue(1);
            if (value > best){
                best = value;
                bestMove = move;
            }
            if (value2 > best2){
                best2 = value2;
                bestMove2 = move;
            }
        }
        //Sorry...   I will rewrite this i promies
        for (MCMove move : children){
            if(bestMove.move.get(0).equals(move.move.get(0)) && bestMove2.move.get(1).equals(move.move.get(1))){
                bestMove = move;
            }
        }
        return bestMove;
    }



    @Override
    public String toString(){
        String result = "( ";
        result +=  "Move: " + ((move != null)? move.toString() : null) + " ";
        result += "N: " + N + " ";
        result += "n: " + this.n + " ";
        result += "value: (" +calcValue(0) + " " + calcValue(1) + " )";
        result += "wins[ "  +  wins[0] + " " + wins[1] + " ]";
        return  result;
    }
}
