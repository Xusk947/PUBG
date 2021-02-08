package PUBG.lobby;

import PUBG.MainX;
import PUBG.game.Game;
import PUBG.logic.State;
import arc.graphics.Color;
import arc.math.Angles;
import arc.struct.Seq;
import arc.util.Interval;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.content.UnitTypes;
import mindustry.game.Rules;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.world.Block;

public class Lobby {

    public static Rules rules;
    public static float angle = 0;
    public static float zone = 125f;
    public static float centreX, centreY;
    public static int startTime = 5, startTimer = 0;
    public static Interval interval = new Interval(2);

    public static void onPlayerJoin(Player player) {
        float width = Vars.world.width() * Vars.tilesize;
        float height = Vars.world.height() * Vars.tilesize;
        Unit unit = UnitTypes.dagger.create(Team.sharded);
        unit.spawnedByCore = true;
        unit.set(width / 2, height / 2 + Vars.tilesize * 4);
        unit.add();
        player.unit(unit);
        if (player.con != null) {
            player.con.viewX = unit.x;
            player.con.viewY = unit.y;
        }
    }

    public static void update() {
        angle += 0.5f;
        if (interval.get(0, 60)) {
            startTimer--;
            Call.label(startTimer + "", 1, centreX, centreY);
        }

        if (startTimer > 0) {
            for (int r = 0; r < 10; r++) {
                Call.effect(Fx.fire, centreX + Angles.trnsx(angle + (36 * r), zone, 0), centreY + Angles.trnsy(angle + (36 * r), zone, 0), 0, Color.white);
            }
        } else {
            Game.go();
        }
    }

    public static void init() {
        rules = new Rules();
        rules.lighting = false;
        rules.defaultTeam = Team.sharded;
        rules.canGameOver = false;
        for (Block block : Vars.content.blocks()) {
            rules.bannedBlocks.add(block);
        }
    }

    public static void go() {
        MainX.state = State.lobby;
        Seq<Player> players = new Seq<>();
        Groups.player.copy(players);

        // Logic Reset Start
        Vars.logic.reset();

        // World Load Start
        Call.worldDataBegin();
        Vars.world.loadGenerator(50, 50, new Generator());
        centreX = Vars.world.width() * Vars.tilesize / 2;
        centreY = Vars.world.height() * Vars.tilesize / 2;
        startTimer = startTime;
        // World Load End

        // Rules Load
        Vars.state.rules = rules.copy();

        // Logic Reset End
        Vars.logic.play();

        // Send World Data To All Players
        for (Player p : players) {
            Vars.netServer.sendWorldData(p);
        }
        
        Groups.player.each(p -> {
            p.team(Team.sharded);
            if (p.con != null) {
                p.con.viewX = 0;
                p.con.viewY = 0;
            }
        });
    }
}
