package org.ggp.base.player.gamer.statemachine.unity;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.ggp.base.player.gamer.exception.MetaGamingException;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.apps.player.detail.SimpleDetailPanel;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.player.gamer.statemachine.unity.MCTS.MCTS;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.ggp.base.player.gamer.exception.MoveSelectionException;

/**
 * SampleGamer is a simplified version of the StateMachineGamer, dropping some
 * advanced functionality so the example gamers can be presented concisely.
 * This class implements 7 of the 8 core functions that need to be implemented
 * for any gamer.
 *
 * If you want to quickly create a gamer of your own, extend this class and
 * add the last core function : public Move stateMachineSelectMove(long timeout)
 */

public class UnityGamer extends StateMachineGamer
{
    private MCTS mcts;
    public Map<Role, Integer> roleMap;
    public ReentrantReadWriteLock lock1= new ReentrantReadWriteLock(true);
    @Override
    public void stateMachineMetaGame(long timeout) {
        roleMap = getStateMachine().getRoleIndices();
        mcts = new MCTS(this, getRole(), lock1);
        long finishBy = timeout - 1000;
        mcts.start();
        // kCTS.blocked = true;
        // Sample gamers do no metagaming at the beginning of the match.
    }

    @Override
    public GdlConstant getRoleName(){
        String first = roleName.getValue();
        List<Role> roles = stateMachine.getRoles();
        if (first.equals("first")){
            other = roles.get(0);
            System.out.println("Sets roles");
            return roles.get(1).getName();
        } else {
            other = roles.get(1);
            System.out.println("Sets roles");
            return roles.get(0).getName();
        }
    }


    @Override
    public String getName() {
        return "Unity";
    }
    // This is the default State Machine,
    @Override
    public StateMachine getInitialStateMachine() {
        return new CachedStateMachine(new ProverStateMachine());
    }

    // This is the defaul Sample Panel
    @Override
    public DetailPanel getDetailPanel() {
        return new SimpleDetailPanel();
    }

    @Override
    public void stateMachineStop() {
        mcts.shutdown();
        try{
            mcts.join();
        } catch (Exception e){}
        mcts = null;
        // Sample gamers do no special cleanup when the match ends normally.
    }

    @Override
    public void stateMachineAbort() {
        mcts.shutdown();
        try{
            mcts.join();
        } catch (Exception e){}
        mcts = null;
        // Sample gamers do no special cleanup when the match ends abruptly.
    }

    @Override
    public void preview(Game g, long timeout) throws GamePreviewException {
        // Sample gamers do no game previewing.
    }


    public void addMove(){
        lock1.writeLock().lock();
        stateMachine.doPerMoveWork();
        try{
            List<GdlTerm> lastMoves = getMatch().getMostRecentMoves();
            if (lastMoves != null) {
                List<Move> moves = new ArrayList<Move>();
                for (GdlTerm sentence : lastMoves) {
                    moves.add(stateMachine.getMoveFromTerm(sentence));
                }

                currentState = stateMachine.getNextState(currentState, moves);
                mcts.newRoot = moves;

                getMatch().appendState(currentState.getContents());
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        lock1.writeLock().unlock();
    }


    @Override
    public GdlTerm selectMove(long timeout) throws MoveSelectionException {
        try {
            List<Move> move = stateMachineSelectMoves(timeout);
            System.out.println("Picking move; " + move.toString());
            currentState = stateMachine.getNextState(currentState, move);
            mcts.newRoot = move;

            getMatch().appendState(currentState.getContents());
            return move.get(roleMap.get(getRole())).getContents(); 
        }
        catch (Exception e) {
            System.out.println(e.toString());
            GamerLogger.logStackTrace("GamePlayer", e);
            throw new MoveSelectionException(e);
        }
    }


    //wrapping this because we don't care about interrupted exception
    private void sleep(int ms){
        try{
            Thread.sleep(ms);
        } catch(Exception e){}//don't care

    }

    private int[] depth = new int[1];
    int performDepthChargeFromMove(MachineState theState, Move myMove) {
        StateMachine theMachine = getStateMachine();
        try {
            MachineState randomState = theMachine.getRandomNextState(theState, getRole(), myMove);
            MachineState finalState = theMachine.performDepthCharge(randomState, depth);
            return theMachine.getGoal(finalState, getRole());
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public List<Move> getLegalMoves(Role role) throws MoveDefinitionException{
        if (role.equals(getRole())){
            return getStateMachine().getLegalMoves(getCurrentState(), getRole());
        } else if (role.equals(getOtherRole())) {
            return getStateMachine().getLegalMoves(getCurrentState(), getOtherRole());
        } else {
            return null;
        }
    }

    @Override
    public String getEvaluation(){
        return "";
    }

    public List<Move> stateMachineSelectMoves(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{

        StateMachine theMachine = getStateMachine();
        lock1.writeLock().lock();
        List<Move> li = mcts.selectMove();
        lock1.writeLock().unlock();
        return li;
    }
    public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{

        return null;
    }
}

