package PUBG.game;

import static PUBG.game.Game.lootZone;
import static PUBG.game.Game.units;
import arc.math.Mathf;
import arc.util.Log;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.game.Team;
import mindustry.gen.Unit;
import mindustry.type.UnitType;
import mindustry.world.Tile;
import mindustry.world.Tiles;
import mindustry.world.blocks.environment.Floor;

public class Generator {

    public static void gen(Tiles tiles) {
        for (int x = 0; x < tiles.width; x++) {
            for (int y = 0; y < tiles.height; y++) {
                Tile tile = tiles.get(x, y);
                
                if (tile.build != null) {
                    if (tile.build.block == Blocks.repairPoint || tile.build.block == Blocks.battery) {
                        for (int xx = -1; xx <= 1; xx++) {
                            for (int yy = -1; yy <= 1; yy++) {
                                if (check(xx + tile.x, yy + tile.y)) {
                                    tiles.getn(xx + tile.x, yy + tile.y).setFloorNet(Blocks.metalFloor);
                                }
                            }
                        }
                        if (tile.build.block == Blocks.repairPoint) {
                            Game.medbay.add(tile);
                        } else {
                            Game.resupplypoint.add(tile);
                        }
                    }
                }
                
                if (tile.floor() == (Floor) Blocks.metalFloor5) {
                    UnitType[] t = units.get(Mathf.random(1, 2));
                    Unit u = t[Mathf.random(t.length - 1)].create(Team.sharded);
                    u.set(tile.drawx(), tile.drawy());
                    Timer.schedule(() -> {
                        u.add();
                        u.team = Team.derelict;
                        u.health = 99999;
                        lootZone.add(u);
                    }, 3);
                }
            }
        }
    }
    
    public static boolean check (int x, int y) {
        return x > 0 && y > 0 && x < Vars.world.width() && y < Vars.world.height();
    }
}
