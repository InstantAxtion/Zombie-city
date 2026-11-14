package com.instantaxtion.zombiesandbox;

import java.util.Random;

/**
 * Where the city is: the look of its buildings, its street, town and people's names, the road markings and the
 * police cars. (Since 11.3 the game is set in the USA only: the other countries and their code are gone.)
 */
final class Country {
    static final int USA = 0;
    static final String[] NAMES = {"USA"};
    static final String[] INFO = {"Wide avenues with yellow centre lines, clapboard houses and black-and-white cruisers."};

    /** The country of the game being played (for people's names). */
    static volatile Country current;

    final int id = USA;
    /** The highway route shield's colour. */
    final int shieldColor = 0xFF1E3A8A;
    /** Colour of the centre line on ordinary streets. */
    final int centreLine;
    final int[] houseRoofs, houseWalls, shopRoofs, shopWalls, apartmentWalls;
    /** ((22.6) Map version 37: many more colours for homes, shops, flats and cars, and more names for places. */
    final int[] houseRoofs37 = {0xFF8E3B2E, 0xFF6E4A3A, 0xFF4E5560, 0xFF7A5A48, 0xFF9A5B3C, 0xFF5A3D35, 0xFF3E4A5C,
            0xFF2F3A2E, 0xFF5E2A2A, 0xFF7C6E5E, 0xFF4A5A4A, 0xFF8A7A6A, 0xFF3A3A40, 0xFFA0603A, 0xFF5A6E7E, 0xFF6A4E6E,
            0xFF2E4E5E, 0xFF7E3A1E, 0xFF9A8A70, 0xFF46505A};
    final int[] houseWalls37 = {0xFFD8CBB0, 0xFFC9B79C, 0xFFE0D6C4, 0xFFB8A48A, 0xFFA7B4BE, 0xFFC7A9A0, 0xFFF2EEE4,
            0xFFE8D8A0, 0xFFB8C8A0, 0xFFA8C0D8, 0xFFE0B8B0, 0xFFC8D8C8, 0xFFD8C0E0, 0xFF9AB0A0, 0xFFB86A50, 0xFFE4C890,
            0xFF8FA8C0, 0xFFD0A878, 0xFFEAE0D0, 0xFF7E9A88, 0xFFF0D8C8, 0xFFC0B0A0};
    final int[] shopRoofs37 = {0xFF8C5A4A, 0xFF5A6E8C, 0xFF7E7A5C, 0xFF6E5A7E, 0xFF8A6A3E, 0xFF4F6F66, 0xFF3E4A5A,
            0xFF9A4A3A, 0xFF5E7A4A, 0xFF7A4A5E, 0xFF4A6A8A, 0xFF8A7A4A, 0xFF5A5A5E, 0xFF6E3E2E};
    final int[] shopWalls37 = {0xFFE0C9A6, 0xFFB9C6D2, 0xFFD8B8A8, 0xFFC9D6B8, 0xFFE8DCC8, 0xFFB8A8C8, 0xFFF0E0B0,
            0xFFA8D0C8, 0xFFE8B8A0, 0xFFC8C0E8, 0xFFD8E8C0, 0xFFB86A50, 0xFF8FB0C8, 0xFFE0D0F0, 0xFFF2F0EA, 0xFFC8A070};
    final int[] apartmentWalls37 = {0xFFC9B8A0, 0xFFB5A08A, 0xFFD4C8B8, 0xFFA89484, 0xFFBFB0C0, 0xFFA8B8C0, 0xFF9AA890,
            0xFFD8C8A0, 0xFFB87860, 0xFFE0D8D0, 0xFF8898A8, 0xFFC0A890, 0xFFA0B0A0, 0xFFD0B8B0};
    final int[] carColors37 = {0xFFB03A2E, 0xFF2E5FB0, 0xFFE0E0E0, 0xFF222428, 0xFFD4A21C, 0xFF3C8A4E, 0xFF8A8F96,
            0xFF6B2E8A, 0xFFE07A2E, 0xFFF4F4F0, 0xFF1E2E4A, 0xFF7A1E24, 0xFF5E6E7A, 0xFF2E5E4E, 0xFFC8B89A, 0xFF4E3A2E,
            0xFF6A9AC8, 0xFFE8C840, 0xFF9AA4A8, 0xFF3A3E44, 0xFFC84A6A, 0xFF4AA0A0};
    final String[] streets37 = {"Main", "Oak", "Pine", "Elm", "Maple", "Cedar", "Lake", "Hill", "Park", "Church", "Market",
            "Mill", "King", "Queen", "Walnut", "Spruce", "Birch", "Chestnut", "Harbor", "Union", "Rose", "Ash", "Willow",
            "Station", "River", "Bridge", "High", "Garden", "Orchard", "Victoria", "Albert", "Greene", "Water", "Grove",
            "Meadow", "Forest", "Bay", "Summit", "Liberty", "Franklin", "Madison", "Jackson", "Lincoln", "Hawthorn",
            "Laurel", "Poplar", "Sycamore", "Holly", "Juniper", "Linden", "Magnolia", "Dogwood", "Cypress", "Redwood",
            "Hickory", "Beech", "Alder", "Aspen", "Sequoia", "Monroe", "Adams", "Grant", "Wilson", "Hayes", "Pierce",
            "Taylor", "Polk", "Tyler", "Fillmore", "Cleveland", "Sherman", "Clay", "Webster", "Hamilton", "Jefferson",
            "Washington", "Broadway", "Canal", "Commerce", "Industrial", "College", "University", "Academy", "Temple",
            "Chapel", "Prospect", "Pleasant", "Sunset", "Sunrise", "Highland", "Valley", "Ridge", "Crest", "Brook",
            "Spring", "Fountain", "Mulberry", "Cherry", "Peach", "Plum", "Apple", "Vine", "Ivy", "Fern", "Clover",
            "Heather", "Thistle", "Lilac", "Tulip", "Daisy", "Primrose", "Foxglove", "Falcon", "Heron", "Sparrow",
            "Robin", "Cardinal", "Bluebird", "Eagle", "Hawk", "Raven", "Lark"};
    final String[] churches37 = {"St. Mary's Church", "St. Luke's Church", "St. Peter's Church", "Grace Church",
            "St. Anne's Church", "Trinity Church", "St. Mark's Church", "Holy Cross Church", "First Baptist Church",
            "St. Joseph's Church", "Christ Church", "Calvary Chapel", "St. Andrew's Church", "Bethel Church",
            "Our Lady of Hope", "St. Francis Church", "Good Shepherd Church", "Faith Chapel", "St. Paul's Church",
            "Emmanuel Church", "St. Michael's Church", "Unity Church"};
    final String[] schools37 = {"Lincoln High", "Westside Elementary", "Roosevelt Middle School", "Jefferson High",
            "Oakwood Academy", "Hamilton Elementary", "Washington High", "Kennedy Middle School", "Maple Grove Elementary",
            "Eastview High", "Riverside Academy", "Franklin Elementary", "Madison Middle School", "Hillcrest High",
            "Cedar Park Elementary", "Central High", "Northgate Academy", "Pine Ridge Elementary", "Sunnyvale Middle School",
            "Brookside Elementary"};
    final String[] markets37 = {"FreshMart", "ValueFoods", "Corner Grocer", "SuperSaver", "GreenBasket", "MegaMart",
            "Harvest Foods", "PricePoint", "Daily Fresh", "FoodTown", "Market Square Grocers", "QuickStop", "Golden Grain",
            "Sunrise Market", "Family Foods", "Shop & Save"};
    final String[] pharmacies37 = {"CityCare Pharmacy", "Main Street Drugs", "HealthPlus", "Corner Pharmacy",
            "WellBeing Pharmacy", "Rx Express", "Family Drug Co.", "Apex Pharmacy", "Greenleaf Chemist", "MedPoint",
            "Sunrise Pharmacy", "CarePlus Drugs"};
    /** Police cars: body colour, door panels, a contrasting roof (0 for none), and a chequered band down the sides. */
    int cruiserBody, cruiserDoor, cruiserRoof;
    boolean cruiserChecks;
    /** The two colours of the chequered band. */
    int checkA, checkB;
    final String[] streets, mainWords, localWords;
    final String ringRoad;
    final String[] townStart, townEnd, districtWords, hamletEnds;
    final String[] churches, schools, markets, pharmacies, gunStores, bases;
    final String hospital, fireStation;
    String policeStation;
    final String[] first, last, dogs;
    /** Pavement, grass and plaza colours (0 keeps the usual), and roofs/walls for offices and old buildings (null keeps the usual). */
    int pavement, grass, plaza;
    /** The everyday traffic: a weighted list of Fleet car models, their paint, and the local taxis and buses. */
    int[] carModels, carColors;
    int taxiColor, busColor;
    int[] officeRoofs, officeWalls, oldWalls, oldRoofs;

