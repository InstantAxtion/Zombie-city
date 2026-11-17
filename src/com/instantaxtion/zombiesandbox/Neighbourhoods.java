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

    /**
     * ((22.9) Desert City's own: the neon of the casino strip, adobe in the old quarter, stucco and terracotta out in
     * the sprawl, motels and diners on the highway side. Only that map uses them (and every district gets one).
     */
    static final Theme[] DESERT_THEMES = {
            new Theme("The Strip", b(City.DT_DOWNTOWN), true,
                    c(0xFFD8A030, 0xFF8E2A6A, 0xFF2A6A8E, 0xFFE8E0D0, 0xFFC03050), c(0xFFF0E0B0, 0xFFE8B8D0, 0xFFF8F4EC, 0xFFE8C860),
                    c(0xFF8E2A6A, 0xFF2A6A8E, 0xFFD8A030, 0xFFC03050), c(0xFFF8D040, 0xFFF070B0, 0xFF40D0D0, 0xFFF8F4EC, 0xFFFF7040),
                    c(0xFFD8B860, 0xFFE070A8, 0xFF40C0C0, 0xFFF0EEE8, 0xFFA07840, 0xFFE8C860, 0xFFB850D0), 0xFFE0CCA8, 3,
                    "Casino", "Grand Hotel", "Nightclub", "Wedding Chapel Gardens", "Boutique Hotel", "Pyramid Building",
                    "Wax Museum", "Multiplex Cinema", "Comedy Club & Bar", "Retro Arcade"),
            new Theme("Old Adobe Quarter", b(City.DT_OLDTOWN, City.DT_MIDTOWN, City.DT_CAMPUS), true,
                    c(0xFFA0603A, 0xFFB86A3E, 0xFF9A5838, 0xFFC07848), c(0xFFD8A878, 0xFFC8905A, 0xFFE4C890, 0xFFB87850, 0xFFE8D0A8),
                    c(0xFFA0603A, 0xFF8E4A2E, 0xFF3E6A6A), c(0xFFE4C890, 0xFF40A0A0, 0xFFE8B8A0, 0xFFD8A878),
                    c(0xFFD8A878, 0xFFC8905A, 0xFFE4C890), 0xFFC8A880, 2,
                    "Adobe Compound", "Spanish Colonial Hacienda", "Taqueria Plaza", "Bazaar", "Rock Garden", "Domed Basilica"),
            new Theme("Highway Motels", b(City.DT_INDUSTRIAL, City.DT_MIDTOWN), true,
                    c(0xFF7E5A3A, 0xFF5A6E7E, 0xFFB86A3E, 0xFF8A8478), c(0xFFE8D8B0, 0xFF60B0B0, 0xFFE08050, 0xFFF0E8D8),
                    c(0xFF3A8AB0, 0xFFD85A3A, 0xFFE0A030), c(0xFFF0E8D8, 0xFF60C0C0, 0xFFF08060, 0xFFF8D860),
                    c(0xFFC8B89A, 0xFFD8C8A8, 0xFFB8A070), 0xFFC8B898, 0,
                    "Motel", "Retro Diner", "UFO Diner", "Giant Donut Drive-In", "Space Rocket Display", "Drive-in Cinema",
                    "Gas Station Superstore", "Car Wash", "Burger Drive-Thru", "Mobile Home Park"),
            new Theme("Desert Modern", b(City.DT_SUBURB, City.DT_PARKSIDE), false,
                    c(0xFFA0603A, 0xFFB86A3E, 0xFF9A8A70, 0xFFC89060), c(0xFFE4C890, 0xFFD0A878, 0xFFE8D8B8, 0xFFC8A070, 0xFFF0E0C8),
                    c(0xFFA0603A, 0xFF9A8A70), c(0xFFE4C890, 0xFFD0A878),
                    c(0xFFE4C890, 0xFFD0A878), 0xFFD8C8A8, 0,
                    "Adobe Compound", "Spanish Colonial Hacienda", "Mid-Century Modern Homes", "Taqueria Plaza", "Rock Garden"),
            new Theme("Adobe Lanes", b(City.DT_OLDTOWN, City.DT_MIDTOWN), false,
                    c(0xFFA0603A, 0xFFB86A3E, 0xFF8E4A2E), c(0xFFD8A878, 0xFFC8905A, 0xFFE4C890, 0xFFB87850),
                    c(0xFFA0603A, 0xFF3E6A6A), c(0xFFE4C890, 0xFFD8A878, 0xFF40A0A0),
                    c(0xFFD8A878, 0xFFC8905A, 0xFFE4C890), 0xFFC8A880, 0,
                    "Adobe Compound", "Taqueria Plaza", "Bazaar", "Spanish Colonial Hacienda"),
            new Theme("Desert Yards", b(City.DT_INDUSTRIAL), false,
                    c(0xFF8A7A62, 0xFF9A6A48, 0xFF7E848A, 0xFFA8A098), c(0xFFC8B89A, 0xFFB8946A, 0xFFA8A098, 0xFFD8C8A8),
                    c(0xFF8A7A62, 0xFF7E848A), c(0xFFC8B89A, 0xFFD8C8A8),
                    c(0xFFC8B89A, 0xFFB8946A, 0xFFA8A098, 0xFFD0C0A0), 0xFFC0B090, 0,
                    "Fuel Depot", "Wind & Solar Farm", "Water Tower", "Self-Storage Units", "Recycling Centre"),
            new Theme("Stucco Sprawl", b(City.DT_SUBURB, City.DT_PARKSIDE, City.DT_MIDTOWN), false,
                    c(0xFFB8643A, 0xFFA8583A, 0xFFE8E2D6, 0xFFC87A4A, 0xFFD8D0C0), c(0xFFF4EEE2, 0xFFEAD8BC, 0xFFF0D8C0, 0xFFE8C8A0, 0xFFF8F4EC),
                    c(0xFFB8643A, 0xFFE8E2D6), c(0xFFF4EEE2, 0xFFEAD8BC, 0xFF70B8B0),
                    c(0xFFF4EEE2, 0xFFEAD8BC, 0xFFE8C8A0), 0xFFDCCCAE, 0,
                    "Mid-Century Modern Homes", "Courtyard Bungalows", "Geodesic Homes", "Mobile Home Park", "Rock Garden"),
    };

    /**
     * ((25.0) Theme Park Island's own: the park itself in the middle, pastel resort hotels and villas along the beaches,
     * a marina village of shops and cafes, palm-shaded lanes of beach houses, and the staff town on the mainland. Only
     * that map uses them (and every district gets one).
     */
    static final Theme[] RESORT_THEMES = {
            new Theme("Adventure Park", b(City.DT_DOWNTOWN, City.DT_MIDTOWN, City.DT_OLDTOWN, City.DT_SUBURB, City.DT_INDUSTRIAL,
                    City.DT_CAMPUS, City.DT_PARKSIDE), true,
                    c(0xFFC8483C, 0xFF3A8AC0, 0xFFE8B040, 0xFF4EA868), c(0xFFF4ECDC, 0xFFF0D8E0, 0xFFE0F0F0),
                    c(0xFFC8483C, 0xFF3A8AC0, 0xFFE8B040), c(0xFFF4ECDC, 0xFFF8D860, 0xFF70C8D8),
                    c(0xFFF4ECDC, 0xFFF0D8E0, 0xFFE0F0F0), 0xFFE0D2B8, 3),
            new Theme("Resort Row", b(City.DT_DOWNTOWN, City.DT_MIDTOWN), true,
                    c(0xFFE8E4DC, 0xFF3E8A98, 0xFFC87A5A, 0xFFF0EEE8), c(0xFFF8F4EC, 0xFFF2DCC8, 0xFFDCEEF0, 0xFFF4E2C0, 0xFFEAD0D8),
                    c(0xFF3E8A98, 0xFFC87A5A, 0xFFE8E4DC), c(0xFFF8F4EC, 0xFF70C8C8, 0xFFF0B8A0, 0xFFF8E0A0),
                    c(0xFFF8F4EC, 0xFFDCEEF0, 0xFFF2DCC8, 0xFFE0E8F0, 0xFF9AD0D8, 0xFFF4E2C0), 0xFFE8DCC4, 2,
                    "Resort Hotel", "Tower Hotel", "Pool Club", "Beach Villas", "Surf Lodge", "Boutique Hotel", "Grand Hotel"),
            new Theme("Marina Village", b(City.DT_MIDTOWN, City.DT_DOWNTOWN, City.DT_OLDTOWN), true,
                    c(0xFF3E6E8E, 0xFFB86A4A, 0xFF4E8A7A, 0xFFE8E4DC), c(0xFFF4EEE2, 0xFFF0D8B8, 0xFFD8ECEC, 0xFFF8E8C8),
                    c(0xFF3E6E8E, 0xFFC8483C, 0xFF4E8A7A), c(0xFFF8F4EC, 0xFFF0C890, 0xFF80C8D0, 0xFFF0A0A0),
                    c(0xFFF4EEE2, 0xFFD8ECEC, 0xFFF0D8B8), 0xFFE0D6C2, 2,
                    "Surf Lodge", "Pool Club", "Boutique Hotel", "Water Park"),
            new Theme("Beach Lanes", b(City.DT_MIDTOWN, City.DT_SUBURB), false,
                    c(0xFF4E8A9A, 0xFFC8805A, 0xFFE8E4DC, 0xFF6A9A6A), c(0xFFF8F4EC, 0xFFF2E0C0, 0xFFD8EEF0, 0xFFF4D8D0, 0xFFE8F0D8),
                    c(0xFF4E8A9A, 0xFFC8805A), c(0xFFF8F4EC, 0xFF80C8D0, 0xFFF0B8A0),
                    c(0xFFF8F4EC, 0xFFD8EEF0, 0xFFF2E0C0), 0xFFE4DCC8, 0,
                    "Beach Villas", "Surf Lodge"),
            new Theme("Palm Gardens", b(City.DT_SUBURB, City.DT_PARKSIDE, City.DT_MIDTOWN, City.DT_DOWNTOWN, City.DT_OLDTOWN,
                    City.DT_INDUSTRIAL, City.DT_CAMPUS), false,
                    c(0xFFC8805A, 0xFF4E8A9A, 0xFFB8A070, 0xFF8AAA70), c(0xFFF8F0E0, 0xFFF0E4C8, 0xFFE0F0EC, 0xFFF8E0D8),
                    c(0xFFC8805A, 0xFF4E8A9A), c(0xFFF8F0E0, 0xFFF0E4C8),
                    c(0xFFF8F0E0, 0xFFE0F0EC), 0xFFE0D8C4, 0,
                    "Beach Villas"),
            new Theme("Staff Town", b(City.DT_SUBURB, City.DT_MIDTOWN, City.DT_DOWNTOWN, City.DT_OLDTOWN, City.DT_INDUSTRIAL,
                    City.DT_CAMPUS, City.DT_PARKSIDE), true,
                    c(0xFF6A6E74, 0xFF7E5A44, 0xFF5A6A7A, 0xFF8A7E6A), c(0xFFE0D8C8, 0xFFD0C8B8, 0xFFC8D0D4, 0xFFE4D4BC),
                    c(0xFF5A6A7A, 0xFF7E5A44), c(0xFFE0D8C8, 0xFFC8D0D4),
                    c(0xFFE0D8C8, 0xFFC8D0D4, 0xFFD0C8B8), 0, 0,
                    "Staff Housing", "Staff Clinic"),
    };

    /** Gives each district of the city a theme that suits it; the named ones once each, and they rename it. */
    static void assign(City city, long seed) {
        Random r = new Random(seed * 7349L + 38);
        if (city.cfg.desert()) {
            assignDesert(city, r);
            return;
        }
        if (city.cfg.themePark()) {
            assignResort(city, r);
            return;
        }
        if (city.cfg.spaceCenter()) {
            assignSpace(city, r);
            return;
        }
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

    /** ((22.9) Desert City: every district one of the desert themes; the heart of town the Strip. */
    private static void assignDesert(City city, Random r) {
        boolean[] used = new boolean[DESERT_THEMES.length];
        for (int k = 0; k < city.districts.size(); k++) {
            City.District d = city.districts.get(k);
            int bit = 1 << d.type, n = 0, pick = -1;
            for (int t = 0; t < DESERT_THEMES.length; t++) {
                Theme th = DESERT_THEMES[t];
                if ((th.types & bit) == 0 || (th.named && used[t]) || (th.named && !d.labelled())) continue;
                if (r.nextInt(++n) == 0) pick = t;
            }
            // (The middle of town is the Strip.)
            if (k == 0 && !used[0]) pick = 0;
            // (Anything else - the country round town - the stucco sprawl.)
            if (pick < 0) pick = DESERT_THEMES.length - 1;
            d.theme = DESERT_THEMES[pick];
            if (d.theme.named) {
                used[pick] = true;
                d.name = d.theme.name;
            }
        }
    }

    /**
     * ((25.0) Theme Park Island: the middle of the island the park, the first district on the mainland the staff town,
     * every other one a resort theme that suits it (Palm Gardens for the rest).
     */
    private static void assignResort(City city, Random r) {
        int last = RESORT_THEMES.length - 1;
        boolean[] used = new boolean[RESORT_THEMES.length];
        for (int k = 0; k < city.districts.size(); k++) {
            City.District d = city.districts.get(k);
            int bit = 1 << d.type, n = 0, pick = -1;
            for (int t = 1; t < last; t++) {
                Theme th = RESORT_THEMES[t];
                if ((th.types & bit) == 0 || (th.named && used[t]) || (th.named && !d.labelled())) continue;
                if (r.nextInt(++n) == 0) pick = t;
            }
            if (k == 0) pick = 0;
            else if (k == 1) pick = last;
            if (pick < 0) pick = last - 1;
            d.theme = RESORT_THEMES[pick];
            if (d.theme.named) {
                used[pick] = true;
                d.name = d.theme.name;
            }
        }
    }

    /**
     * ((26.0) The Space Center's own: the space center itself, and round it the staff town - the homes of the rocket
     * engineers along Astronaut Row, the main street of Rocket Town, the contractors' offices and works, and the plain
     * pastel homes of the space coast everywhere else. Only that map uses them (and every district gets one).
     */
    static final Theme[] SPACE_THEMES = {
            new Theme("Space Center", b(City.DT_DOWNTOWN, City.DT_MIDTOWN, City.DT_OLDTOWN, City.DT_SUBURB, City.DT_INDUSTRIAL,
                    City.DT_CAMPUS, City.DT_PARKSIDE), true,
                    c(0xFFD8D6D0, 0xFFB8B6B0, 0xFF8A8C90), c(0xFFF2F2EE, 0xFFE0E0DA),
                    c(0xFF1E3C82, 0xFFB8B6B0), c(0xFFF2F2EE, 0xFFE0E0DA),
                    c(0xFFF2F2EE, 0xFFD8DCE0), 0xFFC8C4BA, 0),
            new Theme("Astronaut Row", b(City.DT_SUBURB, City.DT_PARKSIDE, City.DT_MIDTOWN), true,
                    c(0xFF5A6A7A, 0xFF8A6A4A, 0xFF6A7A6A, 0xFFB8A888), c(0xFFF4F0E6, 0xFFE8EEF2, 0xFFF0E4CC, 0xFFDCE8E0),
                    c(0xFF3E5A7A, 0xFFB86A4A), c(0xFFF4F0E6, 0xFFE8EEF2),
                    c(0xFFF4F0E6, 0xFFE8EEF2, 0xFFDCE8E0), 0, 0),
            new Theme("Rocket Town", b(City.DT_MIDTOWN, City.DT_DOWNTOWN, City.DT_OLDTOWN), true,
                    c(0xFF3E4E6E, 0xFFB83A2E, 0xFF5A6A7A, 0xFFE8E4DC), c(0xFFF2EEE4, 0xFFE0E8F0, 0xFFF0DCC0, 0xFFE8D8D8),
                    c(0xFF1E3C82, 0xFFC8382E, 0xFF3E4E6E, 0xFFE8C040), c(0xFFF4F2EC, 0xFFE0E8F0, 0xFFF8E0A0, 0xFFF0B8A8),
                    c(0xFFF2EEE4, 0xFFE0E8F0, 0xFFD8DCE0), 0xFFD0CCC2, 3),
            new Theme("Contractor Park", b(City.DT_INDUSTRIAL, City.DT_CAMPUS, City.DT_MIDTOWN), true,
                    c(0xFF6A6E74, 0xFF8A8C90, 0xFF5A6A7A), c(0xFFE0E0DA, 0xFFD0D4D8, 0xFFE8E4DC),
                    c(0xFF5A6A7A, 0xFF6A6E74), c(0xFFE0E0DA, 0xFFD0D4D8),
                    c(0xFFD8DCE0, 0xFF9AB0C4, 0xFFC8D0D8, 0xFFE0E0DA), 0xFFC8C8C4, 0),
            new Theme("Space Coast Homes", b(City.DT_SUBURB, City.DT_PARKSIDE, City.DT_MIDTOWN, City.DT_DOWNTOWN, City.DT_OLDTOWN,
                    City.DT_INDUSTRIAL, City.DT_CAMPUS), false,
                    c(0xFF8A7A6A, 0xFF5A6A7A, 0xFFB8A888, 0xFF6A8A8A), c(0xFFF6F2E8, 0xFFE4EEF0, 0xFFF4E6D0, 0xFFE8F0E0, 0xFFF0E0E0),
                    c(0xFF5A6A7A, 0xFF8A7A6A), c(0xFFF6F2E8, 0xFFE4EEF0),
                    c(0xFFF6F2E8, 0xFFE4EEF0, 0xFFE8F0E0), 0, 0),
    };

    /**
     * ((26.0) The Space Center: the first district the space center itself, every other one a theme of the staff town
     * that suits it (the space coast's homes for the rest).
     */
    private static void assignSpace(City city, Random r) {
        int last = SPACE_THEMES.length - 1;
        boolean[] used = new boolean[SPACE_THEMES.length];
        for (int k = 0; k < city.districts.size(); k++) {
            City.District d = city.districts.get(k);
            int bit = 1 << d.type, n = 0, pick = -1;
            for (int t = 1; t < last; t++) {
                Theme th = SPACE_THEMES[t];
                if ((th.types & bit) == 0 || used[t] || !d.labelled()) continue;
                if (r.nextInt(++n) == 0) pick = t;
            }
            if (k == 0) pick = 0;
            if (pick < 0) pick = last;
            d.theme = SPACE_THEMES[pick];
            if (d.theme.named) {
                used[pick] = true;
                d.name = d.theme.name;
            }
        }
    }

    // ((27.5) Map version 40: every city its own colours, and the new kinds of neighbourhood their own look.)
    private static final int ALL = b(City.DT_DOWNTOWN, City.DT_MIDTOWN, City.DT_OLDTOWN, City.DT_SUBURB, City.DT_INDUSTRIAL,
            City.DT_CAMPUS, City.DT_PARKSIDE);

    /** The homes of a whole city, one of these: where no district has a look of its own, the city's own look. */
    static final Theme[] PALETTES = {
            new Theme("Terracotta & Stucco", ALL, false,
                    c(0xFFB0583A, 0xFFC0683E, 0xFFA04E34, 0xFFC87A4A), c(0xFFF0E2C4, 0xFFE8D0A0, 0xFFF4EADA, 0xFFE0C48E),
                    c(0xFFB0583A, 0xFFA04E34), c(0xFFF0E2C4, 0xFFE8C890), c(0xFFF0E2C4, 0xFFE0C48E, 0xFFE8D0A0), 0xFFD8C8B0, 0),
            new Theme("Slate & Stone", ALL, false,
                    c(0xFF3E4650, 0xFF4A525C, 0xFF353C44, 0xFF56606A), c(0xFFB8B4AA, 0xFFA8A49A, 0xFFC8C2B4, 0xFF9C9890),
                    c(0xFF3E4650, 0xFF353C44), c(0xFFB8B4AA, 0xFFC8C2B4), c(0xFFB8B4AA, 0xFFA8A49A), 0xFFA8A8A4, 0),
            new Theme("Red Brick", ALL, false,
                    c(0xFF4A3A34, 0xFF5A3D35, 0xFF3E3A3A, 0xFF6A4A3A), c(0xFFA04A34, 0xFFB0583E, 0xFF8E4030, 0xFFB86A50),
                    c(0xFF4A3A34, 0xFF2E2E30), c(0xFFA04A34, 0xFFB86A50), c(0xFFA04A34, 0xFF8E4030, 0xFFC8B8A0), 0, 0),
            new Theme("Painted Clapboard", ALL, false,
                    c(0xFF6A6E74, 0xFF8A8E94, 0xFF4E5A6A, 0xFF7A6A5A), c(0xFF8AB0D8, 0xFFF0D870, 0xFF9AD0B0, 0xFFF0A8A8, 0xFFF4F0E8, 0xFFC8A8E0),
                    c(0xFF6A6E74, 0xFF4E5A6A), c(0xFF8AB0D8, 0xFFF0D870, 0xFFF0A8A8), c(0xFFF4F0E8, 0xFF8AB0D8, 0xFF9AD0B0), 0, 0),
            new Theme("White Modern", ALL, false,
                    c(0xFFD8DCDE, 0xFFC8CCD0, 0xFFB8BEC4, 0xFF9AA2AA), c(0xFFF8F8F6, 0xFFEEF0F0, 0xFFE4E6E8, 0xFFD8DCE0),
                    c(0xFFD8DCDE, 0xFF9AA2AA), c(0xFFF8F8F6, 0xFFE4E6E8), c(0xFFF8F8F6, 0xFFD8E4EC, 0xFFB8D0E0), 0xFFE0E2E2, 0),
            new Theme("Timber & Shingle", ALL, false,
                    c(0xFF3E5A3A, 0xFF5A4A3A, 0xFF4A3E34, 0xFF2E4A40), c(0xFFB08A60, 0xFF9A7650, 0xFFC8A478, 0xFF8A6A48, 0xFFD8C8A8),
                    c(0xFF3E5A3A, 0xFF5A4A3A), c(0xFFB08A60, 0xFFC8A478), c(0xFFB08A60, 0xFFD8C8A8), 0, 0),
    };

    /** The new kinds of neighbourhood's own looks, by City.NB_* less City.NB_TRAILERS. */
    static final Theme[] NB_THEMES = {
            new Theme("Trailer Park", ALL, false,
                    c(0xFFC0C4C8, 0xFFA8B0B8, 0xFFD0D0C8, 0xFF9AA0A6), c(0xFFF0EEE6, 0xFFE0E8F0, 0xFFF0E0C8, 0xFFD8E8D8, 0xFFE8D8D8),
                    c(0xFFC0C4C8), c(0xFFF0EEE6), c(0xFFF0EEE6), 0, 0),
            new Theme("Tower Estate", ALL, false,
                    c(0xFF7A7E82, 0xFF6A6E72, 0xFF8A8E90), c(0xFFB8B4AC, 0xFFC8C4BA, 0xFFA8A49C, 0xFFD0C8B8),
                    c(0xFF6A6E72), c(0xFFC8C4BA), c(0xFFB8B4AC, 0xFFC8C4BA), 0xFFB0B0AC, 0),
            new Theme("Market Quarter", ALL, false,
                    c(0xFF8E2A22, 0xFF2E6E4E, 0xFFB8862A, 0xFF7A1E24), c(0xFFE8D8B0, 0xFFC84A3A, 0xFFE0C890, 0xFFF0E0B0),
                    c(0xFF8E2A22, 0xFFB8862A, 0xFF2E6E4E), c(0xFFC8402E, 0xFFE8C860, 0xFFF0E0B0, 0xFFD85A3A),
                    c(0xFFE0C890, 0xFFC84A3A), 0xFFB87A5A, 1, "Night Market", "Asian Supermarket", "Noodle Bar Arcade", "Dim Sum Palace"),
            new Theme("College Quad", ALL, false,
                    c(0xFF3E4650, 0xFF2F3A2E, 0xFF4E5560), c(0xFFA8543A, 0xFF9A4A38, 0xFFC8B8A0),
                    c(0xFF3E4650, 0xFF2F3A2E), c(0xFFA8543A, 0xFFC8B8A0), c(0xFFA8543A, 0xFF9A4A38, 0xFFC8B8A0, 0xFFB86A48), 0xFFC0B4A0, 3,
                    "University Quad", "Student Halls", "Public Library", "Music Conservatory"),
            new Theme("The Circus", ALL, false,
                    c(0xFF3E4650, 0xFF4A525C, 0xFF56606A), c(0xFFF2EAD8, 0xFFEADFC8, 0xFFF6F0E2, 0xFFE4D6BC),
                    c(0xFF3E4650), c(0xFFF2EAD8, 0xFFE4D6BC), c(0xFFF2EAD8, 0xFFEADFC8), 0xFFD8D0BE, 2),
            new Theme("Terraced Rows", ALL, false,
                    c(0xFF4A525C, 0xFF3E4650, 0xFF5A3D35), c(0xFF9A4A38, 0xFFA8543A, 0xFFB86A48, 0xFF8E4434, 0xFFD8C8A8),
                    c(0xFF3E4650), c(0xFFA8543A, 0xFFE8D8A0), c(0xFFA8543A, 0xFF9A4A38), 0, 0, "Worker Cottages", "Brownstone Row", "Irish Pub"),
            new Theme("Loft Quarter", ALL, false,
                    c(0xFF5E8A4A, 0xFF4E7A3E, 0xFF3A3A3E, 0xFF6E9A5A), c(0xFF8E4434, 0xFF9A6A50, 0xFF7C6A5A, 0xFFA8543A),
                    c(0xFF3A3A3E, 0xFF5E8A4A), c(0xFF9A6A50, 0xFFA8543A), c(0xFF8E4434, 0xFF9A6A50, 0xFF7C6A5A, 0xFFA8543A), 0xFF908478, 2,
                    "Riverside Lofts", "Printing Works", "Glassworks"),
    };

    /** ((27.5) The city's own look for every district without one of its own (on map version 40). */
    static void paint(City city, int palette) {
        if (city.cfg.desert() || city.cfg.themePark() || city.cfg.spaceCenter()) return;
        Theme p = PALETTES[palette];
        for (City.District d : city.districts)
            if (d.theme == null && d.type != City.DT_DOWNTOWN && d.type != City.DT_INDUSTRIAL) d.theme = p;
    }

    static int pick(int[] cols, Random r) {
        return cols[r.nextInt(cols.length)];
    }

    static boolean any(List<City.District> ds) {
        for (City.District d : ds) if (d.theme != null) return true;
        return false;
    }
}
