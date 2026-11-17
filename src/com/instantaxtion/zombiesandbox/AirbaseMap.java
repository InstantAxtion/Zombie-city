package com.instantaxtion.zombiesandbox;

/** ((28.0) Work in progress: lays the map out as a plain grid for now. */
final class AirbaseMap extends CityPart {
    AirbaseMap(City city) {
        super(city);
    }

    void layout(int bx0, int by0, int bx1, int by1, int in) {
        gen.splitTop(bx0 + in, by0 + in, bx1 - in, by1 - in);
    }

    void places(boolean[] used) {
    }
}
