package org.ggp.base.player.gamer.statemachine.sample;

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
 * JeffGamer implements a simple MCTS search with UCT
 */

public class JeffGamer extends StateMachineGamer
{
    private MCTS mcts;
    private Role other;
    private Map<Role, Integer> roleMap;
    public ReentrantReadWriteLock lock1= new ReentrantReadWriteLock(true);
    @Override
    public void stateMachineMetaGame(long timeout) {

        roleMap = getStateMachine().getRoleIndices();
        mcts = new MCTS(this, lock1, false);
        long finishBy = timeout - 1100;
        mcts.start();
        while(System.currentTimeMillis() < finishBy){
            try{
                Thread.sleep(200);
            } catch(Exception e){}//don't care
        }
    }


    @Override
    public String getName() {
        return "Jeff";
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
        // Sample gamers do no special cleanup when the match ends abruptly.
    }

    @Override
    public void preview(Game g, long timeout) throws GamePreviewException {
        // Sample gamers do no game previewing.
    }


    @Override
    public GdlTerm selectMove(long timeout) throws MoveSelectionException {
        try
        {
            lock1.writeLock().lock();
            stateMachine.doPerMoveWork();

            List<GdlTerm> lastMoves = getMatch().getMostRecentMoves();
            if (lastMoves != null)
            {
                List<Move> moves = new ArrayList<Move>();
                for (GdlTerm sentence : lastMoves)
                {
                    moves.add(stateMachine.getMoveFromTerm(sentence));
                }

                currentState = stateMachine.getNextState(currentState, moves);
                mcts.newRoot = moves;

                getMatch().appendState(currentState.getContents());
            }
            Move move = stateMachineSelectMove(timeout);
            System.out.println("Picking move; " + move.toString());

            return move.getContents();
        }
        catch (Exception e)
        {
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

    /**
     * Returns the role that this gamer is playing as in the game.
     */
    public final Role getOtherRole() {
        return other;
    }


    public List<Move> getLegalMoves(Role role) throws MoveDefinitionException{
        if (role.equals(getRole())){
            return getStateMachine().getLegalMoves(getCurrentState(), getRole());
        } else if (role.equals(getOtherRole())) {
            return getStateMachine().getLegalMoves(getCurrentState(), getOtherRole());
        } else {
            return null;
        }
    }

    public String getEvaluation(){
        return "";
    }
    public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{

        StateMachine theMachine = getStateMachine();
        long start = System.currentTimeMillis();
        long finishBy = timeout - 1000;
        int me = roleMap.get(getRole());
        lock1.writeLock().unlock();
        while (System.currentTimeMillis() < finishBy){
            sleep(50);
        }
        lock1.writeLock().lock();
        List<Move> li = mcts.selectMove();
        System.out.println(li.toString());
        lock1.writeLock().unlock();
        return li.get(roleMap.get(getRole()));
    }
}


