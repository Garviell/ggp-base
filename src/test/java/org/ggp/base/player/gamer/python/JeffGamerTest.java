package org.ggp.base.player.gamer.python;

import org.ggp.base.player.gamer.Gamer;
import org.ggp.base.player.gamer.python.stubs.JeffPythonGamerStub;
import org.ggp.base.util.game.GameRepository;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.match.Match;
import org.junit.Assert;
import org.junit.Test;

public class JeffGamerTest extends Assert {
    @Test
    public void testJeffGamer() {
        try {
            Gamer g = new JeffPythonGamerStub();
            assertEquals("Jeff", g.getName());

            Match m = new Match("", -1, 1000, 1000, GameRepository.getDefaultRepository().getGame("ticTacToe"), "");
            g.setMatch(m);
            g.setRoleName(GdlPool.getConstant("xplayer"));
            g.metaGame(1000);
            assertTrue(g.selectMove(1000) != null);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
