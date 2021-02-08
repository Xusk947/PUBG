package PUBG;

import PUBG.game.Game;
import PUBG.lobby.Lobby;
import PUBG.logic.Data;
import PUBG.logic.PlayerData;
import PUBG.logic.State;
import arc.Events;
import mindustry.Vars;
import mindustry.content.UnitTypes;
import mindustry.game.EventType;
import mindustry.mod.Plugin;

/**
 *
 * @author Xusk
 */
public class MainX extends Plugin {

    public static boolean DEBUG = true;
    public static State state = State.lobby;

    @Override
    public void init() {
        Game.init();
        Lobby.init();

        Events.on(EventType.ServerLoadEvent.class, event -> {
            Lobby.go();
            Vars.netServer.openServer();
        });

        Events.run(EventType.Trigger.update, () -> {
            switch (state) {
                case game:
                    Game.update();
                    break;
                case lobby:
                    Lobby.update();
            }
        });

        Events.on(EventType.PlayerJoin.class, event -> {
            Data.playerDatas.put(event.player.id, new PlayerData(event.player));
            if (state == State.lobby) {
                Lobby.onPlayerJoin(event.player);
            }
        });
        
        Events.on(EventType.PlayerLeave.class, event -> {
            Data.playerDatas.remove(event.player.id);
        });
        
        UnitTypes.gamma.speed = 0;
        UnitTypes.gamma.weapons.clear();
        UnitTypes.quad.weapons.clear();
    }
}