    private static Country usa;

    /** The country (whatever the id: there is only the one now). */
    static Country get(int id) {
        synchronized (NAMES) {
            if (usa == null) usa = new Country();
            return usa;
        }
    }

    /** A street name: "Oak St". */
    String streetName(String base, boolean main, Random rnd) {
        String[] types = main ? mainWords : localWords;
        String type = types[rnd.nextInt(types.length)];
        if (type.isEmpty()) return base;
        return base + " " + type;
    }

    /** The sign over a gun store's door. */
    String gunSign() {
        return "GUNS";
    }

    // ------------------------------------------------------------------ out in the country

    /** Names for the country's wild places: its mountains, lakes, falls and parks. */
    String[] natureWords() {
        return new String[]{"Bear", "Eagle", "Pine", "Cedar", "Willow", "Elk", "Hawk", "Silver", "Crystal", "Thunder", "Wolf", "Aspen", "Granite", "Misty", "Echo", "Raven"};
    }

    /** The nature park ("Yellow Pine State Park"), or with no word, what its visitor centre is called. */
    String parkName(String word) {
        return word == null ? "Visitor Center" : word + " State Park";
    }

    String mountainName(String word) {
        return "Mount " + word;
    }

    String lakeName(String word) {
        return "Lake " + word;
    }

