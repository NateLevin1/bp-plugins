package net.bridgepractice.bpbridge;

import org.bukkit.Location;
import org.bukkit.World;

public class Maps {
    public static Location getRedSpawnLoc(String mapName, World world) {
        switch(mapName) {
            case "aquatica":
                return Utils.getMapSpawnLoc(world, 29, 97, true);
            case "atlantis":
            case "developedatlantis":
                return Utils.getMapSpawnLoc(world, 34, 104, true);
            case "boo":
            case "sorcery":
            case "developedsorcery":
                return Utils.getMapSpawnLoc(world, 31, 102, true);
            case "cheesy":
                return Utils.getMapSpawnLoc(world, 26, 97, true);
            case "chronon":
                return Utils.getMapSpawnLoc(world, 26, 100, true);
            case "condo":
                return Utils.getMapSpawnLoc(world, 34, 98, true);
            case "dojo":
                return Utils.getMapSpawnLoc(world, 30, 97.5, true);
            case "flora":
            case "lighthouse":
                return Utils.getMapSpawnLoc(world, 30, 97, true);
            case "fortress":
                return Utils.getMapSpawnLoc(world, 31, 100, true);
            case "galaxy":
            case "developedgalaxy":
                return Utils.getMapSpawnLoc(world, 32, 101, true);
            case "hyperfrost":
                return Utils.getMapSpawnLoc(world, 37, 99, true);
            case "licorice":
                return Utils.getMapSpawnLoc(world, 33, 98, true);
            case "stumped":
            case "developedstumped":
                return Utils.getMapSpawnLoc(world, 30, 102, true);
            case "sunstone":
                return Utils.getMapSpawnLoc(world, 23, 100, true);
            case "treehouse":
                return Utils.getMapSpawnLoc(world, 28, 99, true);
            case "tundra":
                return Utils.getMapSpawnLoc(world, 24, 100, true);
            case "twilight":
                return Utils.getMapSpawnLoc(world, 31, 98, true);
            case "urban":
                return Utils.getMapSpawnLoc(world, 30, 96, true);
            case "crystal":
                return Utils.getMapSpawnLoc(world, 41, 98, true);

        }
        return new Location(world, 37.5, 105, 0.5, 90, 0);
    }
    public static Location getBlueSpawnLoc(String mapName, World world) {
        switch(mapName) {
            case "aquatica":
                return Utils.getMapSpawnLoc(world, -29, 97, false);
            case "atlantis":
            case "developedatlantis":
                return Utils.getMapSpawnLoc(world, -34, 104, false);
            case "boo":
            case "sorcery":
            case "developedsorcery":
                return Utils.getMapSpawnLoc(world, -31, 102, false);
            case "cheesy":
                return Utils.getMapSpawnLoc(world, -26, 97, false);
            case "chronon":
                return Utils.getMapSpawnLoc(world, -26, 100, false);
            case "condo":
                return Utils.getMapSpawnLoc(world, -34, 98, false);
            case "dojo":
                return Utils.getMapSpawnLoc(world, -30, 97.5, false);
            case "flora":
            case "lighthouse":
                return Utils.getMapSpawnLoc(world, -30, 97, false);
            case "fortress":
                return Utils.getMapSpawnLoc(world, -31, 100, false);
            case "galaxy":
            case "developedgalaxy":
                return Utils.getMapSpawnLoc(world, -32, 101, false);
            case "hyperfrost":
                return Utils.getMapSpawnLoc(world, -37, 99, false);
            case "licorice":
                return Utils.getMapSpawnLoc(world, -33, 98, false);
            case "stumped":
            case "developedstumped":
                return Utils.getMapSpawnLoc(world, -30, 102, false);
            case "sunstone":
                return Utils.getMapSpawnLoc(world, -23, 100, false);
            case "treehouse":
                return Utils.getMapSpawnLoc(world, -28, 99, false);
            case "tundra":
                return Utils.getMapSpawnLoc(world, -24, 100, false);
            case "twilight":
                return Utils.getMapSpawnLoc(world, -31, 98, false);
            case "urban":
                return Utils.getMapSpawnLoc(world, -30, 96, false);
            case "crystal":
                return Utils.getMapSpawnLoc(world, -41, 98, false);
        }
        return new Location(world, -36.5, 105, 0.5, -90, 0);
    }
    public static int getHeightOfMap(String mapName) {
        switch(mapName) {
            case "aquatica":
            case "cheesy":
            case "lighthouse":
                return 102-5;
            case "atlantis":
            case "developedatlantis":
                return 109-5;
            case "boo":
            case "sorcery":
            case "developedsorcery":
            case "stumped":
            case "developedstumped":
                return 107-5;
            case "chronon":
            case "fortress":
            case "sunstone":
            case "tundra":
                return 105-5;
            case "condo":
            case "flora":
                return 103-5;
            case "dojo":
            case "hyperfrost":
            case "licorice":
            case "treehouse":
            case "twilight":
                return 104-5;
            case "galaxy":
            case "developedgalaxy":
                return 106-5;
            case "urban":
                return 101-5;

        }
        return 99;
    }
    public static Rectangle getRedGoal(String mapName) {
        switch(mapName) {
            case "hyperfrost":
                return new Rectangle(35, 90, -2, 4, 4, 4);

            case "aquatica":
                return new Rectangle(31, 87, -2, 4, 4, 4);
            case "atlantis":
                return new Rectangle(39, 93, -1, 2, 4, 2);
            case "boo":
                return new Rectangle(32, 90, -2, 4, 4, 4);
            case "cheesy":
                return new Rectangle(25, 90, -2, 4, 4, 4);
            case "chronon":
                return new Rectangle(32, 92, -2, 4, 4, 4);
            case "condo":
                return new Rectangle(37, 90, -1, 2, 4, 2);
            case "dojo":
                return new Rectangle(31, 87, -2, 4, 4, 4);
            case "flora":
                return new Rectangle(28, 87, -2, 4, 4, 4);
            case "fortress":
                return new Rectangle(30, 90, -2, 4, 4, 4);
            case "galaxy":
                return new Rectangle(33, 88, -2, 4, 4, 4);
            case "licorice":
                return new Rectangle(33, 90, -2, 4, 4, 4);
            case "lighthouse":
                return new Rectangle(26, 91, -2, 4, 4, 4);
            case "sorcery":
                return new Rectangle(32, 90, -2, 4, 4, 4);
            case "stumped":
                return new Rectangle(32, 90, -2, 4, 4, 4);
            case "sunstone":
                return new Rectangle(28, 91, -2, 4, 4, 4);
            case "treehouse":
                return new Rectangle(31, 89, -2, 4, 4, 4);
            case "tundra":
                return new Rectangle(26, 90, -2, 4, 4, 4);
            case "twilight":
                return new Rectangle(31, 90, -3, 5, 4, 5);
            case "urban":
                return new Rectangle(31, 88, -2, 4, 4, 4);
        }
        return new Rectangle(35, 90, -2, 4, 4, 4);
    }
    public static Rectangle getBlueGoal(String mapName) {
        switch(mapName) {
            case "hyperfrost":
                return new Rectangle(-39, 90, -2, 4, 4, 4);

            case "aquatica":
                return new Rectangle(-35, 87, -2, 4, 4, 4);
            case "atlantis":
            case "developedatlantis":
                return new Rectangle(-41, 93, -1, 2, 4, 2);
            case "boo":
                return new Rectangle(-36, 90, -2, 4, 4, 4);
            case "cheesy":
                return new Rectangle(-29, 90, -2, 4, 4, 4);
            case "chronon":
                return new Rectangle(-36, 92, -2, 4, 4, 4);
            case "condo":
                return new Rectangle(-39, 90, -1, 2, 4, 2);
            case "dojo":
                return new Rectangle(-35, 87, -2, 4, 4, 4);
            case "flora":
                return new Rectangle(-32, 87, -2, 4, 4, 4);
            case "fortress":
                return new Rectangle(-34, 90, -2, 4, 4, 4);
            case "galaxy":
                return new Rectangle(-37, 88, -2, 4, 4, 4);
            case "licorice":
                return new Rectangle(-37, 90, -2, 4, 4, 4);
            case "lighthouse":
                return new Rectangle(-30, 91, -2, 4, 4, 4);
            case "sorcery":
                return new Rectangle(-36, 90, -2, 4, 4, 4);
            case "stumped":
                return new Rectangle(-36, 90, -2, 4, 4, 4);
            case "sunstone":
                return new Rectangle(-32, 91, -2, 4, 4, 4);
            case "treehouse":
                return new Rectangle(-35, 89, -2, 4, 4, 4);
            case "tundra":
                return new Rectangle(-30, 90, -2, 4, 4, 4);
            case "twilight":
                return new Rectangle(-36, 90, -3, 5, 4, 5);
            case "urban":
                return new Rectangle(-35, 88, -2, 4, 4, 4);
        }
        return new Rectangle(-39, 90, -2, 4, 4, 4);
    }
    public static Rectangle getBlocksPlaceableRect(String mapName) {
        switch(mapName) {
            case "aquatica":
            case "dojo":
            case "condo":
                return Rectangle.fromMap(-25);
            case "atlantis":
            case "developedatlantis":
            case "stumped":
            case "developedstumped":
                return Rectangle.fromMap(-27);
            case "boo":
            case "chronon":
            case "fortress":
            case "sorcery":
            case "developedsorcery":
            case "twilight":
                return Rectangle.fromMap(-23);
            case "cheesy":
            case "lighthouse":
            case "sunstone":
            case "tundra":
                return Rectangle.fromMap(-21);
            case "flora":
                return Rectangle.fromMap(-35, -20, 46, 20);
            case "galaxy":
            case "developedgalaxy":
            case "treehouse":
                return Rectangle.fromMap(-22);
            case "hyperfrost":
                return Rectangle.fromMap(-31);
            case "licorice":
                return Rectangle.fromMap(-24);
            case "urban":
                return Rectangle.fromMap(-26);
        }
        return null;
    }
    public static String humanReadableMapName(String lowercaseMapNam) {
        switch(lowercaseMapNam) {
            case "aquatica":
                return "Aquatica";
            case "atlantis":
                return "Atlantis";
            case "developedatlantis":
                return "Atlantis (PVP)";
            case "boo":
                return "Boo";
            case "cheesy":
                return "Mister Cheesy";
            case "chronon":
                return "Chronon";
            case "condo":
                return "Condo";
            case "dojo":
                return "Dojo";
            case "flora":
                return "Flora";
            case "fortress":
                return "Fortress";
            case "galaxy":
                return "Galaxy";
            case "developedgalaxy":
                return "Galaxy (PVP)";
            case "hyperfrost":
                return "Hyperfrost";
            case "licorice":
                return "Licorice";
            case "lighthouse":
                return "Lighthouse";
            case "sorcery":
                return "Sorcery";
            case "developedsorcery":
                return "Sorcery (PVP)";
            case "stumped":
                return "Stumped";
            case "developedstumped":
                return "Stumped (PVP)";
            case "sunstone":
                return "Sunstone";
            case "treehouse":
                return "Treehouse";
            case "tundra":
                return "Tundra";
            case "twilight":
                return "Twilight";
            case "urban":
                return "Urban";
        }
        return "Unknown Map";
    }
    public static int getTimeOfMap(String mapName) {
        switch(mapName) {
            case "aquatica":
            case "twilight":
            case "galaxy":
            case "developedgalaxy":
            case "boo":
                return 18000;
            default:
                return 1000;
        }
    }
}
