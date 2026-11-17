package com.instantaxtion.zombiesandbox;

/** ((28.0) Work in progress. */
final class BridgeCityMap extends WaterMap {
    BridgeCityMap(City city) {
        super(city);
    }

    @Override
    void streets() {
        townPatch(4, 4, city.w - 4, city.h - 4);
    }
}
