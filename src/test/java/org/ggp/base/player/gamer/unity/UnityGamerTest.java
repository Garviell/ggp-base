package org.ggp.base.player.gamer.unity;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.ggp.base.player.gamer.Gamer;
import org.ggp.base.player.gamer.statemachine.unity.UnityGamer;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.match.Match;
import org.ggp.base.util.game.GameRepository;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.player.gamer.exception.MetaGamingException;

public class UnityGamerTest extends Assert {
    protected UnityGamer g;
    protected Match m;

    @Test
    public void testUnityGamer() {
        try {
            //Testing the general setup of the gamer
            setUp("second");
            List<Move> uMoves = g.getLegalMoves(g.getRole());
            List<Move> oMoves = g.getLegalMoves(g.getOtherRole());
            assertEquals(uMoves.size(), 9);
            assertEquals(oMoves.size(), 1);
            tearDown();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testRoles() {
        try {
            //Testing if the correct roles are being set.
            setUp("second");
            assertEquals(g.getRole(), new Role(GdlPool.getConstant("xplayer")));
            assertEquals(g.getOtherRole(), new Role(GdlPool.getConstant("oplayer")));
            tearDown();
            setUp("first");
            assertEquals(g.getRole(), new Role(GdlPool.getConstant("oplayer")));
            assertEquals(g.getOtherRole(), new Role(GdlPool.getConstant("xplayer")));
            tearDown();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    @Test
    public void testSelectMove() {
        try {
            setUp("second");
            GdlTerm move = g.selectMove(1000);
            List<Move> uMoves = g.getLegalMoves(g.getRole());
            List<Move> oMoves = g.getLegalMoves(g.getOtherRole());
            //Does select move return a move?
            assertTrue(move  != null);
            //The move should not be a noop
            assertFalse(move.equals(Move.create("noop").getContents()));
            //Test if the state has changed
            assertEquals(uMoves.size(), 1);
            assertEquals(oMoves.size(), 8);
            for (Move m : oMoves){
                //Make sure that the correct move was set
                assertFalse(m.equals(new Move(move)));
            }
            tearDown();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }


    //I don't know if its the junit version we have or if i'm just bad but i couldn't get the normal
    //setup teardown thing to work
    private void setUp(String role) throws MetaGamingException, InterruptedException{
            g = new UnityGamer();
            g.silent = true;
            m = new Match("", -1, 1000, 1000, GameRepository.getDefaultRepository().getGame("ticTacToe"), "");
            g.setMatch(m);
            g.setRoleName(GdlPool.getConstant(role));
            g.metaGame(1000);
            Thread.sleep(100);
    }

    private void tearDown(){
        try{
            g.stop();
        } catch (Exception e){
        }
    }
}
