package org.ggp.base.player.request.factory;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.player.gamer.Gamer;
import org.ggp.base.util.gdl.factory.GdlFactory;
import org.ggp.base.util.gdl.factory.exceptions.GdlFormatException;
import org.ggp.base.player.request.factory.exceptions.RequestFormatException;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.symbol.factory.SymbolFactory;
import org.ggp.base.util.symbol.grammar.Symbol;
import org.ggp.base.util.symbol.grammar.SymbolAtom;
import org.ggp.base.util.symbol.grammar.SymbolList;
import org.ggp.base.util.game.Game;
import org.ggp.base.player.request.grammar.PushRequest;
import org.ggp.base.player.request.grammar.PullRequest;
import org.ggp.base.player.request.grammar.UnityRequest;
import org.ggp.base.player.request.grammar.AbortRequest;
import org.ggp.base.player.request.grammar.InfoRequest;
import org.ggp.base.player.request.grammar.PlayRequest;
import org.ggp.base.player.request.grammar.PreviewRequest;
import org.ggp.base.player.request.grammar.Request;
import org.ggp.base.player.request.grammar.StartRequest;
import org.ggp.base.player.request.grammar.StopRequest;

public class UnityRequestFactory extends RequestFactory {
    @Override
    public Request create(Gamer gamer, String source) throws RequestFormatException {
        try {
            SymbolList list = (SymbolList) SymbolFactory.create(source);
            SymbolAtom head = (SymbolAtom) list.get(0);

            String type = head.getValue().toLowerCase();
            if (type.equals("play")) {
                return createPlay(gamer, list);
            } else if (type.equals("start")) {
                return createStart(gamer, list);
            } else if (type.equals("unity")) { 
                return createUnity(gamer, list);
            } else if (type.equals("push")) {
                return createPush(gamer, list);
            } else if (type.equals("pull")) {
                return createPull(gamer, list);
            } else if (type.equals("stop")) {
                return createStop(gamer, list);
            } else if (type.equals("abort")) {
                return createAbort(gamer, list);
            } else if (type.equals("info")) {
                return createInfo(gamer, list);
            } else if (type.equals("preview")) {
                return createPreview(gamer, list);
            } else {
                throw new IllegalArgumentException("Unrecognized request type!");
            }
        }
        catch (Exception e) {
            throw new RequestFormatException(source, e);
        }
    }

    private PushRequest createPush(Gamer gamer, SymbolList list) throws GdlFormatException {
        System.out.println(list);
        if (list.size() != 3) {
            throw new IllegalArgumentException("Expected exactly 2 arguments!");
        }

        SymbolAtom arg1 = (SymbolAtom) list.get(1);
        Symbol arg2 = list.get(2);

        String matchId = arg1.getValue();
        List<GdlTerm> moves = parseMoves(arg2);

        return new PushRequest(gamer, matchId, moves);
    }

    private PullRequest createPull(Gamer gamer, SymbolList list) throws GdlFormatException {
        System.out.println(list);
        if (list.size() != 2) {
            throw new IllegalArgumentException("Expected exactly 2 arguments!");
        }

        SymbolAtom arg1 = (SymbolAtom) list.get(1);

        String matchId = arg1.getValue();

        return new PullRequest(gamer, matchId);
    }

    private UnityRequest createUnity(Gamer gamer, SymbolList list) throws GdlFormatException {
        if (list.size() < 6) {
            throw new IllegalArgumentException("Expected at least 5 arguments!");
        }

        SymbolAtom arg1 = (SymbolAtom) list.get(1);
        SymbolAtom arg2 = (SymbolAtom) list.get(2);
        SymbolAtom arg3 = (SymbolAtom) list.get(3);
        SymbolAtom arg4 = (SymbolAtom) list.get(4);
        SymbolAtom arg5 = (SymbolAtom) list.get(5);

        String matchId = arg1.getValue();
        GdlConstant role = (GdlConstant) GdlFactory.createTerm(arg2);
        String gameName = arg3.getValue();
        int startClock = Integer.valueOf(arg4.getValue());
        int playClock = Integer.valueOf(arg5.getValue());

        // For now, there are only five standard arguments. If there are any
        // new standard arguments added to START, they should be added here.
        return new UnityRequest(gamer, role, matchId, gameName, 5000, 5000);
    }

}