    String fallsName(String word) {
        return word + " Falls";
    }

    /** The park rangers' station. */
    String rangerStationName(String park) {
        return "Ranger Station" + (park != null ? ", " + park : "");
    }

    /** A fire lookout tower. */
    String lookoutName(String word) {
        return word + " Fire Lookout";
    }

    String campName(String word) {
        return word + " Campground";
    }

    /** The SWAT team: {its name, what its officers are called}. */
    static String[] swat() {
        return new String[]{"SWAT", "SWAT officer"};
    }

    /** The National Guard: {its armory, the force, who calls it out, the armory's roof sign}. */
    static String[] guard() {
        return new String[]{"National Guard Armory", "National Guard", "Governor", "NATIONAL GUARD"};
    }

    /** The government buildings: {names}, then {roof signs}. */
    static String[][] gov() {
        return new String[][]{{"City Hall", "County Courthouse", "County Jail", "911 Dispatch Center", "Public Works Yard"},
                {"CITY HALL", "COURTHOUSE", "COUNTY JAIL", "911", "PUBLIC WORKS"}};
    }

    /** A sign on a roof. */
    String sign(String english) {
        return english;
    }

    String townName(Random r) {
        String a = townStart[r.nextInt(townStart.length)], b = townEnd[r.nextInt(townEnd.length)];
        // No "Champchamp".
        if (a.toLowerCase().contains(b.trim().toLowerCase()) || b.toLowerCase().contains(a.trim().toLowerCase()))
            b = townEnd[(java.util.Arrays.asList(townEnd).indexOf(b) + 1) % townEnd.length];
        return a + b;
    }

    /** A neighbourhood's name. The type is a City district type, -1 for a plain one or -2 for the countryside. */
    String districtName(String english, String word, int type) {
        return english;
    }

