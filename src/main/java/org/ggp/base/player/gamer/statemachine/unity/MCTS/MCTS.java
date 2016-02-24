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


import java.util.Map;
import java.util.ArrayList;
import java.util.List;


public final class MCTS extends Thread {
    boolean debug = false;
    public boolean silent;
    public MCMove root;
    public List<Move> newRoot;
    public static boolean alive;
    private StateMachineGamer gamer;
    private Map<Role, Integer> roleMap;
    private List<Role> roles;
    public ReentrantReadWriteLock lock1;

    public MCTS(StateMachineGamer gamer, Role starts, ReentrantReadWriteLock lock1, boolean silent){
        this.silent = silent;
        this.gamer = gamer;
        root = new MCMove(null);
        newRoot = null;
        alive = true;
        roles = gamer.getStateMachine().getRoles();
        this.lock1 = lock1;
    }

    @Override
    public void run(){
        Role role = gamer.getRole();
        while(alive){
            StateMachine machine = gamer.getStateMachine();
            try {
                lock1.writeLock().lock();
                if (newRoot != null){
                    selectMove(newRoot);
                    newRoot = null;
                    if (debug){
                        printTree();
                    }
                }
                search(root, machine, gamer.getCurrentState());
                lock1.writeLock().unlock();
            } catch (Exception e){
                System.out.println("EXCEPTION: " + e.toString());
                e.printStackTrace();
                alive = false;
            }
        }
        MCMove.reset();
    }

    private List<Integer> search(MCMove node, StateMachine machine, MachineState state) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
        List<Integer> result;
        MCMove child;
        if(!node.equals(root)){
            if(node.n() == 0){
                state = nextState(machine, state, node);
                node.state = state;
            } else {
                state = node.state;
            }
        }
        if(node.terminal || machine.isTerminal(state)){
            if(node.goals == null){
                node.goals = machine.getGoals(state);
                node.terminal = true;
            }
            result = node.goals;
        } else if (node.n() == 0){
            Role inControl = getControl(machine, state);
            node.expand(machine.getLegalJointMoves(state));
            result = playOut(state, machine, inControl);
        } else {
            child = node.select();
            result = search(child, machine,  state);
        }
        node.update(result);
        return result;
    }

    private List<Integer> playOut(MachineState theState, StateMachine machine, Role role) {
        try {
            //I can't think of a reason to keep the depth at the moment.
            MachineState finalState = machine.performDepthCharge(theState, new int[1]);
            // System.out.println("Getting a goal value from playout");
            return machine.getGoals(finalState);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Move> selectMove() throws MoveDefinitionException {
        long best = 0;
        int index = 0;
        List<Move> bestMove = null;
        int i;
        if (!silent){
            System.out.println("================Available moves================");
        }
        for (i = 0; i < root.children.size(); i++){

            if (!silent){
                System.out.println(root.children.get(i));
            }
            if (root.children.get(i).n() > best){
                best = root.children.get(i).n();
                bestMove = root.children.get(i).move;
                index = i;
            }
        }
        if (!silent){
            System.out.println("Selecting: " + bestMove + " With " + best + " simulations");
        }
        return bestMove;
    }

    public void selectMove(List<Move> moves)  {
        if (!silent){
            System.out.println("Opponent picked !: " + moves.toString());
        }
        for (int i = 0; i < root.children.size(); i++){
            if (moves.get(0).equals(root.children.get(i).move.get(0)) && moves.get(1).equals(root.children.get(i).move.get(1))){
                // System.out.println("---------------");
                // System.out.println("FOUND Opponent picked: " + root.children.get(i));
                root = root.children.get(i);
                return;
            }
        }
            System.out.println("WAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAT");
    }


    //TODO: Move all these exceptions around so that they just break things when they get thrown
    private MachineState nextState(StateMachine machine, MachineState state,MCMove node) throws MoveDefinitionException, TransitionDefinitionException{
        return machine.getNextState(state, node.move);
    }

    private Role getControl(StateMachine machine, MachineState state) throws MoveDefinitionException{
        List<Move> move = machine.getLegalJointMoves(state).get(0);
        if(move.get(0).equals(Move.create("noop"))){
            return roles.get(1);
        } else {
            return roles.get(0);
        }
    }

    public void printTree(){
        printTree("", root);
    }

    public void printTree(String indent, MCMove node){
        System.out.println(indent + node);
        for (MCMove move : node.children){
            printTree(indent + "    ", move);
        }
    }


    public void shutdown(){
        alive = false;
    }

}
