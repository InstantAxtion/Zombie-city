package com.instantaxtion.zombiesandbox;

import java.util.List;
import java.util.Random;

/**
 * ((22.8) Each district its own character, on map version 38: Chinatown's red and gold, Little Italy's terracotta and
 * ochre, the Arts District's painted walls, the glass of the Financial District, pastel suburbs, rust-red docks. A
 * theme sets the colours of the buildings in it, the paving of its pavements, a few strings of lanterns or lights over
 * its streets, which landmarks it would rather have, and, for the famous ones, the district's name on the map.
 */
final class Neighbourhoods {
    private Neighbourhoods() {
    }

    static final class Theme {
        final String name;
        /** The district types (bits of 1 << City.DT_*) it suits. */
        final int types;
        /** Whether the district takes the theme's name (each of those once a city). */
        final boolean named;
        final int[] roofs, walls, shopRoofs, shopWalls, officeWalls;
        /** The pavements' colour (0 to leave them be). */
        final int paving;
        /** Lanterns or lights strung over its streets: 0 none, 1 red lanterns, 2 warm festoon bulbs, 3 coloured flags. */
        final int strings;
        /** Landmarks it would rather have (by name). */
        final String[] favour;

        Theme(String name, int types, boolean named, int[] roofs, int[] walls, int[] shopRoofs, int[] shopWalls,
              int[] officeWalls, int paving, int strings, String... favour) {
            this.name = name;
            this.types = types;
            this.named = named;
            this.roofs = roofs;
            this.walls = walls;
            this.shopRoofs = shopRoofs;
            this.shopWalls = shopWalls;
            this.officeWalls = officeWalls;
            this.paving = paving;
            this.strings = strings;
            this.favour = favour;
        }

        boolean favours(String landmark) {
            for (String f : favour) if (f.equals(landmark)) return true;
            return false;
        }
    }

    private static int b(int... dt) {
        int m = 0;
        for (int d : dt) m |= 1 << d;
        return m;
    }

    private static int[] c(int... cols) {
        return cols;
    }