    private Country() {
        centreLine = 0xFFD9B43A;
        houseRoofs = new int[]{0xFF8E3B2E, 0xFF6E4A3A, 0xFF4E5560, 0xFF7A5A48, 0xFF9A5B3C, 0xFF5A3D35};
        houseWalls = new int[]{0xFFD8CBB0, 0xFFC9B79C, 0xFFE0D6C4, 0xFFB8A48A, 0xFFA7B4BE, 0xFFC7A9A0};
        shopRoofs = new int[]{0xFF8C5A4A, 0xFF5A6E8C, 0xFF7E7A5C, 0xFF6E5A7E, 0xFF8A6A3E, 0xFF4F6F66};
        shopWalls = new int[]{0xFFE0C9A6, 0xFFB9C6D2, 0xFFD8B8A8, 0xFFC9D6B8, 0xFFE8DCC8, 0xFFB8A8C8};
        apartmentWalls = new int[]{0xFFC9B8A0, 0xFFB5A08A, 0xFFD4C8B8, 0xFFA89484, 0xFFBFB0C0};
        cruiserBody = 0xFF1C1D22;
        cruiserDoor = 0xFFEDEDED;
        cruiserRoof = 0;
        cruiserChecks = false;
        streets = new String[]{"Main", "Oak", "Pine", "Elm", "Maple", "Cedar", "Lake", "Hill", "Park",
                "Church", "Market", "Mill", "King", "Queen", "Walnut", "Spruce", "Birch", "Chestnut", "Harbor", "Union",
                "Rose", "Ash", "Willow", "Station", "River", "Bridge", "High", "Garden", "Orchard", "Victoria", "Albert",
                "Greene", "Water", "Grove", "Meadow", "Forest", "Bay", "Summit", "Liberty", "Franklin", "Madison",
                "Jackson", "Lincoln", "Hawthorn", "Laurel", "Poplar", "Sycamore", "Holly", "Juniper", "Linden"};
        mainWords = new String[]{"Ave", "Blvd", "Rd", "Pkwy"};
        localWords = new String[]{"St", "St", "St", "Ln", "Way", "Pl", "Ct", "Dr"};
        ringRoad = "Ring Rd";
        townStart = new String[]{"Ash", "Maple", "Oak", "Stone", "Clay", "Wolf", "Fair", "Green", "North",
                "Silver", "Iron", "Elder", "Hollow", "Red", "Kings", "Bright", "Glen", "Hazel", "Thorn", "Mill"};
        townEnd = new String[]{"ford", "field", "ville", "wood", "ton", "bury", "ridge", "haven", "dale",
                "stead", "wick", "gate", "hurst", "moor"};
        districtWords = new String[]{"Oak", "Cedar", "Mill", "North", "South", "East", "West", "King's",
                "Linden", "Ash", "Fox", "Hazel", "Stone", "Maple", "Willow", "Brook", "Elm", "High", "Rose", "Birch"};
        hamletEnds = new String[]{" Green", " End", " Cross", " Hamlet", " Farm", " Corner", " Bridge", " Hollow"};
        churches = new String[]{"St. Mary's Church", "St. Luke's Church", "St. Peter's Church", "Grace Church",
                "St. Anne's Church", "Trinity Church", "St. Mark's Church", "Holy Cross Church"};
        schools = new String[]{"Lincoln High", "Westside Elementary", "Roosevelt Middle School",
                "Jefferson High", "Oakwood Academy", "Hamilton Elementary"};
        markets = new String[]{"FreshMart", "ValueFoods", "Corner Grocer", "SuperSaver", "GreenBasket", "MegaMart"};
        pharmacies = new String[]{"CityCare Pharmacy", "Main Street Drugs", "HealthPlus", "Corner Pharmacy"};
        gunStores = new String[]{"Liberty Guns", "Ace Firearms", "Frontier Outfitters", "Hunter's Supply", "Patriot Arms"};
        bases = new String[]{"Fort Mercer", "Camp Redstone", "Fort Kessler", "Camp Hollow", "Fort Whitmore",
                "Camp Ironwood"};
        hospital = "City Hospital";
        carModels = new int[]{0, 0, 0, 1, 1, 1, 2, 2, 2, 5, 6, 7};
        carColors = new int[]{0xFFB03A2E, 0xFF2E5FB0, 0xFFE0E0E0, 0xFF222428, 0xFFD4A21C, 0xFF3C8A4E,
                0xFF8A8F96, 0xFF6B2E8A, 0xFFE07A2E};
        taxiColor = 0xFFE8B81A;
        busColor = 0xFFF2B21A;
        policeStation = "Precinct ";
        fireStation = "Fire Station ";
        first = new String[]{"Maria", "James", "Aisha", "Tom", "Sofia", "Daniel", "Mei", "Carlos", "Emma", "Omar", "Grace",
                "Liam", "Priya", "Noah", "Zoe", "Ivan", "Hana", "Lucas", "Nora", "Sam", "Ruth", "Kwame", "Elena",
                "Ben", "Yuki", "Diego", "Chloe", "Marcus", "Leila", "Owen", "Anna", "Jamal", "Rosa", "Felix", "Ines",
                "Hugo", "Tara", "Victor", "Lena", "Eli", "Ava", "Ethan", "Isabella", "Mason", "Mia", "Logan",
                "Harper", "Jacob", "Amelia", "Jackson", "Evelyn", "Aiden", "Abigail", "Elijah", "Ella", "Mateo",
                "Scarlett", "Leo", "Aria", "Henry", "Layla", "Wyatt", "Riley", "Julian", "Nova", "Isaac", "Hazel",
                "Levi", "Violet", "Gabriel", "Aurora", "Caleb", "Savannah", "Ryan", "Audrey", "Nathan", "Brooklyn",
                "Aaron", "Bella", "Adrian", "Claire", "Miles", "Skylar", "Theo", "Lucy", "Ezra", "Paisley", "Kai",
                "Naomi", "Jonah", "Stella", "Wesley", "Maya", "Xavier", "Ruby", "Asher", "Ivy", "Roman", "Alice",
                "Silas", "Jade", "Damian", "Willow", "Andre", "Camila", "Malik", "Valentina", "Rafael", "Ximena",
                "Tariq", "Fatima", "Arjun", "Ananya", "Ravi", "Deepa", "Hiro", "Aiko", "Kenji", "Sakura", "Jin",
                "Soo-ah", "Minh", "Linh", "Thanh", "Chen", "Wei", "Ling", "Kofi", "Amara", "Tunde", "Zara", "Nia",
                "Imani", "Darius", "Kendra", "Andrei", "Katya", "Dmitri", "Olga", "Piotr", "Zofia", "Lars", "Astrid",
                "Sven", "Freya", "Pablo", "Lucia", "Javier", "Carmen", "Mateus", "Beatriz", "Giovanni", "Chiara",
                "Marco", "Giulia", "Pierre", "Amelie", "Jean", "Margot", "Sean", "Siobhan", "Connor", "Niamh",
                "Malcolm", "Fiona", "Duncan", "Isla", "Kieran", "Maeve", "Hassan", "Yara", "Karim", "Samira",
                "Yusuf", "Mariam", "Avi", "Talia", "Moshe", "Noa", "Bodhi", "Sage", "River", "Phoenix", "Dakota",
                "Rowan", "Quinn", "Reese", "Frank", "Dorothy", "Walter", "Betty", "Harold", "Edith", "Earl", "Mabel",
                "Gus", "Pearl", "Rex", "Opal", "Hank", "June", "Clint", "Dolores", "Vince", "Gloria", "Ray", "Wanda"};
        last = new String[]{"Lopez", "Smith", "Khan", "Nguyen", "Brown", "Garcia", "Kim", "Okafor", "Miller", "Rossi", "Novak",
                "Silva", "Cohen", "Walker", "Tanaka", "Murphy", "Patel", "Jensen", "Moreau", "Haddad", "Clarke",
                "Santos", "Weber", "Reyes", "Hughes", "Ali", "Kowalski", "Park", "Diaz", "Stone", "Johnson",
                "Williams", "Jones", "Davis", "Martinez", "Hernandez", "Gonzalez", "Wilson", "Anderson", "Thomas",
                "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson", "White", "Harris", "Sanchez",
                "Ramirez", "Lewis", "Robinson", "Young", "Allen", "King", "Wright", "Scott", "Torres", "Hill",
                "Flores", "Green", "Adams", "Nelson", "Baker", "Hall", "Rivera", "Campbell", "Mitchell", "Carter",
                "Roberts", "Phillips", "Evans", "Turner", "Parker", "Collins", "Edwards", "Morris", "Rogers", "Reed",
                "Cook", "Morgan", "Bell", "Cooper", "Richardson", "Cox", "Howard", "Ward", "Peterson", "Gray",
                "Ramos", "James", "Watson", "Brooks", "Kelly", "Sanders", "Price", "Bennett", "Wood", "Barnes",
                "Ross", "Henderson", "Coleman", "Jenkins", "Perry", "Powell", "Long", "Patterson", "Flynn",
                "Washington", "Butler", "Simmons", "Foster", "Gonzales", "Bryant", "Alexander", "Russell", "Griffin",
                "Hayes", "Myers", "Ford", "Hamilton", "Graham", "Sullivan", "Wallace", "Woods", "West", "Cole",
                "Jordan", "Owens", "Reynolds", "Fisher", "Ellis", "Harrison", "Gibson", "McDonald", "Cruz",
                "Marshall", "Ortiz", "Gomez", "Murray", "Freeman", "Wells", "Webb", "Simpson", "Stevens", "Tucker",
                "Porter", "Hunter", "Hicks", "Crawford", "Henry", "Boyd", "Mason", "Warren", "Dixon", "Burns",
                "Gordon", "Shaw", "Holmes", "Rice", "Robertson", "Hunt", "Black", "Daniels", "Palmer", "Mills",
                "Nichols", "Grant", "Knight", "Ferguson", "Rose", "Hudson", "Spencer", "Wagner", "Becker", "Schmidt",
                "Fischer", "Larsen", "Nilsen", "Andersson", "Lindqvist", "Ivanov", "Petrov", "Volkov", "Horvat",
                "Popescu", "Nowak", "Wojcik", "Yilmaz", "Demir", "Nakamura", "Suzuki", "Watanabe", "Sato", "Chen",
                "Wang", "Liu", "Zhang", "Huang", "Tran", "Le", "Pham", "Choi", "Jung", "Kang", "Sharma", "Gupta",
                "Singh", "Reddy", "Mensah", "Adeyemi", "Mwangi", "Abdullah", "Hassan", "Rahman", "Farouk", "Levi",
                "Friedman", "Goldberg", "O'Brien", "O'Connor", "Byrne", "MacLeod", "Fraser", "Dubois", "Lefevre",
                "Fontaine", "Bianchi", "Romano", "Ferrari", "Costa", "Pereira", "Oliveira", "Fernandes", "Castillo",
                "Vargas", "Morales", "Navarro", "Delgado"};
        dogs = new String[]{"Rex", "Bella", "Max", "Luna", "Buddy", "Daisy", "Rocky", "Molly", "Scout", "Pepper", "Bear",
                "Rosie", "Duke", "Nala", "Biscuit", "Ziggy", "Charlie", "Cooper", "Lucy", "Sadie", "Tucker",
                "Bailey", "Maggie", "Zeus", "Lola", "Bentley", "Coco", "Winston", "Penny", "Milo", "Ruby", "Oliver",
                "Stella", "Jax", "Gizmo", "Hank", "Ginger", "Rusty", "Shadow", "Bandit", "Ranger", "Copper",
                "Diesel", "Ellie", "Gus", "Harley", "Jasper", "Kona", "Louie", "Mocha", "Nova", "Oreo", "Peanut",
                "Remy", "Sasha", "Thor", "Waffles", "Yoshi", "Zoe", "Bruno", "Chester", "Dexter", "Frankie", "Honey",
                "Indy", "Juno", "Kobe", "Lady", "Mango", "Noodle", "Pickles", "Queenie", "Riley", "Sparky", "Teddy",
                "Murphy"};
        agencies();
    }

