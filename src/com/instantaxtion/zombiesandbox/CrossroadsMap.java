package com.instantaxtion.zombiesandbox;

final class CrossroadsMap extends RegionMap {
    CrossroadsMap(City city) {
        super(city);
    }

    @Override
    int ground(int x, int y) {
        return 0xFF4F6F36;
    }
}
