package org.ggp.base.player.gamer.statemachine.unity.MCTS;

import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;


import java.util.Map;
import java.util.ArrayList;
import java.util.List;


/*
 * A simple MCTS Thread that defines a Monte carlo search algorithm for
 * a tree made up of MCMove nodes.
 */ 
public final class MCTS extends Thread {
    private static final int threads = 4;
    private ExecutorService executor;
    private ReentrantReadWriteLock lock;
    private static boolean alive;
    protected StateMachineGamer gamer;
    protected StateMachine machine;
    protected MCMove root;
    public boolean silent;
    public List<Move> newRoot;
    boolean debug = false;

/**
 * Simple constructor
 *
 *
 * @param gamer The gamer using this search
 * @param lock A lock just to be safe
 * @param silent Set to false to make it silent
 */
    public MCTS(StateMachineGamer gamer, ReentrantReadWriteLock lock, boolean silent){
        this.silent = silent;
        this.gamer = gamer;
        machine = gamer.getStateMachine();
        root = new MCMove(null);
        newRoot = null;
        alive = true;
        this.lock = lock;
        executor  = Executors.newFixedThreadPool(threads);
    }

    @Override
    public void run(){
        //While we are alive we keep on searching
        while(alive){
            try {
                lock.writeLock().lock(); //Making sure the statemachine and tree are in sync
                if (newRoot != null){
                    applyMove(newRoot); //Move to our new root
                    newRoot = null;
                    if (debug){
                        printTree(); //Print our new tree
                    }
                }
                search(root, gamer.getCurrentState());
                lock.writeLock().unlock();
            } catch (Exception e){
                System.out.println("EXCEPTION: " + e.toString());
                e.printStackTrace();
                alive = false;
            }
        }
        MCMove.reset(); //Reset N
    }

    /**
     * A recursive MCTS search function that searches through MCM nodes.
     * It only expands one node each run when it hits a new leaf.
     * 
     * @param node The node we are searching
     * @param state The current state entering this node
     *
     * @return The simulated value of this node for each player from one simulation.
     */
    private List<Integer> search(MCMove node, MachineState state) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
        List<Integer> result;
        int expanded = 0; //We use this to increment size only when a node is added
        if(!node.equals(root)){ //If we aren't at the root we change states
            if(node.n() == 0){ //We only ever use the SM once for each state
                state = machine.getNextState(state, node.move); 
                node.state = state;
            } else {
                state = node.state;
            }
        }
        if(node.terminal || machine.isTerminal(state)){
            if(node.goals == null){
                node.goals = machine.getGoals(state); //Decreasing terminal calls
                node.terminal = true;
            }
            result = node.goals;
        } else if (node.n() == 0){
            node.expand(machine.getLegalJointMoves(state));
            expanded = node.children.size(); //We have added nodes to the tree
            result = playOut(state); //We do one playout
        } else {
            MCMove child = node.select();
            /* This is ugly but its more efficient than checking them all each time */
            int prev = child.size();
            result = search(child, state);
            node.size(child.size(), prev);
        }
        node.update(result, expanded);
        return result;
    }

    /**
     * Performs one depthcharge down to a terminal state
     *
     * @param state The state we start our charge from
     *
     * @return The results of the depth charge for each player
     */
    private List<Integer> playOut(MachineState state) {
        List<Future<List<Integer>>> list = new ArrayList<Future<List<Integer>>>();
        for (int i = 0; i < threads; i++){
            Callable<List<Integer>> worker = new Play(state, machine);
            Future<List<Integer>> future = executor.submit(worker);
            list.add(future);
        }
        int sum[] = new int[] {0,0};
        try {
            for (Future<List<Integer>> f : list){
                sum[0] += f.get().get(0);
                sum[1] += f.get().get(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        List<Integer> res =  new ArrayList<Integer>();
        res.add(sum[0]);
        res.add(sum[1]);
        return res;
    }

    public static class Play implements Callable<List<Integer>>{
        private MachineState state;
        private final StateMachine machine;
        public List<Integer> goals;

        Play(MachineState state, StateMachine machine){
            this.state = state;
            this.machine = machine;
        }

        @Override
        public List<Integer> call(){
            try {
                state = machine.performDepthCharge(state, new int[1]);
                return machine.getGoals(state);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        
    }


    /**
     * @return The best move at this point
     */
    public List<Move> selectMove() throws MoveDefinitionException {
        long best = 0;
        List<Move> bestMove = null;
        if (!silent){
            System.out.println("================================Available moves================================");
        }
        for (int i = 0; i < root.children.size(); i++){
            if (!silent){
                System.out.println(root.children.get(i));
            }
            if (root.children.get(i).n() > best){
                best = root.children.get(i).n();
                bestMove = root.children.get(i).move;
            }
        }
        if (!silent){
            System.out.println("Selecting: " + bestMove + " With " + best + " simulations");
        }
        return bestMove;
    }

    /**
     * Updates the root node to the given moves
     *
     * @param moves The moves that need to be made
     */
    public void applyMove(List<Move> moves)  {
        if (!silent){
            System.out.println("The applied move !: " + moves.toString());
        }
        for (int i = 0; i < root.children.size(); i++){
            if (moves.get(0).equals(root.children.get(i).move.get(0)) &&
                moves.get(1).equals(root.children.get(i).move.get(1))){

                root = root.children.get(i);
                MCMove.N = root.n();
                return;
            }
        }
        throw new IllegalStateException("A move was selected that was not one of the root node moves");
    }


    private void printTree(String indent, MCMove node){
        System.out.println(indent + node);
        for (MCMove move : node.children){
            printTree(indent + "    ", move);
        }
    }

    /**
     * Pretty prints the tree
     */
    public void printTree(){
        printTree("", root);
    }

    /**
     * @return the size of the tree
     */
    public long size(){
        return root.size();
    }


    /**
     * Breaks the searcher out of his loop
     */
    public void shutdown(){
        alive = false;
    }

}
