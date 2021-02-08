package PUBG.game;

import PUBG.MainX;
import PUBG.lobby.Lobby;
import PUBG.logic.Data;
import PUBG.logic.PlayerData;
import PUBG.logic.State;
import arc.Events;
import arc.graphics.Color;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Interval;
import arc.util.Log;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Bullets;
import mindustry.content.Fx;
import mindustry.content.UnitTypes;
import mindustry.game.Gamemode;
import mindustry.game.Rules;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Nulls;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.maps.Map;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;

public class Game {

    public static int ID = 10;
    public static int dropTimerStart = 60 * 14, dropTimer = 0;
    public static float centreX, centreY;
    public static float dropDistance = 250f;
    public static float dangerZoneMaxRadius = 1000f,
            dangerZoneRadius =dangerZoneMaxRadius;
    public static boolean dropTimerIs = false, gameEnd = false, dropTimeIsClose = false;

    public static Rules rules;

    public static Seq<PlayerData> inGamePlayers = new Seq<>();
    public static Seq<Unit> lootZone = new Seq<>();
    public static Seq<Tile> medbay = new Seq<>();
    public static Seq<Tile> resupplypoint = new Seq<>();

    public static ObjectMap<Integer, UnitType[]> units = new ObjectMap<>();
    public static Unit dropper;

    public static void init() {
        rules = new Rules();
        rules.blockDamageMultiplier = 0;
        rules.canGameOver = false;
        rules.unitAmmo = true;
        rules.defaultTeam = Team.purple;
        rules.blockHealthMultiplier = 99999f;

        units.put(1, new UnitType[]{UnitTypes.dagger, UnitTypes.nova});
        units.put(2, new UnitType[]{UnitTypes.mace, UnitTypes.pulsar, UnitTypes.atrax});
        units.put(3, new UnitType[]{UnitTypes.fortress});

        for (Block block : Vars.content.blocks()) {
            rules.bannedBlocks.add(block);
        }
    }

    public static void update() {
        dropTimer--;
        // quad zone
        if (dropper != null) {
            dropper.moveAt(new Vec2(1, 1));
            for (int r = 0; r < 36; r++) {
                Call.effect(Fx.fire, dropper.x + Angles.trnsx(r * 10, dropDistance, 0), dropper.y + Angles.trnsy(r * 10, dropDistance, 0), 0, Color.white);
            }
        }
        // quad zone end
        for (PlayerData data : inGamePlayers) {
            Player player = data.player;
            // check when bullet die
            if (dropper != null) {
                if (dropper.isPlayer()) {
                    dropper.getPlayer().unit(Nulls.unit);
                }
                if (dropper.dst(Vars.world.width() * Vars.tilesize, Vars.world.height() * Vars.tilesize) < Vars.tilesize * 6) {
                    dropper.kill();
                    dropTimeIsClose = true;
                    dropper = null;
                } else {
                    if (player.con != null) {
                        player.con.viewX = dropper.x;
                        player.con.viewY = dropper.y;
                    }
                }
            }

            if (data.drop != null && data.drop.x != 0) {
                data.lastX = data.drop.x;
                data.lastY = data.drop.y;
            }
            if (dropTimer < 0) {
                // start game when timer end
                if (dropTimerIs && dropper != null) {
                    dropTimerIs = false;
                    Call.label("[lime]DROP NOW", 3, dropper.x, dropper.y);
                }
                if (dropper != null) {
                    // check when player want to drop
                    if (!data.spawned && data.drop == null && dropper.dst(player.unit().aimX, player.unit().aimY) < dropDistance && player.shooting()) {
                        Call.createBullet(Bullets.artilleryExplosive, Team.sharded, dropper.x, dropper.y, dropper.angleTo(player.unit().aimX, player.unit().aimY), 0, 1, dropper.dst(player.unit().aimX, player.unit().aimY) / dropDistance);
                        data.drop = Groups.bullet.index(Groups.bullet.size() - 1);
                        Events.fire(new Game.OnPlayerDropEvent(player));
                    } else if (data.drop != null && !data.spawned) {
                        // spawn player when bullet dead
                        if (data.drop.x == 0) {
                            Events.fire(new Game.PlayerDropEvent(player));
                            data.spawned = true;
                            Unit unit = UnitTypes.dagger.create(Team.sharded);
                            unit.set(data.lastX, data.lastY);
                            unit.add();
                            data.unit = unit;
                            player.team(Team.get(player.id));
                            player.unit(unit);
                        }
                    }
                }
            }

            // Unit Switch
            for (Unit unit : lootZone) {
                if (player.unit() != unit && !unit.dead && !player.unit().dead) {
                    if (player.dst(unit) <= 10) {
                        player.unit().kill();
                        unit.team(player.team());
                        unit.health = unit.type.health;
                        data.unit = unit;
                        player.unit(unit);
                        lootZone.remove(unit);
                    }
                }
            }
            // end region

            // Repair point
            for (Tile tile : medbay) {
                if (tile.build != null && tile.dst(player) < 10 && player.unit().health < player.unit().maxHealth) {
                    tile.build.kill();
                    player.unit().health = player.unit().maxHealth;
                    Call.label("[lime]|Used|", 3, tile.drawx(), tile.drawy());
                    Timer.schedule(() -> {
                        tile.setNet(Blocks.repairPoint, Team.sharded, 0);
                    }, 10);
                }
            }
            // end region

            // Resupply point
            for (Tile tile : resupplypoint) {
                if (tile.build != null && tile.dst(player) < 10 && player.unit().ammo < player.unit().type.ammoCapacity) {
                    tile.build.kill();
                    player.unit().ammo = player.unit().type.ammoCapacity;
                    Call.label("[lime]|Used|", 3, tile.drawx(), tile.drawy());
                    Timer.schedule(() -> {
                        tile.setNet(Blocks.battery, Team.sharded, 0);
                    }, 10);
                }
                // end region
            }
        }

        // Check winner
        if (Groups.unit.size() > 0 && !gameEnd && dropTimeIsClose) {
            Unit lastUnit = Groups.unit.find(u -> u.team != Team.derelict && u.team != Team.sharded);
            boolean end = true;
            // check if some one player was dropped then
            if (lastUnit != null) {
                for (Unit unit : Groups.unit) {
                    if (!unit.dead && (unit.team != Team.derelict && unit.team != Team.sharded) && unit.team != lastUnit.team && lastUnit != unit) {
                        end = false;
                        lastUnit = unit;
                    }
                }
                // end game if only 1 player
                if (end) {
                    if (lastUnit.getPlayer() != null) {
                        endGame(lastUnit.team);
                    }
                }
            } else /* end game when no one be dropper */ {
                endGame(Team.derelict);
            }
        }
        updateDangerZone();
    }
    
