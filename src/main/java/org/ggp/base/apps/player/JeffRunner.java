package org.ggp.base.apps.player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ggp.base.player.UnityPlayer;
import org.ggp.base.player.gamer.Gamer;
import org.ggp.base.util.reflection.ProjectSearcher;

/**
 * This is a simple command line app for running Jeff
 * keeping the search for now in case we need it
 * adapted from PlayerRunner
 */
public final class JeffRunner
{
    public static void main(String[] args) throws IOException, 
           InstantiationException,
           IllegalAccessException
    {
        if (args.length != 1 || args[0].equals("${arg0}")) {
            System.out.println("PlayerRunner [port]");
            return;
        }
        int port = Integer.parseInt(args[0]);
        String name = "UnityGamer";
        System.out.println("Starting up preconfigured player on port " +
                port +
                " using player class named " +
                name);
        Class<?> chosenGamerClass = null;
        List<String> availableGamers = new ArrayList<String>();
        for (Class<?> gamerClass : ProjectSearcher.GAMERS.getConcreteClasses()) {
            availableGamers.add(gamerClass.getSimpleName());
            if (gamerClass.getSimpleName().equals(name)) {
                chosenGamerClass = gamerClass;
            }
        }
        if (chosenGamerClass == null) {
            System.out.println("Could not find player class with that name." +
                    " Available choices are: " +
                    Arrays.toString(availableGamers.toArray()));
            return;
        }
        Gamer gamer = (Gamer) chosenGamerClass.newInstance();
        new UnityPlayer(port, gamer).start();
    }
}