    /** Kinds of tree (City.treeKind): broadleaf and pine. */
    static final byte TK_BROAD = 0, TK_PINE = 1;
    /**
     * The land (cities built since 10.10): how hilly it is, how big its mountains are, how wooded, what share of
     * the woods are pines, how many lakes, how far up the mountains the snow starts (as a share of the highest
     * peak), and the trees of the lowlands.
     */
    final float hills = 1f, mountains = 1f, forest = 1f, pineShare = 0.5f, lakes = 1f, snowLine = 0.86f;
    final byte lowTree = TK_BROAD;

    /** Highways, as {name, what's on the signs}. */
    String[][] highways;
    /**
     * Who polices the highway and the countryside: the force's name, what an officer is called on the radio,
     * and their car's body and door colours.
     */
    String hpName, hpShort, ruralName, ruralShort;
    int hpBody, hpDoor, hpShirt, ruralBody, ruralDoor, ruralShirt;

    private void agencies() {
        highways = new String[][]{{"Interstate 80", "I-80"}, {"Interstate 40", "I-40"}, {"Interstate 95", "I-95"}, {"Interstate 70", "I-70"}};
        hpName = "Highway Patrol";
        hpShort = "Trooper";
        hpBody = 0xFFC8B48A;
        hpDoor = 0xFF3A2E22;
        hpShirt = 0xFFC8B48A;
        ruralName = "Sheriff";
        ruralShort = "Deputy";
        ruralBody = 0xFFF2F2F2;
        ruralDoor = 0xFF2E5A3A;
        ruralShirt = 0xFF6A5A3A;
    }
}
