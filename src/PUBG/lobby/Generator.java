package PUBG.lobby;

import arc.func.Cons;
import arc.struct.StringMap;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.game.Team;
import mindustry.maps.Map;
import mindustry.world.Tile;
import mindustry.world.Tiles;
import mindustry.world.blocks.environment.Floor;

public class Generator implements Cons<Tiles> {

    public static int roomSpacing = 3, roomSize = 5;

    @Override
    public void get(Tiles tiles) {
        for (int x = 0; x < tiles.width; x++) {
            for (int y = 0; y < tiles.height; y++) {
                tiles.set(x, y, new Tile(x, y, Blocks.space, Blocks.air, Blocks.air));
            }
        }
        tiles.getn(tiles.width / 2, tiles.height / 2).setNet(Blocks.coreNucleus, Team.sharded, 0);
        tiles.getn(tiles.width / 2, tiles.height / 2 + 4).setFloor((Floor) Blocks.metalFloor);
        Vars.state.map = new Map(StringMap.of("PUBG", "lobby"));
    }
}
