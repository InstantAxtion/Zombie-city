package com.instantaxtion.zombiesandbox;

/**
 * (27.3) One of the parts City.java was split into: it works on the city it belongs to, and can reach the other
 * parts (the generator, the painter, the builders of each special map).
 */
abstract class CityPart {
    final City city;
    CityGenerator gen;
    CityPainter art;
    CityRoads roadGen;
    CityBlocks lotGen;
    CityOutskirts landGen;
    MilitaryBases baseGen;
    IslandsMap islandsMap;
    ThemeParkMap parkMap;
    SpaceCenterMap spaceMap;
    DesertMap desertMap;
    PrisonMap prisonMap;
    RiverMap riverMap;
    CoastMap coastMap;
    AirportMap airportMap;

    CityPart(City city) {
        this.city = city;
    }

    /** Once every part exists: each learns of the others. */
    final void wire() {
        gen = city.gen;
        art = city.art;
        roadGen = city.roadGen;
        lotGen = city.lotGen;
        landGen = city.landGen;
        baseGen = city.baseGen;
        islandsMap = city.islandsMap;
        parkMap = city.parkMap;
        spaceMap = city.spaceMap;
        desertMap = city.desertMap;
        prisonMap = city.prisonMap;
        riverMap = city.riverMap;
        coastMap = city.coastMap;
        airportMap = city.airportMap;
    }
}