    public static void updateDangerZone() {
        dangerZoneRadius -= 0.2f;
        float v = (dangerZoneMaxRadius / (dangerZoneMaxRadius / 100));
        v = v < 50 ? 90 : 120;
        for (int r = 0; r < v; r++) {
            Call.effect(Fx.fire, centreX + Angles.trnsx(r * (v / 30), dangerZoneRadius, 0), centreY + Angles.trnsy(r * (v / 30), dangerZoneRadius, 0), 0, Color.white);
        }
        Groups.unit.each(unit -> {
            if (unit.dst(centreX, centreY) > dangerZoneRadius) {
                unit.kill();
            }
        });
    }
    
    public static void go() {
        dropTimeIsClose = false;
        gameEnd = false;
        dropTimerIs = true;
        dropTimer = dropTimerStart;
        dropper = null;
        resupplypoint.clear();
        medbay.clear();
        inGamePlayers.clear();
        inGamePlayers = Data.playerDatas.values().toSeq();
        MainX.state = State.game;
        Seq<Player> players = new Seq<>();
        Groups.player.copy(players);

        // Logic Reset Start
        Vars.logic.reset();

        // World Load Start
        Call.worldDataBegin();
        Map map = Vars.maps.getNextMap(Gamemode.survival, Vars.state.map);
        Vars.world.loadMap(map);
        Vars.world.tile(7, 7).setNet(Blocks.coreNucleus, Team.sharded, 0);
        // World Load End

        // Rules Load
        Vars.state.rules = rules.copy();

        Generator.gen(Vars.world.tiles);
        for (PlayerData data : Data.playerDatas.values()) {
            data.player.team(Team.sharded);
            data.spawned = false;
        }
        Vars.logic.play();
        // Logic Reset End
        
        
        // values
        dangerZoneMaxRadius = Math.max(Vars.world.width(), Vars.world.height()) * Vars.tilesize;
        dangerZoneRadius = dangerZoneMaxRadius;
        centreX = Vars.world.width() * Vars.tilesize / 2;
        centreY = Vars.world.height() * Vars.tilesize / 2;
        // region end
        
        
        // Send World Data To All Players
        for (Player p : players) {
            Vars.netServer.sendWorldData(p);
        }
        // region end
        
        // summon dropper
        Unit unit = UnitTypes.quad.create(Team.sharded);
        unit.set(0, 0);

        Timer.schedule(() -> {
            unit.add();
            if (unit.team.core() != null) {
                unit.team.core().kill();
            }
            dropper = unit;
        }, 2);
        //region end
    }

    public static class PlayerDropEvent {

        public final Player player;

        public PlayerDropEvent(Player player) {
            this.player = player;
        }
    }

    public static class OnPlayerDropEvent {

        public final Player player;

        public OnPlayerDropEvent(Player player) {
            this.player = player;
        }
    }

    public static void endGame(Team team) {
        gameEnd = true;
        if (team == Team.derelict) {
            Call.infoMessage("[gray]| Draw |");
        } else {
            if (team.data().units.size > 0) {
                Unit un = team.data().units.find(u-> u.isPlayer());
                if (un != null) {
                    Call.infoMessage("[gold]| Winner is: [white]" + un.getPlayer().name + "[gold] |");
                    inGamePlayers.clear();
                }
            }
        }
        Timer.schedule(() -> {
            Lobby.go();
        }, 3);
    }
}
