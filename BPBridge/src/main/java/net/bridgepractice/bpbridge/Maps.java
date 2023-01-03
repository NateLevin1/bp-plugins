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
            case "fortress2":
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
            case "tundra2":
                return Utils.getMapSpawnLoc(world, 24, 100, true);
            case "twilight":
                return Utils.getMapSpawnLoc(world, 31, 98, true);
            case "urban":
                return Utils.getMapSpawnLoc(world, 30, 96, true);
            case "crystal":
                return Utils.getMapSpawnLoc(world, 41, 98, true);
            case "ashgate":
                return Utils.getMapSpawnLoc(world, 28, 101, true);
            case "lighthouse2":
                return Utils.getMapSpawnLoc(world, 28, 98, true);
            case "outpost":
                return Utils.getMapSpawnLoc(world, 30, 103, true);
            case "palaestra":
                return Utils.getMapSpawnLoc(world, 41, 102, true);
            case "undercity":
                return Utils.getMapSpawnLoc(world, 24, 100, true);
            case "flagship":
                return Utils.getMapSpawnLoc(world, 38, 99, true);
            case "oasis":
                return Utils.getMapSpawnLoc(world, 40, 99, true);
            case "stonehenge":
                return Utils.getMapSpawnLoc(world, 38, 102, true);
        }
        return new Location(world, 37.5, 105, 0.5, 90, 0);
    }
    public static Location getBlueSpawnLoc(String mapName, World world) {
        // TODO: refactor this so that it is just getRedSpawnLoc with a negative x
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
            case "fortress2":
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
            case "tundra2":
                return Utils.getMapSpawnLoc(world, -24, 100, false);
            case "twilight":
                return Utils.getMapSpawnLoc(world, -31, 98, false);
            case "urban":
                return Utils.getMapSpawnLoc(world, -30, 96, false);
            case "crystal":
                return Utils.getMapSpawnLoc(world, -41, 98, false);
            case "ashgate":
                return Utils.getMapSpawnLoc(world, -28, 101, false);
            case "lighthouse2":
                return Utils.getMapSpawnLoc(world, -28, 98, false);
            case "outpost":
                return Utils.getMapSpawnLoc(world, -30, 103, false);
            case "palaestra":
                return Utils.getMapSpawnLoc(world, -41, 102, false);
            case "undercity":
                return Utils.getMapSpawnLoc(world, -24, 100, false);
            case "flagship":
                return Utils.getMapSpawnLoc(world, -38, 99, false);
            case "oasis":
                return Utils.getMapSpawnLoc(world, -40, 99, false);
            case "stonehenge":
                return Utils.getMapSpawnLoc(world, -38, 102, false);
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
            case "palaestra":
                return 107-5;
            case "chronon":
            case "fortress":
            case "fortress2":
            case "sunstone":
            case "tundra":
            case "tundra2":
                return 105-5;
            case "condo":
            case "flora":
            case "lighthouse2":
            case "stonehenge":
                return 103-5;
            case "dojo":
            case "hyperfrost":
            case "licorice":
            case "treehouse":
            case "twilight":
            case "flagship":
            case "oasis":
            case "crystal":
                return 104-5;
            case "galaxy":
            case "developedgalaxy":
            case "ashgate":
                return 106-5;
            case "urban":
                return 101-5;
            case "outpost":
                return 108-5;
            case "undercity":
                return 105-5;
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
            case "fortress2":
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
            case "tundra2":
                return new Rectangle(26, 90, -2, 4, 4, 4);
            case "twilight":
                return new Rectangle(31, 90, -3, 5, 4, 5);
            case "urban":
                return new Rectangle(31, 88, -2, 4, 4, 4);
            case "ashgate":
                return new Rectangle(34, 90, -2, 4, 4, 4);
            case "lighthouse2":
                return new Rectangle(33, 89, -1, 3, 3, 3);
            case "outpost":
                return new Rectangle(33, 89, -1, 3, 3, 3);
            case "palaestra":
                return new Rectangle(45, 91, -2, 4, 4, 4);
            case "undercity":
                return new Rectangle(31, 92, -2, 4, 4, 4);
            case "flagship":
                return new Rectangle(41, 90, -1, 3, 3, 3);
            case "oasis":
                return new Rectangle(44, 90, -2, 4, 1, 4);
            case "stonehenge":
                return new Rectangle(38, 90, -2, 4, 4, 4);
            case "crystal":
                return new Rectangle(42, 90, -2, 3, 4, 3);
        }
        return new Rectangle(35, 90, -2, 4, 4, 4);
    }
    public static Rectangle getBlueGoal(String mapName) {
        // TODO: refactor to be based off of getRedGoal
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
            case "fortress2":
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
            case "tundra2":
                return new Rectangle(-30, 90, -2, 4, 4, 4);
            case "twilight":
                return new Rectangle(-36, 90, -3, 5, 4, 5);
            case "urban":
                return new Rectangle(-35, 88, -2, 4, 4, 4);
            case "ashgate":
                return new Rectangle(-38, 90, -2, 4, 4, 4);
            case "lighthouse2":
                return new Rectangle(-35, 89, -1, 3, 3, 3);
            case "outpost":
                return new Rectangle(-35, 89, -1, 3, 3, 3);
            case "palaestra":
                return new Rectangle(-49, 91, -2, 4, 4, 4);
            case "undercity":
                return new Rectangle(-35, 92, -2, 4, 4, 4);
            case "flagship":
                return new Rectangle(-44, 90, -1, 3, 3, 3);
            case "oasis":
                return new Rectangle(-48, 90, -2, 4, 1, 4);
            case "stonehenge":
                return new Rectangle(-42, 90, -2, 4, 4, 4);
            case "crystal":
                return new Rectangle(-45, 90, -2, 3, 4, 3);
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
            case "fortress2":
            case "sorcery":
            case "developedsorcery":
            case "twilight":
                return Rectangle.fromMap(-23);
            case "cheesy":
            case "lighthouse":
            case "sunstone":
            case "tundra":
            case "tundra2":
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
            case "ashgate":
                return Rectangle.fromMap(-27);
            case "lighthouse2":
                return Rectangle.fromMap(-25);
            case "outpost":
                return Rectangle.fromMap(-30);
            case "palaestra":
            case "oasis":
                return Rectangle.fromMap(-35);
            case "undercity":
                return Rectangle.fromMap(-20);
            case "flagship":
                return Rectangle.fromMap(-32);
            case "stonehenge":
                return Rectangle.fromMap(-33);
            case "crystal":
                return Rectangle.fromMap(-38);
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
            case "ashgate":
                return "Ashgate";
            case "lighthouse2":
                return "Lighthouse V2";
            case "outpost":
                return "Outpost";
            case "palaestra":
                return "Palaestra";
            case "tundra2":
                return "Tundra V2";
            case "fortress2":
                return "Fortress V2";
            case "undercity":
                return "Undercity";
            case "flagship":
                return "Flagship";
            case "oasis":
                return "Oasis";
            case "stonehenge":
                return "Stonehenge";
            case "crystal":
                return "Crystal";
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