    static final Theme[] THEMES = {
            new Theme("Financial District", b(City.DT_DOWNTOWN), true,
                    c(0xFF3E4650, 0xFF4E5560, 0xFF2E3640, 0xFF5A626C), c(0xFF6F8898, 0xFF5E7686, 0xFF8898A4),
                    c(0xFF3E4A5A, 0xFF2E3A48, 0xFF4A5662), c(0xFF8FA8C0, 0xFF7C94A4, 0xFFB9C6D2),
                    c(0xFF5E7A94, 0xFF6F8898, 0xFF4E6A84, 0xFF8AA6BE, 0xFF3E5A74, 0xFF9AB4C8), 0xFFB8BCC0, 0,
                    "Glass Skyscraper", "Twisted Tower", "Stock Exchange", "Bank Headquarters", "Corporate Atrium",
                    "Sky Bridge Towers", "Penthouse Tower", "Pyramid Building", "Luxury Condos"),
            new Theme("Theatre District", b(City.DT_DOWNTOWN, City.DT_MIDTOWN), true,
                    c(0xFF5E2A2A, 0xFF3A3A40, 0xFF6E3A2E), c(0xFFD8C8A8, 0xFFC9B79C, 0xFFE0D2B4),
                    c(0xFF7A1E24, 0xFF3A2A40, 0xFF5E2A2A, 0xFF2E2E36), c(0xFFE8D8A0, 0xFFD8B8A8, 0xFFF0E0B0),
                    c(0xFFC8B090, 0xFFD8C8A8, 0xFFB8A080), 0xFFC8B8A0, 2,
                    "Vaudeville Theatre", "Opera House", "Concert Hall", "Comedy Club & Bar", "Jazz Club", "Multiplex Cinema",
                    "Opera Café", "Nightclub", "Casino", "Wax Museum"),
            new Theme("Chinatown", b(City.DT_MIDTOWN, City.DT_OLDTOWN), true,
                    c(0xFF2E6E4E, 0xFF3E7E5A, 0xFF8E2A22, 0xFF2E5E46), c(0xFFE8D8B0, 0xFFD8C098, 0xFFC84A3A, 0xFFE0C890),
                    c(0xFF2E6E4E, 0xFFA02A22, 0xFF3E7E5A, 0xFFB8862A), c(0xFFC8402E, 0xFFE8C860, 0xFFD85A3A, 0xFFF0E0B0, 0xFFB8302A),
                    c(0xFFC8A070, 0xFFB86A50, 0xFFE0C890), 0xFFB86A5A, 1,
                    "Chinatown Gate", "Dim Sum Palace", "Asian Supermarket", "Noodle Bar Arcade", "Tea House Garden",
                    "Pagoda Temple", "Night Market", "Sushi Pavilion"),
            new Theme("Little Italy", b(City.DT_MIDTOWN, City.DT_OLDTOWN), true,
                    c(0xFFA0603A, 0xFF8E4A2E, 0xFFB86A3E, 0xFF9A5838), c(0xFFE4C890, 0xFFD8A878, 0xFFE8D8A0, 0xFFC8905A, 0xFFF0D8C8),
                    c(0xFFA0603A, 0xFF8E4A2E, 0xFF3E6A3E, 0xFFB86A3E), c(0xFFE4C890, 0xFFF0E0B0, 0xFFE8B8A0, 0xFFD8C890),
                    c(0xFFD8A878, 0xFFE4C890, 0xFFC8905A), 0xFFC8B090, 2,
                    "Little Italy Deli Row", "Gelato Piazza", "Trattoria Piazza", "Pizza Parlour", "Wine Bar Courtyard",
                    "Opera Café", "Domed Basilica", "Bakery Row"),
            new Theme("Arts District", b(City.DT_MIDTOWN, City.DT_INDUSTRIAL), true,
                    c(0xFF2E8A8A, 0xFF8E4AA0, 0xFFD8702E, 0xFF3A6AB0, 0xFFB8384A, 0xFF5A9A3A, 0xFFD8B02E), c(0xFFB86A50, 0xFFE0A030, 0xFF3A8AB0, 0xFFD85A7A, 0xFF7AB060, 0xFF8E5AB8),
                    c(0xFF2E2E36, 0xFFD85A3A, 0xFF3A8AB0, 0xFFE0A030, 0xFF8E5AB8), c(0xFFE8D040, 0xFF48C0B0, 0xFFE87A9A, 0xFF9AD060, 0xFFF0F0E8),
                    c(0xFFB86A50, 0xFFA85A40, 0xFF9A4A38), 0xFFA8A098, 3,
                    "Gallery Warehouse", "Street Art Alley", "Artist Lofts", "Art Museum", "Record & Comic Row",
                    "Sculpture Park", "Antiques Emporium", "Recording Studio", "Retro Arcade"),
            new Theme("Historic Quarter", b(City.DT_OLDTOWN), true,
                    c(0xFF4E5560, 0xFF5A5E66, 0xFF3E4650, 0xFF6E4A3E), c(0xFFE0D6C4, 0xFFD8CBB0, 0xFFC8C0A8, 0xFFEAE0D0),
                    c(0xFF4E5560, 0xFF5A3D35, 0xFF3E4A3E), c(0xFFE0D6C4, 0xFFD8C8A8, 0xFFC8B8A0),
                    c(0xFFD8C8A8, 0xFFC8B8A0, 0xFFE0D2B4), 0xFFB8AC98, 0,
                    "Old Town Gate", "Castle Keep", "Cathedral", "Customs House", "Grand Post Office", "Clock Tower Plaza",
                    "Townhouse Mews", "Tudor Cottages", "Victorian Cemetery", "Hall of Records"),
            new Theme("Brick Row", b(City.DT_MIDTOWN, City.DT_OLDTOWN, City.DT_CAMPUS), false,
                    c(0xFF4E5560, 0xFF3E4A3E, 0xFF5A3D35), c(0xFFA8543A, 0xFFB86A48, 0xFF9A4A38, 0xFF8E4434),
                    c(0xFF3E4A3E, 0xFF4E5560, 0xFF2E3A2E), c(0xFFA8543A, 0xFFB86A48, 0xFF9A4A38),
                    c(0xFFA8543A, 0xFF9A4A38, 0xFFB86A48), 0, 0,
                    "Brownstone Row", "Worker Cottages", "Printing Works", "Irish Pub", "Harbour Pub"),
            new Theme("Pastel Heights", b(City.DT_SUBURB, City.DT_PARKSIDE), false,
                    c(0xFF8AA8C0, 0xFFC89A9A, 0xFF9AB89A, 0xFFB8A0C8, 0xFFD0C08A, 0xFF8AB8B8), c(0xFFA8C0D8, 0xFFE0B8B0, 0xFFC8D8C8, 0xFFD8C0E0, 0xFFE8D8A0, 0xFFF0D8C8),
                    c(0xFF5A6E8C, 0xFF7E7A5C, 0xFF6E5A7E), c(0xFFA8D0C8, 0xFFE8B8A0, 0xFFC8C0E8, 0xFFD8E8C0),
                    c(0xFFA8C0D8, 0xFFE0B8B0, 0xFFC8D8C8), 0, 0,
                    "Painted Ladies", "Cape Cod Cottages", "Art Deco Apartments", "Ice Cream Parlour", "Splash Pad Playground"),
            new Theme("Leafy Avenues", b(City.DT_SUBURB, City.DT_PARKSIDE), false,
                    c(0xFF2F3A2E, 0xFF3A3A40, 0xFF2E4E5E, 0xFF46505A), c(0xFFF2EEE4, 0xFFEAE0D0, 0xFFE0D6C4, 0xFFC8D0C8),
                    c(0xFF2F3A2E, 0xFF3E4A5A), c(0xFFF2F0EA, 0xFFE8DCC8, 0xFFD8E0D8),
                    c(0xFFE0D6C4, 0xFFEAE0D0), 0, 0,
                    "Victorian Mansion", "Gated Estate", "Arboretum", "Formal Rose Garden", "Bowls & Croquet Club", "Tennis Club"),
            new Theme("Desert Modern", b(City.DT_SUBURB), false,
                    c(0xFFA0603A, 0xFFB86A3E, 0xFF9A8A70, 0xFFC89060), c(0xFFE4C890, 0xFFD0A878, 0xFFE8D8B8, 0xFFC8A070, 0xFFF0E0C8),
                    c(0xFFA0603A, 0xFF9A8A70), c(0xFFE4C890, 0xFFD0A878),
                    c(0xFFE4C890, 0xFFD0A878), 0xFFD8C8A8, 0,
                    "Adobe Compound", "Spanish Colonial Hacienda", "Mid-Century Modern Homes", "Taqueria Plaza", "Rock Garden"),
            new Theme("Docklands", b(City.DT_INDUSTRIAL), true,
                    c(0xFF5A6E7E, 0xFF3E5A74, 0xFF7A3A2A, 0xFF4E6A5E), c(0xFF8FA8C0, 0xFFB86A50, 0xFF7E9A88, 0xFFC8B89A),
                    c(0xFF3E5A74, 0xFF7A3A2A, 0xFF4E6A5E), c(0xFF8FA8C0, 0xFFC8B89A),
                    c(0xFF8FA8C0, 0xFF7E9A88, 0xFFB86A50), 0xFF9A9C9E, 0,
                    "Fish Market Hall", "Chandlery & Sail Loft", "Harbour Pub", "Cold Store", "Fuel Depot", "Ice Factory",
                    "Coast Guard Station", "Riverside Lofts"),
            new Theme("Rust Belt", b(City.DT_INDUSTRIAL), false,
                    c(0xFF7E3A1E, 0xFF8A4A2A, 0xFF6E4A3A, 0xFF5A4A40), c(0xFF9A6A50, 0xFF8A7A6A, 0xFFA87A5A, 0xFF7C6A5A),
                    c(0xFF7E3A1E, 0xFF6E4A3A), c(0xFF9A6A50, 0xFF8A7A6A),
                    c(0xFF9A6A50, 0xFF8A7A6A, 0xFFA87A5A), 0xFF908478, 0,
                    "Gasometer", "Brick Kilns", "Glassworks", "Sawmill", "Bell Foundry", "Railway Museum", "Recycling Centre"),
            new Theme("Tech Park", b(City.DT_CAMPUS, City.DT_MIDTOWN), true,
                    c(0xFFE0E4E8, 0xFFC8CCD0, 0xFF5A626C), c(0xFFF2F4F6, 0xFFE0E4E8, 0xFF8FA8C0, 0xFFA8B8C0),
                    c(0xFFE0E4E8, 0xFF5A626C), c(0xFFF2F4F6, 0xFFA8D0C8),
                    c(0xFFE8ECF0, 0xFF8FA8C0, 0xFFA8C8D8, 0xFF7C94A4), 0xFFC8CCD0, 0,
                    "Research Campus", "Robotics Factory", "Data Centre", "Science Centre", "Radio Telescope", "Corporate Atrium"),
            new Theme("University Town", b(City.DT_CAMPUS), false,
                    c(0xFF4E5560, 0xFF3E4650, 0xFF2F3A2E), c(0xFFA8543A, 0xFFC8B8A0, 0xFFB86A48, 0xFFD8C8A8),
                    c(0xFF3E4A3E, 0xFF4E5560), c(0xFFC8B8A0, 0xFFA8543A),
                    c(0xFFA8543A, 0xFFC8B8A0, 0xFFB86A48), 0, 0,
                    "University Quad", "Music Conservatory", "Public Library", "Student Halls", "Branch Library", "Natural History Museum"),
            new Theme("Garden Village", b(City.DT_PARKSIDE, City.DT_SUBURB), false,
                    c(0xFF3E6A3E, 0xFF4E7A4A, 0xFF5A3D35, 0xFF2E5E3E), c(0xFFF2EEE4, 0xFFE8D8A0, 0xFFB8C8A0, 0xFFE0D6C4),
                    c(0xFF3E6A3E, 0xFF5E7A4A), c(0xFFF2EEE4, 0xFFD8E8C0),
                    c(0xFFE8D8A0, 0xFFB8C8A0), 0, 0,
                    "Eco Village", "Garden Co-op", "Community Allotments", "Tiny House Village", "Wildflower Meadow", "Community Orchard"),
    };

    /** Gives each district of the city a theme that suits it; the named ones once each, and they rename it. */
    static void assign(City city, long seed) {
        Random r = new Random(seed * 7349L + 38);
        boolean[] used = new boolean[THEMES.length];
        for (City.District d : city.districts) {
            int bit = 1 << d.type;
            int n = 0, pick = -1;
            for (int k = 0; k < THEMES.length; k++) {
                Theme t = THEMES[k];
                if ((t.types & bit) == 0 || (t.named && used[k])) continue;
                // (The famous neighbourhoods only in districts big enough to have their name on the map.)
                if (t.named && !d.labelled()) continue;
                if (r.nextInt(++n) == 0) pick = k;
            }
            // (Now and then a district is just itself.)
            if (pick < 0 || r.nextFloat() < 0.15f) continue;
            d.theme = THEMES[pick];
            if (d.theme.named) {
                used[pick] = true;
                d.name = d.theme.name;
            }
        }
    }

    static int pick(int[] cols, Random r) {
        return cols[r.nextInt(cols.length)];
    }

    static boolean any(List<City.District> ds) {
        for (City.District d : ds) if (d.theme != null) return true;
        return false;
    }
}
