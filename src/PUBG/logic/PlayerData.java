package PUBG.logic;

import mindustry.gen.Bullet;
import mindustry.gen.Player;
import mindustry.gen.Unit;

public class PlayerData {
    public Player player;
    public Bullet drop;
    public Unit unit;
    public  float lastX, lastY;
    public boolean spawned = false;
    public int id;
    
    public PlayerData(Player player) {
        this.player = player;
        this.id = player.id;
    }
}
