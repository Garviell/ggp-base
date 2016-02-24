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
 * SampleGamer is a simplified version of the StateMachineGamer, dropping some
 * advanced functionality so the example gamers can be presented concisely.
 * This class implements 7 of the 8 core functions that need to be implemented
 * for any gamer.
 *
 * If you want to quickly create a gamer of your own, extend this class and
 * add the last core function : public Move stateMachineSelectMove(long timeout)
 */

public class JeffGamer extends StateMachineGamer
{
    private MCTS mcts;
    private Map<Role, Integer> roleMap;
    public ReentrantReadWriteLock lock1= new ReentrantReadWriteLock(true);
    @Override
    public void stateMachineMetaGame(long timeout) {

        roleMap = getStateMachine().getRoleIndices();
        mcts = new MCTS(this, getRole(), lock1, false);
        long finishBy = timeout - 1100;
        mcts.start();
        while(System.currentTimeMillis() < finishBy){
            try{
                Thread.sleep(200);
            } catch(Exception e){}//don't care
        }
        // kCTS.blocked = true;
        // Sample gamers do no metagaming at the beginning of the match.
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
        MCTS.alive = false;
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

    public GdlTerm uSelectMove(long timeout) throws MoveSelectionException {
        try {
            stateMachine.doPerMoveWork();

            List<GdlTerm> lastMoves = getMatch().getMostRecentMoves();
            if (lastMoves != null) {
                List<Move> moves = new ArrayList<Move>();
                for (GdlTerm sentence : lastMoves) {
                    moves.add(stateMachine.getMoveFromTerm(sentence));
                }

                currentState = stateMachine.getNextState(currentState, moves);
                getMatch().appendState(currentState.getContents());
            }
            List<Move> move;

            while (true){
                move = stateMachineSelectMoves(timeout);
                currentState = stateMachine.getNextState(currentState, move);
                getMatch().appendState(currentState.getContents());
                List<Move> legal  = getStateMachine().getLegalMoves(getCurrentState(), getRole());
                if (legal.size() == 1){
                    if (!legal.get(0).toString().equals("noop")){ //no idea if this works to detect noop
                        System.out.println("Noop detected in JeffGamer");
                        break;
                    }
                }

            }
            return move.get(roleMap.get(me)).getContents();
        } catch (Exception e) {
            GamerLogger.logStackTrace("GamePlayer", e);
            throw new MoveSelectionException(e);
        }
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

    private void advanceTree(List<Move> moves){
        mcts.selectMove(moves);
    }

    @Override
    public  void metaGame(long timeout) throws MetaGamingException
    {
        try
        {
            stateMachine = getInitialStateMachine();
            stateMachine.initialize(getMatch().getGame().getRules());
            currentState = stateMachine.getInitialState();
            me = stateMachine.getRoleFromConstant(getRoleName());
            //This is fine.
            List<Role> roles = stateMachine.getRoles();
            other = (roles.get(0).equals(me)? roles.get(1) : roles.get(0));
            getMatch().appendState(currentState.getContents());

            stateMachineMetaGame(timeout);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            GamerLogger.logStackTrace("GamePlayer", e);
            throw new MetaGamingException(e);
        }
    }

    /**
     * Employs a simple sample "Monte Carlo" algorithm.
     */
    public List<Move> stateMachineSelectMoves(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        StateMachine theMachine = getStateMachine();
        long start = System.currentTimeMillis();
        long finishBy = timeout - 1000;
        int me = roleMap.get(getRole());

        List<List<Move>> moves = theMachine.getLegalJointMoves(getCurrentState());
        List<Move> selection = moves.get(0);
        if (moves.size() > 1) {
            int[] moveTotalPoints = new int[moves.size()];
            int[] moveTotalAttempts = new int[moves.size()];

            // Perform depth charges for each candidate move, and keep track
            // of the total score and total attempts accumulated for each move.
            for (int i = 0; true; i = (i+1) % moves.size()) {
                if (System.currentTimeMillis() > finishBy)
                    break;

                int theScore = performDepthChargeFromMove(getCurrentState(), moves.get(i).get(me));
                moveTotalPoints[i] += theScore;
                moveTotalAttempts[i] += 1;
            }

            // Compute the expected score for each move.
            double[] moveExpectedPoints = new double[moves.size()];
            for (int i = 0; i < moves.size(); i++) {
                moveExpectedPoints[i] = (double)moveTotalPoints[i] / moveTotalAttempts[i];
            }

            // Find the move with the best expected score.
            int bestMove = 0;
            double bestMoveScore = moveExpectedPoints[0];
            for (int i = 1; i < moves.size(); i++) {
                if (moveExpectedPoints[i] > bestMoveScore) {
                    bestMoveScore = moveExpectedPoints[i];
                    bestMove = i;
                }
            }
            selection = moves.get(bestMove);
        }

        long stop = System.currentTimeMillis();
        System.out.println("Exited select moves");

        return selection;
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


